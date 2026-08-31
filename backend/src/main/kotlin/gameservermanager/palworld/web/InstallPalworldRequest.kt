package gameservermanager.palworld.web

import jakarta.validation.constraints.NotBlank

data class InstallPalworldRequest(
    @field:NotBlank(message = "SteamCMDの保存先を入力してください")
    val steamCmdPath: String,
    @field:NotBlank(message = "インストール先を入力してください")
    val installPath: String,
)
