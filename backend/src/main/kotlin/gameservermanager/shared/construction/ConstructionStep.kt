package gameservermanager.shared.construction

data class ConstructionStep(
    val id: String,
    val label: String,
    val status: ConstructionStepStatus,
    val message: String,
)

enum class ConstructionStepStatus {
    COMPLETED,
    ERROR,
}
