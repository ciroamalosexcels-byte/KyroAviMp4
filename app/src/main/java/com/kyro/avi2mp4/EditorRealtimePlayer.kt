package com.kyro.avi2mp4

import android.content.Context
import android.net.Uri
import android.view.SurfaceView
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.audio.GainProcessor
import androidx.media3.common.util.ExperimentalApi
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.RgbMatrix
import androidx.media3.transformer.Composition
import androidx.media3.transformer.CompositionPlayer
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

@OptIn(UnstableApi::class, ExperimentalApi::class)
internal class EditorRealtimePlayer(context: Context) {
    private val colorEffects = mutableMapOf<String, MutableEditorRgbMatrix>()
    private val musicGain = MutableMusicGainProvider()
    private var structure: PlaybackStructure? = null
    private var projectDurationMs: Long = 0L
    private var listener: Listener? = null

    val player: CompositionPlayer = CompositionPlayer.Builder(context)
        .experimentalSetEnableReplayableCache(true)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build(),
            true
        )
        .build()
        .also { compositionPlayer ->
            compositionPlayer.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    listener?.onStateChanged(
                        ready = playbackState == Player.STATE_READY,
                        playing = compositionPlayer.isPlaying
                    )
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    listener?.onStateChanged(
                        ready = compositionPlayer.playbackState == Player.STATE_READY,
                        playing = isPlaying
                    )
                }

                override fun onPlayerError(error: PlaybackException) {
                    listener?.onError(error.message ?: "No se pudo reproducir la edición")
                }
            })
        }

    interface Listener {
        fun onStateChanged(ready: Boolean, playing: Boolean)
        fun onError(message: String)
    }

    fun setListener(listener: Listener?) {
        this.listener = listener
    }

    fun setSurfaceView(surfaceView: SurfaceView) {
        player.setVideoSurfaceView(surfaceView)
    }

    fun updateParameters(project: EditorProject) {
        val activeIds = project.clips.mapTo(mutableSetOf(), EditorClip::id)
        colorEffects.keys.retainAll(activeIds)
        project.clips.forEach { clip ->
            colorEffects.getOrPut(clip.id) { MutableEditorRgbMatrix(clip.color) }.update(clip.color)
        }
        musicGain.update(project.music, project.durationMs)
        player.experimentalRedrawLastFrame()
    }

    fun setProject(project: EditorProject, startPositionMs: Long) {
        if (project.clips.isEmpty()) {
            structure = null
            projectDurationMs = 0L
            player.stop()
            return
        }
        updateParameters(project)
        val nextStructure = PlaybackStructure.from(project)
        if (nextStructure == structure) return

        val resumePlayback = player.playWhenReady
        val position = startPositionMs.coerceIn(0L, project.durationMs)
        structure = nextStructure
        projectDurationMs = project.durationMs
        player.setComposition(buildComposition(project), position)
        player.prepare()
        player.playWhenReady = resumePlayback
    }

    fun seekTo(positionMs: Long) {
        if (structure != null) player.seekTo(positionMs.coerceIn(0L, projectDurationMs))
    }

    fun setScrubbing(enabled: Boolean) {
        player.setScrubbingModeEnabled(enabled)
    }

    fun togglePlayback() {
        if (player.isPlaying) player.pause() else player.play()
    }

    fun release() {
        player.release()
    }

    private fun buildComposition(project: EditorProject): Composition {
        val clipItems = project.clips.map { clip ->
            val mediaItem = MediaItem.Builder()
                .setUri(Uri.parse(clip.uri))
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(clip.trimStartMs)
                        .setEndPositionMs(clip.trimEndMs)
                        .build()
                )
                .build()
            EditedMediaItem.Builder(mediaItem)
                .setDurationUs(clip.durationMs * 1_000L)
                .setEffects(
                    Effects(
                        emptyList(),
                        listOf(colorEffects.getValue(clip.id))
                    )
                )
                .build()
        }
        val sequences = mutableListOf(
            EditedMediaItemSequence.withAudioAndVideoFrom(clipItems)
        )

        project.music?.let { music ->
            val availableMs = (project.durationMs - music.timelineStartMs).coerceAtLeast(0L)
            val audibleMs = min(music.trimmedDurationMs, availableMs)
            if (audibleMs > 0L) {
                val musicItem = MediaItem.Builder()
                    .setUri(Uri.parse(music.uri))
                    .setClippingConfiguration(
                        MediaItem.ClippingConfiguration.Builder()
                            .setStartPositionMs(music.trimStartMs)
                            .setEndPositionMs(music.trimStartMs + audibleMs)
                            .build()
                    )
                    .build()
                val editedMusic = EditedMediaItem.Builder(musicItem)
                    .setDurationUs(music.durationMs * 1_000L)
                    .setRemoveVideo(true)
                    .setEffects(
                        Effects(
                            listOf(GainProcessor(musicGain)),
                            emptyList()
                        )
                    )
                    .build()
                sequences += EditedMediaItemSequence.Builder(setOf(C.TRACK_TYPE_AUDIO)).apply {
                    if (music.timelineStartMs > 0L) addGap(music.timelineStartMs * 1_000L)
                    addItem(editedMusic)
                }.build()
            }
        }
        return Composition.Builder(sequences).build()
    }
}

