package com.kyro.avi2mp4

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EditorRealtimePlayerTest {
    @Test
    fun mutableColorMatrixUpdatesInPlace() {
        val effect = MutableEditorRgbMatrix(EditorColorSettings())
        val neutral = effect.getMatrix(0L, false).copyOf()

        effect.update(
            EditorColorSettings(
                grayscale = true,
                exposure = 0.5f,
                contrast = 0.3f,
                shadowTint = 0.4f,
                highlightTint = 0.2f
            )
        )
        val adjusted = effect.getMatrix(0L, false)

        assertFalse(neutral.contentEquals(adjusted))
        assertEquals(16, adjusted.size)
        assertTrue(adjusted.all(Float::isFinite))
    }

    @Test
    fun mutableMusicGainAppliesVolumeAndFades() {
        val provider = MutableMusicGainProvider()
        provider.update(
            EditorMusicTrack(
                uri = "content://test/music.mp3",
                name = "music.mp3",
                durationMs = 4_000L,
                volume = 0.5f,
                fadeInMs = 1_000L,
                fadeOutMs = 1_000L
            ),
            projectDurationMs = 4_000L
        )

        assertEquals(0f, provider.getGainFactorAtSamplePosition(0L, 44_100), 0.001f)
        assertEquals(0.25f, provider.getGainFactorAtSamplePosition(22_050L, 44_100), 0.01f)
        assertEquals(0.5f, provider.getGainFactorAtSamplePosition(88_200L, 44_100), 0.01f)
        assertTrue(provider.getGainFactorAtSamplePosition(154_350L, 44_100) < 0.3f)
    }
}
