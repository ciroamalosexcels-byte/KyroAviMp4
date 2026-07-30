package com.kyro.avi2mp4

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFprobeKit
import com.arthenica.ffmpegkit.ReturnCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import org.json.JSONObject
import java.io.File
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.math.abs
import kotlin.math.roundToInt

internal data class EditorExportProgress(val message: String, val fraction: Float?)

internal sealed interface EditorExportResult {
    data object Success : EditorExportResult
    data class Failure(val message: String) : EditorExportResult
}

internal data class EditorExportSegment(
    val clip: EditorClip,
    val inputPath: String,
    val hasAudio: Boolean
)

internal data class EditorExportPlan(
    val project: EditorProject,
    val segments: List<EditorExportSegment>,
    val musicPath: String?,
    val canvasWidth: Int,
    val canvasHeight: Int
)

internal class EditorExporter(private val context: Context) {
    suspend fun export(
        project: EditorProject,
        destination: Uri,
        onProgress: (EditorExportProgress) -> Unit
    ): EditorExportResult = withContext(Dispatchers.IO) {
        if (project.clips.isEmpty() || project.durationMs <= 0L) {
            return@withContext EditorExportResult.Failure("El proyecto no tiene clips para exportar")
        }
        removeStaleEditorExports(context.cacheDir)
        val workDirectory = File(context.cacheDir, "editor-export-${System.nanoTime()}")
        val output = File(workDirectory, "montaje.mp4")
        try {
            check(workDirectory.mkdirs()) { "No se pudo preparar el espacio temporal" }
            onProgress(EditorExportProgress("Preparando archivos...", null))
            val stagedSources = linkedMapOf<String, File>()
            (project.clips.map(EditorClip::uri) + listOfNotNull(project.music?.uri)).distinct()
                .forEachIndexed { index, sourceUri ->
                    currentCoroutineContext().ensureActive()
                    val file = File(workDirectory, "source_$index")
                    context.copyUriToFile(Uri.parse(sourceUri), file)
                    stagedSources[sourceUri] = file
                }

            onProgress(EditorExportProgress("Analizando videos...", null))
            val probes = stagedSources.mapValues { (_, file) -> probeEditorSource(file) }
            val firstProbe = probes[project.clips.first().uri]
                ?: error("No se pudo analizar el primer clip")
            val sourceWidth = firstProbe.width ?: error("El primer archivo no contiene video")
            val sourceHeight = firstProbe.height ?: error("El primer archivo no contiene video")
            val canvasWidth = project.outputWidth.coerceIn(320, 3840).let { if (it % 2 == 0) it else it - 1 }
            val canvasHeight = ((canvasWidth.toDouble() * sourceHeight / sourceWidth) / 2.0)
                .roundToInt().times(2).coerceAtLeast(2)

            val segments = project.clips.map { clip ->
                val source = stagedSources[clip.uri] ?: error("No se pudo preparar ${clip.name}")
                val probe = probes[clip.uri] ?: error("No se pudo analizar ${clip.name}")
                require(probe.width != null && probe.height != null) { "${clip.name} no contiene una pista de video" }
                require(clip.trimStartMs in 0 until clip.trimEndMs && clip.trimEndMs <= clip.durationMs) {
                    "El recorte de ${clip.name} no es válido"
                }
                EditorExportSegment(clip, source.absolutePath, probe.hasAudio)
            }
            val musicPath = project.music?.let { music ->
                val source = stagedSources[music.uri] ?: error("No se pudo preparar ${music.name}")
                require(probes[music.uri]?.hasAudio == true) { "${music.name} no contiene audio reproducible" }
                source.absolutePath
            }
            val plan = EditorExportPlan(project, segments, musicPath, canvasWidth, canvasHeight)

            onProgress(EditorExportProgress("Exportando montaje...", 0f))
            val execution = runEditorFfmpeg(buildEditorExportArguments(plan, output), project.durationMs) { fraction ->
                onProgress(EditorExportProgress("Exportando montaje...", fraction))
            }
            if (!execution.succeeded || !output.isFile || output.length() == 0L) {
                context.deleteDocumentBestEffort(destination)
                return@withContext EditorExportResult.Failure(
                    "FFmpeg no pudo exportar: ${execution.failureDetail()}"
                )
            }

            onProgress(EditorExportProgress("Guardando MP4...", 1f))
            context.copyFileToUri(output, destination)
            EditorExportResult.Success
        } catch (cancelled: CancellationException) {
            context.deleteDocumentBestEffort(destination)
            throw cancelled
        } catch (error: Exception) {
            context.deleteDocumentBestEffort(destination)
            EditorExportResult.Failure(error.message ?: "No se pudo exportar el montaje")
        } finally {
            workDirectory.deleteRecursively()
        }
    }
}

