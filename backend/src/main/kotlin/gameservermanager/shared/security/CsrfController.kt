package gameservermanager.shared.security

import org.springframework.security.web.csrf.CsrfToken
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/security")
class CsrfController {
    @GetMapping("/csrf")
    fun csrf(csrfToken: CsrfToken): CsrfResponse {
        return CsrfResponse(csrfToken.token, csrfToken.headerName)
    }
}

data class CsrfResponse(
    val token: String,
    val headerName: String,
)
