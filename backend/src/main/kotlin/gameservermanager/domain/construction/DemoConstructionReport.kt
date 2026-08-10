package gameservermanager.domain.construction

data class DemoConstructionReport(
    val completed: Boolean,
    val mode: String,
    val workspacePath: String,
    val steps: List<ConstructionStep>,
)
