package com.cambria.rosarium.media

import com.cambria.rosarium.core.CrownType

object RosaryMediaIds {

    const val ROOT = "root"

    private const val PREFIX_PACK = "pack"
    private const val PREFIX_CROWN = "crown"

    fun root(): String = ROOT

    fun pack(packId: String): String {
        require(packId.isNotBlank()) { "packId must not be blank" }
        return "$PREFIX_PACK:$packId"
    }

    fun crown(packId: String, crownType: CrownType): String {
        require(packId.isNotBlank()) { "packId must not be blank" }
        return "$PREFIX_CROWN:$packId:${crownType.token}"
    }

    fun isRoot(mediaId: String?): Boolean {
        return mediaId == ROOT
    }

    fun isPack(mediaId: String?): Boolean {
        if (mediaId.isNullOrBlank()) return false
        val parts = mediaId.split(':')
        return parts.size == 2 && parts[0] == PREFIX_PACK && parts[1].isNotBlank()
    }

    fun isCrown(mediaId: String?): Boolean {
        return parseCrown(mediaId) != null
    }

    fun parsePackId(mediaId: String?): String? {
        if (!isPack(mediaId)) return null
        return mediaId!!.substringAfter(':')
    }

    fun parseCrown(mediaId: String?): CrownRef? {
        if (mediaId.isNullOrBlank()) return null

        val parts = mediaId.split(':')
        if (parts.size != 3) return null
        if (parts[0] != PREFIX_CROWN) return null

        val packId = parts[1].trim()
        val crownToken = parts[2].trim()

        if (packId.isEmpty() || crownToken.isEmpty()) return null

        val crownType = crownTypeFromToken(crownToken) ?: return null

        return CrownRef(
            packId = packId,
            crownType = crownType
        )
    }

    private fun crownTypeFromToken(token: String): CrownType? {
        return when (token.lowercase()) {
            "joyful" -> CrownType.JOYFUL
            "sorrowful" -> CrownType.SORROWFUL
            "glorious" -> CrownType.GLORIOUS
            "luminous" -> CrownType.LUMINOUS
            else -> null
        }
    }

    data class CrownRef(
        val packId: String,
        val crownType: CrownType
    )
}

val CrownType.token: String
    get() = when (this) {
        CrownType.JOYFUL -> "joyful"
        CrownType.SORROWFUL -> "sorrowful"
        CrownType.GLORIOUS -> "glorious"
        CrownType.LUMINOUS -> "luminous"
    }