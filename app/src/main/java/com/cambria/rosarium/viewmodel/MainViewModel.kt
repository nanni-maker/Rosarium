package com.cambria.rosarium.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.cambria.rosarium.core.CrownSet
import com.cambria.rosarium.core.RosaryCalendar
import com.cambria.rosarium.core.RosaryPack
import com.cambria.rosarium.media.RosaryPlaybackGateway
import com.cambria.rosarium.repository.RosaryRepository
import java.time.LocalDate

class MainViewModel : ViewModel() {

    var packs by mutableStateOf(RosaryRepository.defaultPacks())
        private set

    var activePack by mutableStateOf(packs.first())
        private set

    var currentCrown by mutableStateOf(RosaryCalendar.crownForDate(LocalDate.now()))
        private set

    init {
        syncGatewayPacks()
    }

    val currentCrownSet: CrownSet
        get() = activePack.crowns.first { it.type == currentCrown }

    fun loadPersistedState(
        storedPacks: List<RosaryPack>,
        storedActivePackId: String?
    ) {
        if (storedPacks.isNotEmpty()) {
            packs = storedPacks
            activePack = storedPacks.first()
            syncGatewayPacks()
        }

        if (!storedActivePackId.isNullOrBlank()) {
            selectPack(storedActivePackId)
        } else if (packs.isNotEmpty()) {
            activePack = packs.first()
        }

        syncCurrentCrownToToday()
    }

    fun syncCurrentCrownToToday() {
        currentCrown = RosaryCalendar.crownForDate(LocalDate.now())
    }

    fun nextCrown() {
        currentCrown = RosaryCalendar.next(currentCrown)
    }

    fun previousCrown() {
        currentCrown = RosaryCalendar.previous(currentCrown)
    }

    fun selectPack(packId: String) {
        val selected = packs.firstOrNull { it.id == packId } ?: return
        activePack = selected
    }

    fun addPack(name: String): RosaryPack {
        val newPack = RosaryRepository.createUserPack(name)
        packs = packs + newPack
        activePack = newPack
        syncGatewayPacks()
        return newPack
    }

    fun updatePackMetadata(
        packId: String,
        newName: String,
        newDescription: String
    ) {
        val cleanName = newName.trim()
        val cleanDescription = newDescription.trim()

        if (cleanName.isBlank()) return

        packs = packs.map { pack ->
            if (pack.id == packId) {
                pack.copy(
                    name = cleanName,
                    description = cleanDescription
                )
            } else {
                pack
            }
        }

        activePack = packs.firstOrNull { it.id == activePack.id } ?: packs.first()
        syncGatewayPacks()
    }

    fun canDeletePack(pack: RosaryPack): Boolean {
        return packs.size > 1
    }

    fun canEditPack(pack: RosaryPack): Boolean {
        return true
    }

    fun canMovePackUp(pack: RosaryPack): Boolean {
        val index = packs.indexOfFirst { it.id == pack.id }
        return index > 0
    }

    fun canMovePackDown(pack: RosaryPack): Boolean {
        val index = packs.indexOfFirst { it.id == pack.id }
        return index in 0 until packs.lastIndex
    }

    fun movePackUp(packId: String): Boolean {
        val index = packs.indexOfFirst { it.id == packId }
        if (index <= 0) return false

        val mutable = packs.toMutableList()
        val temp = mutable[index - 1]
        mutable[index - 1] = mutable[index]
        mutable[index] = temp
        packs = mutable.toList()

        activePack = packs.firstOrNull { it.id == activePack.id } ?: packs.first()
        syncGatewayPacks()
        return true
    }

    fun movePackDown(packId: String): Boolean {
        val index = packs.indexOfFirst { it.id == packId }
        if (index < 0 || index >= packs.lastIndex) return false

        val mutable = packs.toMutableList()
        val temp = mutable[index + 1]
        mutable[index + 1] = mutable[index]
        mutable[index] = temp
        packs = mutable.toList()

        activePack = packs.firstOrNull { it.id == activePack.id } ?: packs.first()
        syncGatewayPacks()
        return true
    }

    fun deletePack(packId: String): Boolean {
        if (packs.size <= 1) {
            return false
        }

        val updated = packs.filterNot { it.id == packId }
        if (updated.isEmpty() || updated.size == packs.size) {
            return false
        }

        packs = updated

        if (activePack.id == packId) {
            activePack = packs.first()
        } else {
            activePack = packs.firstOrNull { it.id == activePack.id } ?: packs.first()
        }

        syncGatewayPacks()
        return true
    }

    fun crownDisplayName(): String {
        return currentCrownSet.title
    }

    fun firstMysteryDisplayName(): String {
        val firstMystery = currentCrownSet.mysteries.firstOrNull()
        return firstMystery?.title ?: ""
    }

    private fun syncGatewayPacks() {
        RosaryPlaybackGateway.setPacks(packs)
    }
}