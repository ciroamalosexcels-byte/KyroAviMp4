package com.kyro.avi2mp4

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EditorProjectTest {
    @Test
    fun savesRestoresAndReordersNonDestructiveClips() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = EditorProjectStore(context, "editor_project_test")
        val first = EditorClip(
            id = "first",
            uri = "content://test/first.mp4",
            name = "Primer clip.mp4",
            durationMs = 10_000L,
            trimStartMs = 1_000L,
            trimEndMs = 8_500L
        )
        val second = EditorClip(
            id = "second",
            uri = "content://test/second.mp4",
            name = "Segundo clip.mp4",
            durationMs = 4_000L
        )

        try {
            store.clear()
            store.saveClips(listOf(first, second))

            val restored = store.loadClips()
            assertEquals(listOf(first, second), restored)
            assertEquals(7_500L, restored.first().trimmedDurationMs)

            val reordered = moveEditorClip(restored, fromIndex = 1, toIndex = 0)
            assertEquals(listOf("second", "first"), reordered.map(EditorClip::id))
            assertEquals("content://test/second.mp4", reordered.first().uri)
        } finally {
            store.clear()
        }

        assertTrue(store.loadClips().isEmpty())
    }
}
