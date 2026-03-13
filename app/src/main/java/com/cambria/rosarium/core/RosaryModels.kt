package com.cambria.rosarium.core

data class RosaryPack(
    val id: String,
    val name: String,
    val description: String,
    val language: String,
    val crowns: List<CrownSet>
)

data class CrownSet(
    val type: CrownType,
    val title: String,
    val mysteries: List<Mystery>,
    val audioTrack: AudioTrack
)

data class Mystery(
    val number: Int,
    val title: String
)

data class AudioTrack(
    val id: String,
    val title: String,
    val assetPath: String
)

enum class CrownType {
    JOYFUL,
    SORROWFUL,
    GLORIOUS,
    LUMINOUS
}