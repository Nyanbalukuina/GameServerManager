package gameservermanager.palworld.domain

data class PalworldOperationReport(
    val completed: Boolean,
    val operation: String,
    val message: String,
)
