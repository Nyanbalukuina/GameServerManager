package gameservermanager.domain.asa

// ASA設定ファイルの作成結果を保持する。
data class AsaConfigurationReport(
    val completed: Boolean,
    val settingsPath: String,
    val backupPath: String?,
    val configuredKeys: List<String>,
)