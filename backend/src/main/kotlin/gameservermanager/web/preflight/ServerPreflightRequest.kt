package gameservermanager.web.preflight

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class ServerPreflightRequest(
    @field:NotBlank(message = "インストール先を入力してください")
    val installPath: String = "",

    @field:NotBlank(message = "SteamCMDの保存先を入力してください")
    val steamCmdPath: String = "",

    @field:NotNull(message = "ゲームポートを入力してください")
    @field:Min(1)
    @field:Max(65535)
    val gamePort: Int? = null,

    @field:NotNull(message = "RCONポートを入力してください")
    @field:Min(1)
    @field:Max(65535)
    val rconPort: Int? = null,
)
