package gameservermanager.web.server

import jakarta.validation.constraints.Pattern

data class DemoServerOperationRequest(
    @field:Pattern(regexp = "START|STOP|RESTART", message = "許可されていないデモ操作です")
    val action: String,
)
