package com.kyro.avi2mp4

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.widget.MediaController
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.math.roundToLong

private val EditorBackground = Color(0xFF111216)
private val EditorSurface = Color(0xFF1A1C22)
private val EditorSurfaceRaised = Color(0xFF242730)
private val EditorLine = Color(0xFF353945)
private val EditorText = Color(0xFFF4F1EA)
private val EditorMuted = Color(0xFFA5A7B0)
private val EditorAccent = Color(0xFFE8B04B)
private val EditorAccentDark = Color(0xFFB77A1D)

private val EditorColors = darkColorScheme(
    primary = EditorAccent,
    onPrimary = Color(0xFF231700),
    background = EditorBackground,
    onBackground = EditorText,
    surface = EditorSurface,
    onSurface = EditorText,
    outline = EditorLine
)

class EditorActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { EditorApp(onClose = ::finish) }
    }
}

@Composable
private fun EditorApp(onClose: () -> Unit) {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("editor_preferences", Context.MODE_PRIVATE) }
    val projectStore = remember { EditorProjectStore(context) }
    val clips = remember {
        mutableStateListOf<EditorClip>().apply { addAll(projectStore.loadClips()) }
    }
    var selectedClipId by remember { mutableStateOf(clips.firstOrNull()?.id) }
    var lastInputLocation by remember {
        mutableStateOf(preferences.getString("input_location_uri", null)?.let(Uri::parse))
    }
    var importing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun persist() = projectStore.saveClips(clips.toList())

    fun moveSelected(offset: Int) {
        val from = clips.indexOfFirst { it.id == selectedClipId }
        val to = from + offset
        if (from !in clips.indices || to !in clips.indices) return
        val moved = moveEditorClip(clips, from, to)
        clips.clear()
        clips.addAll(moved)
        persist()
    }

    fun removeSelected() {
        val index = clips.indexOfFirst { it.id == selectedClipId }
        if (index !in clips.indices) return
        clips.removeAt(index)
        selectedClipId = clips.getOrNull(index.coerceAtMost(clips.lastIndex))?.id
        persist()
    }

    val pickClips = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data ?: return@rememberLauncherForActivityResult
        val uris = buildList {
            data.clipData?.let { clipData ->
                repeat(clipData.itemCount) { index -> add(clipData.getItemAt(index).uri) }
            }
            data.data?.let(::add)
        }.distinct()
        if (uris.isEmpty()) return@rememberLauncherForActivityResult

        importing = true
        scope.launch {
            var imported = 0
            uris.forEach { uri ->
                try {
                    context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (_: SecurityException) {
                    // The current session grant is still usable.
                }
                if (clips.none { it.uri == uri.toString() }) {
                    val clip = withContext(Dispatchers.IO) { context.editorClip(uri) }
                    if (clip != null) {
                        clips += clip
                        if (selectedClipId == null) selectedClipId = clip.id
                        imported++
                    }
                }
            }
            val firstUri = uris.first()
            lastInputLocation = context.editorInitialLocation(firstUri)
            preferences.edit().putString("input_location_uri", lastInputLocation.toString()).apply()
            persist()
            importing = false
            if (imported == 0) {
                Toast.makeText(context, "No se encontró un MP4 reproducible nuevo", Toast.LENGTH_LONG).show()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { persist() }
    }

    val selectedClip = clips.firstOrNull { it.id == selectedClipId }
    val selectedIndex = clips.indexOfFirst { it.id == selectedClipId }

    MaterialTheme(colorScheme = EditorColors) {
        Surface(modifier = Modifier.fillMaxSize(), color = EditorBackground) {
            Column(
                modifier = Modifier.background(
                    Brush.verticalGradient(listOf(Color(0xFF171920), EditorBackground))
                )
            ) {
                EditorTopBar(
                    importing = importing,
                    onClose = onClose,
                    onImport = { pickClips.launch(editorPickerIntent(lastInputLocation)) }
                )
                if (clips.isEmpty()) {
                    EmptyEditor(
                        importing = importing,
                        onImport = { pickClips.launch(editorPickerIntent(lastInputLocation)) }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            EditorPreview(selectedClip)
                        }
                        item {
                            ProjectSummary(clips)
                        }
                        item {
                            TimelineSection(
                                clips = clips,
                                selectedClipId = selectedClipId,
                                onSelect = { selectedClipId = it.id }
                            )
                        }
                        selectedClip?.let { clip ->
                            item {
                                TrimSection(
                                    clip = clip,
                                    canMoveEarlier = selectedIndex > 0,
                                    canMoveLater = selectedIndex in 0 until clips.lastIndex,
                                    onTrimChange = { updated ->
                                        val index = clips.indexOfFirst { it.id == updated.id }
                                        if (index >= 0) clips[index] = updated
                                    },
                                    onTrimFinished = ::persist,
                                    onMoveEarlier = { moveSelected(-1) },
                                    onMoveLater = { moveSelected(1) },
                                    onDelete = ::removeSelected
                                )
                            }
                        }
                        item {
                            OutlinedButton(
                                onClick = {
                                    clips.clear()
                                    selectedClipId = null
                                    projectStore.clear()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                border = BorderStroke(1.dp, EditorLine),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Vaciar proyecto", color = EditorMuted)
                            }
                        }
                        item { Spacer(Modifier.navigationBarsPadding().height(8.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorTopBar(importing: Boolean, onClose: () -> Unit, onImport: () -> Unit) {
    Surface(color = Color(0xFF15171C), shadowElevation = 8.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(66.dp)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onClose) {
                Text("Volver", color = EditorMuted)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Editor", color = EditorText, fontSize = 17.sp, fontWeight = FontWeight.Black)
                Text("Proyecto automático", color = EditorMuted, fontSize = 9.sp)
            }
            Button(
                onClick = onImport,
                enabled = !importing,
                colors = ButtonDefaults.buttonColors(containerColor = EditorAccent),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 13.dp, vertical = 9.dp)
            ) {
                Text(if (importing) "Leyendo..." else "+ Clips", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun EmptyEditor(importing: Boolean, onImport: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp),
            color = EditorSurface,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, EditorLine)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 26.dp, vertical = 34.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier.size(74.dp),
                    color = EditorSurfaceRaised,
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("01:00", color = EditorAccent, fontSize = 13.sp, fontWeight = FontWeight.Black)
                    }
                }
                Spacer(Modifier.height(18.dp))
                Text("Armá tu primera secuencia", color = EditorText, fontSize = 22.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Agregá los MP4 convertidos. El orden y los cortes se guardan sin modificar los originales.",
                    color = EditorMuted,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(22.dp))
                Button(
                    onClick = onImport,
                    enabled = !importing,
                    colors = ButtonDefaults.buttonColors(containerColor = EditorAccent),
                    shape = RoundedCornerShape(11.dp)
                ) {
                    Text(if (importing) "Leyendo clips..." else "Agregar videos MP4", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun EditorPreview(clip: EditorClip?) {
    var videoView by remember { mutableStateOf<VideoView?>(null) }
    val uri = clip?.uri

    DisposableEffect(Unit) {
        onDispose { videoView?.stopPlayback() }
    }

    LaunchedEffect(clip?.id, clip?.trimStartMs, clip?.trimEndMs, videoView) {
        val player = videoView ?: return@LaunchedEffect
        val active = clip ?: return@LaunchedEffect
        while (true) {
            delay(100)
            if (player.isPlaying && player.currentPosition.toLong() >= active.trimEndMs) {
                player.pause()
                player.seekTo(active.trimStartMs.toInt())
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Black,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, EditorLine),
        shadowElevation = 12.dp
    ) {
        if (clip == null) {
            Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f), contentAlignment = Alignment.Center) {
                Text("Seleccioná un clip", color = EditorMuted)
            }
        } else {
            AndroidView(
                factory = { context ->
                    VideoView(context).apply {
                        val controls = MediaController(context)
                        controls.setAnchorView(this)
                        setMediaController(controls)
                        videoView = this
                    }
                },
                update = { player ->
                    if (player.tag != uri) {
                        player.tag = uri
                        player.setOnPreparedListener {
                            player.seekTo(clip.trimStartMs.toInt())
                            player.start()
                        }
                        player.setVideoURI(Uri.parse(uri))
                    } else if (player.currentPosition.toLong() !in clip.trimStartMs..clip.trimEndMs) {
                        player.seekTo(clip.trimStartMs.toInt())
                    }
                },
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
            )
        }
    }
}

@Composable
private fun ProjectSummary(clips: List<EditorClip>) {
    val duration = clips.sumOf(EditorClip::trimmedDurationMs)
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text("LÍNEA DE TIEMPO", color = EditorAccent, fontSize = 10.sp, fontWeight = FontWeight.Black)
            Text("${clips.size} clip${if (clips.size == 1) "" else "s"}", color = EditorText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.weight(1f))
        Surface(color = EditorSurfaceRaised, shape = RoundedCornerShape(50)) {
            Text(
                formatEditorTime(duration),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                color = EditorText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun TimelineSection(
    clips: List<EditorClip>,
    selectedClipId: String?,
    onSelect: (EditorClip) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF0C0D10),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, EditorLine)
    ) {
        Column(modifier = Modifier.padding(vertical = 14.dp)) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.width(2.dp).height(14.dp).background(EditorAccent))
            }
            Spacer(Modifier.height(7.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(clips, key = { _, clip -> clip.id }) { index, clip ->
                    TimelineClipCard(
                        clip = clip,
                        index = index,
                        selected = clip.id == selectedClipId,
                        onClick = { onSelect(clip) }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = EditorLine)
            Text(
                "00:00",
                modifier = Modifier.padding(start = 13.dp, top = 7.dp),
                color = EditorMuted,
                fontSize = 9.sp
            )
        }
    }
}

@Composable
private fun TimelineClipCard(clip: EditorClip, index: Int, selected: Boolean, onClick: () -> Unit) {
    val seconds = clip.trimmedDurationMs / 1000f
    val cardWidth = (118f + seconds.coerceAtMost(20f) * 5f).dp
    Surface(
        modifier = Modifier.width(cardWidth).height(104.dp).clickable(onClick = onClick),
        color = if (selected) Color(0xFF33303A) else EditorSurface,
        shape = RoundedCornerShape(11.dp),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) EditorAccent else EditorLine)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().height(58.dp).padding(5.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                repeat(6) { frame ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .background(
                                if ((frame + index) % 2 == 0) Color(0xFF4C5260) else Color(0xFF333843),
                                RoundedCornerShape(4.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${index + 1}", color = Color.White.copy(alpha = 0.28f), fontSize = 8.sp)
                    }
                }
            }
            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                Text(
                    clip.name,
                    color = EditorText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(formatEditorTime(clip.trimmedDurationMs), color = EditorMuted, fontSize = 8.sp)
            }
        }
    }
}

@Composable
private fun TrimSection(
    clip: EditorClip,
    canMoveEarlier: Boolean,
    canMoveLater: Boolean,
    onTrimChange: (EditorClip) -> Unit,
    onTrimFinished: () -> Unit,
    onMoveEarlier: () -> Unit,
    onMoveLater: () -> Unit,
    onDelete: () -> Unit
) {
    val durationSeconds = (clip.durationMs / 1000f).coerceAtLeast(0.1f)
    val selectedRange = (clip.trimStartMs / 1000f)..(clip.trimEndMs / 1000f)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = EditorSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, EditorLine)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("RECORTAR CLIP", color = EditorAccent, fontSize = 10.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(4.dp))
            Text(clip.name, color = EditorText, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(14.dp))
            RangeSlider(
                value = selectedRange,
                onValueChange = { range ->
                    val minimumGapMs = minOf(100L, clip.durationMs)
                    val start = (range.start * 1000).roundToLong().coerceIn(0L, clip.durationMs - minimumGapMs)
                    val end = (range.endInclusive * 1000).roundToLong().coerceIn(start + minimumGapMs, clip.durationMs)
                    onTrimChange(clip.copy(trimStartMs = start, trimEndMs = end))
                },
                onValueChangeFinished = onTrimFinished,
                valueRange = 0f..durationSeconds
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("Entrada ${formatEditorTime(clip.trimStartMs)}", color = EditorMuted, fontSize = 10.sp)
                Spacer(Modifier.weight(1f))
                Text("Salida ${formatEditorTime(clip.trimEndMs)}", color = EditorMuted, fontSize = 10.sp)
            }
            Spacer(Modifier.height(15.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                OutlinedButton(
                    onClick = onMoveEarlier,
                    enabled = canMoveEarlier,
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, EditorLine),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("← Antes", fontSize = 11.sp)
                }
                OutlinedButton(
                    onClick = onMoveLater,
                    enabled = canMoveLater,
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, EditorLine),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Después →", fontSize = 11.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                Text("Quitar clip del proyecto", color = Color(0xFFE48A8F), fontSize = 11.sp)
            }
        }
    }
}

private fun Context.editorClip(uri: Uri): EditorClip? {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(this, uri)
        val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            ?.toLongOrNull()?.takeIf { it > 0L } ?: return null
        EditorClip(
            id = UUID.randomUUID().toString(),
            uri = uri.toString(),
            name = editorDisplayName(uri),
            durationMs = duration
        )
    } catch (_: Exception) {
        null
    } finally {
        retriever.release()
    }
}

private fun Context.editorDisplayName(uri: Uri): String {
    contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) return cursor.getString(0)
    }
    return uri.lastPathSegment ?: "Clip MP4"
}

private fun editorPickerIntent(initialLocation: Uri?): Intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
    addCategory(Intent.CATEGORY_OPENABLE)
    type = "video/mp4"
    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        initialLocation?.let { putExtra(DocumentsContract.EXTRA_INITIAL_URI, it) }
    }
    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
}

private fun Context.editorInitialLocation(uri: Uri): Uri {
    if (!DocumentsContract.isDocumentUri(this, uri)) return uri
    return runCatching {
        val documentId = DocumentsContract.getDocumentId(uri)
        val parentId = documentId.substringBeforeLast('/', missingDelimiterValue = documentId)
        val authority = uri.authority ?: return@runCatching uri
        if (parentId == documentId) uri else DocumentsContract.buildDocumentUri(authority, parentId)
    }.getOrDefault(uri)
}

private fun formatEditorTime(milliseconds: Long): String {
    val totalSeconds = (milliseconds / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}