private data class PlaybackStructure(
    val clips: List<ClipStructure>,
    val music: MusicStructure?
) {
    data class ClipStructure(
        val id: String,
        val uri: String,
        val durationMs: Long,
        val trimStartMs: Long,
        val trimEndMs: Long
    )

    data class MusicStructure(
        val uri: String,
        val durationMs: Long,
        val trimStartMs: Long,
        val trimEndMs: Long,
        val timelineStartMs: Long
    )

    companion object {
        fun from(project: EditorProject): PlaybackStructure = PlaybackStructure(
            clips = project.clips.map { clip ->
                ClipStructure(
                    clip.id,
                    clip.uri,
                    clip.durationMs,
                    clip.trimStartMs,
                    clip.trimEndMs
                )
            },
            music = project.music?.let { music ->
                MusicStructure(
                    music.uri,
                    music.durationMs,
                    music.trimStartMs,
                    music.trimEndMs,
                    music.timelineStartMs
                )
            }
        )
    }
}

@OptIn(UnstableApi::class)
internal class MutableEditorRgbMatrix(initial: EditorColorSettings) : RgbMatrix {
    @Volatile
    private var matrix: FloatArray = editorColorMatrix(initial)

    fun update(settings: EditorColorSettings) {
        matrix = editorColorMatrix(settings)
    }

    override fun getMatrix(presentationTimeUs: Long, useHdr: Boolean): FloatArray = matrix

    override fun isNoOp(inputWidth: Int, inputHeight: Int): Boolean = false
}

@OptIn(UnstableApi::class)
internal class MutableMusicGainProvider : GainProcessor.GainProvider {
    @Volatile private var volume = 0f
    @Volatile private var fadeInMs = 0L
    @Volatile private var fadeOutMs = 0L
    @Volatile private var durationMs = 0L

    fun update(music: EditorMusicTrack?, projectDurationMs: Long) {
        if (music == null) {
            volume = 0f
            fadeInMs = 0L
            fadeOutMs = 0L
            durationMs = 0L
            return
        }
        volume = music.volume.coerceIn(0f, 1f)
        durationMs = min(
            music.trimmedDurationMs,
            (projectDurationMs - music.timelineStartMs).coerceAtLeast(0L)
        )
        fadeInMs = music.fadeInMs.coerceIn(0L, durationMs)
        fadeOutMs = music.fadeOutMs.coerceIn(0L, durationMs)
    }

    override fun getGainFactorAtSamplePosition(samplePosition: Long, sampleRate: Int): Float {
        if (sampleRate <= 0 || durationMs <= 0L) return 0f
        val positionMs = samplePosition * 1_000.0 / sampleRate
        val fadeIn = if (fadeInMs > 0L) (positionMs / fadeInMs).coerceIn(0.0, 1.0) else 1.0
        val remainingMs = durationMs - positionMs
        val fadeOut = if (fadeOutMs > 0L) (remainingMs / fadeOutMs).coerceIn(0.0, 1.0) else 1.0
        return (volume * min(fadeIn, fadeOut)).toFloat().coerceIn(0f, 1f)
    }

    override fun isUnityUntil(samplePosition: Long, sampleRate: Int): Long {
        return if (getGainFactorAtSamplePosition(samplePosition, sampleRate) == 1f) {
            samplePosition + 1L
        } else {
            C.TIME_UNSET
        }
    }
}

private fun editorColorMatrix(settings: EditorColorSettings): FloatArray {
    val saturation = if (settings.grayscale) 0f else settings.saturation.coerceIn(0f, 2f)
    val inverse = 1f - saturation
    val redLuma = 0.2126f * inverse
    val greenLuma = 0.7152f * inverse
    val blueLuma = 0.0722f * inverse

    val exposureGain = 2.0.pow(settings.exposure.coerceIn(-2f, 2f).toDouble() * 0.5).toFloat()
    val contrastGain = 1f + settings.contrast.coerceIn(-1f, 1f) * 0.8f
    val tonalGain = 1f + settings.highlights.coerceIn(-1f, 1f) * 0.18f -
        settings.shadows.coerceIn(-1f, 1f) * 0.08f
    val shadowTint = realtimeTint(settings.shadowHue, settings.shadowTint)
    val highlightTint = realtimeTint(settings.highlightHue, settings.highlightTint)
    val channelScale = FloatArray(3) { index ->
        exposureGain * contrastGain * tonalGain *
            (1f + shadowTint[index] + highlightTint[index]).coerceAtLeast(0.05f)
    }
    val brightnessOffset = settings.shadows.coerceIn(-1f, 1f) * 0.11f +
        settings.highlights.coerceIn(-1f, 1f) * 0.04f +
        (1f - contrastGain) * 0.5f

    return floatArrayOf(
        (redLuma + saturation) * channelScale[0], redLuma * channelScale[1], redLuma * channelScale[2], 0f,
        greenLuma * channelScale[0], (greenLuma + saturation) * channelScale[1], greenLuma * channelScale[2], 0f,
        blueLuma * channelScale[0], blueLuma * channelScale[1], (blueLuma + saturation) * channelScale[2], 0f,
        brightnessOffset, brightnessOffset, brightnessOffset, 1f
    )
}

private fun realtimeTint(hue: Float, amount: Float): FloatArray {
    val strength = amount.coerceIn(0f, 1f) * 0.24f
    if (strength == 0f) return floatArrayOf(0f, 0f, 0f)
    val sectorValue = (((hue % 360f) + 360f) % 360f) / 60f
    val sector = sectorValue.toInt() % 6
    val fraction = sectorValue - sectorValue.toInt()
    val (red, green, blue) = when (sector) {
        0 -> Triple(1f, fraction, 0f)
        1 -> Triple(1f - fraction, 1f, 0f)
        2 -> Triple(0f, 1f, fraction)
        3 -> Triple(0f, 1f - fraction, 1f)
        4 -> Triple(fraction, 0f, 1f)
        else -> Triple(1f, 0f, 1f - fraction)
    }
    val average = (red + green + blue) / 3f
    return floatArrayOf(
        (red - average) * strength,
        (green - average) * strength,
        (blue - average) * strength
    )
}