internal fun buildEditorExportArguments(
    plan: EditorExportPlan,
    output: File,
    videoQuality: Int = 4,
    audioBitrate: String = "128k"
): Array<String> = buildList {
    addAll(listOf("-y", "-nostdin", "-hide_banner", "-loglevel", "warning"))
    plan.segments.forEach { segment ->
        addAll(
            listOf(
                "-fflags", "+genpts+discardcorrupt", "-err_detect", "ignore_err",
                "-i", segment.inputPath
            )
        )
    }
    plan.musicPath?.let { path ->
        addAll(listOf("-fflags", "+genpts+discardcorrupt", "-err_detect", "ignore_err", "-i", path))
    }
    addAll(
        listOf(
            "-filter_complex", buildEditorFilterGraph(plan),
            "-map", "[editor_video]",
            "-map", if (plan.musicPath == null) "[editor_audio]" else "[mixed_audio]",
            "-c:v", "mpeg4", "-q:v", videoQuality.coerceIn(2, 12).toString(), "-pix_fmt", "yuv420p", "-r", "30",
            "-c:a", "aac", "-b:a", audioBitrate, "-ar", "44100", "-ac", "2",
            "-t", seconds(plan.project.durationMs),
            "-avoid_negative_ts", "make_zero", "-max_muxing_queue_size", "1024",
            "-movflags", "+faststart", output.absolutePath
        )
    )
}.toTypedArray()

internal fun buildEditorFilterGraph(plan: EditorExportPlan): String {
    require(plan.segments.isNotEmpty())
    require(plan.canvasWidth > 0 && plan.canvasWidth % 2 == 0)
    require(plan.canvasHeight > 0 && plan.canvasHeight % 2 == 0)
    val filters = mutableListOf<String>()
    plan.segments.forEachIndexed { index, segment ->
        val clip = segment.clip
        val duration = seconds(clip.trimmedDurationMs)
        val videoFilters = mutableListOf(
            "[$index:v:0]trim=start=${seconds(clip.trimStartMs)}:end=${seconds(clip.trimEndMs)}",
            "setpts=PTS-STARTPTS",
            "fps=30",
            "scale=${plan.canvasWidth}:${plan.canvasHeight}:force_original_aspect_ratio=decrease",
            "pad=${plan.canvasWidth}:${plan.canvasHeight}:(ow-iw)/2:(oh-ih)/2:color=black",
            "setsar=1"
        )
        videoFilters += editorColorFilters(clip.color)
        videoFilters += listOf("format=yuv420p", "settb=AVTB[v$index]")
        filters += videoFilters.joinToString(",")

        filters += if (segment.hasAudio) {
            listOf(
                "[$index:a:0]atrim=start=${seconds(clip.trimStartMs)}:end=${seconds(clip.trimEndMs)}",
                "asetpts=PTS-STARTPTS",
                "aresample=44100:async=1:first_pts=0",
                "aformat=sample_fmts=fltp:sample_rates=44100:channel_layouts=stereo",
                "apad=whole_dur=$duration",
                "atrim=duration=$duration",
                "asetpts=N/SR/TB[a$index]"
            ).joinToString(",")
        } else {
            listOf(
                "anullsrc=channel_layout=stereo:sample_rate=44100",
                "atrim=duration=$duration",
                "asetpts=N/SR/TB[a$index]"
            ).joinToString(",")
        }
    }

    val concatInputs = plan.segments.indices.joinToString("") { "[v$it][a$it]" }
    filters += "${concatInputs}concat=n=${plan.segments.size}:v=1:a=1[editor_video][editor_audio]"

    val music = plan.project.music
    if (music != null && plan.musicPath != null) {
        val musicIndex = plan.segments.size
        val audibleDuration = music.trimmedDurationMs
        val musicFilters = mutableListOf(
            "[$musicIndex:a:0]atrim=start=${seconds(music.trimStartMs)}:end=${seconds(music.trimEndMs)}",
            "asetpts=PTS-STARTPTS",
            "aresample=44100:async=1:first_pts=0",
            "aformat=sample_fmts=fltp:sample_rates=44100:channel_layouts=stereo",
            "volume=${decimal(music.volume.coerceIn(0f, 1f))}"
        )
        if (music.fadeInMs > 0L) {
            musicFilters += "afade=t=in:st=0:d=${seconds(music.fadeInMs.coerceAtMost(audibleDuration))}"
        }
        if (music.fadeOutMs > 0L) {
            val fadeDuration = music.fadeOutMs.coerceAtMost(audibleDuration)
            musicFilters += "afade=t=out:st=${seconds((audibleDuration - fadeDuration).coerceAtLeast(0L))}:d=${seconds(fadeDuration)}"
        }
        if (music.timelineStartMs > 0L) {
            musicFilters += "adelay=delays=${music.timelineStartMs}:all=1"
        }
        musicFilters += listOf(
            "apad=whole_dur=${seconds(plan.project.durationMs)}",
            "atrim=duration=${seconds(plan.project.durationMs)}",
            "asetpts=N/SR/TB[music]"
        )
        filters += musicFilters.joinToString(",")
        filters += "[editor_audio][music]amix=inputs=2:duration=first:dropout_transition=0:normalize=0[mixed_audio]"
    }
    return filters.joinToString(";")
}

