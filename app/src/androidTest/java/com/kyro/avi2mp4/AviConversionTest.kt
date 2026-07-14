package com.kyro.avi2mp4

import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.arthenica.mobileffmpeg.Config
import com.arthenica.mobileffmpeg.FFmpeg
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class AviConversionTest {
    @Test
    fun convertsMjpegAviAndExtractsPreview() {
        val cacheDir = InstrumentationRegistry.getInstrumentation().targetContext.cacheDir
        val input = File(cacheDir, "fixture.avi")
        val preview = File(cacheDir, "fixture-preview.jpg")
        val output = File(cacheDir, "fixture.mp4")

        try {
            assertFfmpegSuccess(
                "-y", "-f", "lavfi", "-i", "testsrc2=size=320x240:rate=1", "-t", "1",
                "-c:v", "mjpeg", "-q:v", "2", input.absolutePath
            )
            assertTrue(input.isFile && input.length() > 0)

            assertFfmpegSuccess(
                "-y", "-hide_banner", "-loglevel", "error", "-i", input.absolutePath,
                "-frames:v", "1", "-vf", "scale=640:-2", preview.absolutePath
            )
            assertNotNull(BitmapFactory.decodeFile(preview.absolutePath))

            assertFfmpegSuccess(
                "-y", "-hide_banner", "-loglevel", "error", "-i", input.absolutePath,
                "-vf", "scale=1280:ih", "-c:v", "mpeg4", "-q:v", "4", "-pix_fmt", "yuv420p",
                "-c:a", "aac", "-movflags", "+faststart", output.absolutePath
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
        val returnCode = FFmpeg.execute(arguments)
        assertEquals(Config.getLastCommandOutput(), Config.RETURN_CODE_SUCCESS, returnCode)
    }
}
