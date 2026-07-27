package com.kyro.avi2mp4

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt
import kotlin.math.roundToLong

private val EditorBackground = Color(0xFF111216)
private val EditorSurface = Color(0xFF1A1C22)
private val EditorSurfaceRaised = Color(0xFF242730)
private val EditorLine = Color(0xFF353945)
private val EditorText = Color(0xFFF4F1EA)
private val EditorMuted = Color(0xFFA5A7B0)
private val EditorAccent = Color(0xFFE8B04B)
private val EditorDanger = Color(0xFFE48A8F)
private val MusicAccent = Color(0xFF67C6A3)

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
    val loadedProject = remember { projectStore.loadProject() }
    var history by remember {
        mutableStateOf(
            EditorHistory(
                EditorSnapshot(loadedProject, loadedProject.clips.firstOrNull()?.id)
            )
        )
    }
    var draft by remember { mutableStateOf<EditorSnapshot?>(null) }
    var previewPositionMs by remember { mutableStateOf(0L) }
    var lastInputLocation by remember {
        mutableStateOf(preferences.getString("input_location_uri", null)?.let(Uri::parse))
    }
    var lastMusicLocation by remember {
        mutableStateOf(preferences.getString("music_location_uri", null)?.let(Uri::parse))
    }
    var importing by remember { mutableStateOf(false) }
    var exportJob by remember { mutableStateOf<Job?>(null) }
    var exportProgress by remember { mutableStateOf<EditorExportProgress?>(null) }
    var exportMessage by remember { mutableStateOf<String?>(null) }
    var lastExportUri by remember { mutableStateOf<Uri?>(null) }
    var pendingExportProject by remember { mutableStateOf<EditorProject?>(null) }
    val scope = rememberCoroutineScope()
    val visibleSnapshot = draft ?: history.present
    val project = visibleSnapshot.project
    val clips = project.clips
    val selectedClip = clips.firstOrNull { it.id == visibleSnapshot.selectedClipId }
    val selectedIndex = clips.indexOfFirst { it.id == visibleSnapshot.selectedClipId }
    val busy = importing || exportJob != null

    fun save(updatedHistory: EditorHistory) {
        projectStore.saveProject(updatedHistory.present.project)
    }

    fun commitProject(nextProject: EditorProject, selectedId: String? = visibleSnapshot.selectedClipId) {
        val baseHistory = draft?.let(history::commit) ?: history
        val committed = baseHistory.commit(EditorSnapshot(nextProject, selectedId))
        draft = null
        history = committed
        save(committed)
    }

    fun updateDraft(transform: (EditorProject) -> EditorProject) {
        val base = draft ?: history.present
        draft = base.copy(project = transform(base.project))
    }

    fun commitDraft() {
        val pending = draft ?: return
        val committed = history.commit(pending)
        draft = null
        history = committed
        save(committed)
    }

    fun selectClip(clipId: String) {
        val baseHistory = draft?.let(history::commit) ?: history
        val selected = baseHistory.copy(
            present = baseHistory.present.copy(selectedClipId = clipId)
        )
        if (draft != null) save(baseHistory)
        draft = null
        history = selected
        previewPositionMs = selected.present.project.clips.firstOrNull { it.id == clipId }?.trimStartMs ?: 0L
    }

    fun updateSelectedDraft(transform: (EditorClip) -> EditorClip) {
        updateDraft { current ->
            val index = current.clips.indexOfFirst { it.id == visibleSnapshot.selectedClipId }
            if (index < 0) current else current.copy(
                clips = current.clips.toMutableList().apply { this[index] = transform(this[index]) }
            )
        }
    }

    fun undo() {
        val baseHistory = draft?.let(history::commit) ?: history
        val updated = baseHistory.undo()
        draft = null
        history = updated
        previewPositionMs = updated.present.project.clips
            .firstOrNull { it.id == updated.present.selectedClipId }?.trimStartMs ?: 0L
        save(updated)
    }

    fun redo() {
        val baseHistory = draft?.let(history::commit) ?: history
        val updated = baseHistory.redo()
        draft = null
        history = updated
        previewPositionMs = updated.present.project.clips
            .firstOrNull { it.id == updated.present.selectedClipId }?.trimStartMs ?: 0L
        save(updated)
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
            val imported = mutableListOf<EditorClip>()
            uris.forEach { uri ->
                context.persistReadPermission(uri)
                if (project.clips.none { it.uri == uri.toString() } && imported.none { it.uri == uri.toString() }) {
                    withContext(Dispatchers.IO) { context.editorClip(uri) }?.let(imported::add)
                }
            }
            uris.firstOrNull()?.let { uri ->
                lastInputLocation = context.editorInitialLocation(uri)
                preferences.edit().putString("input_location_uri", lastInputLocation.toString()).apply()
            }
            importing = false
            if (imported.isEmpty()) {
                Toast.makeText(context, "No se encontró un MP4 reproducible nuevo", Toast.LENGTH_LONG).show()
            } else {
                val next = project.copy(clips = project.clips + imported)
                commitProject(next, visibleSnapshot.selectedClipId ?: imported.first().id)
            }
        }
    }

    val pickMusic = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.data ?: return@rememberLauncherForActivityResult
        importing = true
        scope.launch {
            context.persistReadPermission(uri)
            val music = withContext(Dispatchers.IO) { context.editorMusic(uri) }
            importing = false
            if (music == null) {
                Toast.makeText(context, "No se pudo leer la pista de audio", Toast.LENGTH_LONG).show()
            } else {
                lastMusicLocation = context.editorInitialLocation(uri)
                preferences.edit().putString("music_location_uri", lastMusicLocation.toString()).apply()
                commitProject(project.copy(music = music))
            }
        }
    }

    val createExport = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val destination = result.data?.data ?: run {
            pendingExportProject = null
            return@rememberLauncherForActivityResult
        }
        val exportProject = pendingExportProject ?: return@rememberLauncherForActivityResult
        pendingExportProject = null
        exportMessage = null
        exportProgress = EditorExportProgress("Preparando exportación...", null)
        exportJob = scope.launch {
            try {
                val resultValue = EditorExporter(context).export(exportProject, destination) { progress ->
                    scope.launch { exportProgress = progress }
                }
                when (resultValue) {
                    EditorExportResult.Success -> {
                        lastExportUri = destination
                        exportMessage = "Montaje MP4 guardado correctamente"
                    }
                    is EditorExportResult.Failure -> exportMessage = resultValue.message
                }
            } catch (_: CancellationException) {
                exportMessage = "Exportación cancelada"
            } finally {
                exportProgress = null
                exportJob = null
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            draft?.let { projectStore.saveProject(it.project) }
            exportJob?.cancel()
        }
    }

    MaterialTheme(colorScheme = EditorColors) {
        Surface(modifier = Modifier.fillMaxSize(), color = EditorBackground) {
            Column(
                modifier = Modifier.background(
                    Brush.verticalGradient(listOf(Color(0xFF171920), EditorBackground))
                )
            ) {
                EditorTopBar(
                    importing = importing,
                    exporting = exportJob != null,
                    canUndo = history.canUndo || draft != null,
                    canRedo = draft == null && history.canRedo,
                    onClose = onClose,
                    onUndo = ::undo,
                    onRedo = ::redo,
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
                            EditorPreview(selectedClip, onPositionChange = { clipId, position ->
                                if (clipId == visibleSnapshot.selectedClipId) previewPositionMs = position
                            })
                        }
                        item { ProjectSummary(project) }
                        item {
                            TimelineSection(
                                project = project,
                                selectedClipId = visibleSnapshot.selectedClipId,
                                enabled = !busy,
                                onSelect = { selectClip(it.id) },
                                onMove = { clipId, offset ->
                                    val from = project.clips.indexOfFirst { it.id == clipId }
                                    val to = (from + offset).coerceIn(0, project.clips.lastIndex)
                                    if (from >= 0 && to != from) {
                                        commitProject(project.copy(clips = moveEditorClip(project.clips, from, to)), clipId)
                                    }
                                }
                            )
                        }
                        selectedClip?.let { clip ->
                            item {
                                ClipEditSection(
                                    clip = clip,
                                    previewPositionMs = previewPositionMs,
                                    canMoveEarlier = selectedIndex > 0,
                                    canMoveLater = selectedIndex in 0 until clips.lastIndex,
                                    enabled = !busy,
                                    onTrimChange = { updated -> updateSelectedDraft { updated } },
                                    onTrimFinished = ::commitDraft,
                                    onSplit = {
                                        val updated = splitEditorClip(
                                            project,
                                            clip.id,
                                            previewPositionMs,
                                            UUID.randomUUID().toString()
                                        )
                                        if (updated == null) {
                                            Toast.makeText(context, "Mové la reproducción lejos de los bordes para dividir", Toast.LENGTH_SHORT).show()
                                        } else {
                                            val right = updated.clips.getOrNull(selectedIndex + 1)
                                            commitProject(updated, right?.id)
                                        }
                                    },
                                    onMoveEarlier = {
                                        commitProject(project.copy(clips = moveEditorClip(clips, selectedIndex, selectedIndex - 1)), clip.id)
                                    },
                                    onMoveLater = {
                                        commitProject(project.copy(clips = moveEditorClip(clips, selectedIndex, selectedIndex + 1)), clip.id)
                                    },
                                    onDelete = {
                                        val updated = clips.toMutableList().apply { removeAt(selectedIndex) }
                                        val nextSelection = updated.getOrNull(selectedIndex.coerceAtMost(updated.lastIndex))?.id
                                        commitProject(project.copy(clips = updated), nextSelection)
                                    }
                                )
                            }
                            item {
                                ColorSection(
                                    color = clip.color,
                                    enabled = !busy,
                                    onChange = { color -> updateSelectedDraft { it.copy(color = color) } },
                                    onChangeFinished = ::commitDraft,
                                    onReset = { commitProject(project.replaceClip(clip.copy(color = EditorColorSettings())), clip.id) }
                                )
                            }
                        }
                        item {
                            MusicSection(
                                music = project.music,
                                projectDurationMs = project.durationMs,
                                enabled = !busy,
                                onPick = { pickMusic.launch(editorMusicPickerIntent(lastMusicLocation)) },
                                onChange = { music -> updateDraft { it.copy(music = music) } },
                                onChangeFinished = ::commitDraft,
                                onRemove = { commitProject(project.copy(music = null)) }
                            )
                        }
                        item {
                            ExportSection(
                                outputWidth = project.outputWidth,
                                enabled = !importing,
                                exporting = exportJob != null,
                                progress = exportProgress,
                                message = exportMessage,
                                hasResult = lastExportUri != null,
                                onWidthChange = { width -> updateDraft { it.copy(outputWidth = width) } },
                                onWidthFinished = ::commitDraft,
                                onExport = {
                                    pendingExportProject = project
                                    commitDraft()
                                    createExport.launch(editorExportIntent())
                                },
                                onCancel = { exportJob?.cancel() },
                                onOpen = { lastExportUri?.let(context::openExportedVideo) }
                            )
                        }
                        item {
                            OutlinedButton(
                                onClick = { commitProject(EditorProject(), null) },
                                enabled = !busy,
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
private fun EditorTopBar(
    importing: Boolean,
    exporting: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    onClose: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onImport: () -> Unit
) {
    Surface(color = Color(0xFF15171C), shadowElevation = 8.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().height(66.dp).padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onClose, contentPadding = PaddingValues(horizontal = 7.dp)) {
                Text("Volver", color = EditorMuted, fontSize = 11.sp)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Editor", color = EditorText, fontSize = 16.sp, fontWeight = FontWeight.Black)
                Text(if (exporting) "Exportando" else "Guardado automático", color = EditorMuted, fontSize = 8.sp)
            }
            TextButton(onClick = onUndo, enabled = canUndo && !importing && !exporting, contentPadding = PaddingValues(7.dp)) {
                Text("↶", fontSize = 21.sp, color = if (canUndo) EditorText else EditorMuted)
            }
            TextButton(onClick = onRedo, enabled = canRedo && !importing && !exporting, contentPadding = PaddingValues(7.dp)) {
                Text("↷", fontSize = 21.sp, color = if (canRedo) EditorText else EditorMuted)
            }
            Button(
                onClick = onImport,
                enabled = !importing && !exporting,
                colors = ButtonDefaults.buttonColors(containerColor = EditorAccent),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(if (importing) "Leyendo" else "+ Clips", fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
                Surface(modifier = Modifier.size(74.dp), color = EditorSurfaceRaised, shape = RoundedCornerShape(22.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("01:00", color = EditorAccent, fontSize = 13.sp, fontWeight = FontWeight.Black)
                    }
                }
                Spacer(Modifier.height(18.dp))
                Text("Armá tu primera secuencia", color = EditorText, fontSize = 22.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Agregá los MP4 convertidos. Cada corte, orden y ajuste se guarda sin modificar los originales.",
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
private fun EditorPreview(clip: EditorClip?, onPositionChange: (String, Long) -> Unit) {
    var videoView by remember { mutableStateOf<VideoView?>(null) }
    val uri = clip?.uri

    DisposableEffect(Unit) {
        onDispose { videoView?.stopPlayback() }
    }

    LaunchedEffect(clip?.id, clip?.trimStartMs, clip?.trimEndMs, videoView) {
        val player = videoView ?: return@LaunchedEffect
        val active = clip ?: return@LaunchedEffect
        if (player.currentPosition.toLong() !in active.trimStartMs until active.trimEndMs) {
            player.seekTo(active.trimStartMs.toInt())
        }
        while (true) {
            delay(100)
            val position = player.currentPosition.toLong()
            onPositionChange(active.id, position.coerceIn(active.trimStartMs, active.trimEndMs))
            if (player.isPlaying && position >= active.trimEndMs) {
                player.pause()
                player.seekTo(active.trimStartMs.toInt())
                onPositionChange(active.id, active.trimStartMs)
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
                    } else if (player.currentPosition.toLong() !in clip.trimStartMs until clip.trimEndMs) {
                        player.seekTo(clip.trimStartMs.toInt())
                    }
                },
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
            )
        }
    }
}

@Composable
private fun ProjectSummary(project: EditorProject) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text("LÍNEA DE TIEMPO", color = EditorAccent, fontSize = 10.sp, fontWeight = FontWeight.Black)
            Text(
                "${project.clips.size} segmento${if (project.clips.size == 1) "" else "s"}",
                color = EditorText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.weight(1f))
        Surface(color = EditorSurfaceRaised, shape = RoundedCornerShape(50)) {
            Text(
                formatEditorTime(project.durationMs),
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
    project: EditorProject,
    selectedClipId: String?,
    enabled: Boolean,
    onSelect: (EditorClip) -> Unit,
    onMove: (String, Int) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF0C0D10),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, EditorLine)
    ) {
        Column(modifier = Modifier.padding(vertical = 12.dp)) {
            Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("VIDEO", color = EditorMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text("Mantené pulsado y arrastrá para ordenar", color = EditorMuted, fontSize = 8.sp)
            }
            Spacer(Modifier.height(7.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(project.clips, key = { _, clip -> clip.id }) { index, clip ->
                    TimelineClipCard(
                        clip = clip,
                        index = index,
                        selected = clip.id == selectedClipId,
                        enabled = enabled,
                        onClick = { onSelect(clip) },
                        onMoveBy = { offset -> onMove(clip.id, offset) }
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = EditorLine)
            Spacer(Modifier.height(8.dp))
            Text("MÚSICA", modifier = Modifier.padding(horizontal = 12.dp), color = EditorMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            MusicTimeline(project.music, project.durationMs)
            Spacer(Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.width(2.dp).height(12.dp).background(EditorAccent))
            }
        }
    }
}

@Composable
private fun TimelineClipCard(
    clip: EditorClip,
    index: Int,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    onMoveBy: (Int) -> Unit
) {
    var dragX by remember(clip.id) { mutableStateOf(0f) }
    var dragging by remember(clip.id) { mutableStateOf(false) }
    val stepPx = with(LocalDensity.current) { 144.dp.toPx() }
    Surface(
        modifier = Modifier
            .width(136.dp)
            .height(105.dp)
            .zIndex(if (dragging) 2f else 0f)
            .graphicsLayer { translationX = dragX }
            .pointerInput(clip.id, enabled) {
                if (enabled) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { dragging = true },
                        onDragCancel = { dragX = 0f; dragging = false },
                        onDragEnd = {
                            val offset = (dragX / stepPx).roundToInt()
                            dragX = 0f
                            dragging = false
                            if (offset != 0) onMoveBy(offset)
                        },
                        onDrag = { change, amount ->
                            change.consume()
                            dragX += amount.x
                        }
                    )
                }
            }
            .clickable(enabled = enabled, onClick = onClick),
        color = if (selected) Color(0xFF33303A) else EditorSurface,
        shape = RoundedCornerShape(11.dp),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) EditorAccent else EditorLine),
        shadowElevation = if (dragging) 12.dp else 0.dp
    ) {
        Column {
            TimelineThumbnail(clip, index)
            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)) {
                Text(
                    "${index + 1}. ${clip.name}",
                    color = EditorText,
                    fontSize = 9.sp,
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
private fun TimelineThumbnail(clip: EditorClip, index: Int) {
    val context = LocalContext.current
    var bitmap by remember(clip.id, clip.trimStartMs, clip.trimEndMs) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(clip.uri, clip.trimStartMs, clip.trimEndMs) {
        bitmap = withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, Uri.parse(clip.uri))
                val midpointUs = (clip.trimStartMs + clip.trimmedDurationMs / 2L) * 1000L
                retriever.getFrameAtTime(midpointUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            } catch (_: Exception) {
                null
            } finally {
                retriever.release()
            }
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth().height(62.dp).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        repeat(3) { frame ->
            Box(
                modifier = Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(4.dp)).background(
                    if ((frame + index) % 2 == 0) Color(0xFF4C5260) else Color(0xFF333843)
                ),
                contentAlignment = Alignment.Center
            ) {
                bitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } ?: Text("${index + 1}", color = Color.White.copy(alpha = 0.35f), fontSize = 8.sp)
            }
        }
    }
}

@Composable
private fun MusicTimeline(music: EditorMusicTrack?, projectDurationMs: Long) {
    if (music == null) {
        Box(
            modifier = Modifier.fillMaxWidth().height(34.dp).padding(horizontal = 12.dp)
                .background(EditorSurface, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.CenterStart
        ) {
            Text("Sin pista MP3", modifier = Modifier.padding(horizontal = 10.dp), color = EditorMuted, fontSize = 9.sp)
        }
        return
    }
    val duration = projectDurationMs.coerceAtLeast(1L)
    val startFraction = (music.timelineStartMs.toFloat() / duration).coerceIn(0f, 0.92f)
    val widthFraction = (music.trimmedDurationMs.toFloat() / duration).coerceIn(0.08f, 1f - startFraction)
    Box(modifier = Modifier.fillMaxWidth().height(38.dp).padding(horizontal = 12.dp)) {
        Box(
            modifier = Modifier.fillMaxSize().background(EditorSurface, RoundedCornerShape(8.dp))
        )
        Row(modifier = Modifier.fillMaxSize()) {
            Spacer(Modifier.weight(startFraction.coerceAtLeast(0.001f)))
            Box(
                modifier = Modifier.weight(widthFraction).fillMaxSize()
                    .background(MusicAccent.copy(alpha = 0.32f), RoundedCornerShape(8.dp))
                    .drawBehind {
                        val center = size.height / 2f
                        repeat(24) { bar ->
                            val x = size.width * bar / 23f
                            val amplitude = size.height * (0.16f + (bar % 5) * 0.055f)
                            drawLine(MusicAccent, start = androidx.compose.ui.geometry.Offset(x, center - amplitude), end = androidx.compose.ui.geometry.Offset(x, center + amplitude), strokeWidth = 1.dp.toPx())
                        }
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                Text(music.name, modifier = Modifier.padding(horizontal = 8.dp), color = EditorText, fontSize = 8.sp, maxLines = 1)
            }
            val remaining = (1f - startFraction - widthFraction).coerceAtLeast(0.001f)
            Spacer(Modifier.weight(remaining))
        }
    }
}

@Composable
private fun ClipEditSection(
    clip: EditorClip,
    previewPositionMs: Long,
    canMoveEarlier: Boolean,
    canMoveLater: Boolean,
    enabled: Boolean,
    onTrimChange: (EditorClip) -> Unit,
    onTrimFinished: () -> Unit,
    onSplit: () -> Unit,
    onMoveEarlier: () -> Unit,
    onMoveLater: () -> Unit,
    onDelete: () -> Unit
) {
    val durationSeconds = (clip.durationMs / 1000f).coerceAtLeast(0.1f)
    val selectedRange = (clip.trimStartMs / 1000f)..(clip.trimEndMs / 1000f)
    val canSplit = previewPositionMs - clip.trimStartMs >= MIN_EDITOR_SEGMENT_MS &&
        clip.trimEndMs - previewPositionMs >= MIN_EDITOR_SEGMENT_MS
    EditorPanel(title = "CLIP SELECCIONADO") {
        Text(clip.name, color = EditorText, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(14.dp))
        Text("Recorte de entrada y salida", color = EditorMuted, fontSize = 10.sp)
        RangeSlider(
            value = selectedRange,
            onValueChange = { range ->
                val minimumGapMs = minOf(MIN_EDITOR_SEGMENT_MS, clip.durationMs)
                val start = (range.start * 1000).roundToLong().coerceIn(0L, clip.durationMs - minimumGapMs)
                val end = (range.endInclusive * 1000).roundToLong().coerceIn(start + minimumGapMs, clip.durationMs)
                onTrimChange(clip.copy(trimStartMs = start, trimEndMs = end))
            },
            onValueChangeFinished = onTrimFinished,
            enabled = enabled,
            valueRange = 0f..durationSeconds
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            Text("Entrada ${formatEditorTime(clip.trimStartMs)}", color = EditorMuted, fontSize = 10.sp)
            Spacer(Modifier.weight(1f))
            Text("Salida ${formatEditorTime(clip.trimEndMs)}", color = EditorMuted, fontSize = 10.sp)
        }
        Spacer(Modifier.height(13.dp))
        Button(
            onClick = onSplit,
            enabled = enabled && canSplit,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = EditorAccent),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("Dividir en ${formatEditorTime(previewPositionMs - clip.trimStartMs)}", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            OutlinedButton(
                onClick = onMoveEarlier,
                enabled = enabled && canMoveEarlier,
                modifier = Modifier.weight(1f),
                border = BorderStroke(1.dp, EditorLine),
                shape = RoundedCornerShape(10.dp)
            ) { Text("← Antes", fontSize = 11.sp) }
            OutlinedButton(
                onClick = onMoveLater,
                enabled = enabled && canMoveLater,
                modifier = Modifier.weight(1f),
                border = BorderStroke(1.dp, EditorLine),
                shape = RoundedCornerShape(10.dp)
            ) { Text("Después →", fontSize = 11.sp) }
        }
        TextButton(onClick = onDelete, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
            Text("Quitar segmento", color = EditorDanger, fontSize = 11.sp)
        }
    }
}

@Composable
private fun ColorSection(
    color: EditorColorSettings,
    enabled: Boolean,
    onChange: (EditorColorSettings) -> Unit,
    onChangeFinished: () -> Unit,
    onReset: () -> Unit
) {
    EditorPanel(title = "COLOR DEL SEGMENTO") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FilterChip(
                selected = color.grayscale,
                onClick = { onChange(color.copy(grayscale = !color.grayscale)); onChangeFinished() },
                enabled = enabled,
                label = { Text("Blanco y negro") }
            )
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onReset, enabled = enabled) { Text("Restablecer", color = EditorMuted, fontSize = 10.sp) }
        }
        EditorSlider("Saturación", color.saturation, 0f..2f, enabled, onChange = { onChange(color.copy(saturation = it)) }, onFinished = onChangeFinished)
        EditorSlider("Exposición", color.exposure, -2f..2f, enabled, onChange = { onChange(color.copy(exposure = it)) }, onFinished = onChangeFinished)
        EditorSlider("Contraste", color.contrast, -1f..1f, enabled, onChange = { onChange(color.copy(contrast = it)) }, onFinished = onChangeFinished)
        EditorSlider("Sombras", color.shadows, -1f..1f, enabled, onChange = { onChange(color.copy(shadows = it)) }, onFinished = onChangeFinished)
        EditorSlider("Luces", color.highlights, -1f..1f, enabled, onChange = { onChange(color.copy(highlights = it)) }, onFinished = onChangeFinished)
        Spacer(Modifier.height(6.dp))
        Text("Tinte de sombras", color = EditorText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        EditorSlider("Tono", color.shadowHue, 0f..359f, enabled, valueText = "${color.shadowHue.roundToInt()}°", onChange = { onChange(color.copy(shadowHue = it)) }, onFinished = onChangeFinished)
        EditorSlider("Intensidad", color.shadowTint, 0f..1f, enabled, onChange = { onChange(color.copy(shadowTint = it)) }, onFinished = onChangeFinished)
        Spacer(Modifier.height(6.dp))
        Text("Tinte de luces", color = EditorText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        EditorSlider("Tono", color.highlightHue, 0f..359f, enabled, valueText = "${color.highlightHue.roundToInt()}°", onChange = { onChange(color.copy(highlightHue = it)) }, onFinished = onChangeFinished)
        EditorSlider("Intensidad", color.highlightTint, 0f..1f, enabled, onChange = { onChange(color.copy(highlightTint = it)) }, onFinished = onChangeFinished)
        Text("Los filtros se aplican al MP4 final.", color = EditorMuted, fontSize = 9.sp)
    }
}

@Composable
private fun MusicSection(
    music: EditorMusicTrack?,
    projectDurationMs: Long,
    enabled: Boolean,
    onPick: () -> Unit,
    onChange: (EditorMusicTrack) -> Unit,
    onChangeFinished: () -> Unit,
    onRemove: () -> Unit
) {
    EditorPanel(title = "PISTA DE MÚSICA") {
        if (music == null) {
            Text("Agregá un MP3 para acompañar toda la secuencia.", color = EditorMuted, fontSize = 11.sp)
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onPick, enabled = enabled, modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, MusicAccent)) {
                Text("Agregar música MP3", color = MusicAccent, fontWeight = FontWeight.Bold)
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(music.name, color = EditorText, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(formatEditorTime(music.trimmedDurationMs), color = MusicAccent, fontSize = 9.sp)
                }
                TextButton(onClick = onPick, enabled = enabled) { Text("Cambiar", color = MusicAccent, fontSize = 10.sp) }
            }
            Spacer(Modifier.height(8.dp))
            Text("Recorte del MP3", color = EditorMuted, fontSize = 10.sp)
            RangeSlider(
                value = (music.trimStartMs / 1000f)..(music.trimEndMs / 1000f),
                onValueChange = { range ->
                    val gap = minOf(MIN_EDITOR_SEGMENT_MS, music.durationMs)
                    val start = (range.start * 1000).roundToLong().coerceIn(0L, music.durationMs - gap)
                    val end = (range.endInclusive * 1000).roundToLong().coerceIn(start + gap, music.durationMs)
                    onChange(music.copy(trimStartMs = start, trimEndMs = end))
                },
                onValueChangeFinished = onChangeFinished,
                enabled = enabled,
                valueRange = 0f..(music.durationMs / 1000f).coerceAtLeast(0.1f)
            )
            val maxStartSeconds = (projectDurationMs / 1000f).coerceAtLeast(0.1f)
            EditorSlider(
                "Inicio en la historia",
                music.timelineStartMs.coerceAtMost(projectDurationMs) / 1000f,
                0f..maxStartSeconds,
                enabled,
                valueText = formatEditorTime(music.timelineStartMs),
                onChange = { onChange(music.copy(timelineStartMs = (it * 1000).roundToLong())) },
                onFinished = onChangeFinished
            )
            EditorSlider("Volumen", music.volume, 0f..1f, enabled, valueText = "${(music.volume * 100).roundToInt()}%", onChange = { onChange(music.copy(volume = it)) }, onFinished = onChangeFinished)
            val maxFadeSeconds = (music.trimmedDurationMs / 1000f).coerceAtLeast(0.1f)
            EditorSlider("Fundido de entrada", music.fadeInMs / 1000f, 0f..maxFadeSeconds, enabled, valueText = formatEditorTime(music.fadeInMs), onChange = { onChange(music.copy(fadeInMs = (it * 1000).roundToLong())) }, onFinished = onChangeFinished)
            EditorSlider("Fundido de salida", music.fadeOutMs / 1000f, 0f..maxFadeSeconds, enabled, valueText = formatEditorTime(music.fadeOutMs), onChange = { onChange(music.copy(fadeOutMs = (it * 1000).roundToLong())) }, onFinished = onChangeFinished)
            TextButton(onClick = onRemove, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
                Text("Quitar música", color = EditorDanger, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun ExportSection(
    outputWidth: Int,
    enabled: Boolean,
    exporting: Boolean,
    progress: EditorExportProgress?,
    message: String?,
    hasResult: Boolean,
    onWidthChange: (Int) -> Unit,
    onWidthFinished: () -> Unit,
    onExport: () -> Unit,
    onCancel: () -> Unit,
    onOpen: () -> Unit
) {
    EditorPanel(title = "EXPORTAR MONTAJE") {
        Text("Un solo MP4 · MPEG-4 + AAC · 30 FPS", color = EditorMuted, fontSize = 10.sp)
        EditorSlider(
            label = "Ancho de salida",
            value = outputWidth.toFloat(),
            range = 320f..1920f,
            enabled = enabled && !exporting,
            valueText = "$outputWidth px",
            onChange = { onWidthChange(((it.roundToInt() / 2) * 2).coerceIn(320, 1920)) },
            onFinished = onWidthFinished
        )
        progress?.let {
            Text(it.message, color = EditorText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            if (it.fraction == null) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = EditorAccent)
            } else {
                LinearProgressIndicator(progress = it.fraction, modifier = Modifier.fillMaxWidth(), color = EditorAccent)
                Spacer(Modifier.height(4.dp))
                Text("${(it.fraction * 100).roundToInt()}%", color = EditorMuted, fontSize = 9.sp)
            }
            Spacer(Modifier.height(10.dp))
        }
        Button(
            onClick = if (exporting) onCancel else onExport,
            enabled = exporting || enabled,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = if (exporting) EditorDanger else EditorAccent),
            shape = RoundedCornerShape(11.dp)
        ) {
            Text(if (exporting) "Cancelar exportación" else "Exportar montaje MP4", fontWeight = FontWeight.Black)
        }
        message?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, color = if (hasResult) MusicAccent else EditorMuted, fontSize = 10.sp, lineHeight = 15.sp)
        }
        if (hasResult && !exporting) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onOpen, modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, MusicAccent)) {
                Text("Abrir MP4 exportado", color = MusicAccent)
            }
        }
    }
}

@Composable
private fun EditorPanel(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = EditorSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, EditorLine)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, color = EditorAccent, fontSize = 10.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun EditorSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    enabled: Boolean,
    valueText: String = String.format(Locale.getDefault(), "%.2f", value),
    onChange: (Float) -> Unit,
    onFinished: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = EditorMuted, fontSize = 10.sp)
        Spacer(Modifier.weight(1f))
        Text(valueText, color = EditorText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
    Slider(
        value = value.coerceIn(range.start, range.endInclusive),
        onValueChange = onChange,
        onValueChangeFinished = onFinished,
        enabled = enabled,
        valueRange = range,
        modifier = Modifier.fillMaxWidth().height(34.dp)
    )
}

