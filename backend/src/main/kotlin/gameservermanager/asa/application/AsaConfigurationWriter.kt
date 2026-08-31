package gameservermanager.asa.application

import java.nio.file.Path

// ASA設定ファイルの保存方法を定義する。
interface AsaConfigurationWriter {
    fun write(command: AsaConfigurationWriteCommand): AsaConfigurationWriteResult
}

// ASA設定ファイルの保存に必要な情報を保持する。
data class AsaConfigurationWriteCommand(
    val settingsPath: Path,
    val backupDirectory: Path,
    val sections: Map<String, Map<String, String>>,
)

// ASA設定ファイルの保存結果を保持する。
data class AsaConfigurationWriteResult(
    val settingsPath: String,
    val backupPath: String?,
)