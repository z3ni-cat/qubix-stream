package com.qubix.stream.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.qubix.stream.data.Subtitle
import com.qubix.stream.data.VideoSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Wrapper ExoPlayer untuk streaming video + subtitle.
 */
@OptIn(UnstableApi::class)
class StreamPlayer(context: Context) {
    private val httpFactory = DefaultHttpDataSource.Factory()
        .setUserAgent("Mozilla/5.0 (Linux; Android 14) QubixStream/1.0")

    val player: ExoPlayer = ExoPlayer.Builder(context)
        .setSeekBackIncrementMs(10_000)
        .setSeekForwardIncrementMs(10_000)
        .build()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        player.addListener(object : androidx.media3.common.Player.Listener {
            override fun onIsLoadingChanged(loading: Boolean) { _isLoading.value = loading }
            override fun onPlayerError(err: androidx.media3.common.PlaybackException) {
                _error.value = err.message
            }
        })
    }

    fun playSource(source: VideoSource) {
        _error.value = null
        val factory = httpFactory.apply {
            source.headers["Referer"]?.let { setDefaultRequestProperties(mapOf("Referer" to it)) }
        }
        // Subtitle: pakai yang bahasa Indonesia dulu
        val subs = source.subtitles.sortedBy { if (it.language == "id") 0 else 1 }
        val configs = subs.map {
            MediaItem.SubtitleConfiguration.Builder(android.net.Uri.parse(it.url))
                .setMimeType("text/vtt")
                .setLanguage(it.language)
                .setLabel(it.label)
                .build()
        }
        val item = MediaItem.Builder()
            .setUri(source.url)
            .setSubtitleConfigurations(configs)
            .build()
        player.setMediaSource(
            if (source.isM3u8 || source.url.contains("m3u8"))
                HlsMediaSource.Factory(factory).createMediaSource(item)
            else
                ProgressiveMediaSource.Factory(factory).createMediaSource(item)
        )
        player.prepare()
        player.play()
    }

    fun release() = player.release()
}
