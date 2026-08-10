package gameservermanager.web.authentication

import gameservermanager.application.authentication.AdministratorCredentialStore
import gameservermanager.application.authentication.SetupAdministrator
import gameservermanager.domain.authentication.AuthenticationStatus
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.security.web.csrf.CsrfToken
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthenticationController(
    private val credentialStore: AdministratorCredentialStore,
    private val setupAdministrator: SetupAdministrator,
) {
    @GetMapping("/status")
    fun status(
        authentication: Authentication?,
        servletRequest: HttpServletRequest,
    ): AuthenticationStatus {
        val configured = credentialStore.exists()
        val authenticated = authentication?.isAuthenticated == true && authentication.name != "anonymousUser"
        return AuthenticationStatus(
            configured = configured,
            authenticated = authenticated,
            username = if (authenticated) authentication?.name else null,
            setupAllowed = !configured && isLoopback(servletRequest.remoteAddr),
        )
    }

    @GetMapping("/csrf")
    fun csrf(csrfToken: CsrfToken): CsrfResponse {
        return CsrfResponse(csrfToken.token, csrfToken.headerName)
    }

    @PostMapping("/setup")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun setup(
        @Valid @RequestBody request: SetupAdministratorRequest,
        servletRequest: HttpServletRequest,
    ) {
        setupAdministrator.execute(
            SetupAdministrator.Command(
                password = request.password,
                passwordConfirmation = request.passwordConfirmation,
                remoteAddress = servletRequest.remoteAddr,
            ),
        )
    }

    private fun isLoopback(address: String): Boolean {
        return runCatching { java.net.InetAddress.getByName(address).isLoopbackAddress }
            .getOrDefault(false)
    }
}

data class CsrfResponse(
    val token: String,
    val headerName: String,
)
