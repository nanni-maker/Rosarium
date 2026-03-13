package com.cambria.rosarium.player

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.cambria.rosarium.core.CrownSet

class RosaryAudioPlayer(
    context: Context,
    private val onPlaybackStateChanged: (isPlaying: Boolean) -> Unit
) {

    private val player: ExoPlayer =
        ExoPlayer.Builder(context).build().apply {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                .build()

            setAudioAttributes(audioAttributes, true)

            addListener(
                object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        onPlaybackStateChanged(isPlaying)
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_ENDED) {
                            onPlaybackStateChanged(false)
                        }
                    }
                }
            )
        }

    private var currentCrownId: String? = null

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
        onPlaybackStateChanged(false)
    }

    fun isPlaying(): Boolean {
        return player.isPlaying
    }

    fun release() {
        player.release()
    }
}