package com.cambria.rosarium.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.media3.session.MediaSession
import com.cambria.rosarium.R
import com.cambria.rosarium.media.RosaryPlaybackGateway
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class RosaryPlaybackService : Service() {

    companion object {
        const val ACTION_START = "com.cambria.rosarium.action.START"
        const val ACTION_TOGGLE = "com.cambria.rosarium.action.TOGGLE"
        const val ACTION_STOP = "com.cambria.rosarium.action.STOP"

        private const val CHANNEL_ID = "rosarium_playback"
        private const val CHANNEL_NAME = "Rosarium Playback"
        private const val NOTIFICATION_ID = 1001
        private const val REQUEST_CODE_TOGGLE = 2000
        private const val REQUEST_CODE_STOP = 2001
        private const val SESSION_ID = "rosarium_embedded_session"

        private const val DEFAULT_TITLE = "Rosario"
        private const val DEFAULT_SUBTITLE = "Riproduzione audio"
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        PlayerController.initialize(applicationContext)
        createEmbeddedMediaSession()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        observePlaybackState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                refreshNotification()
            }

            ACTION_TOGGLE -> {
                RosaryPlaybackGateway.toggleCurrent()
                refreshNotification()
            }

            ACTION_STOP -> {
                RosaryPlaybackGateway.stop()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }

            else -> {
                refreshNotification()
            }
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        serviceScope.cancel()
        mediaSession?.release()
        mediaSession = null
        RosaryPlaybackGateway.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createEmbeddedMediaSession() {
        if (mediaSession != null) return

        mediaSession = MediaSession.Builder(this, PlayerController.requireExoPlayer())
            .setId(SESSION_ID)
            .build()
    }

    private fun observePlaybackState() {
        serviceScope.launch {
            PlayerController.isPlaying.collectLatest {
                refreshNotification()
            }
        }
    }

    private fun refreshNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val title = resolveNotificationTitle()
        val subtitle = resolveNotificationSubtitle()
        val isPlaying = RosaryPlaybackGateway.currentIsPlaying()

        val toggleIntent = Intent(this, RosaryPlaybackService::class.java).apply {
            action = ACTION_TOGGLE
        }

        val togglePendingIntent = PendingIntent.getService(
            this,
            REQUEST_CODE_TOGGLE,
            toggleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or pendingIntentImmutableFlag()
        )

        val stopIntent = Intent(this, RosaryPlaybackService::class.java).apply {
            action = ACTION_STOP
        }

        val stopPendingIntent = PendingIntent.getService(
            this,
            REQUEST_CODE_STOP,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or pendingIntentImmutableFlag()
        )

        val toggleIcon = if (isPlaying) {
            android.R.drawable.ic_media_pause
        } else {
            android.R.drawable.ic_media_play
        }

        val toggleText = if (isPlaying) {
            "Pausa"
        } else {
            "Riprendi"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(toggleIcon, toggleText, togglePendingIntent)
            .addAction(android.R.drawable.ic_media_pause, "Stop", stopPendingIntent)
            .build()
    }

    private fun resolveNotificationTitle(): String {
        return RosaryPlaybackGateway.currentTitle()
            ?: PlayerController.currentCrown()?.title
            ?: DEFAULT_TITLE
    }

    private fun resolveNotificationSubtitle(): String {
        return RosaryPlaybackGateway.currentSubtitle()
            ?: PlayerController.currentCrown()?.audioTrack?.title
            ?: DEFAULT_SUBTITLE
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        )

        manager.createNotificationChannel(channel)
    }

    private fun pendingIntentImmutableFlag(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE
        } else {
            0
        }
    }
}