internal fun editorColorFilters(color: EditorColorSettings): List<String> = buildList {
    val saturation = if (color.grayscale) 0f else color.saturation.coerceIn(0f, 2f)
    if (color.grayscale || abs(saturation - 1f) > 0.005f) add("hue=s=${decimal(saturation)}")
    if (abs(color.exposure) > 0.005f) {
        add("exposure=exposure=${decimal(color.exposure.coerceIn(-2f, 2f))}")
    }
    if (abs(color.contrast) > 0.005f || abs(color.shadows) > 0.005f || abs(color.highlights) > 0.005f) {
        val low = (0.25f - color.contrast.coerceIn(-1f, 1f) * 0.15f +
            color.shadows.coerceIn(-1f, 1f) * 0.15f).coerceIn(0.02f, 0.48f)
        val high = (0.75f + color.contrast.coerceIn(-1f, 1f) * 0.15f +
            color.highlights.coerceIn(-1f, 1f) * 0.15f).coerceIn(0.52f, 0.98f)
        add("curves=master='0/0 0.25/${decimal(low)} 0.75/${decimal(high)} 1/1':interp=pchip")
    }
    val shadows = tintBalance(color.shadowHue, color.shadowTint)
    val highlights = tintBalance(color.highlightHue, color.highlightTint)
    if (shadows.any { abs(it) > 0.0005f } || highlights.any { abs(it) > 0.0005f }) {
        add(
            "colorbalance=rs=${decimal(shadows[0])}:gs=${decimal(shadows[1])}:bs=${decimal(shadows[2])}:" +
                "rh=${decimal(highlights[0])}:gh=${decimal(highlights[1])}:bh=${decimal(highlights[2])}:pl=1"
        )
    }
}

internal data class EditorSourceProbe(val width: Int?, val height: Int?, val hasAudio: Boolean)

internal fun probeEditorSource(file: File): EditorSourceProbe {
    val session = FFprobeKit.executeWithArguments(
        arrayOf(
            "-v", "error", "-show_entries",
            "stream=codec_type,width,height:stream_tags=rotate:stream_side_data=rotation",
            "-of", "json", file.absolutePath
        )
    )
    require(ReturnCode.isSuccess(session.returnCode)) { "No se pudo analizar ${file.name}" }
    val streams = JSONObject(session.output.orEmpty()).optJSONArray("streams")
        ?: error("El archivo no contiene pistas multimedia")
    var width: Int? = null
    var height: Int? = null
    var hasAudio = false
    repeat(streams.length()) { index ->
        val stream = streams.getJSONObject(index)
        when (stream.optString("codec_type")) {
            "audio" -> hasAudio = true
            "video" -> if (width == null) {
                var videoWidth = stream.optInt("width", 0)
                var videoHeight = stream.optInt("height", 0)
                var rotation = stream.optJSONObject("tags")?.optInt("rotate", 0) ?: 0
                stream.optJSONArray("side_data_list")?.let { sideData ->
                    repeat(sideData.length()) { sideIndex ->
                        val item = sideData.optJSONObject(sideIndex)
                        if (item?.has("rotation") == true) rotation = item.optInt("rotation", rotation)
                    }
                }
                if (abs(rotation) % 180 == 90) {
                    val swap = videoWidth
                    videoWidth = videoHeight
                    videoHeight = swap
                }
                if (videoWidth > 0 && videoHeight > 0) {
                    width = videoWidth
                    height = videoHeight
                }
            }
        }
    }
    return EditorSourceProbe(width, height, hasAudio)
}

