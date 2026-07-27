package com.kyro.avi2mp4

import android.media.MediaCodecList
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFprobeKit
import com.arthenica.ffmpegkit.ReturnCode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class EditorCapabilityTest {
    @Test
    fun requiredEditorCodecsAndFiltersAreBundled() {
        val buildConfiguration = ffmpegOutput("-hide_banner", "-buildconf")
        val filters = ffmpegOutput("-hide_banner", "-filters")
        val encoders = ffmpegOutput("-hide_banner", "-encoders")
        val decoders = ffmpegOutput("-hide_banner", "-decoders")

        assertTrue(buildConfiguration, buildConfiguration.contains("--enable-mediacodec"))
        assertFalse(buildConfiguration, buildConfiguration.contains("--enable-gpl"))
        assertFalse(buildConfiguration, buildConfiguration.contains("--enable-libx264"))
        assertFalse(buildConfiguration, buildConfiguration.contains("--enable-libopenh264"))

        listOf("concat", "trim", "atrim", "amix", "eq", "exposure", "curves", "colorbalance")
            .forEach { assertListed(filters, it) }
        listOf("mpeg4", "aac", "h264_mediacodec").forEach { assertListed(encoders, it) }
        listOf("mjpeg", "h264", "mp3", "pcm_s16le").forEach { assertListed(decoders, it) }
    }

    @Test
    fun processesTimelineMusicAndColorInOneFilterGraph() {
        val cacheDir = InstrumentationRegistry.getInstrumentation().targetContext.cacheDir
        val output = File(cacheDir, "editor-capability.mp4")
        try {
            assertFfmpegSuccess(
                "-y",
                "-f", "lavfi", "-i", "testsrc2=size=160x120:rate=10:duration=1",
                "-f", "lavfi", "-i", "sine=frequency=440:sample_rate=44100:duration=1",
                "-f", "lavfi", "-i", "testsrc2=size=160x120:rate=10:duration=1",
                "-f", "lavfi", "-i", "sine=frequency=660:sample_rate=44100:duration=1",
                "-f", "lavfi", "-i", "sine=frequency=220:sample_rate=44100:duration=1",
                "-filter_complex", editorFilterGraph,
                "-map", "[video]", "-map", "[audio]",
                "-c:v", "mpeg4", "-q:v", "5", "-pix_fmt", "yuv420p",
                "-c:a", "aac", "-b:a", "96k", "-shortest", "-movflags", "+faststart",
                output.absolutePath
            )
            assertTrue(output.isFile && output.length() > 0L)

            val mediaInformation = ffprobeOutput(
                "-v", "error",
                "-show_entries", "stream=codec_type,codec_name,width,height:format=duration",
                "-of", "default=noprint_wrappers=1",
                output.absolutePath
            )
            assertTrue(mediaInformation, mediaInformation.contains("codec_name=mpeg4"))
            assertTrue(mediaInformation, mediaInformation.contains("codec_name=aac"))
            assertTrue(mediaInformation, mediaInformation.contains("width=160"))
            assertTrue(mediaInformation, mediaInformation.contains("height=120"))
            val duration = Regex("duration=([0-9.]+)").find(mediaInformation)
                ?.groupValues?.get(1)?.toDoubleOrNull()
            assertTrue("Duración inesperada: $mediaInformation", duration != null && duration in 0.9..1.2)
        } finally {
            output.delete()
        }
    }

    @Test
    fun ffmpegCanUseAnAndroidAvcEncoder() {
        val avcEncoders = MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.filter { codec ->
            codec.isEncoder && codec.supportedTypes.any { it.equals("video/avc", ignoreCase = true) }
        }
        val report = avcEncoders.joinToString(separator = "\n") { codec ->
            buildString {
                append(codec.name)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    append(" hardware=").append(codec.isHardwareAccelerated)
                    append(" software=").append(codec.isSoftwareOnly)
                    append(" vendor=").append(codec.isVendor)
                }
            }
        }
        assumeTrue("Android no expone un encoder AVC:\n$report", avcEncoders.isNotEmpty())

        val cacheDir = InstrumentationRegistry.getInstrumentation().targetContext.cacheDir
        val output = File(cacheDir, "mediacodec-capability.mp4")
        try {
            assertFfmpegSuccess(
                "-y", "-f", "lavfi", "-i", "testsrc2=size=160x120:rate=10:duration=1",
                "-an", "-c:v", "h264_mediacodec", "-b:v", "600k", "-pix_fmt", "yuv420p",
                "-movflags", "+faststart", output.absolutePath
            )
            val mediaInformation = ffprobeOutput(
                "-v", "error", "-select_streams", "v:0",
                "-show_entries", "stream=codec_name,width,height", "-of", "default=noprint_wrappers=1",
                output.absolutePath
            )
            assertTrue("Encoders AVC:\n$report\n$mediaInformation", mediaInformation.contains("codec_name=h264"))
            assertTrue(mediaInformation, mediaInformation.contains("width=160"))
            assertTrue(mediaInformation, mediaInformation.contains("height=120"))
        } finally {
            output.delete()
        }
    }

    private fun assertListed(listing: String, name: String) {
        val found = listing.lineSequence().any { line ->
            val tokens = line.trim().split(Regex("\\s+"), limit = 3)
            tokens.size >= 2 && tokens[1] == name
        }
        assertTrue("No se encontró $name en:\n$listing", found)
    }

    private fun ffmpegOutput(vararg arguments: String): String {
        val session = FFmpegKit.executeWithArguments(arguments)
        assertTrue(session.output, ReturnCode.isSuccess(session.returnCode))
        return session.output.orEmpty()
    }

    private fun ffprobeOutput(vararg arguments: String): String {
        val session = FFprobeKit.executeWithArguments(arguments)
        assertTrue(session.output, ReturnCode.isSuccess(session.returnCode))
        return session.output.orEmpty()
    }

    private fun assertFfmpegSuccess(vararg arguments: String) {
        val session = FFmpegKit.executeWithArguments(arguments)
        assertTrue(session.output, ReturnCode.isSuccess(session.returnCode))
    }

    private companion object {
        val editorFilterGraph = listOf(
            "[0:v]trim=start=0.1:end=0.6,setpts=PTS-STARTPTS,eq=contrast=1.1:brightness=0.02,exposure=exposure=0.1,curves=preset=lighter,colorbalance=rs=0.03:bh=0.03,format=yuv420p[clip0v]",
            "[2:v]trim=start=0.2:end=0.7,setpts=PTS-STARTPTS,eq=contrast=0.95:brightness=-0.01,format=yuv420p[clip1v]",
            "[clip0v][clip1v]concat=n=2:v=1:a=0[video]",
            "[1:a]atrim=start=0.1:end=0.6,asetpts=PTS-STARTPTS[clip0a]",
            "[3:a]atrim=start=0.2:end=0.7,asetpts=PTS-STARTPTS[clip1a]",
            "[clip0a][clip1a]concat=n=2:v=0:a=1[clips]",
            "[4:a]atrim=duration=1,asetpts=PTS-STARTPTS,volume=0.2[music]",
            "[clips][music]amix=inputs=2:duration=first:normalize=0[audio]"
        ).joinToString(";")
    }
}
