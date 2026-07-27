package com.kyro.avi2mp4

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EditorProjectTest {
    @Test
    fun savesAndRestoresCompleteNonDestructiveProject() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = EditorProjectStore(context, "editor_project_complete_test")
        val first = sampleClip("first").copy(
            color = EditorColorSettings(
                grayscale = true,
                exposure = 0.4f,
                contrast = 0.3f,
                shadowHue = 210f,
                shadowTint = 0.25f
            )
        )
        val second = sampleClip("second").copy(durationMs = 4_000L, trimStartMs = 0L, trimEndMs = 4_000L)
        val music = EditorMusicTrack(
            uri = "content://test/music.mp3",
            name = "Música.mp3",
            durationMs = 30_000L,
            trimStartMs = 2_000L,
            trimEndMs = 18_000L,
            timelineStartMs = 500L,
            volume = 0.55f,
            fadeInMs = 700L,
            fadeOutMs = 900L
        )
        val project = EditorProject(listOf(first, second), music, outputWidth = 1280)

        try {
            store.clear()
            store.saveProject(project)
            assertEquals(project, store.loadProject())
            assertEquals(11_500L, store.loadProject().durationMs)
        } finally {
            store.clear()
        }

        assertTrue(store.loadProject().clips.isEmpty())
    }

    @Test
    fun migratesVersion15ClipArray() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val preferenceName = "editor_project_migration_test"
        val store = EditorProjectStore(context, preferenceName)
        val preferences = context.getSharedPreferences(preferenceName, 0)
        val legacy = JSONArray().put(
            JSONObject()
                .put("id", "legacy")
                .put("uri", "content://test/legacy.mp4")
                .put("name", "Anterior.mp4")
                .put("durationMs", 9_000L)
                .put("trimStartMs", 1_000L)
                .put("trimEndMs", 7_000L)
        )

        try {
            store.clear()
            preferences.edit().putString("clips", legacy.toString()).commit()
            val migrated = store.loadProject()
            assertEquals("legacy", migrated.clips.single().id)
            assertEquals(EditorColorSettings(), migrated.clips.single().color)
            assertNull(migrated.music)
            assertEquals(1010, migrated.outputWidth)
            assertFalse(preferences.contains("clips"))
            assertTrue(preferences.contains("project"))
        } finally {
            store.clear()
        }
    }

    @Test
    fun retainsMalformedPersistedDataForRecovery() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val preferenceName = "editor_project_malformed_test"
        val store = EditorProjectStore(context, preferenceName)
        val preferences = context.getSharedPreferences(preferenceName, 0)
        try {
            store.clear()
            preferences.edit().putString("clips", "not-json").commit()
            assertTrue(store.loadProject().clips.isEmpty())
            assertEquals("not-json", preferences.getString("clips", null))
            assertFalse(preferences.contains("project"))

            store.clear()
            preferences.edit().putString("project", "{broken").commit()
            assertTrue(store.loadProject().clips.isEmpty())
            assertEquals("{broken", preferences.getString("project", null))
        } finally {
            store.clear()
        }
    }

    @Test
    fun splitAndHistoryPreserveMediaAndSupportUndoRedo() {
        val clip = sampleClip("original")
        val initial = EditorSnapshot(EditorProject(listOf(clip)), clip.id)
        val splitProject = splitEditorClip(initial.project, clip.id, 4_000L, "right")
        requireNotNull(splitProject)

        assertEquals(listOf("original", "right"), splitProject.clips.map(EditorClip::id))
        assertEquals(listOf(3_000L, 4_500L), splitProject.clips.map(EditorClip::trimmedDurationMs))
        assertEquals(clip.uri, splitProject.clips.last().uri)
        assertNull(splitEditorClip(initial.project, clip.id, clip.trimStartMs + 50L, "too-close"))

        val committed = EditorHistory(initial).commit(EditorSnapshot(splitProject, "right"))
        assertTrue(committed.canUndo)
        val undone = committed.undo()
        assertEquals(initial, undone.present)
        assertTrue(undone.canRedo)
        assertEquals(splitProject, undone.redo().present.project)
    }

    private fun sampleClip(id: String) = EditorClip(
        id = id,
        uri = "content://test/$id.mp4",
        name = "$id.mp4",
        durationMs = 10_000L,
        trimStartMs = 1_000L,
        trimEndMs = 8_500L
    )
}
