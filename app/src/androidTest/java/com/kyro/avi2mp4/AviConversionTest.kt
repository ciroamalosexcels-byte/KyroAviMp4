package com.kyro.avi2mp4

import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class AviConversionTest {
    @Test
    fun convertsMjpegAviWithPcmAudioAndExtractsPreview() {
        val cacheDir = InstrumentationRegistry.getInstrumentation().targetContext.cacheDir
        val input = File(cacheDir, "fixture.avi")
        val preview = File(cacheDir, "fixture-preview.jpg")
        val output = File(cacheDir, "fixture.mp4")

        try {
            assertFfmpegSuccess(
                "-y", "-f", "lavfi", "-i", "testsrc2=size=320x240:rate=5",
                "-f", "lavfi", "-i", "sine=frequency=440:sample_rate=22050",
                "-t", "1", "-c:v", "mjpeg", "-q:v", "2", "-c:a", "pcm_s16le",
                input.absolutePath
            )
            assertTrue(input.isFile && input.length() > 0)

            assertFfmpegSuccess(
                "-y", "-hide_banner", "-loglevel", "warning",
                "-fflags", "+genpts+discardcorrupt", "-err_detect", "ignore_err",
                "-i", input.absolutePath,
                "-an", "-frames:v", "1", "-vf", "scale=640:-2", preview.absolutePath
            )
            assertNotNull(BitmapFactory.decodeFile(preview.absolutePath))

            assertFfmpegSuccess(
                "-y", "-hide_banner", "-loglevel", "warning",
                "-fflags", "+genpts+discardcorrupt", "-err_detect", "ignore_err",
                "-i", input.absolutePath,
                "-map", "0:v:0", "-vf", "scale=1280:trunc(ih/2)*2",
                "-c:v", "mpeg4", "-q:v", "4", "-pix_fmt", "yuv420p",
                "-map", "0:a:0?", "-c:a", "aac", "-b:a", "128k", "-ar", "44100",
                "-avoid_negative_ts", "make_zero", "-max_muxing_queue_size", "1024",
                "-movflags", "+faststart", output.absolutePath
            )
            assertTrue(output.isFile && output.length() > 0)

            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(output.absolutePath)
                assertEquals("1280", retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH))
                assertEquals("240", retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT))
            } finally {
                retriever.release()
            }
        } finally {
            input.delete()
            preview.delete()
            output.delete()
        }
    }

    private fun assertFfmpegSuccess(vararg arguments: String) {
        val session = FFmpegKit.executeWithArguments(arguments)
        assertTrue(session.output, ReturnCode.isSuccess(session.returnCode))
    }
}
