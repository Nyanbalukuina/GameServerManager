package gameservermanager.palworld.domain

import java.time.Instant

data class PalworldOperationHistoryEntry(
    val timestamp: Instant,
    val operation: String,
    val status: String,
    val message: String,
)
