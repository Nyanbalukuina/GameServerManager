package gameservermanager.palworld.web

import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class UpdateDemoPalworldSettingsRequest(
    @field:NotBlank(message = "サーバー名を入力してください")
    @field:Size(max = 100, message = "サーバー名は100文字以内で入力してください")
    val serverName: String,
    @field:Size(max = 500, message = "サーバー説明は500文字以内で入力してください")
    val serverDescription: String,
    @field:Min(value = 1, message = "最大プレイヤー数は1以上で入力してください")
    @field:Max(value = 32, message = "最大プレイヤー数は32以下で入力してください")
    val maxPlayers: Int,
    @field:Size(max = 100, message = "サーバーパスワードは100文字以内で入力してください")
    val serverPassword: String = "",
    @field:Size(max = 100, message = "管理者パスワードは100文字以内で入力してください")
    val adminPassword: String = "",
    @field:DecimalMin(value = "0.1", message = "経験値倍率は0.1以上で入力してください")
    @field:DecimalMax(value = "5.0", message = "経験値倍率は5.0以下で入力してください")
    val expRate: Double,
    @field:DecimalMin(value = "0.1", message = "捕獲率は0.1以上で入力してください")
    @field:DecimalMax(value = "5.0", message = "捕獲率は5.0以下で入力してください")
    val palCaptureRate: Double,
    @field:DecimalMin(value = "0.1", message = "パル出現倍率は0.1以上で入力してください")
    @field:DecimalMax(value = "3.0", message = "パル出現倍率は3.0以下で入力してください")
    val palSpawnRate: Double,
    @field:DecimalMin(value = "0.1", message = "ドロップ倍率は0.1以上で入力してください")
    @field:DecimalMax(value = "5.0", message = "ドロップ倍率は5.0以下で入力してください")
    val enemyDropRate: Double,
    @field:DecimalMin(value = "0.0", message = "タマゴ孵化時間は0以上で入力してください")
    @field:DecimalMax(value = "240.0", message = "タマゴ孵化時間は240以下で入力してください")
    val eggHatchingTime: Double,
    @field:Pattern(regexp = "None|Item|ItemAndEquipment|All", message = "デスペナルティを選択してください")
    val deathPenalty: String,
    val pvpEnabled: Boolean,
    val friendlyFireEnabled: Boolean,
    @field:Min(value = 1, message = "拠点数は1以上で入力してください")
    @field:Max(value = 128, message = "拠点数は128以下で入力してください")
    val baseCampMaxNum: Int,
    @field:Min(value = 1, message = "拠点作業パル数は1以上で入力してください")
    @field:Max(value = 50, message = "拠点作業パル数は50以下で入力してください")
    val baseCampWorkerMaxNum: Int,
)
