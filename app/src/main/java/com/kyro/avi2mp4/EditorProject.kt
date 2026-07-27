package com.kyro.avi2mp4

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

internal const val MIN_EDITOR_SEGMENT_MS = 100L

internal data class EditorColorSettings(
    val grayscale: Boolean = false,
    val saturation: Float = 1f,
    val exposure: Float = 0f,
    val contrast: Float = 0f,
    val shadows: Float = 0f,
    val highlights: Float = 0f,
    val shadowHue: Float = 30f,
    val shadowTint: Float = 0f,
    val highlightHue: Float = 45f,
    val highlightTint: Float = 0f
)

internal data class EditorClip(
    val id: String,
    val uri: String,
    val name: String,
    val durationMs: Long,
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = durationMs,
    val color: EditorColorSettings = EditorColorSettings()
) {
    val trimmedDurationMs: Long
        get() = (trimEndMs - trimStartMs).coerceAtLeast(0L)
}

internal data class EditorMusicTrack(
    val uri: String,
    val name: String,
    val durationMs: Long,
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = durationMs,
    val timelineStartMs: Long = 0L,
    val volume: Float = 0.35f,
    val fadeInMs: Long = 0L,
    val fadeOutMs: Long = 0L
) {
    val trimmedDurationMs: Long
        get() = (trimEndMs - trimStartMs).coerceAtLeast(0L)
}

internal data class EditorProject(
    val clips: List<EditorClip> = emptyList(),
    val music: EditorMusicTrack? = null,
    val outputWidth: Int = 1010
) {
    val durationMs: Long
        get() = clips.sumOf(EditorClip::trimmedDurationMs)
}

internal data class EditorSnapshot(
    val project: EditorProject,
    val selectedClipId: String?
)

internal data class EditorHistory(
    val present: EditorSnapshot,
    val past: List<EditorSnapshot> = emptyList(),
    val future: List<EditorSnapshot> = emptyList()
) {
    val canUndo: Boolean
        get() = past.isNotEmpty()
    val canRedo: Boolean
        get() = future.isNotEmpty()
}

internal fun EditorHistory.commit(next: EditorSnapshot): EditorHistory {
    if (next.project == present.project) return copy(present = next)
    return EditorHistory(
        present = next,
        past = (past + present).takeLast(MAX_EDITOR_HISTORY),
        future = emptyList()
    )
}

internal fun EditorHistory.undo(): EditorHistory {
    val previous = past.lastOrNull() ?: return this
    return EditorHistory(
        present = previous,
        past = past.dropLast(1),
        future = (future + present).takeLast(MAX_EDITOR_HISTORY)
    )
}

internal fun EditorHistory.redo(): EditorHistory {
    val next = future.lastOrNull() ?: return this
    return EditorHistory(
        present = next,
        past = (past + present).takeLast(MAX_EDITOR_HISTORY),
        future = future.dropLast(1)
    )
}

internal fun splitEditorClip(
    project: EditorProject,
    clipId: String,
    sourcePositionMs: Long,
    rightClipId: String
): EditorProject? {
    val index = project.clips.indexOfFirst { it.id == clipId }
    if (index < 0 || rightClipId.isBlank() || project.clips.any { it.id == rightClipId }) return null
    val clip = project.clips[index]
    if (sourcePositionMs - clip.trimStartMs < MIN_EDITOR_SEGMENT_MS ||
        clip.trimEndMs - sourcePositionMs < MIN_EDITOR_SEGMENT_MS
    ) {
        return null
    }
    val updated = project.clips.toMutableList().apply {
        this[index] = clip.copy(trimEndMs = sourcePositionMs)
        add(index + 1, clip.copy(id = rightClipId, trimStartMs = sourcePositionMs))
    }
    return project.copy(clips = updated)
}

