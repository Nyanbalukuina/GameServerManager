package com.github.gameservermanager.web.api

data class ApiValidationError(
    val errors: Map<String, String>,
)

