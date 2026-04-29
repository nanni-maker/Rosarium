package com.cambria.rosarium.player

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.cambria.rosarium.core.CrownSet

class RosaryAudioPlayer(
    context: Context
) {

    private var onPlaybackChangedListener: ((Boolean) -> Unit)? = null

    private val player: ExoPlayer =
        ExoPlayer.Builder(context).build().apply {

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                .build()

            setAudioAttributes(audioAttributes, true)

            // Volume massimo lato app
            volume = 1.0f

            // Gestione disconnessione cuffie / BT
            setHandleAudioBecomingNoisy(true)

            addListener(
                object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        onPlaybackChangedListener?.invoke(isPlaying)
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_ENDED) {
                            onPlaybackChangedListener?.invoke(false)
                        }
                    }
                }
            )
        }

    private var currentCrownId: String? = null

    fun setOnPlaybackChangedListener(listener: (Boolean) -> Unit) {
        onPlaybackChangedListener = listener
    }

    fun playOrPause(crownSet: CrownSet) {
        val requestedId = crownSet.audioTrack.id

        if (currentCrownId != requestedId) {
            playNewCrown(crownSet)
            return
        }

        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    private fun playNewCrown(crownSet: CrownSet) {
        currentCrownId = crownSet.audioTrack.id

        val mediaItem = MediaItem.fromUri("asset:///${crownSet.audioTrack.assetPath}")

        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
    }

    fun pause() {
        player.pause()
    }

    fun stop() {
        player.stop()
        player.clearMediaItems()
        currentCrownId = null
        onPlaybackChangedListener?.invoke(false)
    }

    fun isPlaying(): Boolean {
        return player.isPlaying
    }

    fun exoPlayer(): ExoPlayer {
        return player
    }

    fun release() {
        player.release()
    }
}