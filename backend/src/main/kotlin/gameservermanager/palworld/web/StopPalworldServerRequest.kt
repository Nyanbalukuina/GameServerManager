package gameservermanager.palworld.web

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank

data class StopPalworldServerRequest(
    @field:Min(1)
    @field:Max(65535)
    val restApiPort: Int?,
    @field:NotBlank(message = "管理者パスワードを入力してください")
    val adminPassword: String,
)
