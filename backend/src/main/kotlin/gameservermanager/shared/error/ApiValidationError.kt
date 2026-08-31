package gameservermanager.shared.error

data class ApiValidationError(
    val errors: Map<String, String>,
)
