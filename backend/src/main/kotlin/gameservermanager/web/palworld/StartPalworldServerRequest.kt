package gameservermanager.web.palworld

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank

data class StartPalworldServerRequest(
    @field:NotBlank(message = "インストール先を入力してください")
    val installPath: String,
    @field:Min(value = 1, message = "ゲームポートは1以上で入力してください")
    @field:Max(value = 65535, message = "ゲームポートは65535以下で入力してください")
    val gamePort: Int?,
    @field:Min(value = 1, message = "最大プレイヤー数は1以上で入力してください")
    @field:Max(value = 32, message = "最大プレイヤー数は32以下で入力してください")
    val maxPlayers: Int?,
)
