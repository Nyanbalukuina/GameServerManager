package gameservermanager.web

data class ApiValidationError(
    val errors: Map<String, String>,
)

