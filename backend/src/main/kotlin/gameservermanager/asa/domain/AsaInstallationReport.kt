package gameservermanager.asa.domain

// ASA専用サーバーのインストール結果を保持する。
data class AsaInstallationReport(
    val completed: Boolean,
    val appId: Int,
    val installPath: String,
    val executablePath: String,
    val logPath: String,
    val exitCode: Int,
)