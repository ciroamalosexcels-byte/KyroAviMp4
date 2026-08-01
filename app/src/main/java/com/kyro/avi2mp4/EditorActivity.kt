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
import android.widget.Toast
import android.view.SurfaceView
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
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
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
private const val TIMELINE_DP_PER_SECOND = 64f

private enum class EditorTool(val label: String, val symbol: String) {
    CLIP("Editar", "✂"),
    AUDIO("Audio", "♫"),
    COLOR("Ajustar", "◐"),
    EXPORT("Exportar", "↑")
}

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
    var activeTool by remember { mutableStateOf(EditorTool.CLIP) }
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
    val realtimePlayer = remember { EditorRealtimePlayer(context) }
    var previewReady by remember { mutableStateOf(false) }
    var previewPlaying by remember { mutableStateOf(false) }
    var previewError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val visibleSnapshot = draft ?: history.present
    val project = visibleSnapshot.project
    val clips = project.clips
    val selectedClip = clips.firstOrNull { it.id == visibleSnapshot.selectedClipId }
    val selectedIndex = clips.indexOfFirst { it.id == visibleSnapshot.selectedClipId }
    val playheadLocation = project.locateTimelinePosition(previewPositionMs)
    val busy = importing || exportJob != null

    fun save(updatedHistory: EditorHistory) {
        projectStore.saveProject(updatedHistory.present.project)
    }

    fun seekPreview(positionMs: Long, durationMs: Long = project.durationMs) {
        val bounded = positionMs.coerceIn(0L, durationMs)
        previewPositionMs = bounded
        realtimePlayer.seekTo(bounded)
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
        seekPreview(selected.present.project.timelineStartOf(clipId) ?: 0L, selected.present.project.durationMs)
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
        seekPreview(
            updated.present.project.timelineStartOf(updated.present.selectedClipId.orEmpty()) ?: 0L,
            updated.present.project.durationMs
        )
        save(updated)
    }

    fun redo() {
        val baseHistory = draft?.let(history::commit) ?: history
        val updated = baseHistory.redo()
        draft = null
        history = updated
        seekPreview(
            updated.present.project.timelineStartOf(updated.present.selectedClipId.orEmpty()) ?: 0L,
            updated.present.project.durationMs
        )
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
                withContext(Dispatchers.IO) { context.editorClip(uri) }?.let(imported::add)
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
        val exportProject = pendingExportProject
            ?: projectStore.loadProject().takeIf { it.clips.isNotEmpty() }
            ?: return@rememberLauncherForActivityResult
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

    SideEffect {
        realtimePlayer.updateParameters(project)
    }

    LaunchedEffect(history.present.project) {
        previewError = null
        realtimePlayer.setProject(history.present.project, previewPositionMs)
    }

    LaunchedEffect(realtimePlayer, project.durationMs) {
        while (true) {
            delay(50)
            val position = realtimePlayer.player.currentPosition.coerceIn(0L, project.durationMs)
            previewPositionMs = position
            val activeClipId = project.locateTimelinePosition(position)?.clip?.id
            if (draft == null && activeClipId != null && activeClipId != history.present.selectedClipId) {
                history = history.copy(present = history.present.copy(selectedClipId = activeClipId))
            }
        }
    }

    DisposableEffect(realtimePlayer) {
        realtimePlayer.setListener(object : EditorRealtimePlayer.Listener {
            override fun onStateChanged(ready: Boolean, playing: Boolean) {
                previewReady = ready
                previewPlaying = playing
            }

            override fun onError(message: String) {
                previewReady = false
                previewError = message
            }
        })
        onDispose {
            exportJob?.cancel()
            realtimePlayer.setListener(null)
            realtimePlayer.release()
        }
    }

    val lifecycleOwner = context as? LifecycleOwner
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) realtimePlayer.player.pause()
        }
        lifecycleOwner?.lifecycle?.addObserver(observer)
        onDispose { lifecycleOwner?.lifecycle?.removeObserver(observer) }
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
                    EditorPreview(
                        realtimePlayer = realtimePlayer,
                        ready = previewReady,
                        error = previewError
                    )
                    PreviewTransport(
                        positionMs = previewPositionMs,
                        durationMs = project.durationMs,
                        clipCount = project.clips.size,
                        previewReady = previewReady,
                        playing = previewPlaying,
                        onSeekStart = { seekPreview(0L) },
                        onSeekEnd = { seekPreview(project.durationMs) },
                        onTogglePlayback = realtimePlayer::togglePlayback
                    )
                    TimelineSection(
                        project = project,
                        selectedClipId = visibleSnapshot.selectedClipId,
                        playheadPositionMs = previewPositionMs,
                        enabled = !busy,
                        onSelect = {
                            activeTool = EditorTool.CLIP
                            selectClip(it.id)
                        },
                        onMove = { clipId, offset ->
                            val from = project.clips.indexOfFirst { it.id == clipId }
                            val to = (from + offset).coerceIn(0, project.clips.lastIndex)
                            if (from >= 0 && to != from) {
                                commitProject(project.copy(clips = moveEditorClip(project.clips, from, to)), clipId)
                            }
                        },
                        onClipTrimChange = { updated ->
                            updateDraft { current -> current.replaceClip(updated) }
                        },
                        onClipTrimFinished = ::commitDraft,
                        onMusicChange = { updated -> updateDraft { it.copy(music = updated) } },
                        onMusicFinished = ::commitDraft,
                        onSeek = { position -> seekPreview(position) },
                        onScrubbingChanged = realtimePlayer::setScrubbing
                    )
                    ToolDock(activeTool = activeTool, onSelect = { activeTool = it })
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        when (activeTool) {
                            EditorTool.CLIP -> selectedClip?.let { clip ->
                                item {
                                    val splitSourcePosition = playheadLocation
                                        ?.takeIf { it.clip.id == clip.id }
                                        ?.sourcePositionMs
                                    ClipEditSection(
                                        clip = clip,
                                        splitSourcePositionMs = splitSourcePosition,
                                        enabled = !busy,
                                        onSplit = {
                                            val location = project.locateTimelinePosition(previewPositionMs)
                                            val updated = splitEditorClip(
                                                project,
                                                location?.clip?.id.orEmpty(),
                                                location?.sourcePositionMs ?: -1L,
                                                UUID.randomUUID().toString()
                                            )
                                            if (updated == null || location == null) {
                                                Toast.makeText(context, "Mové el cabezal lejos de los bordes para dividir", Toast.LENGTH_SHORT).show()
                                            } else {
                                                val right = updated.clips.getOrNull(location.clipIndex + 1)
                                                commitProject(updated, right?.id)
                                            }
                                        },
                                        onDuplicate = {
                                            val duplicateId = UUID.randomUUID().toString()
                                            val updated = duplicateEditorClip(project, clip.id, duplicateId)
                                            if (updated != null) commitProject(updated, duplicateId)
                                        },
                                        onDelete = {
                                            val updated = clips.toMutableList().apply { removeAt(selectedIndex) }
                                            val nextSelection = updated.getOrNull(selectedIndex.coerceAtMost(updated.lastIndex))?.id
                                            commitProject(project.copy(clips = updated), nextSelection)
                                        }
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
                            }
                            EditorTool.COLOR -> selectedClip?.let { clip ->
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
                            EditorTool.AUDIO -> item {
                                MusicSection(
                                    music = project.music,
                                    enabled = !busy,
                                    onPick = { pickMusic.launch(editorMusicPickerIntent(lastMusicLocation)) },
                                    onChange = { music -> updateDraft { it.copy(music = music) } },
                                    onChangeFinished = ::commitDraft,
                                    onRemove = { commitProject(project.copy(music = null)) }
                                )
                            }
                            EditorTool.EXPORT -> item {
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
private fun EditorPreview(
    realtimePlayer: EditorRealtimePlayer,
    ready: Boolean,
    error: String?
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Black,
        border = BorderStroke(1.dp, EditorLine),
        shadowElevation = 8.dp
    ) {
        Box(Modifier.fillMaxWidth().height(190.dp), contentAlignment = Alignment.Center) {
            AndroidView(
                factory = { context ->
                    SurfaceView(context).also(realtimePlayer::setSurfaceView)
                },
                modifier = Modifier.fillMaxSize()
            )
            if (!ready && error == null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("PREVIEW EN TIEMPO REAL", color = EditorAccent, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(7.dp))
                    Text("Preparando clips y música...", color = EditorMuted, fontSize = 11.sp)
                }
            }
            error?.let {
                Surface(
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(10.dp),
                    color = Color(0xFF241A1D),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, EditorDanger.copy(alpha = 0.55f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(it, modifier = Modifier.weight(1f), color = EditorDanger, fontSize = 9.sp, maxLines = 3)
                    }
                }
            }
            if (!ready && error == null) {
                LinearProgressIndicator(
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(3.dp),
                    color = EditorAccent,
                    trackColor = EditorLine
                )
            }
        }
    }
}

@Composable
private fun PreviewTransport(
    positionMs: Long,
    durationMs: Long,
    clipCount: Int,
    previewReady: Boolean,
    playing: Boolean,
    onSeekStart: () -> Unit,
    onSeekEnd: () -> Unit,
    onTogglePlayback: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(42.dp).background(Color(0xFF0D0E11)).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onSeekStart, contentPadding = PaddingValues(horizontal = 5.dp)) {
            Text("|◀", color = EditorMuted, fontSize = 11.sp)
        }
        Text(
            "${formatEditorTime(positionMs)} / ${formatEditorTime(durationMs)}",
            color = EditorText,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.weight(1f))
        TextButton(
            onClick = onTogglePlayback,
            enabled = previewReady,
            contentPadding = PaddingValues(horizontal = 12.dp)
        ) {
            Text(if (playing) "Ⅱ" else "▶", color = if (previewReady) EditorText else EditorMuted, fontSize = 17.sp)
        }
        Text(
            if (previewReady) "EN VIVO · $clipCount clips" else "$clipCount clips",
            color = if (previewReady) MusicAccent else EditorMuted,
            fontSize = 8.sp,
            fontWeight = FontWeight.Black
        )
        TextButton(onClick = onSeekEnd, contentPadding = PaddingValues(horizontal = 5.dp)) {
            Text("▶|", color = EditorMuted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun ToolDock(activeTool: EditorTool, onSelect: (EditorTool) -> Unit) {
    Surface(color = Color(0xFF141519), shadowElevation = 10.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EditorTool.entries.forEach { tool ->
                val selected = tool == activeTool
                Column(
                    modifier = Modifier.weight(1f).fillMaxSize().clickable { onSelect(tool) }.padding(top = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(tool.symbol, color = if (selected) EditorAccent else EditorMuted, fontSize = 18.sp)
                    Text(tool.label, color = if (selected) EditorText else EditorMuted, fontSize = 9.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                    Spacer(Modifier.height(5.dp))
                    Box(Modifier.width(28.dp).height(2.dp).background(if (selected) EditorAccent else Color.Transparent))
                }
            }
        }
    }
}

@Composable
private fun TimelineSection(
    project: EditorProject,
    selectedClipId: String?,
    playheadPositionMs: Long,
    enabled: Boolean,
    onSelect: (EditorClip) -> Unit,
    onMove: (String, Int) -> Unit,
    onClipTrimChange: (EditorClip) -> Unit,
    onClipTrimFinished: () -> Unit,
    onMusicChange: (EditorMusicTrack) -> Unit,
    onMusicFinished: () -> Unit,
    onSeek: (Long) -> Unit,
    onScrubbingChanged: (Boolean) -> Unit
) {
    val density = LocalDensity.current
    val scrollState = rememberScrollState()
    var viewportWidthPx by remember { mutableIntStateOf(0) }
    val clipWidths = project.clips.map { clip ->
        (clip.trimmedDurationMs / 1000f * TIMELINE_DP_PER_SECOND).dp
    }
    val timelineWidth = (project.durationMs / 1000f * TIMELINE_DP_PER_SECOND).dp
    val pxPerMs = with(density) { TIMELINE_DP_PER_SECOND.dp.toPx() / 1000f }
    val playheadOffsetPx = playheadPositionMs * pxPerMs

    LaunchedEffect(playheadOffsetPx, viewportWidthPx, scrollState.maxValue, scrollState.isScrollInProgress) {
        if (viewportWidthPx > 0 && !scrollState.isScrollInProgress) {
            scrollState.scrollTo(playheadOffsetPx.roundToInt().coerceIn(0, scrollState.maxValue))
        }
    }
    LaunchedEffect(scrollState, pxPerMs, project.durationMs) {
        snapshotFlow { scrollState.isScrollInProgress to scrollState.value }
            .collect { (scrolling, value) ->
                onScrubbingChanged(scrolling)
                if (scrolling && pxPerMs > 0f) {
                    onSeek((value / pxPerMs).roundToLong().coerceIn(0L, project.durationMs))
                }
            }
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF0C0D10),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, EditorLine)
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("VIDEO", color = EditorMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text(
                    "${formatEditorTimePrecise(playheadPositionMs)} · arrastrá bordes para recortar",
                    color = EditorMuted,
                    fontSize = 8.sp
                )
            }
            Spacer(Modifier.height(7.dp))
            Box(modifier = Modifier.fillMaxWidth().onSizeChanged { viewportWidthPx = it.width }) {
                Row(modifier = Modifier.horizontalScroll(scrollState)) {
                    val sidePadding = with(density) { (viewportWidthPx / 2f).toDp() }
                    Spacer(Modifier.width(sidePadding))
                    Column(modifier = Modifier.width(timelineWidth.coerceAtLeast(1.dp))) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            project.clips.forEachIndexed { index, clip ->
                                TimelineClipCard(
                                    clip = clip,
                                    index = index,
                                    cardWidth = clipWidths[index],
                                    selected = clip.id == selectedClipId,
                                    enabled = enabled,
                                    onClick = { onSelect(clip) },
                                    onMoveBy = { offset -> onMove(clip.id, offset) },
                                    onTrimChange = onClipTrimChange,
                                    onTrimFinished = onClipTrimFinished
                                )
                            }
                        }
                        Spacer(Modifier.height(7.dp))
                        HorizontalDivider(color = EditorLine)
                        Spacer(Modifier.height(5.dp))
                        MusicTimeline(
                            music = project.music,
                            projectDurationMs = project.durationMs,
                            enabled = enabled,
                            onChange = onMusicChange,
                            onChangeFinished = onMusicFinished
                        )
                    }
                    Spacer(Modifier.width(sidePadding))
                }
                Box(
                    modifier = Modifier.align(Alignment.TopCenter).zIndex(4f).width(2.dp).height(144.dp)
                        .background(EditorAccent)
                )
            }
        }
    }
}

@Composable
private fun TimelineClipCard(
    clip: EditorClip,
    index: Int,
    cardWidth: Dp,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    onMoveBy: (Int) -> Unit,
    onTrimChange: (EditorClip) -> Unit,
    onTrimFinished: () -> Unit
) {
    var dragX by remember(clip.id) { mutableStateOf(0f) }
    var dragging by remember(clip.id) { mutableStateOf(false) }
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val stepPx = with(density) { cardWidth.coerceAtLeast(72.dp).toPx() }
    Box(
        modifier = Modifier.width(cardWidth).height(92.dp).zIndex(if (dragging || selected) 2f else 0f)
            .graphicsLayer { translationX = dragX }
    ) {
        Surface(
            modifier = Modifier.fillMaxSize()
                .pointerInput(clip.id, enabled) {
                    if (enabled) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                dragging = true
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
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
            shape = RoundedCornerShape(4.dp),
            border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) EditorAccent else EditorLine),
            shadowElevation = if (dragging) 12.dp else 0.dp
        ) {
            Column {
                TimelineThumbnail(clip, index)
                Text(
                    "${index + 1} · ${formatEditorTimePrecise(clip.trimmedDurationMs)}",
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
                    color = EditorText,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (selected && enabled) {
            ClipTrimHandle(
                clip = clip,
                startEdge = true,
                modifier = Modifier.align(Alignment.CenterStart),
                onChange = onTrimChange,
                onFinished = onTrimFinished
            )
            ClipTrimHandle(
                clip = clip,
                startEdge = false,
                modifier = Modifier.align(Alignment.CenterEnd),
                onChange = onTrimChange,
                onFinished = onTrimFinished
            )
        }
    }
}

@Composable
private fun ClipTrimHandle(
    clip: EditorClip,
    startEdge: Boolean,
    modifier: Modifier,
    onChange: (EditorClip) -> Unit,
    onFinished: () -> Unit
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = modifier.fillMaxHeight().width(14.dp).zIndex(6f)
            .background(EditorAccent.copy(alpha = 0.82f), RoundedCornerShape(3.dp))
            .pointerInput(clip.id, startEdge) {
                var baseline = clip
                var accumulatedPx = 0f
                detectHorizontalDragGestures(
                    onDragStart = {
                        baseline = clip
                        accumulatedPx = 0f
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    },
                    onHorizontalDrag = { change, amount ->
                        change.consume()
                        accumulatedPx += amount
                        val deltaMs = with(density) {
                            accumulatedPx.toDp().value / TIMELINE_DP_PER_SECOND * 1_000f
                        }.roundToLong()
                        val minimumDuration = minOf(MIN_EDITOR_SEGMENT_MS, baseline.trimmedDurationMs)
                        val updated = if (startEdge) {
                            baseline.copy(
                                trimStartMs = (baseline.trimStartMs + deltaMs)
                                    .coerceIn(0L, baseline.trimEndMs - minimumDuration)
                            )
                        } else {
                            baseline.copy(
                                trimEndMs = (baseline.trimEndMs + deltaMs)
                                    .coerceIn(baseline.trimStartMs + minimumDuration, baseline.durationMs)
                            )
                        }
                        onChange(updated)
                    },
                    onDragEnd = onFinished,
                    onDragCancel = {
                        onChange(baseline)
                        onFinished()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.width(2.dp).height(26.dp).background(Color(0xFF231700)))
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
private fun MusicTimeline(
    music: EditorMusicTrack?,
    projectDurationMs: Long,
    enabled: Boolean,
    onChange: (EditorMusicTrack) -> Unit,
    onChangeFinished: () -> Unit
) {
    if (music == null) {
        Box(
            modifier = Modifier.fillMaxWidth().height(38.dp)
                .background(EditorSurface, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.CenterStart
        ) {
            Text("+ Agregar audio", modifier = Modifier.padding(horizontal = 10.dp), color = EditorMuted, fontSize = 9.sp)
        }
        return
    }
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val start = (music.timelineStartMs / 1000f * TIMELINE_DP_PER_SECOND).dp
    val visibleDuration = minOf(music.trimmedDurationMs, (projectDurationMs - music.timelineStartMs).coerceAtLeast(0L))
    val width = (visibleDuration / 1000f * TIMELINE_DP_PER_SECOND).dp
    Box(modifier = Modifier.fillMaxWidth().height(38.dp).background(EditorSurface, RoundedCornerShape(5.dp))) {
        Box(
            modifier = Modifier.offset(x = start).width(width).fillMaxHeight().zIndex(2f)
                .background(MusicAccent.copy(alpha = 0.34f), RoundedCornerShape(5.dp))
                .pointerInput(music.uri, enabled) {
                    if (enabled) {
                        var baseline = music
                        var accumulatedPx = 0f
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                baseline = music
                                accumulatedPx = 0f
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            onDrag = { change, amount ->
                                change.consume()
                                accumulatedPx += amount.x
                                val deltaMs = with(density) {
                                    accumulatedPx.toDp().value / TIMELINE_DP_PER_SECOND * 1_000f
                                }.roundToLong()
                                onChange(
                                    baseline.copy(
                                        timelineStartMs = (baseline.timelineStartMs + deltaMs)
                                            .coerceIn(0L, (projectDurationMs - MIN_EDITOR_SEGMENT_MS).coerceAtLeast(0L))
                                    )
                                )
                            },
                            onDragEnd = onChangeFinished,
                            onDragCancel = { onChange(baseline); onChangeFinished() }
                        )
                    }
                }
                .drawBehind {
                    val center = size.height / 2f
                    repeat(24) { bar ->
                        val x = size.width * bar / 23f
                        val amplitude = size.height * (0.12f + (bar % 5) * 0.045f)
                        drawLine(MusicAccent, androidx.compose.ui.geometry.Offset(x, center - amplitude), androidx.compose.ui.geometry.Offset(x, center + amplitude), 1.dp.toPx())
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text(music.name, color = EditorText, fontSize = 8.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            MusicTrimHandle(music, true, Modifier.align(Alignment.CenterStart), onChange, onChangeFinished)
            MusicTrimHandle(music, false, Modifier.align(Alignment.CenterEnd), onChange, onChangeFinished)
        }
    }
}

@Composable
private fun MusicTrimHandle(
    music: EditorMusicTrack,
    startEdge: Boolean,
    modifier: Modifier,
    onChange: (EditorMusicTrack) -> Unit,
    onFinished: () -> Unit
) {
    val density = LocalDensity.current
    Box(
        modifier = modifier.fillMaxHeight().width(14.dp).zIndex(5f)
            .background(MusicAccent, RoundedCornerShape(4.dp))
            .pointerInput(music.uri, startEdge) {
                var baseline = music
                var accumulatedPx = 0f
                detectHorizontalDragGestures(
                    onDragStart = { baseline = music; accumulatedPx = 0f },
                    onHorizontalDrag = { change, amount ->
                        change.consume()
                        accumulatedPx += amount
                        val deltaMs = with(density) {
                            accumulatedPx.toDp().value / TIMELINE_DP_PER_SECOND * 1_000f
                        }.roundToLong()
                        val minimumDuration = minOf(MIN_EDITOR_SEGMENT_MS, baseline.trimmedDurationMs)
                        val updated = if (startEdge) {
                            val applied = deltaMs.coerceIn(
                                maxOf(-baseline.trimStartMs, -baseline.timelineStartMs),
                                baseline.trimmedDurationMs - minimumDuration
                            )
                            baseline.copy(
                                trimStartMs = baseline.trimStartMs + applied,
                                timelineStartMs = baseline.timelineStartMs + applied
                            )
                        } else {
                            baseline.copy(
                                trimEndMs = (baseline.trimEndMs + deltaMs)
                                    .coerceIn(baseline.trimStartMs + minimumDuration, baseline.durationMs)
                            )
                        }
                        onChange(updated.copy(
                            fadeInMs = updated.fadeInMs.coerceAtMost(updated.trimmedDurationMs),
                            fadeOutMs = updated.fadeOutMs.coerceAtMost(updated.trimmedDurationMs)
                        ))
                    },
                    onDragEnd = onFinished,
                    onDragCancel = { onChange(baseline); onFinished() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.width(2.dp).height(18.dp).background(Color(0xFF123C30)))
    }
}

@Composable
private fun ClipEditSection(
    clip: EditorClip,
    splitSourcePositionMs: Long?,
    enabled: Boolean,
    onSplit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    val splitPosition = splitSourcePositionMs ?: clip.trimStartMs
    val canSplit = splitSourcePositionMs != null &&
        splitPosition - clip.trimStartMs >= MIN_EDITOR_SEGMENT_MS &&
        clip.trimEndMs - splitPosition >= MIN_EDITOR_SEGMENT_MS
    EditorPanel(title = "CLIP SELECCIONADO") {
        Text(clip.name, color = EditorText, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Text("Entrada ${formatEditorTimePrecise(clip.trimStartMs)}", color = EditorMuted, fontSize = 9.sp)
            Spacer(Modifier.weight(1f))
            Text("Salida ${formatEditorTimePrecise(clip.trimEndMs)}", color = EditorMuted, fontSize = 9.sp)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            OutlinedButton(
                onClick = onSplit,
                enabled = enabled && canSplit,
                modifier = Modifier.weight(1f),
                border = BorderStroke(1.dp, EditorAccent),
                shape = RoundedCornerShape(10.dp)
            ) { Text("✂ Dividir", color = EditorAccent, fontSize = 10.sp) }
            OutlinedButton(
                onClick = onDuplicate,
                enabled = enabled,
                modifier = Modifier.weight(1f),
                border = BorderStroke(1.dp, EditorLine),
                shape = RoundedCornerShape(10.dp)
            ) { Text("▣ Duplicar", fontSize = 10.sp) }
            OutlinedButton(
                onClick = onDelete,
                enabled = enabled,
                modifier = Modifier.weight(1f),
                border = BorderStroke(1.dp, EditorDanger.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(10.dp)
            ) { Text("Eliminar", color = EditorDanger, fontSize = 10.sp) }
        }
        Spacer(Modifier.height(6.dp))
        Text("Arrastrá los bordes dorados del bloque para recortar. Mantené pulsado el centro para reordenar.", color = EditorMuted, fontSize = 9.sp)
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
        Text("El color se aplica por GPU mientras movés cada control.", color = EditorMuted, fontSize = 9.sp)
    }
}

@Composable
private fun MusicSection(
    music: EditorMusicTrack?,
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
            Text(
                "Recortá con las manijas verdes y mantené pulsado el bloque para moverlo en la timeline.",
                color = EditorMuted,
                fontSize = 9.sp
            )
            Spacer(Modifier.height(8.dp))
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

private fun formatEditorTimePrecise(milliseconds: Long): String {
    val bounded = milliseconds.coerceAtLeast(0L)
    val totalSeconds = bounded / 1000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    val millis = bounded % 1000L
    return "%02d:%02d.%03d".format(minutes, seconds, millis)
}
