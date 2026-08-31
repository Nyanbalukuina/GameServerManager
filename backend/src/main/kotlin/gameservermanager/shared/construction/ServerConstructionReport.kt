package gameservermanager.shared.construction

data class ServerConstructionReport(
    val completed: Boolean,
    val mode: String,
    val installPath: String,
    val steps: List<ConstructionStep>,
)