private fun EditorProject.replaceClip(updated: EditorClip): EditorProject = copy(
    clips = clips.map { if (it.id == updated.id) updated else it }
)

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

private fun Context.editorMusic(uri: Uri): EditorMusicTrack? {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(this, uri)
        val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            ?.toLongOrNull()?.takeIf { it > 0L } ?: return null
        EditorMusicTrack(uri = uri.toString(), name = editorDisplayName(uri), durationMs = duration)
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
    return uri.lastPathSegment ?: "Archivo multimedia"
}

private fun Context.persistReadPermission(uri: Uri) {
    try {
        contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    } catch (_: SecurityException) {
        // The current session grant is still usable.
    }
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

private fun editorMusicPickerIntent(initialLocation: Uri?): Intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
    addCategory(Intent.CATEGORY_OPENABLE)
    type = "audio/*"
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        initialLocation?.let { putExtra(DocumentsContract.EXTRA_INITIAL_URI, it) }
    }
    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
}

private fun editorExportIntent(): Intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
    addCategory(Intent.CATEGORY_OPENABLE)
    type = "video/mp4"
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
    putExtra(Intent.EXTRA_TITLE, "Kyro_montaje_$timestamp.mp4")
    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
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

private fun Context.openExportedVideo(uri: Uri) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "video/mp4")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(this, "No hay una aplicación disponible para abrir el MP4", Toast.LENGTH_LONG).show()
    }
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
