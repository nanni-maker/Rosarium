package com.cambria.rosarium.media

import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.MediaItemsWithStartPosition
import com.cambria.rosarium.data.AppStore
import com.cambria.rosarium.player.PlayerController
import com.cambria.rosarium.repository.RosaryRepository
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.runBlocking

class RosaryMediaLibraryService : MediaLibraryService() {

    companion object {
        private const val SESSION_ID = "rosarium_media_library_session"
        private const val ROOT_TITLE = "Rosarium"
    }

    private var mediaLibrarySession: MediaLibrarySession? = null

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

        PlayerController.initialize(applicationContext)
        loadPacksIntoGateway()

        mediaLibrarySession = MediaLibrarySession.Builder(
            this,
            PlayerController.requireExoPlayer(),
            callback
        )
            .setId(SESSION_ID)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return mediaLibrarySession
    }

    override fun onDestroy() {
        mediaLibrarySession?.release()
        mediaLibrarySession = null
        super.onDestroy()
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
        RosaryPlaybackGateway.rememberCurrentMediaId(
            playlist[safeIndex].mediaId
        )
    }

    private fun safeStartIndex(requestedIndex: Int, size: Int): Int {
        if (size <= 0) return 0
        return requestedIndex.coerceIn(0, size - 1)
    }
}