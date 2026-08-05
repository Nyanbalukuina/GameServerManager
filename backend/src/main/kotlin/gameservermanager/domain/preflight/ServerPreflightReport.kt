package gameservermanager.domain.preflight

data class ServerPreflightReport(
    val canProceed: Boolean,
    val checks: List<PreflightCheck>,
)

