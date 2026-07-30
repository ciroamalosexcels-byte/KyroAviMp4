package com.kyro.avi2mp4

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.io.File
import java.security.MessageDigest
import kotlin.math.roundToInt

internal sealed interface EditorPreviewResult {
    data class Success(val file: File) : EditorPreviewResult
    data class Failure(val message: String) : EditorPreviewResult
}

internal class EditorPreviewRenderer(private val context: Context) : Closeable {
    private val directory = File(context.cacheDir, "editor-live-preview-${System.nanoTime()}")
    private val sourceDirectory = File(directory, "sources")
    private val probes = mutableMapOf<String, EditorSourceProbe>()
    private val sourceStamps = mutableMapOf<String, String>()
    private var lastProject: EditorProject? = null
    private var lastOutput: File? = null

    init {
        val staleBefore = System.currentTimeMillis() - STALE_PREVIEW_AGE_MS
        context.cacheDir.listFiles()
            ?.filter { it.isDirectory && it.name.startsWith("editor-live-preview-") && it.lastModified() < staleBefore }
            ?.forEach(File::deleteRecursively)
    }

    suspend fun render(
        project: EditorProject,
        onProgress: (EditorExportProgress) -> Unit
    ): EditorPreviewResult = withContext(Dispatchers.IO) {
        if (project.clips.isEmpty() || project.durationMs <= 0L) {
            return@withContext EditorPreviewResult.Failure("Agregá clips para generar la preview")
        }
        var output: File? = null
        try {
            check(sourceDirectory.exists() || sourceDirectory.mkdirs()) {
                "No se pudo preparar la preview"
            }
            onProgress(EditorExportProgress("Preparando preview final...", null))
            val stagedSources = linkedMapOf<String, File>()
            var allSourcesReusable = true
            (project.clips.map(EditorClip::uri) + listOfNotNull(project.music?.uri)).distinct()
                .forEachIndexed { index, sourceUri ->
                    currentCoroutineContext().ensureActive()
                    val file = File(sourceDirectory, sourceUri.previewCacheName())
                    val stamp = context.previewSourceStamp(Uri.parse(sourceUri))
                    val reusable = stamp != null && sourceStamps[sourceUri] == stamp && file.isFile && file.length() > 0L
                    if (!reusable) {
                        allSourcesReusable = false
                        val pending = File(sourceDirectory, "pending_$index")
                        pending.delete()
                        context.copyUriToFile(Uri.parse(sourceUri), pending)
                        file.delete()
                        check(pending.renameTo(file)) { "No se pudo guardar un archivo de preview" }
                        probes.remove(sourceUri)
                    }
                    if (stamp != null) sourceStamps[sourceUri] = stamp else sourceStamps.remove(sourceUri)
                    stagedSources[sourceUri] = file
                }

            if (allSourcesReusable && lastProject == project && lastOutput?.isFile == true) {
                return@withContext EditorPreviewResult.Success(requireNotNull(lastOutput))
            }

            onProgress(EditorExportProgress("Analizando montaje...", null))
            val sourceProbes = stagedSources.mapValues { (uri, file) ->
                probes.getOrPut(uri) { probeEditorSource(file) }
            }
            val firstProbe = sourceProbes[project.clips.first().uri]
                ?: error("No se pudo analizar el primer clip")
            val sourceWidth = firstProbe.width ?: error("El primer archivo no contiene video")
            val sourceHeight = firstProbe.height ?: error("El primer archivo no contiene video")
            val previewWidth = project.outputWidth.coerceIn(320, 640).let { if (it % 2 == 0) it else it - 1 }
            val previewHeight = ((previewWidth.toDouble() * sourceHeight / sourceWidth) / 2.0)
                .roundToInt().times(2).coerceAtLeast(2)
            val segments = project.clips.map { clip ->
                val source = stagedSources[clip.uri] ?: error("No se pudo preparar ${clip.name}")
                val probe = sourceProbes[clip.uri] ?: error("No se pudo analizar ${clip.name}")
                require(probe.width != null && probe.height != null) {
                    "${clip.name} no contiene una pista de video"
                }
                EditorExportSegment(clip, source.absolutePath, probe.hasAudio)
            }
            val musicPath = project.music?.let { music ->
                val source = stagedSources[music.uri] ?: error("No se pudo preparar ${music.name}")
                require(sourceProbes[music.uri]?.hasAudio == true) {
                    "${music.name} no contiene audio reproducible"
                }
                source.absolutePath
            }
            val plan = EditorExportPlan(project, segments, musicPath, previewWidth, previewHeight)
            val rendered = File(directory, "preview_${System.nanoTime()}.mp4")
            output = rendered
            onProgress(EditorExportProgress("Aplicando clips, color y música...", 0f))
            val execution = runEditorFfmpeg(
                buildEditorExportArguments(
                    plan = plan,
                    output = rendered,
                    videoQuality = 7,
                    audioBitrate = "96k"
                ),
                project.durationMs
            ) { fraction ->
                onProgress(EditorExportProgress("Aplicando clips, color y música...", fraction))
            }
            if (!execution.succeeded || !rendered.isFile || rendered.length() == 0L) {
                rendered.delete()
                return@withContext EditorPreviewResult.Failure(
                    "No se pudo actualizar la preview: ${execution.failureDetail()}"
                )
            }
            directory.listFiles()
                ?.filter { it.isFile && it.name.startsWith("preview_") && it != rendered }
                ?.sortedByDescending(File::lastModified)
                ?.drop(1)
                ?.forEach(File::delete)
            lastProject = project
            lastOutput = rendered
            EditorPreviewResult.Success(rendered)
        } catch (cancelled: CancellationException) {
            output?.delete()
            throw cancelled
        } catch (error: Exception) {
            output?.delete()
            EditorPreviewResult.Failure(error.message ?: "No se pudo generar la preview")
        }
    }

    override fun close() {
        directory.deleteRecursively()
    }
}

private fun String.previewCacheName(): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(toByteArray())
    return digest.joinToString(separator = "") { byte -> "%02x".format(byte) }
}

private fun Context.previewSourceStamp(uri: Uri): String? = runCatching {
    contentResolver.query(
        uri,
        arrayOf(OpenableColumns.SIZE, DocumentsContract.Document.COLUMN_LAST_MODIFIED),
        null,
        null,
        null
    )?.use { cursor ->
        if (!cursor.moveToFirst()) return@use null
        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
        val modifiedIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
        val size = if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) cursor.getLong(sizeIndex) else -1L
        val modified = if (modifiedIndex >= 0 && !cursor.isNull(modifiedIndex)) cursor.getLong(modifiedIndex) else -1L
        if (size < 0L && modified < 0L) null else "$size:$modified"
    }
}.getOrNull()

private const val STALE_PREVIEW_AGE_MS = 6L * 60L * 60L * 1000L
