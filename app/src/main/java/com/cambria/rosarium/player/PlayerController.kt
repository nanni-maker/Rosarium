package com.cambria.rosarium.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.cambria.rosarium.core.CrownSet
import com.cambria.rosarium.media.RosaryPlaybackGateway
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object PlayerController {

    private var player: RosaryAudioPlayer? = null
    private var initialized = false
    private var currentCrown: CrownSet? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    fun initialize(context: Context) {
        if (initialized) return

        val appContext = context.applicationContext

        player = RosaryAudioPlayer(appContext).also { rosaryPlayer ->
            rosaryPlayer.setOnPlaybackChangedListener { playing ->
                _isPlaying.value = playing
            }

            rosaryPlayer.exoPlayer().addListener(
                object : Player.Listener {

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _isPlaying.value = isPlaying
                    }

                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        syncCurrentFromPlayerMediaItem(mediaItem)
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_ENDED) {
                            _isPlaying.value = false
                        }
                    }
                }
            )
        }

        initialized = true
    }

    fun playOrPause(crown: CrownSet) {
        checkNotNull(player) { "PlayerController not initialized" }
        currentCrown = crown
        player!!.playOrPause(crown)
    }

    fun toggleCurrent() {
        val crown = currentCrown ?: return
        playOrPause(crown)
    }

    fun skipToNext() {
        val exoPlayer = requireExoPlayer()
        if (!exoPlayer.hasNextMediaItem()) return

        exoPlayer.seekToNextMediaItem()
        if (!exoPlayer.isPlaying) {
            exoPlayer.play()
        }
    }

    fun skipToPrevious() {
        val exoPlayer = requireExoPlayer()
        if (!exoPlayer.hasPreviousMediaItem()) return

        exoPlayer.seekToPreviousMediaItem()
        if (!exoPlayer.isPlaying) {
            exoPlayer.play()
        }
    }

    fun hasNext(): Boolean {
        return requireExoPlayer().hasNextMediaItem()
    }

    fun hasPrevious(): Boolean {
        return requireExoPlayer().hasPreviousMediaItem()
    }

    fun stop() {
        player?.stop()
        _isPlaying.value = false
        RosaryPlaybackGateway.clearCurrentMediaId()
    }

    fun pause() {
        player?.pause()
        _isPlaying.value = false
    }

    fun currentCrown(): CrownSet? {
        return currentCrown
    }

    fun currentIsPlaying(): Boolean {
        return player?.isPlaying() ?: false
    }

    fun requireExoPlayer(): ExoPlayer {
        checkNotNull(player) { "PlayerController not initialized" }
        return player!!.exoPlayer()
    }

    fun release() {
        player?.release()
        player = null
        currentCrown = null
        _isPlaying.value = false
        RosaryPlaybackGateway.clearCurrentMediaId()
        initialized = false
    }

    private fun syncCurrentFromPlayerMediaItem(mediaItem: MediaItem?) {
        val mediaId = mediaItem?.mediaId.orEmpty()

        if (mediaId.isBlank()) {
            return
        }

        val crownFromGateway = RosaryPlaybackGateway.findCrownByMediaId(mediaId)
        if (crownFromGateway != null) {
            currentCrown = crownFromGateway
            RosaryPlaybackGateway.rememberCurrentMediaId(mediaId)
        }
    }
}