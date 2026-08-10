package gameservermanager.domain.authentication

import java.time.Instant

data class AdministratorCredentials(
    val version: Int = 1,
    val username: String = "admin",
    val passwordHash: String,
    val createdAt: Instant,
)

data class AuthenticationStatus(
    val configured: Boolean,
    val authenticated: Boolean,
    val username: String?,
    val setupAllowed: Boolean,
)
