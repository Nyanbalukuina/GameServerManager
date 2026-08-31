package gameservermanager.shared.construction

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin

data class NewServerRequest(
    @field:NotBlank(message = "サーバー名を入力してください")
    @field:Size(max = 100, message = "サーバー名は100文字以内で入力してください")
    val serverName: String = "",

    @field:NotBlank(message = "インストール先を入力してください")
    @field:Size(max = 500, message = "インストール先は500文字以内で入力してください")
    val installPath: String = "",

    @field:NotBlank(message = "SteamCMDの保存先を入力してください")
    @field:Size(max = 500, message = "SteamCMDの保存先は500文字以内で入力してください")
    val steamCmdPath: String = "",

    @field:NotNull(message = "ゲームポートを入力してください")
    @field:Min(value = 1, message = "ゲームポートは1以上で入力してください")
    @field:Max(value = 65535, message = "ゲームポートは65535以下で入力してください")
    val gamePort: Int? = null,

    @field:NotNull(message = "RCONポートを入力してください")
    @field:Min(value = 1, message = "RCONポートは1以上で入力してください")
    @field:Max(value = 65535, message = "RCONポートは65535以下で入力してください")
    val rconPort: Int? = null,

    @field:NotNull(message = "最大プレイヤー数を入力してください")
    @field:Min(value = 1, message = "最大プレイヤー数は1人以上で入力してください")
    @field:Max(value = 32, message = "最大プレイヤー数は32人以下で入力してください")
    val maxPlayers: Int? = null,

    @field:Size(max = 64, message = "サーバーパスワードは64文字以内で入力してください")
    val serverPassword: String = "",

    @field:NotBlank(message = "管理者パスワードを入力してください")
    @field:Size(min = 8, max = 64, message = "管理者パスワードは8文字以上64文字以内で入力してください")
    val adminPassword: String = "",

    val automationEnabled: Boolean = false,

    @field:Pattern(regexp = "(?:[01]\\d|2[0-3]):[0-5]\\d", message = "停止時刻をHH:mm形式で入力してください")
    val shutdownTime: String = "04:00",

    @field:Pattern(regexp = "(?:[01]\\d|2[0-3]):[0-5]\\d", message = "起動時刻をHH:mm形式で入力してください")
    val startupTime: String = "09:00",


    val allowLocalSubnet: Boolean = true,

    val allowTailscale: Boolean = true,

    @field:Size(max = 2000, message = "接続元の手動指定は2000文字以内で入力してください")
    val customRemoteAddresses: String = "",

    val allowAnyRemoteAddress: Boolean = false,

    @field:Size(max = 500) val serverDescription: String = "",
    @field:DecimalMin("0.1") @field:DecimalMax("5.0") val expRate: Double = 1.0,
    @field:DecimalMin("0.1") @field:DecimalMax("5.0") val palCaptureRate: Double = 1.0,
    @field:DecimalMin("0.1") @field:DecimalMax("3.0") val palSpawnRate: Double = 1.0,
    @field:DecimalMin("0.1") @field:DecimalMax("5.0") val enemyDropRate: Double = 1.0,
    @field:DecimalMin("0.0") @field:DecimalMax("240.0") val eggHatchingTime: Double = 2.0,
    @field:Pattern(regexp = "None|Item|ItemAndEquipment|All") val deathPenalty: String = "All",
    val pvpEnabled: Boolean = false,
    val friendlyFireEnabled: Boolean = false,
    @field:Min(1) @field:Max(128) val baseCampMaxNum: Int = 128,
    @field:Min(1) @field:Max(50) val baseCampWorkerMaxNum: Int = 15,
)