internal data class EditorFfmpegExecution(
    val succeeded: Boolean,
    val output: String
) {
    fun failureDetail(): String = output.lineSequence()
        .map(String::trim)
        .filter(String::isNotEmpty)
        .toList()
        .takeLast(4)
        .joinToString(" | ")
        .take(500)
        .ifEmpty { "FFmpeg terminó sin detalles" }
}

internal suspend fun runEditorFfmpeg(
    arguments: Array<String>,
    totalDurationMs: Long,
    onProgress: (Float) -> Unit
): EditorFfmpegExecution = suspendCancellableCoroutine { continuation ->
    var sessionId = 0L
    val session = FFmpegKit.executeWithArgumentsAsync(
        arguments,
        { completed ->
            if (continuation.isActive) {
                continuation.resume(
                    EditorFfmpegExecution(
                        succeeded = ReturnCode.isSuccess(completed.returnCode),
                        output = completed.output.orEmpty()
                    )
                )
            }
        },
        { _ -> },
        { statistics ->
            if (continuation.isActive && totalDurationMs > 0L) {
                onProgress((statistics.time / totalDurationMs.toDouble()).toFloat().coerceIn(0f, 1f))
            }
        }
    )
    sessionId = session.sessionId
    continuation.invokeOnCancellation { if (sessionId != 0L) FFmpegKit.cancel(sessionId) }
    if (!continuation.isActive) FFmpegKit.cancel(sessionId)
}

internal suspend fun Context.copyUriToFile(uri: Uri, destination: File) {
    contentResolver.openInputStream(uri)?.use { source ->
        destination.outputStream().use { target ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                currentCoroutineContext().ensureActive()
                val count = source.read(buffer)
                if (count < 0) break
                target.write(buffer, 0, count)
            }
        }
    } ?: error("No se pudo leer uno de los archivos del proyecto")
}

private suspend fun Context.copyFileToUri(source: File, destination: Uri) {
    contentResolver.openOutputStream(destination, "wt")?.use { target ->
        source.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                currentCoroutineContext().ensureActive()
                val count = input.read(buffer)
                if (count < 0) break
                target.write(buffer, 0, count)
            }
        }
    } ?: error("No se pudo guardar el MP4")
}

private fun Context.deleteDocumentBestEffort(uri: Uri) {
    runCatching {
        if (DocumentsContract.isDocumentUri(this, uri)) {
            DocumentsContract.deleteDocument(contentResolver, uri)
        }
    }
}

private fun removeStaleEditorExports(cacheDir: File) {
    cacheDir.listFiles()
        ?.filter { it.isDirectory && it.name.startsWith("editor-export-") }
        ?.forEach(File::deleteRecursively)
}

private fun tintBalance(hue: Float, amount: Float): FloatArray {
    val strength = amount.coerceIn(0f, 1f) * 0.35f
    if (strength <= 0f) return floatArrayOf(0f, 0f, 0f)
    val normalizedHue = ((hue % 360f) + 360f) % 360f / 60f
    val sector = normalizedHue.toInt() % 6
    val fraction = normalizedHue - normalizedHue.toInt()
    val (red, green, blue) = when (sector) {
        0 -> Triple(1f, fraction, 0f)
        1 -> Triple(1f - fraction, 1f, 0f)
        2 -> Triple(0f, 1f, fraction)
        3 -> Triple(0f, 1f - fraction, 1f)
        4 -> Triple(fraction, 0f, 1f)
        else -> Triple(1f, 0f, 1f - fraction)
    }
    val average = (red + green + blue) / 3f
    return floatArrayOf(
        (red - average) * strength,
        (green - average) * strength,
        (blue - average) * strength
    )
}

private fun seconds(milliseconds: Long): String = String.format(Locale.US, "%.3f", milliseconds / 1000.0)

private fun decimal(value: Float): String = String.format(Locale.US, "%.3f", value)
