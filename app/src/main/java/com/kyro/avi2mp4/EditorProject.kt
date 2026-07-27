package com.kyro.avi2mp4

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

internal data class EditorClip(
    val id: String,
    val uri: String,
    val name: String,
    val durationMs: Long,
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = durationMs
) {
    val trimmedDurationMs: Long
        get() = (trimEndMs - trimStartMs).coerceAtLeast(0L)
}

internal class EditorProjectStore(
    context: Context,
    preferenceName: String = "editor_project"
) {
    private val preferences = context.getSharedPreferences(preferenceName, Context.MODE_PRIVATE)

    fun loadClips(): List<EditorClip> {
        val encoded = preferences.getString(KEY_CLIPS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(encoded)
            buildList {
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
                            trimEndMs = end
                        )
                    )
                }
            }
        }.getOrElse { emptyList() }
    }

    fun saveClips(clips: List<EditorClip>) {
        val array = JSONArray()
        clips.forEach { clip ->
            array.put(
                JSONObject()
                    .put("id", clip.id)
                    .put("uri", clip.uri)
                    .put("name", clip.name)
                    .put("durationMs", clip.durationMs)
                    .put("trimStartMs", clip.trimStartMs)
                    .put("trimEndMs", clip.trimEndMs)
            )
        }
        preferences.edit().putString(KEY_CLIPS, array.toString()).apply()
    }

    fun clear() {
        preferences.edit().remove(KEY_CLIPS).apply()
    }

    private companion object {
        const val KEY_CLIPS = "clips"
    }
}

internal fun moveEditorClip(clips: List<EditorClip>, fromIndex: Int, toIndex: Int): List<EditorClip> {
    if (fromIndex !in clips.indices || toIndex !in clips.indices || fromIndex == toIndex) return clips
    return clips.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
}
