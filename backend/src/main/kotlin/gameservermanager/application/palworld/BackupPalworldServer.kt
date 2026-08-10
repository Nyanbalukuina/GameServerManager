package gameservermanager.application.palworld

import gameservermanager.application.preflight.ManagedPathPolicy
import gameservermanager.application.palworld.PalworldServerProcessManager
import gameservermanager.domain.palworld.PalworldBackupReport
import gameservermanager.domain.palworld.PalworldOperationHistoryEntry
import org.springframework.stereotype.Service
import java.time.Clock
import java.nio.file.Files
import java.nio.file.Path

@Service
class BackupPalworldServer(
    private val managedPathPolicy: ManagedPathPolicy,
    private val processManager: PalworldServerProcessManager,
    private val backupCreator: PalworldBackupCreator,
    private val historyStore: PalworldOperationHistoryStore,
) {
    fun execute(command: Command): PalworldBackupReport {
        require(managedPathPolicy.isServerPathAllowed(command.installPath)) {
            "インストール先が管理範囲外です"
        }
        require(processManager.current()?.alive != true) {
            "整合性を保つためPalworldサーバーを停止してからバックアップしてください"
        }

        val installPath = Path.of(command.installPath).toAbsolutePath().normalize()
        val savedPath = installPath.resolve("Pal/Saved")
        require(Files.isDirectory(savedPath)) {
            "PalworldのSavedフォルダーが見つかりません"
        }
        val backupDirectory = Path.of(managedPathPolicy.managedRoot())
            .resolve("backups/palworld-main/world")
        val result = backupCreator.create(savedPath, backupDirectory)
        historyStore.append(
            PalworldOperationHistoryEntry(
                Clock.systemUTC().instant(),
                "BACKUP",
                "COMPLETED",
                "Palworldのワールドデータをバックアップしました",
            ),
        )

        return PalworldBackupReport(
            completed = true,
            backupPath = result.backupPath,
            sizeBytes = result.sizeBytes,
            fileCount = result.fileCount,
        )
    }

    data class Command(
        val installPath: String,
    )
}
