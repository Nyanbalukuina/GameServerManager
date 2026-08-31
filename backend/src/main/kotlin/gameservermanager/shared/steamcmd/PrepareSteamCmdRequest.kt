package gameservermanager.shared.steamcmd

import jakarta.validation.constraints.NotBlank

data class PrepareSteamCmdRequest(
    @field:NotBlank(message = "SteamCMDの保存先を入力してください")
    val installPath: String = "",
)
