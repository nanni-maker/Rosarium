package com.cambria.rosarium.repository

import com.cambria.rosarium.core.AudioTrack
import com.cambria.rosarium.core.CrownSet
import com.cambria.rosarium.core.CrownType
import com.cambria.rosarium.core.Mystery
import com.cambria.rosarium.core.RosaryPack

object RosaryRepository {

    fun defaultPacks(): List<RosaryPack> {
        return listOf(
            createItalianTemplatePack(
                id = "comastri",
                name = "Cardinale Angelo Comastri",
                description = "Rosario del Cardinal Angelo Comastri"
            ),
            createItalianTemplatePack(
                id = "betania",
                name = "Comunità di Betania",
                description = "Rosario della Comunità di Betania"
            ),
            createItalianTemplatePack(
                id = "cenacolo",
                name = "Comunità Cenacolo",
                description = "Rosario della Comunità Cenacolo"
            ),
            createItalianTemplatePack(
                id = "oasi_pace",
                name = "Comunità Oasi della Pace",
                description = "Rosario della Comunità Oasi della Pace"
            )
        )
    }

    fun createUserPack(name: String): RosaryPack {
        val trimmedName = name.trim()
        val safeName = if (trimmedName.isBlank()) "Nuovo Rosario" else trimmedName

        return createItalianTemplatePack(
            id = "user_${System.currentTimeMillis()}",
            name = safeName,
            description = "Pack personalizzato"
        )
    }

    private fun createItalianTemplatePack(
        id: String,
        name: String,
        description: String
    ): RosaryPack {

        fun mystery(number: Int, title: String): Mystery {
            return Mystery(
                number = number,
                title = title
            )
        }

        fun crown(
            type: CrownType,
            title: String,
            audioSuffix: String,
            mysteries: List<Mystery>
        ): CrownSet {
            return CrownSet(
                type = type,
                title = title,
                mysteries = mysteries,
                audioTrack = AudioTrack(
                    id = "${id}_${audioSuffix}",
                    title = title,
                    assetPath = "audio/${id}_${audioSuffix}.mp3"
                )
            )
        }

        return RosaryPack(
            id = id,
            name = name,
            description = description,
            language = "it",
            crowns = listOf(
                crown(
                    type = CrownType.JOYFUL,
                    title = "Misteri Gaudiosi",
                    audioSuffix = "joyful",
                    mysteries = listOf(
                        mystery(1, "L'Annunciazione dell'Angelo a Maria"),
                        mystery(2, "La Visitazione di Maria a Santa Elisabetta"),
                        mystery(3, "La Nascita di Gesù a Betlemme"),
                        mystery(4, "La Presentazione di Gesù al Tempio"),
                        mystery(5, "Il Ritrovamento di Gesù nel Tempio")
                    )
                ),
                crown(
                    type = CrownType.SORROWFUL,
                    title = "Misteri Dolorosi",
                    audioSuffix = "sorrowful",
                    mysteries = listOf(
                        mystery(1, "L'Agonia di Gesù nell'Orto"),
                        mystery(2, "La Flagellazione di Gesù"),
                        mystery(3, "L'Incoronazione di spine"),
                        mystery(4, "Gesù sale al Calvario portando la Croce"),
                        mystery(5, "La Crocifissione e Morte di Gesù")
                    )
                ),
                crown(
                    type = CrownType.GLORIOUS,
                    title = "Misteri Gloriosi",
                    audioSuffix = "glorious",
                    mysteries = listOf(
                        mystery(1, "La Risurrezione di Gesù"),
                        mystery(2, "L'Ascensione di Gesù al Cielo"),
                        mystery(3, "La Discesa dello Spirito Santo"),
                        mystery(4, "L'Assunzione di Maria al Cielo"),
                        mystery(5, "L'Incoronazione di Maria Regina del Cielo e della Terra")
                    )
                ),
                crown(
                    type = CrownType.LUMINOUS,
                    title = "Misteri Luminosi",
                    audioSuffix = "luminous",
                    mysteries = listOf(
                        mystery(1, "Il Battesimo di Gesù nel Giordano"),
                        mystery(2, "Le Nozze di Cana"),
                        mystery(3, "L'Annuncio del Regno di Dio"),
                        mystery(4, "La Trasfigurazione di Gesù"),
                        mystery(5, "L'Istituzione dell'Eucaristia")
                    )
                )
            )
        )
    }
}