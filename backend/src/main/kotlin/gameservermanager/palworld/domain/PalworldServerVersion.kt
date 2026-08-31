package gameservermanager.palworld.domain

import java.time.Instant

data class PalworldServerVersion(
    val currentBuildId: String,
    val latestBuildId: String,
    val updateAvailable: Boolean,
    val checkedAt: Instant,
    val message: String,
)
