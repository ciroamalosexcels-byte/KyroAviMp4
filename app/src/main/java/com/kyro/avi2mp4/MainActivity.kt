package com.kyro.avi2mp4

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import com.github.pao11.libffmpeg.FFmpegRunner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume

private data class VideoJob(
    val uri: Uri,
    val name: String,
    val status: String = "En espera"
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { AviConverterApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AviConverterApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val jobs = remember { mutableStateListOf<VideoJob>() }
    var outputFolder by remember { mutableStateOf<Uri?>(null) }
    var outputFolderName by remember { mutableStateOf("Sin carpeta seleccionada") }
    var targetWidth by remember { mutableStateOf("1280") }
    var running by remember { mutableStateOf(false) }
    var completed by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    val pickVideos = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        uris.forEach { uri ->
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // Some providers grant read access for this app session only.
            }
            if (jobs.none { it.uri == uri }) jobs += VideoJob(uri, context.displayName(uri))
        }
    }
    val pickFolder = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // The temporary grant is still sufficient until the app is closed.
            }
            outputFolder = uri
            outputFolderName = DocumentFile.fromTreeUri(context, uri)?.name ?: "Carpeta elegida"
        }
    }

    MaterialTheme {
        Column(Modifier.fillMaxSize()) {
            TopAppBar(title = { Text("AVI a MP4", fontWeight = FontWeight.Bold) })
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Convierte varios videos y modifica únicamente su ancho. El alto se conserva, por lo que la imagen se estira o se estrecha sin recortarse.")
                OutlinedTextField(
                    value = targetWidth,
                    onValueChange = { targetWidth = it.filter(Char::isDigit) },
                    label = { Text("Ancho de salida (px)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !running
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { pickVideos.launch(arrayOf("video/*")) }, enabled = !running) {
                        Text("Elegir videos")
                    }
                    Button(onClick = { pickFolder.launch(null) }, enabled = !running) {
                        Text("Carpeta de salida")
                    }
                }
                Text("Salida: $outputFolderName", style = MaterialTheme.typography.bodySmall)
                Button(
                    onClick = {
                        val width = targetWidth.toIntOrNull()
                        val folder = outputFolder
                        if (width == null || width < 2 || width % 2 != 0 || folder == null || jobs.isEmpty()) return@Button
                        running = true
                        completed = 0
                        scope.launch(Dispatchers.IO) {
                            jobs.indices.forEach { index ->
                                updateJob(jobs, index, "Preparando archivo...")
                                val result = convertVideo(context, jobs[index], folder, width) {
                                    updateJob(jobs, index, it)
                                }
                                updateJob(jobs, index, result)
                                withContext(Dispatchers.Main) { completed++ }
                            }
                            withContext(Dispatchers.Main) { running = false }
                        }
                    },
                    enabled = !running && jobs.isNotEmpty() && outputFolder != null &&
                        targetWidth.toIntOrNull()?.let { it >= 2 && it % 2 == 0 } == true,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (running) "Convirtiendo $completed de ${jobs.size}" else "Convertir ${jobs.size} video(s)")
                }
                if (targetWidth.isNotEmpty() && targetWidth.toIntOrNull()?.rem(2) != 0) {
                    Text("Para MP4 compatible, el ancho debe ser un número par.", color = MaterialTheme.colorScheme.error)
                }
                HorizontalDivider()
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(jobs, key = { it.uri }) { job ->
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Column(Modifier.padding(12.dp)) {
                                Text(job.name, fontWeight = FontWeight.Medium)
                                Spacer(Modifier.height(3.dp))
                                Text(job.status, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    item { Spacer(Modifier.width(1.dp).height(12.dp)) }
                }
            }
        }
    }
}

private suspend fun updateJob(jobs: MutableList<VideoJob>, index: Int, status: String) {
    withContext(Dispatchers.Main) { jobs[index] = jobs[index].copy(status = status) }
}

private suspend fun convertVideo(
    context: Context,
    job: VideoJob,
    folderUri: Uri,
    width: Int,
    onStatus: suspend (String) -> Unit
): String {
    val input = File(context.cacheDir, "source_${System.nanoTime()}.avi")
    val output = File(context.cacheDir, "output_${System.nanoTime()}.mp4")
    return try {
        context.contentResolver.openInputStream(job.uri)?.use { source ->
            input.outputStream().use { target -> source.copyTo(target) }
        }
            ?: return "Error: no se pudo leer el archivo"
        onStatus("Convirtiendo a MP4...")
        val error = runFfmpeg(
            context,
            arrayOf(
                "-y", "-i", input.absolutePath,
                "-map", "0:v:0", "-map", "0:a?",
                "-vf", "scale=$width:ih",
                "-c:v", "libx264", "-preset", "medium", "-crf", "23",
                "-c:a", "aac", "-movflags", "+faststart", output.absolutePath
            )
        )
        if (error != null) {
            return "Error al convertir: ${error.lineSequence().firstOrNull() ?: "FFmpeg no pudo procesar el video"}"
        }
        onStatus("Guardando resultado...")
        val folder = DocumentFile.fromTreeUri(context, folderUri) ?: return "Error: carpeta de salida no disponible"
        val filename = job.name.substringBeforeLast('.', job.name) + "_mp4.mp4"
        val destination = folder.createFile("video/mp4", uniqueName(folder, filename))
            ?: return "Error: no se pudo crear el archivo de salida"
        context.contentResolver.openOutputStream(destination.uri)?.use { target ->
            output.inputStream().use { source -> source.copyTo(target) }
        }
            ?: return "Error: no se pudo guardar el archivo"
        "Completado: ${destination.name}"
    } catch (error: Exception) {
        "Error: ${error.message ?: "problema desconocido"}"
    } finally {
        input.delete()
        output.delete()
    }
}

private fun uniqueName(folder: DocumentFile, initial: String): String {
    val stem = initial.substringBeforeLast('.', initial)
    val extension = initial.substringAfterLast('.', "")
    var name = initial
    var copy = 2
    while (folder.findFile(name) != null) {
        name = "$stem ($copy).$extension"
        copy++
    }
    return name
}

private suspend fun runFfmpeg(context: Context, arguments: Array<String>): String? =
    suspendCancellableCoroutine { continuation ->
        FFmpegRunner.execute(context, arguments, object : FFmpegRunner.Callback {
                override fun onSuccess() {
                    if (continuation.isActive) continuation.resume(null)
                }

                override fun onFailure(message: String) {
                    if (continuation.isActive) continuation.resume(message)
                }
            })
    }

private fun Context.displayName(uri: Uri): String {
    contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) return cursor.getString(0)
    }
    return uri.lastPathSegment ?: "Video sin nombre"
}
