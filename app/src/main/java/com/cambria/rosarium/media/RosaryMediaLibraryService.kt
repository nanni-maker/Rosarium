package com.cambria.rosarium.media

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.MediaItemsWithStartPosition
import com.cambria.rosarium.R
import com.cambria.rosarium.data.AppStore
import com.cambria.rosarium.player.PlayerController
import com.cambria.rosarium.repository.RosaryRepository
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class RosaryMediaLibraryService : MediaLibraryService() {

    companion object {
        const val ACTION_START = "com.cambria.rosarium.action.MEDIA_LIBRARY_START"
        const val ACTION_STOP = "com.cambria.rosarium.action.MEDIA_LIBRARY_STOP"

        private const val SESSION_ID = "rosarium_media_library_session"
        private const val ROOT_TITLE = "Rosarium"

        private const val CHANNEL_ID = "rosarium_media_playback"
        private const val CHANNEL_NAME = "Rosarium Playback"
        private const val NOTIFICATION_ID = 1001
        private const val REQUEST_CODE_STOP = 3001

        private const val DEFAULT_TITLE = "Rosario"
        private const val DEFAULT_SUBTITLE = "Riproduzione audio"
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var mediaLibrarySession: MediaLibrarySession? = null
    private var foregroundStarted = false

    private val callback = object : MediaLibrarySession.Callback {

        @OptIn(UnstableApi::class)
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            return MediaSession.ConnectionResult.accept(
                MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS,
                MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS
            )
        }

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val rootItem = MediaItem.Builder()
                .setMediaId(RosaryMediaIds.root())
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(ROOT_TITLE)
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .build()
                )
                .build()

            return Futures.immediateFuture(LibraryResult.ofItem(rootItem, params))
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val items = buildChildren(parentId)

            return if (
                items.isNotEmpty() ||
                RosaryMediaIds.isRoot(parentId) ||
                RosaryMediaIds.isPack(parentId)
            ) {
                Futures.immediateFuture(
                    LibraryResult.ofItemList(ImmutableList.copyOf(items), params)
                )
            } else {
                Futures.immediateFuture(
                    LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE)
                )
            }
        }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val item = buildItem(mediaId)
                ?: return Futures.immediateFuture(
                    LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE)
                )

            return Futures.immediateFuture(LibraryResult.ofItem(item, null))
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>
        ): ListenableFuture<List<MediaItem>> {
            return Futures.immediateFuture(resolveMediaItems(mediaItems))
        }

        @OptIn(UnstableApi::class)
        override fun onSetMediaItems(
            mediaSession: MediaSession,
            browser: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>,
            startIndex: Int,
            startPositionMs: Long
        ): ListenableFuture<MediaItemsWithStartPosition> {

            if (mediaItems.isEmpty()) {
                RosaryPlaybackGateway.clearCurrentMediaId()
                return Futures.immediateFuture(
                    MediaItemsWithStartPosition(emptyList(), 0, startPositionMs)
                )
            }

            if (mediaItems.size == 1) {
                maybeExpandSingleItemToPlaylist(
                    mediaItem = mediaItems.first(),
                    startIndex = startIndex,
                    startPositionMs = startPositionMs
                )?.also { expanded ->
                    rememberCurrentFromPlaylist(expanded.mediaItems, expanded.startIndex)
                    return Futures.immediateFuture(expanded)
                }
            }

            val resolvedItems = resolveMediaItems(mediaItems)
            val safeStartIndex = safeStartIndex(startIndex, resolvedItems.size)
            rememberCurrentFromPlaylist(resolvedItems, safeStartIndex)

            return Futures.immediateFuture(
                MediaItemsWithStartPosition(
                    resolvedItems,
                    safeStartIndex,
                    startPositionMs
                )
            )
        }
    }

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        PlayerController.initialize(applicationContext)
        loadPacksIntoGateway()

        mediaLibrarySession = MediaLibrarySession.Builder(
            this,
            PlayerController.requireExoPlayer(),
            callback
        )
            .setId(SESSION_ID)
            .build()

        observePlaybackState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                ensureForeground()
            }

            ACTION_STOP -> {
                PlayerController.stop()
                stopForegroundIfNeeded()
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!PlayerController.currentIsPlaying()) {
            stopForegroundIfNeeded()
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return mediaLibrarySession
    }

    override fun onDestroy() {
        serviceScope.cancel()
        mediaLibrarySession?.release()
        mediaLibrarySession = null
        PlayerController.stop()
        stopForegroundIfNeeded()
        super.onDestroy()
    }

    private fun observePlaybackState() {
        serviceScope.launch {
            PlayerController.isPlaying.collectLatest { isPlaying ->
                if (isPlaying) {
                    ensureForeground()
                } else {
                    refreshNotificationIfForeground()
                }
            }
        }
    }

    private fun ensureForeground() {
        val notification = buildNotification()
        startForeground(NOTIFICATION_ID, notification)
        foregroundStarted = true
    }

    private fun refreshNotificationIfForeground() {
        if (!foregroundStarted) return
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun stopForegroundIfNeeded() {
        if (!foregroundStarted) return
        stopForeground(STOP_FOREGROUND_REMOVE)
        foregroundStarted = false
    }

    private fun buildNotification(): Notification {
        val stopIntent = Intent(this, RosaryMediaLibraryService::class.java).apply {
            action = ACTION_STOP
        }

        val stopPendingIntent = PendingIntent.getService(
            this,
            REQUEST_CODE_STOP,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or pendingIntentImmutableFlag()
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(resolveNotificationTitle())
            .setContentText(resolveNotificationSubtitle())
            .setOngoing(PlayerController.currentIsPlaying())
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
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

    private fun loadPacksIntoGateway() {
        val storedPacks = runBlocking {
            AppStore(applicationContext).loadPacks()
        }

        val packs = if (storedPacks.isNotEmpty()) {
            storedPacks
        } else {
            RosaryRepository.defaultPacks()
        }

        RosaryPlaybackGateway.setPacks(packs)
    }

    private fun buildChildren(parentId: String): List<MediaItem> {
        return when {
            RosaryMediaIds.isRoot(parentId) -> {
                RosaryPlaybackGateway.childrenOf(parentId).mapNotNull {
                    when (it) {
                        is RosaryMediaCatalog.Node.Pack -> buildPackItem(it)
                        else -> null
                    }
                }
            }

            RosaryMediaIds.isPack(parentId) -> {
                RosaryPlaybackGateway.childrenOf(parentId).mapNotNull {
                    when (it) {
                        is RosaryMediaCatalog.Node.Crown -> buildCrownItem(it)
                        else -> null
                    }
                }
            }

            else -> emptyList()
        }
    }

    private fun buildItem(mediaId: String): MediaItem? {
        if (RosaryMediaIds.isRoot(mediaId)) {
            return MediaItem.Builder()
                .setMediaId(RosaryMediaIds.root())
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(ROOT_TITLE)
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .build()
                )
                .build()
        }

        RosaryPlaybackGateway.findPackByMediaId(mediaId)?.let { pack ->
            return MediaItem.Builder()
                .setMediaId(RosaryMediaIds.pack(pack.id))
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(pack.name)
                        .setSubtitle(pack.description)
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .build()
                )
                .build()
        }

        RosaryPlaybackGateway.findCrownEntryByMediaId(mediaId)?.let { entry ->
            return MediaItem.Builder()
                .setMediaId(mediaId)
                .setUri("asset:///${entry.crown.audioTrack.assetPath}")
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(entry.crown.title)
                        .setSubtitle(entry.pack.name)
                        .setArtist(entry.pack.name)
                        .setIsBrowsable(false)
                        .setIsPlayable(true)
                        .build()
                )
                .build()
        }

        return null
    }

    private fun buildPackItem(node: RosaryMediaCatalog.Node.Pack): MediaItem {
        return MediaItem.Builder()
            .setMediaId(node.mediaId)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(node.title)
                    .setSubtitle(node.subtitle)
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .build()
            )
            .build()
    }

    private fun buildCrownItem(node: RosaryMediaCatalog.Node.Crown): MediaItem {
        return MediaItem.Builder()
            .setMediaId(node.mediaId)
            .setUri("asset:///${node.crown.audioTrack.assetPath}")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(node.title)
                    .setSubtitle(node.subtitle)
                    .setArtist(node.subtitle)
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .build()
            )
            .build()
    }

    private fun resolveMediaItems(mediaItems: List<MediaItem>): List<MediaItem> {
        return mediaItems.mapNotNull { resolveMediaItem(it) }
    }

    private fun resolveMediaItem(mediaItem: MediaItem): MediaItem? {
        if (mediaItem.mediaId.isBlank()) return null
        return buildItem(mediaItem.mediaId)
    }

    @OptIn(UnstableApi::class)
    private fun maybeExpandSingleItemToPlaylist(
        mediaItem: MediaItem,
        startIndex: Int,
        startPositionMs: Long
    ): MediaItemsWithStartPosition? {
        val mediaId = mediaItem.mediaId
        if (mediaId.isBlank()) return null

        if (RosaryMediaIds.isPack(mediaId)) {
            val playlist = buildChildren(mediaId)
            if (playlist.isEmpty()) return null

            return MediaItemsWithStartPosition(
                playlist,
                safeStartIndex(startIndex, playlist.size),
                startPositionMs
            )
        }

        if (RosaryMediaIds.isCrown(mediaId)) {
            val ref = RosaryMediaIds.parseCrown(mediaId) ?: return null
            val parentPack = RosaryMediaIds.pack(ref.packId)
            val playlist = buildChildren(parentPack)
            if (playlist.isEmpty()) return null

            val index = playlist.indexOfFirst { it.mediaId == mediaId }
            if (index < 0) return null

            return MediaItemsWithStartPosition(
                playlist,
                index,
                startPositionMs
            )
        }

        return null
    }

    private fun rememberCurrentFromPlaylist(
        playlist: List<MediaItem>,
        startIndex: Int
    ) {
        if (playlist.isEmpty()) {
            RosaryPlaybackGateway.clearCurrentMediaId()
            return
        }

        val safeIndex = safeStartIndex(startIndex, playlist.size)
        RosaryPlaybackGateway.rememberCurrentMediaId(playlist[safeIndex].mediaId)
    }

    private fun safeStartIndex(requestedIndex: Int, size: Int): Int {
        if (size <= 0) return 0
        return requestedIndex.coerceIn(0, size - 1)
    }
}