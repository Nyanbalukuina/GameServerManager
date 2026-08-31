package gameservermanager.palworld.application

import gameservermanager.palworld.domain.PalworldOperationHistoryEntry

interface PalworldOperationHistoryStore {
    fun append(entry: PalworldOperationHistoryEntry)

    fun latest(limit: Int): List<PalworldOperationHistoryEntry>
}
