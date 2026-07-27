package com.kyro.avi2mp4

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFprobeKit
import com.arthenica.ffmpegkit.ReturnCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class EditorExportTest {
    @Test
    fun buildsTimelineGraphWithSilentClipMusicAndColor() {
        val first = EditorClip(
            id = "first",
            uri = "content://test/shared.mp4",
            name = "first.mp4",
            durationMs = 4_000L,
            trimStartMs = 500L,
            trimEndMs = 2_000L,
            color = EditorColorSettings(grayscale = true, exposure = 0.2f, contrast = 0.4f)
        )
        val second = first.copy(
            id = "second",
            trimStartMs = 2_000L,
            trimEndMs = 3_500L,
            color = EditorColorSettings(shadowTint = 0.4f, highlightTint = 0.3f)
        )
        val music = EditorMusicTrack(
            uri = "content://test/music.mp3",
            name = "music.mp3",
            durationMs = 10_000L,
            trimStartMs = 1_000L,
            trimEndMs = 6_000L,
            timelineStartMs = 300L,
            volume = 0.4f,
            fadeInMs = 500L,
            fadeOutMs = 700L
        )
        val project = EditorProject(listOf(first, second), music)
        val plan = EditorExportPlan(
            project = project,
            segments = listOf(
                EditorExportSegment(first, "/cache/shared.mp4", hasAudio = true),
                EditorExportSegment(second, "/cache/shared.mp4", hasAudio = false)
            ),
            musicPath = "/cache/music.mp3",
            canvasWidth = 1010,
            canvasHeight = 758
        )

        val graph = buildEditorFilterGraph(plan)
        assertTrue(graph, graph.contains("[0:v:0]trim=start=0.500:end=2.000"))
        assertTrue(graph, graph.contains("anullsrc=channel_layout=stereo:sample_rate=44100"))
        assertTrue(graph, graph.contains("concat=n=2:v=1:a=1[editor_video][editor_audio]"))
        assertTrue(graph, graph.contains("hue=s=0.000,exposure=exposure=0.200,curves="))
        assertTrue(graph, graph.contains("colorbalance="))
        assertTrue(graph, graph.contains("adelay=delays=300:all=1"))
        assertTrue(graph, graph.contains("afade=t=in"))
        assertTrue(graph, graph.contains("amix=inputs=2:duration=first"))

        val arguments = buildEditorExportArguments(plan, File("/cache/output.mp4")).toList()
        assertEquals(3, arguments.count { it == "-i" })
        assertEquals(2, arguments.count { it == "/cache/shared.mp4" })
        assertTrue(arguments.contains("[mixed_audio]"))
    }

    @Test
    fun omitsNeutralColorFilters() {
        assertTrue(editorColorFilters(EditorColorSettings()).isEmpty())
        val filters = editorColorFilters(EditorColorSettings(saturation = 0.8f, highlights = 0.5f))
        assertEquals(2, filters.size)
        assertTrue(filters.first().startsWith("hue="))
        assertTrue(filters.last().startsWith("curves="))
        assertFalse(filters.any { it.startsWith("exposure=") })
    }

    @Test
    fun executesProductionGraphWithAudioSilenceMusicAndColor() {
        val cacheDir = InstrumentationRegistry.getInstrumentation().targetContext.cacheDir
        val audioVideo = File(cacheDir, "editor-export-audio.mp4")
        val silentVideo = File(cacheDir, "editor-export-silent.mp4")
        val musicFile = File(cacheDir, "editor-export-music.m4a")
        val output = File(cacheDir, "editor-export-production.mp4")
        try {
            assertFfmpegSuccess(
                "-y", "-f", "lavfi", "-i", "testsrc2=size=160x120:rate=10:duration=1",
                "-f", "lavfi", "-i", "sine=frequency=440:sample_rate=44100:duration=1",
                "-c:v", "mpeg4", "-q:v", "6", "-c:a", "aac", "-shortest", audioVideo.absolutePath
            )
            assertFfmpegSuccess(
                "-y", "-f", "lavfi", "-i", "testsrc2=size=120x160:rate=12:duration=1",
                "-an", "-c:v", "mpeg4", "-q:v", "6", silentVideo.absolutePath
            )
            assertFfmpegSuccess(
                "-y", "-f", "lavfi", "-i", "sine=frequency=220:sample_rate=44100:duration=1",
                "-c:a", "aac", musicFile.absolutePath
            )

            val first = EditorClip(
                id = "audio",
                uri = "content://test/audio",
                name = "audio.mp4",
                durationMs = 1_000L,
                trimStartMs = 100L,
                trimEndMs = 600L,
                color = EditorColorSettings(exposure = 0.1f, contrast = 0.2f)
            )
            val second = EditorClip(
                id = "silent",
                uri = "content://test/silent",
                name = "silent.mp4",
                durationMs = 1_000L,
                trimStartMs = 200L,
                trimEndMs = 700L,
                color = EditorColorSettings(grayscale = true, shadowTint = 0.2f)
            )
            val music = EditorMusicTrack(
                uri = "content://test/music",
                name = "music.m4a",
                durationMs = 1_000L,
                timelineStartMs = 100L,
                volume = 0.2f,
                fadeInMs = 100L,
                fadeOutMs = 100L
            )
            val project = EditorProject(listOf(first, second), music, outputWidth = 160)
            val plan = EditorExportPlan(
                project = project,
                segments = listOf(
                    EditorExportSegment(first, audioVideo.absolutePath, hasAudio = true),
                    EditorExportSegment(second, silentVideo.absolutePath, hasAudio = false)
                ),
                musicPath = musicFile.absolutePath,
                canvasWidth = 160,
                canvasHeight = 120
            )
            val session = FFmpegKit.executeWithArguments(buildEditorExportArguments(plan, output))
            assertTrue(session.output, ReturnCode.isSuccess(session.returnCode))
            assertTrue(output.isFile && output.length() > 0L)

            val probe = FFprobeKit.executeWithArguments(
                arrayOf(
                    "-v", "error",
                    "-show_entries", "stream=codec_type,codec_name,width,height:format=duration",
                    "-of", "default=noprint_wrappers=1", output.absolutePath
                )
            )
            assertTrue(probe.output, ReturnCode.isSuccess(probe.returnCode))
            assertTrue(probe.output, probe.output.contains("codec_name=mpeg4"))
            assertTrue(probe.output, probe.output.contains("codec_name=aac"))
            assertTrue(probe.output, probe.output.contains("width=160"))
            assertTrue(probe.output, probe.output.contains("height=120"))
            val duration = Regex("duration=([0-9.]+)").find(probe.output.orEmpty())
                ?.groupValues?.get(1)?.toDoubleOrNull()
            assertTrue("Duración inesperada: ${probe.output}", duration != null && duration in 0.9..1.2)
        } finally {
            audioVideo.delete()
            silentVideo.delete()
            musicFile.delete()
            output.delete()
        }
    }

    private fun assertFfmpegSuccess(vararg arguments: String) {
        val session = FFmpegKit.executeWithArguments(arguments)
        assertTrue(session.output, ReturnCode.isSuccess(session.returnCode))
    }
}
