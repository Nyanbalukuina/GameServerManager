package gameservermanager.asa.domain

import java.time.Instant

data class AsaServerVersion(
    val currentBuildId: String,
    val latestBuildId: String,
    val updateAvailable: Boolean,
    val checkedAt: Instant,
    val message: String,
)
