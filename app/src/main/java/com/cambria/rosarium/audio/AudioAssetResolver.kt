package com.cambria.rosarium.audio

import com.cambria.rosarium.core.CrownType

object AudioAssetResolver {

    private const val BASE_PATH = "audio"

    fun buildAssetPath(packId: String, crownType: CrownType): String {
        return "$BASE_PATH/${packId}_${crownType.fileSuffix()}.mp3"
    }

    private fun CrownType.fileSuffix(): String {
        return when (this) {
            CrownType.JOYFUL -> "gioiosi"
            CrownType.SORROWFUL -> "dolorosi"
            CrownType.GLORIOUS -> "gloriosi"
            CrownType.LUMINOUS -> "luminosi"
        }
    }
}