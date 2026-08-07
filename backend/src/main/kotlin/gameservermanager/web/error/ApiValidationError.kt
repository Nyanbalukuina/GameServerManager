package gameservermanager.web.error

data class ApiValidationError(
    val errors: Map<String, String>,
)