internal class EditorProjectStore(
    context: Context,
    preferenceName: String = "editor_project"
) {
    private val preferences = context.getSharedPreferences(preferenceName, Context.MODE_PRIVATE)

    fun loadProject(): EditorProject {
        preferences.getString(KEY_PROJECT, null)?.let { encoded ->
            return runCatching { decodeProject(JSONObject(encoded)) }.getOrDefault(EditorProject())
        }
        val legacy = preferences.getString(KEY_LEGACY_CLIPS, null) ?: return EditorProject()
        val project = runCatching { EditorProject(clips = decodeClips(JSONArray(legacy))) }
            .getOrNull() ?: return EditorProject()
        saveProject(project)
        return project
    }

    fun saveProject(project: EditorProject) {
        val encoded = encodeProject(project).toString()
        preferences.edit()
            .putString(KEY_PROJECT, encoded)
            .remove(KEY_LEGACY_CLIPS)
            .apply()
    }

    fun clear() {
        preferences.edit().remove(KEY_PROJECT).remove(KEY_LEGACY_CLIPS).apply()
    }

    private fun encodeProject(project: EditorProject): JSONObject = JSONObject()
        .put("schemaVersion", PROJECT_SCHEMA_VERSION)
        .put("outputWidth", project.outputWidth)
        .put("clips", encodeClips(project.clips))
        .put("music", project.music?.let(::encodeMusic) ?: JSONObject.NULL)

    private fun decodeProject(root: JSONObject): EditorProject {
        val version = root.optInt("schemaVersion", 1)
        require(version in 1..PROJECT_SCHEMA_VERSION)
        return EditorProject(
            clips = decodeClips(root.optJSONArray("clips") ?: JSONArray()),
            music = root.optJSONObject("music")?.let(::decodeMusic),
            outputWidth = root.optInt("outputWidth", 1010).coerceIn(320, 3840).let {
                if (it % 2 == 0) it else it - 1
            }
        )
    }

    private fun encodeClips(clips: List<EditorClip>): JSONArray = JSONArray().apply {
        clips.forEach { clip ->
            put(
                JSONObject()
                    .put("id", clip.id)
                    .put("uri", clip.uri)
                    .put("name", clip.name)
                    .put("durationMs", clip.durationMs)
                    .put("trimStartMs", clip.trimStartMs)
                    .put("trimEndMs", clip.trimEndMs)
                    .put("color", encodeColor(clip.color))
            )
        }
    }

    private fun decodeClips(array: JSONArray): List<EditorClip> = buildList {
        repeat(array.length()) { index ->
            val item = array.getJSONObject(index)
            val duration = item.getLong("durationMs").coerceAtLeast(1L)
            val start = item.optLong("trimStartMs", 0L).coerceIn(0L, duration - 1L)
            val end = item.optLong("trimEndMs", duration).coerceIn(start + 1L, duration)
            add(
                EditorClip(
                    id = item.getString("id"),
                    uri = item.getString("uri"),
                    name = item.getString("name"),
                    durationMs = duration,
                    trimStartMs = start,
                    trimEndMs = end,
                    color = item.optJSONObject("color")?.let(::decodeColor) ?: EditorColorSettings()
                )
            )
        }
    }

    private fun encodeMusic(music: EditorMusicTrack): JSONObject = JSONObject()
        .put("uri", music.uri)
        .put("name", music.name)
        .put("durationMs", music.durationMs)
        .put("trimStartMs", music.trimStartMs)
        .put("trimEndMs", music.trimEndMs)
        .put("timelineStartMs", music.timelineStartMs)
        .put("volume", music.volume.toDouble())
        .put("fadeInMs", music.fadeInMs)
        .put("fadeOutMs", music.fadeOutMs)

    private fun decodeMusic(item: JSONObject): EditorMusicTrack {
        val duration = item.getLong("durationMs").coerceAtLeast(1L)
        val start = item.optLong("trimStartMs", 0L).coerceIn(0L, duration - 1L)
        val end = item.optLong("trimEndMs", duration).coerceIn(start + 1L, duration)
        val trimmedDuration = end - start
        return EditorMusicTrack(
            uri = item.getString("uri"),
            name = item.getString("name"),
            durationMs = duration,
            trimStartMs = start,
            trimEndMs = end,
            timelineStartMs = item.optLong("timelineStartMs", 0L).coerceAtLeast(0L),
            volume = item.optDouble("volume", 0.35).toFloat().coerceIn(0f, 1f),
            fadeInMs = item.optLong("fadeInMs", 0L).coerceIn(0L, trimmedDuration),
            fadeOutMs = item.optLong("fadeOutMs", 0L).coerceIn(0L, trimmedDuration)
        )
    }

    private fun encodeColor(color: EditorColorSettings): JSONObject = JSONObject()
        .put("grayscale", color.grayscale)
        .put("saturation", color.saturation.toDouble())
        .put("exposure", color.exposure.toDouble())
        .put("contrast", color.contrast.toDouble())
        .put("shadows", color.shadows.toDouble())
        .put("highlights", color.highlights.toDouble())
        .put("shadowHue", color.shadowHue.toDouble())
        .put("shadowTint", color.shadowTint.toDouble())
        .put("highlightHue", color.highlightHue.toDouble())
        .put("highlightTint", color.highlightTint.toDouble())

    private fun decodeColor(item: JSONObject): EditorColorSettings = EditorColorSettings(
        grayscale = item.optBoolean("grayscale", false),
        saturation = item.optDouble("saturation", 1.0).toFloat().coerceIn(0f, 2f),
        exposure = item.optDouble("exposure", 0.0).toFloat().coerceIn(-2f, 2f),
        contrast = item.optDouble("contrast", 0.0).toFloat().coerceIn(-1f, 1f),
        shadows = item.optDouble("shadows", 0.0).toFloat().coerceIn(-1f, 1f),
        highlights = item.optDouble("highlights", 0.0).toFloat().coerceIn(-1f, 1f),
        shadowHue = normalizeHue(item.optDouble("shadowHue", 30.0).toFloat()),
        shadowTint = item.optDouble("shadowTint", 0.0).toFloat().coerceIn(0f, 1f),
        highlightHue = normalizeHue(item.optDouble("highlightHue", 45.0).toFloat()),
        highlightTint = item.optDouble("highlightTint", 0.0).toFloat().coerceIn(0f, 1f)
    )

    private companion object {
        const val KEY_PROJECT = "project"
        const val KEY_LEGACY_CLIPS = "clips"
        const val PROJECT_SCHEMA_VERSION = 2
    }
}

internal fun moveEditorClip(clips: List<EditorClip>, fromIndex: Int, toIndex: Int): List<EditorClip> {
    if (fromIndex !in clips.indices || toIndex !in clips.indices || fromIndex == toIndex) return clips
    return clips.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
}

private fun normalizeHue(value: Float): Float = if (value.isFinite()) {
    ((value % 360f) + 360f) % 360f
} else {
    0f
}

private const val MAX_EDITOR_HISTORY = 50
