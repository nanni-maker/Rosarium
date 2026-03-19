package com.cambria.rosarium.media

import com.cambria.rosarium.core.CrownSet
import com.cambria.rosarium.core.RosaryPack

class RosaryMediaCatalog(
    private val packs: List<RosaryPack>
) {

    fun rootId(): String = RosaryMediaIds.root()

    fun rootEntry(): Node = Node.Root(
        mediaId = RosaryMediaIds.root(),
        title = "Rosarium"
    )

    fun packEntries(): List<Node.Pack> {
        return packs.map { pack ->
            Node.Pack(
                mediaId = RosaryMediaIds.pack(pack.id),
                packId = pack.id,
                title = pack.name,
                subtitle = pack.description
            )
        }
    }

    fun crownEntries(packId: String): List<Node.Crown> {
        val pack = findPackById(packId) ?: return emptyList()

        return pack.crowns.map { crown ->
            Node.Crown(
                mediaId = RosaryMediaIds.crown(pack.id, crown.type),
                packId = pack.id,
                title = crown.title,
                subtitle = pack.name,
                crown = crown
            )
        }
    }

    fun childrenOf(mediaId: String): List<Node> {
        return when {
            RosaryMediaIds.isRoot(mediaId) -> packEntries()
            RosaryMediaIds.isPack(mediaId) -> {
                val packId = RosaryMediaIds.parsePackId(mediaId) ?: return emptyList()
                crownEntries(packId)
            }
            else -> emptyList()
        }
    }

    fun findPackByMediaId(mediaId: String): RosaryPack? {
        val packId = RosaryMediaIds.parsePackId(mediaId) ?: return null
        return findPackById(packId)
    }

    fun findPackById(packId: String): RosaryPack? {
        return packs.firstOrNull { it.id == packId }
    }

    fun findCrownByMediaId(mediaId: String): CrownSet? {
        return findCrownEntryByMediaId(mediaId)?.crown
    }

    fun findCrownEntryByMediaId(mediaId: String): CrownEntry? {
        val ref = RosaryMediaIds.parseCrown(mediaId) ?: return null
        val pack = findPackById(ref.packId) ?: return null
        val crown = pack.crowns.firstOrNull { it.type == ref.crownType } ?: return null

        return CrownEntry(
            mediaId = mediaId,
            pack = pack,
            crown = crown
        )
    }

    fun contains(mediaId: String): Boolean {
        return when {
            RosaryMediaIds.isRoot(mediaId) -> true
            RosaryMediaIds.isPack(mediaId) -> findPackByMediaId(mediaId) != null
            RosaryMediaIds.isCrown(mediaId) -> findCrownByMediaId(mediaId) != null
            else -> false
        }
    }

    data class CrownEntry(
        val mediaId: String,
        val pack: RosaryPack,
        val crown: CrownSet
    )

    sealed class Node {
        abstract val mediaId: String
        abstract val title: String

        data class Root(
            override val mediaId: String,
            override val title: String
        ) : Node()

        data class Pack(
            override val mediaId: String,
            val packId: String,
            override val title: String,
            val subtitle: String
        ) : Node()

        data class Crown(
            override val mediaId: String,
            val packId: String,
            override val title: String,
            val subtitle: String,
            val crown: CrownSet
        ) : Node()
    }
}