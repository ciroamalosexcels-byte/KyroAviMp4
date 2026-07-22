package com.kyro.avi2mp4

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

private val Primary = Color(0xFF6047E8)
private val PrimaryDark = Color(0xFF4D35CE)
private val PrimarySoft = Color(0xFFF0EDFF)
private val Ink = Color(0xFF1B1B22)
private val Muted = Color(0xFF6F7280)
private val Line = Color(0xFFE2E3E9)
private val SurfaceAlt = Color(0xFFF7F7FA)
private val Success = Color(0xFF13895A)
private val Danger = Color(0xFFC83F49)

private val ConverterColors = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = PrimarySoft,
    onPrimaryContainer = PrimaryDark,
    background = Color(0xFFF8F8FC),
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    outline = Line,
    error = Danger
)

private data class VideoJob(
    val uri: Uri,
    val name: String,
    val sizeBytes: Long? = null,
    val status: String = "En espera"
)

private data class VideoPreview(val bitmap: Bitmap, val width: Int, val height: Int)

private data class FfmpegResult(
    val returnCode: Int,
    val output: String,
    val succeeded: Boolean
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { AviConverterApp() }
    }
}

@Composable
private fun AviConverterApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val preferences = remember { context.getSharedPreferences("converter", Context.MODE_PRIVATE) }
    val jobs = remember { mutableStateListOf<VideoJob>() }
    var outputFolder by remember { mutableStateOf<Uri?>(null) }
    var outputFolderName by remember { mutableStateOf("Sin carpeta seleccionada") }
    var targetWidth by remember { mutableStateOf(preferences.getString("target_width", "1280") ?: "1280") }
    var preview by remember { mutableStateOf<VideoPreview?>(null) }
    var previewLoading by remember { mutableStateOf(false) }
    var activeUri by remember { mutableStateOf<Uri?>(null) }
    var running by remember { mutableStateOf(false) }
    var completed by remember { mutableIntStateOf(0) }
    var summary by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun loadPreview(uri: Uri) {
        activeUri = uri
        preview = null
        previewLoading = true
        scope.launch {
            preview = context.videoPreview(uri)
            previewLoading = false
        }
    }

    fun updateWidth(value: String) {
        targetWidth = value.filter(Char::isDigit)
        preferences.edit().putString("target_width", targetWidth).apply()
    }

    fun removeJob(job: VideoJob) {
        val wasActive = activeUri == job.uri
        jobs.remove(job)
        summary = null
        if (wasActive) {
            activeUri = null
            preview = null
            jobs.firstOrNull()?.let { loadPreview(it.uri) }
        }
    }

    fun clearJobs() {
        jobs.clear()
        activeUri = null
        preview = null
        completed = 0
        summary = null
    }

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
            if (jobs.none { it.uri == uri }) {
                jobs += VideoJob(uri, context.displayName(uri), context.fileSize(uri))
            }
            if (activeUri == null && !previewLoading) loadPreview(uri)
        }
        summary = null
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

    fun startConversion() {
        val width = targetWidth.toIntOrNull()
        val folder = outputFolder
        if (width == null || width < 2 || width % 2 != 0 || folder == null || jobs.isEmpty()) return
        running = true
        completed = 0
        summary = null
        scope.launch(Dispatchers.IO) {
            var successful = 0
            jobs.indices.forEach { index ->
                updateJob(jobs, index, "Preparando archivo...")
                val result = convertVideo(context, jobs[index], folder, width) {
                    updateJob(jobs, index, it)
                }
                if (result.startsWith("Completado:")) successful++
                updateJob(jobs, index, result)
                withContext(Dispatchers.Main) { completed++ }
            }
            withContext(Dispatchers.Main) {
                running = false
                summary = if (successful == jobs.size) {
                    "$successful archivo${if (successful == 1) "" else "s"} convertido${if (successful == 1) "" else "s"} correctamente."
                } else {
                    "$successful de ${jobs.size} archivos convertidos. Revisá los estados de la cola."
                }
            }
        }
    }

    val widthValue = targetWidth.toIntOrNull()
    val validWidth = widthValue?.let { it >= 2 && it % 2 == 0 } == true
    val allSuccessful = jobs.isNotEmpty() && jobs.all { it.status.startsWith("Completado:") }

    MaterialTheme(colorScheme = ConverterColors) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.White, Color(0xFFF7F7FB))
                        )
                    )
            ) {
                ConverterTopBar()
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 30.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    item {
                        ConverterHero()
                    }
                    item {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .widthIn(max = 760.dp),
                                shape = RoundedCornerShape(22.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Line),
                                shadowElevation = 12.dp
                            ) {
                                Column {
                                    UploadArea(
                                        enabled = !running && !previewLoading,
                                        compact = jobs.isNotEmpty(),
                                        onSelect = { pickVideos.launch(arrayOf("video/*")) }
                                    )

                                    if (jobs.isNotEmpty()) {
                                        QueueSection(
                                            jobs = jobs,
                                            activeUri = activeUri,
                                            enabled = !running && !previewLoading,
                                            onSelect = { loadPreview(it.uri) },
                                            onRemove = ::removeJob
                                        )
                                        HorizontalDivider(color = Line)
                                        PreviewSection(
                                            preview = preview,
                                            loading = previewLoading,
                                            targetWidth = widthValue
                                        )
                                        HorizontalDivider(color = Line)
                                        SettingsSection(
                                            targetWidth = targetWidth,
                                            validWidth = validWidth,
                                            outputFolderName = outputFolderName,
                                            enabled = !running,
                                            onWidthChange = ::updateWidth,
                                            onFolderClick = { pickFolder.launch(null) }
                                        )
                                    }

                                    ActionsSection(
                                        jobCount = jobs.size,
                                        completed = completed,
                                        running = running,
                                        previewLoading = previewLoading,
                                        canConvert = jobs.isNotEmpty() && outputFolder != null && validWidth,
                                        summary = summary,
                                        summarySuccess = allSuccessful,
                                        onConvert = ::startConversion,
                                        onClear = ::clearJobs
                                    )
                                }
                            }
                        }
                    }
                    item {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .widthIn(max = 760.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                InfoCard("FFmpeg nativo", "El motor funciona sin internet y contiene binarios arm64 para Android.")
                                InfoCard("Vista previa real", "El fotograma se extrae con FFmpeg aunque Android no reproduzca el AVI.")
                                InfoCard("Conversión por cola", "Los videos se procesan uno por uno para cuidar la memoria del teléfono.")
                            }
                        }
                    }
                    item {
                        Spacer(Modifier.navigationBarsPadding().height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ConverterTopBar() {
    Surface(color = Color.White.copy(alpha = 0.96f), shadowElevation = 2.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(68.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(38.dp),
                shape = RoundedCornerShape(12.dp),
                color = Primary,
                shadowElevation = 8.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("⇄", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(10.dp))
            Text("LocalConvert", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.weight(1f))
            Surface(shape = RoundedCornerShape(50), color = Color(0xFFF7F5FF), border = BorderStroke(1.dp, Color(0xFFDCD7FF))) {
                Text(
                    "FFmpeg · v${BuildConfig.VERSION_NAME}",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    color = Color(0xFF5741CA),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ConverterHero() {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 760.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Convertidor múltiple\nde AVI a MP4",
                color = Ink,
                fontSize = 34.sp,
                lineHeight = 37.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(14.dp))
            Text(
                "Agregá varios videos, revisá la vista previa, ajustá el ancho y convertí toda la cola.",
                modifier = Modifier.widthIn(max = 620.dp),
                color = Muted,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun UploadArea(enabled: Boolean, compact: Boolean, onSelect: () -> Unit) {
    Box(modifier = Modifier.padding(14.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(listOf(Color(0xFFFBFAFF), Color(0xFFFBFBFD))),
                    shape = RoundedCornerShape(17.dp)
                )
                .dashedRoundedBorder(if (enabled) Color(0xFFCDCFDA) else Line)
                .clickable(enabled = enabled, onClick = onSelect)
                .padding(horizontal = 20.dp, vertical = if (compact) 20.dp else 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(modifier = Modifier.size(58.dp), shape = RoundedCornerShape(18.dp), color = PrimarySoft) {
                Box(contentAlignment = Alignment.Center) {
                    Text("AVI", color = Primary, fontSize = 13.sp, fontWeight = FontWeight.Black)
                }
            }
            Spacer(Modifier.height(13.dp))
            Text(
                if (compact) "Agregar más archivos AVI" else "Seleccioná uno o varios archivos AVI",
                color = Ink,
                fontSize = if (compact) 17.sp else 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(5.dp))
            Text(
                "Se convierten uno por uno para cuidar la memoria del celular.",
                color = Muted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onSelect,
                enabled = enabled,
                colors = ButtonDefaults.buttonColors(containerColor = Primary, disabledContainerColor = Primary.copy(alpha = 0.45f)),
                shape = RoundedCornerShape(11.dp),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)
            ) {
                Text("Agregar archivos", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun QueueSection(
    jobs: List<VideoJob>,
    activeUri: Uri?,
    enabled: Boolean,
    onSelect: (VideoJob) -> Unit,
    onRemove: (VideoJob) -> Unit
) {
    Column(modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 18.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Archivos agregados", color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text(
                "${jobs.size} archivo${if (jobs.size == 1) "" else "s"} · ${formatBytes(jobs.mapNotNull { it.sizeBytes }.sum())}",
                color = Muted,
                fontSize = 11.sp
            )
        }
        Spacer(Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            jobs.forEach { job ->
                QueueItem(
                    job = job,
                    active = activeUri == job.uri,
                    enabled = enabled,
                    onSelect = { onSelect(job) },
                    onRemove = { onRemove(job) }
                )
            }
        }
    }
}

@Composable
private fun QueueItem(job: VideoJob, active: Boolean, enabled: Boolean, onSelect: () -> Unit, onRemove: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onSelect),
        shape = RoundedCornerShape(13.dp),
        color = Color.White,
        border = BorderStroke(if (active) 1.5.dp else 1.dp, if (active) Color(0xFFBFB4FF) else Line),
        shadowElevation = if (active) 3.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Surface(modifier = Modifier.size(45.dp), shape = RoundedCornerShape(12.dp), color = Color(0xFF343540)) {
                Box(contentAlignment = Alignment.Center) {
                    Text("AVI", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    job.name,
                    color = Ink,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                job.sizeBytes?.let {
                    Text(formatBytes(it), color = Muted, fontSize = 10.sp)
                }
                Spacer(Modifier.height(4.dp))
                StatusBadge(job.status)
                if (job.status.startsWith("Error")) {
                    Text(job.status, color = Danger, fontSize = 9.sp, lineHeight = 12.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
            }
            TextButton(
                onClick = onRemove,
                enabled = enabled,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(36.dp)
            ) {
                Text("×", color = if (enabled) Danger else Muted, fontSize = 24.sp, fontWeight = FontWeight.Normal)
            }
        }
    }
}

@Composable
private fun StatusBadge(status: String) {
    val processing = listOf("Preparando", "Convirtiendo", "Reintentando", "Guardando").any(status::startsWith)
    val done = status.startsWith("Completado:")
    val error = status.startsWith("Error")
    val label = when {
        done -> "MP4 listo"
        error -> "Error"
        processing -> status.substringBefore("...")
        else -> "Pendiente"
    }
    val foreground = when {
        done -> Color(0xFF116F49)
        error -> Color(0xFFA32932)
        processing -> Color(0xFF5B42CE)
        else -> Color(0xFF666976)
    }
    val background = when {
        done -> Color(0xFFE9F8F0)
        error -> Color(0xFFFFF0F1)
        processing -> Color(0xFFEEEBFF)
        else -> Color(0xFFF1F1F5)
    }
    Surface(shape = RoundedCornerShape(50), color = background) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            color = foreground,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun PreviewSection(preview: VideoPreview?, loading: Boolean, targetWidth: Int?) {
    Column(modifier = Modifier.padding(18.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Vista previa", color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text(
                when {
                    loading -> "Generando..."
                    preview != null -> "Fotograma AVI"
                    else -> "No disponible"
                },
                color = Muted,
                fontSize = 10.sp
            )
        }
        Spacer(Modifier.height(12.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(15.dp),
            color = SurfaceAlt,
            border = BorderStroke(1.dp, Color(0xFFD9DAE2))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                when {
                    loading -> {
                        Column(
                            modifier = Modifier.fillMaxWidth().height(170.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(color = Primary, strokeWidth = 3.dp, modifier = Modifier.size(32.dp))
                            Spacer(Modifier.height(12.dp))
                            Text("Preparando vista previa...", color = Muted, fontSize = 12.sp)
                        }
                    }
                    preview != null -> {
                        val ratio = ((targetWidth ?: preview.width).toFloat() / preview.height.coerceAtLeast(1)).coerceIn(0.55f, 3.5f)
                        Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFF17171C), shadowElevation = 10.dp) {
                            Image(
                                bitmap = preview.bitmap.asImageBitmap(),
                                contentDescription = "Vista previa del video deformada al ancho elegido",
                                contentScale = ContentScale.FillBounds,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(ratio)
                            )
                        }
                    }
                    else -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(170.dp)
                                .background(Color(0xFF1C1C22), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No se pudo extraer el fotograma.\nLa conversión puede continuar igualmente.",
                                modifier = Modifier.padding(20.dp),
                                color = Color(0xFFD8D8DF),
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "La forma del cuadro representa cómo quedará el ancho del MP4.",
            modifier = Modifier.fillMaxWidth(),
            color = Muted,
            fontSize = 10.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SettingsSection(
    targetWidth: String,
    validWidth: Boolean,
    outputFolderName: String,
    enabled: Boolean,
    onWidthChange: (String) -> Unit,
    onFolderClick: () -> Unit
) {
    val sliderValue = (targetWidth.toIntOrNull() ?: 1280).coerceIn(320, 3840).toFloat()
    Column(modifier = Modifier.padding(18.dp)) {
        Text("Ajustes de salida", color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(15.dp))
        Text("Ancho del video", color = Color(0xFF3F414A), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(7.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Slider(
                value = sliderValue,
                onValueChange = {
                    val even = ((it.roundToInt() / 2) * 2).coerceIn(320, 3840)
                    onWidthChange(even.toString())
                },
                enabled = enabled,
                valueRange = 320f..3840f,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = targetWidth,
                onValueChange = onWidthChange,
                enabled = enabled,
                isError = targetWidth.isNotEmpty() && !validWidth,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                suffix = { Text("px", fontSize = 10.sp) },
                modifier = Modifier.width(112.dp)
            )
        }
        Text(
            "El alto se conserva. El ancho debe ser un número par.",
            color = if (targetWidth.isNotEmpty() && !validWidth) Danger else Muted,
            fontSize = 10.sp
        )
        Spacer(Modifier.height(18.dp))
        Text("Carpeta de salida", color = Color(0xFF3F414A), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = onFolderClick,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, if (outputFolderName == "Sin carpeta seleccionada") Line else Primary.copy(alpha = 0.5f))
        ) {
            Text(
                if (outputFolderName == "Sin carpeta seleccionada") "Elegir carpeta" else outputFolderName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ActionsSection(
    jobCount: Int,
    completed: Int,
    running: Boolean,
    previewLoading: Boolean,
    canConvert: Boolean,
    summary: String?,
    summarySuccess: Boolean,
    onConvert: () -> Unit,
    onClear: () -> Unit
) {
    Column(modifier = Modifier.padding(18.dp)) {
        if (running) {
            Surface(shape = RoundedCornerShape(13.dp), color = Color(0xFFFAFAFE), border = BorderStroke(1.dp, Line)) {
                Column(Modifier.padding(14.dp)) {
                    Row(Modifier.fillMaxWidth()) {
                        Text("Convirtiendo la cola...", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        Text("$completed / $jobCount", color = Primary, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Spacer(Modifier.height(9.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Primary, trackColor = Color(0xFFE4E4EB))
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        Button(
            onClick = onConvert,
            enabled = canConvert && !running && !previewLoading,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(11.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary, disabledContainerColor = Primary.copy(alpha = 0.42f))
        ) {
            Text(
                when {
                    running -> "Convirtiendo la cola..."
                    jobCount == 0 -> "Convertir todos a MP4"
                    else -> "Convertir $jobCount archivo${if (jobCount == 1) "" else "s"} a MP4"
                },
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
        if (jobCount > 0) {
            Spacer(Modifier.height(9.dp))
            OutlinedButton(
                onClick = onClear,
                enabled = !running && !previewLoading,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(11.dp),
                border = BorderStroke(1.dp, Line)
            ) {
                Text("Vaciar lista", color = Muted, fontWeight = FontWeight.Bold)
            }
        }
        summary?.let {
            Spacer(Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (summarySuccess) Color(0xFFF1FAF6) else Color(0xFFFFF8EC),
                border = BorderStroke(1.dp, if (summarySuccess) Color(0xFFC7E6D7) else Color(0xFFF0D7A8))
            ) {
                Text(
                    it,
                    modifier = Modifier.padding(13.dp),
                    color = if (summarySuccess) Success else Color(0xFF8A5B08),
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun InfoCard(title: String, description: String) {
    Surface(shape = RoundedCornerShape(14.dp), color = Color.White.copy(alpha = 0.82f), border = BorderStroke(1.dp, Line)) {
        Column(Modifier.padding(15.dp)) {
            Text(title, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(3.dp))
            Text(description, color = Muted, fontSize = 10.sp, lineHeight = 15.sp)
        }
    }
}

private fun Modifier.dashedRoundedBorder(color: Color): Modifier = drawBehind {
    val strokeWidth = 2.dp.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
        size = Size(size.width - strokeWidth, size.height - strokeWidth),
        cornerRadius = CornerRadius(17.dp.toPx()),
        style = Stroke(
            width = strokeWidth,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 7.dp.toPx()))
        )
    )
}

private fun formatBytes(bytes: Long?): String {
    if (bytes == null || bytes <= 0L) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1 -> String.format("%.1f GB", gb)
        mb >= 1 -> String.format("%.1f MB", mb)
        kb >= 1 -> String.format("%.0f KB", kb)
        else -> "$bytes B"
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
        var conversion = runFfmpeg(conversionArguments(input, output, width, includeAudio = true))
        if (!conversion.succeeded || !output.isFile || output.length() == 0L) {
            output.delete()
            onStatus("Reintentando sin audio...")
            conversion = runFfmpeg(conversionArguments(input, output, width, includeAudio = false))
        }
        if (!conversion.succeeded || !output.isFile || output.length() == 0L) {
            return "Error al convertir (${conversion.returnCode}): ${conversion.failureDetail()}"
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

private fun conversionArguments(
    input: File,
    output: File,
    width: Int,
    includeAudio: Boolean
): Array<String> = buildList {
    addAll(
        listOf(
            "-y", "-hide_banner", "-loglevel", "warning",
            "-fflags", "+genpts+discardcorrupt", "-err_detect", "ignore_err",
            "-i", input.absolutePath,
            "-map", "0:v:0",
            "-vf", "scale=$width:trunc(ih/2)*2",
            "-c:v", "mpeg4", "-q:v", "4", "-pix_fmt", "yuv420p"
        )
    )
    if (includeAudio) {
        addAll(listOf("-map", "0:a:0?", "-c:a", "aac", "-b:a", "128k", "-ar", "44100"))
    } else {
        add("-an")
    }
    addAll(
        listOf(
            "-avoid_negative_ts", "make_zero", "-max_muxing_queue_size", "1024",
            "-movflags", "+faststart", output.absolutePath
        )
    )
}.toTypedArray()

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

private fun runFfmpeg(arguments: Array<String>): FfmpegResult {
    val session = FFmpegKit.executeWithArguments(arguments)
    val returnCode = session.returnCode
    return FfmpegResult(
        returnCode = returnCode?.value ?: -1,
        output = session.output.orEmpty(),
        succeeded = returnCode != null && ReturnCode.isSuccess(returnCode)
    )
}

private fun FfmpegResult.failureDetail(): String {
    val detail = output.lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .toList()
        .takeLast(4)
        .joinToString(" | ")
        .take(500)
    return detail.ifEmpty { "FFmpeg termino sin detalles" }
}

private fun Context.displayName(uri: Uri): String {
    contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) return cursor.getString(0)
    }
    return uri.lastPathSegment ?: "Video sin nombre"
}

private fun Context.fileSize(uri: Uri): Long? {
    contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val index = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (index >= 0 && !cursor.isNull(index)) return cursor.getLong(index)
        }
    }
    return null
}

private suspend fun Context.videoPreview(uri: Uri): VideoPreview? = withContext(Dispatchers.IO) {
    val input = File(cacheDir, "preview_source_${System.nanoTime()}.avi")
    val image = File(cacheDir, "preview_frame_${System.nanoTime()}.jpg")
    try {
        contentResolver.openInputStream(uri)?.use { source ->
            input.outputStream().use { target -> source.copyTo(target) }
        } ?: return@withContext null
        val result = runFfmpeg(
            arrayOf(
                "-y", "-hide_banner", "-loglevel", "warning",
                "-fflags", "+genpts+discardcorrupt", "-err_detect", "ignore_err",
                "-i", input.absolutePath,
                "-an", "-frames:v", "1", "-vf", "scale=640:-2", image.absolutePath
            )
        )
        if (!result.succeeded || !image.isFile) return@withContext null
        BitmapFactory.decodeFile(image.absolutePath)?.let { bitmap ->
            VideoPreview(bitmap, bitmap.width, bitmap.height)
        }
    } catch (_: Exception) {
        null
    } finally {
        input.delete()
        image.delete()
    }
}
