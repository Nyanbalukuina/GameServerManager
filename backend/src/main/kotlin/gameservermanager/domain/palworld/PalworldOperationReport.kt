package gameservermanager.domain.palworld

data class PalworldOperationReport(
    val completed: Boolean,
    val operation: String,
    val message: String,
)
