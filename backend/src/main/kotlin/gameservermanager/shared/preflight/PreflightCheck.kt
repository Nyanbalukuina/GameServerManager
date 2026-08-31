package gameservermanager.shared.preflight

data class PreflightCheck(
    val id: String,
    val label: String,
    val status: PreflightStatus,
    val message: String,
)

enum class PreflightStatus {
    PASS,
    WARNING,
    ERROR,
}

