package gameservermanager.web.palworld

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class ConfigurePalworldRequest(
    @field:NotBlank(message = "サーバー名を入力してください")
    @field:Size(max = 100, message = "サーバー名は100文字以内で入力してください")
    val serverName: String,
    @field:NotBlank(message = "インストール先を入力してください")
    val installPath: String,
    @field:Min(value = 1, message = "ゲームポートは1以上で入力してください")
    @field:Max(value = 65535, message = "ゲームポートは65535以下で入力してください")
    val gamePort: Int?,
    @field:Min(value = 1, message = "RCONポートは1以上で入力してください")
    @field:Max(value = 65535, message = "RCONポートは65535以下で入力してください")
    val rconPort: Int?,
    @field:Min(value = 1, message = "最大プレイヤー数は1以上で入力してください")
    @field:Max(value = 32, message = "最大プレイヤー数は32以下で入力してください")
    val maxPlayers: Int?,
    @field:Size(max = 100, message = "サーバーパスワードは100文字以内で入力してください")
    val serverPassword: String,
    @field:NotBlank(message = "管理者パスワードを入力してください")
    @field:Size(max = 100, message = "管理者パスワードは100文字以内で入力してください")
    val adminPassword: String,
)
