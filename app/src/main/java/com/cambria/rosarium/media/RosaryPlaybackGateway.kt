package com.cambria.rosarium.media

import com.cambria.rosarium.core.CrownSet
import com.cambria.rosarium.core.RosaryPack
import com.cambria.rosarium.player.PlayerController

object RosaryPlaybackGateway {

    private var packs: List<RosaryPack> = emptyList()
    private var currentMediaId: String? = null

    fun setPacks(packs: List<RosaryPack>) {
        this.packs = packs
        if (currentMediaId != null && !contains(currentMediaId!!)) {
            currentMediaId = null
        }
    }

    fun rememberCurrentMediaId(mediaId: String?) {
        currentMediaId = mediaId?.takeIf { contains(it) }
    }

    fun clearCurrentMediaId() {
        currentMediaId = null
    }

    fun rootId(): String {
        return RosaryMediaIds.root()
    }

    fun childrenOf(mediaId: String): List<RosaryMediaCatalog.Node> {
        return catalog().childrenOf(mediaId)
    }

    fun contains(mediaId: String): Boolean {
        return catalog().contains(mediaId)
    }

    fun play(mediaId: String): Boolean {
        val crown = catalog().findCrownByMediaId(mediaId) ?: return false
        PlayerController.playOrPause(crown)
        currentMediaId = mediaId
        return true
    }

    fun playIfNotCurrent(mediaId: String): Boolean {
        val targetCrown = catalog().findCrownByMediaId(mediaId) ?: return false
        val currentCrown = PlayerController.currentCrown()
        val isCurrentMedia = currentMediaId == mediaId

        if (isCurrentMedia && currentCrown != null && areSameCrown(currentCrown, targetCrown)) {
            if (!PlayerController.currentIsPlaying()) {
                PlayerController.playOrPause(targetCrown)
            }
            return true
        }

        if (PlayerController.currentIsPlaying() && currentCrown != null) {
            PlayerController.stop()
        }

        PlayerController.playOrPause(targetCrown)
        currentMediaId = mediaId
        return true
    }

    fun toggleCurrent() {
        PlayerController.toggleCurrent()

        if (PlayerController.currentCrown() == null || !containsCurrentMedia()) {
            currentMediaId = null
        }
    }

    fun pause() {
        PlayerController.pause()
    }

    fun stop() {
        PlayerController.stop()
        currentMediaId = null
    }

    fun currentMediaId(): String? {
        if (!containsCurrentMedia()) {
            return null
        }
        return currentMediaId
    }

    fun currentCrown(): CrownSet? {
        val mediaId = currentMediaId ?: return PlayerController.currentCrown()
        return catalog().findCrownByMediaId(mediaId) ?: PlayerController.currentCrown()
    }

    fun currentIsPlaying(): Boolean {
        return PlayerController.currentIsPlaying()
    }

    fun currentEntry(): RosaryMediaCatalog.CrownEntry? {
        val mediaId = currentMediaId() ?: return null
        return catalog().findCrownEntryByMediaId(mediaId)
    }

    fun currentTitle(): String? {
        return currentEntry()?.crown?.title
    }

    fun currentSubtitle(): String? {
        return currentEntry()?.pack?.name
    }

    fun findPackByMediaId(mediaId: String): RosaryPack? {
        return catalog().findPackByMediaId(mediaId)
    }

    fun findCrownByMediaId(mediaId: String): CrownSet? {
        return catalog().findCrownByMediaId(mediaId)
    }

    fun findCrownEntryByMediaId(mediaId: String): RosaryMediaCatalog.CrownEntry? {
        return catalog().findCrownEntryByMediaId(mediaId)
    }

    private fun containsCurrentMedia(): Boolean {
        val mediaId = currentMediaId ?: return false
        return catalog().contains(mediaId)
    }

    private fun catalog(): RosaryMediaCatalog {
        return RosaryMediaCatalog(packs)
    }

    private fun areSameCrown(first: CrownSet, second: CrownSet): Boolean {
        return first.type == second.type &&
            first.title == second.title &&
            first.audioTrack.assetPath == second.audioTrack.assetPath
    }
}