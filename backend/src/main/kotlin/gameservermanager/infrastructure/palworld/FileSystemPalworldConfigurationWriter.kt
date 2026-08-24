package gameservermanager.infrastructure.palworld

import gameservermanager.application.palworld.PalworldConfigurationWriteCommand
import gameservermanager.application.palworld.PalworldConfigurationWriteResult
import gameservermanager.application.palworld.PalworldConfigurationWriter
import gameservermanager.application.palworld.PalworldSettingsIni
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Clock
import java.time.format.DateTimeFormatter

@Component
class FileSystemPalworldConfigurationWriter() : PalworldConfigurationWriter {
    private var clock: Clock = Clock.systemUTC()

    constructor(clock: Clock) : this() {
        this.clock = clock
    }

    override fun write(command: PalworldConfigurationWriteCommand): PalworldConfigurationWriteResult {
        require(Files.isRegularFile(command.defaultSettingsPath)) {
            "DefaultPalWorldSettings.iniが見つかりません。先にPalworldをインストールしてください"
        }

        val source = if (Files.isRegularFile(command.settingsPath)) {
            command.settingsPath
        } else {
            command.defaultSettingsPath
        }
        val original = Files.readString(source, StandardCharsets.UTF_8)
        val updated = PalworldSettingsIni.update(original, command.values)
        val backupPath = backupExistingSettings(command)

        Files.createDirectories(requireNotNull(command.settingsPath.parent))
        val temporaryPath = Files.createTempFile(
            command.settingsPath.parent,
            ".palworld-settings-",
            ".tmp",
        )
        try {
            Files.writeString(temporaryPath, updated, StandardCharsets.UTF_8)
            moveReplacing(temporaryPath, command.settingsPath)
        } finally {
            Files.deleteIfExists(temporaryPath)
        }

        return PalworldConfigurationWriteResult(
            settingsPath = command.settingsPath.toString(),
            backupPath = backupPath?.toString(),
        )
    }

    private fun backupExistingSettings(command: PalworldConfigurationWriteCommand): Path? {
        if (!Files.isRegularFile(command.settingsPath)) {
            return null
        }

        Files.createDirectories(command.backupDirectory)
        val timestamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS")
            .withZone(clock.zone)
            .format(clock.instant())
        val backupPath = command.backupDirectory.resolve("PalWorldSettings-$timestamp.ini")
        Files.copy(command.settingsPath, backupPath)
        return backupPath
    }

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
