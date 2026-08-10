package gameservermanager.application.palworld

import gameservermanager.domain.palworld.PalworldOperationHistoryEntry

interface PalworldOperationHistoryStore {
    fun append(entry: PalworldOperationHistoryEntry)

    fun latest(limit: Int): List<PalworldOperationHistoryEntry>
}
