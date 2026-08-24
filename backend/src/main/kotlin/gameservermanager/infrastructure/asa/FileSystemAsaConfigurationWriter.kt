package gameservermanager.infrastructure.asa

import gameservermanager.application.asa.AsaConfigurationWriteCommand
import gameservermanager.application.asa.AsaConfigurationWriteResult
import gameservermanager.application.asa.AsaConfigurationWriter
import gameservermanager.application.asa.AsaGameUserSettingsIni
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Clock
import java.time.format.DateTimeFormatter

// GameUserSettings.iniをファイルシステムへ保存する。
@Component
class FileSystemAsaConfigurationWriter() : AsaConfigurationWriter {
    private var clock: Clock = Clock.systemUTC()

    // テストから固定時刻を渡せるようにする。
    constructor(clock: Clock) : this() {
        this.clock = clock
    }

    // 既存設定を読み込み、指定されたASA設定を保存する。
    override fun write(command: AsaConfigurationWriteCommand): AsaConfigurationWriteResult {
        val original = if (Files.isRegularFile(command.settingsPath)) {
            Files.readString(command.settingsPath, StandardCharsets.UTF_8)
        } else {
            ""
        }

        // 既存設定を残したまま対象項目を更新する。
        val updated = AsaGameUserSettingsIni.update(original, command.sections)
        val backupPath = backupExistingSettings(command)

        // 保存先を作成し、一時ファイル経由で安全に置き換える。
        Files.createDirectories(requireNotNull(command.settingsPath.parent))
        val temporaryPath = Files.createTempFile(command.settingsPath.parent, ".asa-settings-", ".tmp")

        try {
            Files.writeString(temporaryPath, updated, StandardCharsets.UTF_8)
            moveReplacing(temporaryPath, command.settingsPath)
        } finally {
            Files.deleteIfExists(temporaryPath)
        }

        return AsaConfigurationWriteResult(
            settingsPath = command.settingsPath.toString(),
            backupPath = backupPath?.toString(),
        )
    }

    // 既存のGameUserSettings.iniがある場合だけバックアップする。
    private fun backupExistingSettings(command: AsaConfigurationWriteCommand): Path? {
        if (!Files.isRegularFile(command.settingsPath)) return null

        Files.createDirectories(command.backupDirectory)
        val timestamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS")
            .withZone(clock.zone)
            .format(clock.instant())
        val backupPath = command.backupDirectory.resolve("GameUserSettings-$timestamp.ini")
        Files.copy(command.settingsPath, backupPath)

        return backupPath
    }

    // 対応している環境では原子的にファイルを置き換える。
    private fun moveReplacing(source: Path, destination: Path) {
        try {
            Files.move(
                source,
                destination,
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING,
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING)
        }
    }
}