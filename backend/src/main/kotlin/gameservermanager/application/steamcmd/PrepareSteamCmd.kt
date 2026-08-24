package gameservermanager.application.steamcmd

import gameservermanager.application.preflight.ManagedPathPolicy
import gameservermanager.domain.server.steamcmd.SteamCmdPreparationReport
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.Comparator

@Service
class PrepareSteamCmd(
    private val managedPathPolicy: ManagedPathPolicy,
    private val archiveDownloader: SteamCmdArchiveDownloader,
    private val archiveExtractor: SteamCmdArchiveExtractor,
) {
    fun execute(command: Command): SteamCmdPreparationReport {
        require(managedPathPolicy.isToolPathAllowed(command.installPath)) {
            "SteamCMD保存先が管理範囲外です"
        }

        val installPath = Path.of(command.installPath).toAbsolutePath().normalize()
        val parentPath = requireNotNull(installPath.parent) {
            "SteamCMD保存先の親フォルダーを確認できません"
        }
        Files.createDirectories(parentPath)

        val workingDirectory = Files.createTempDirectory(parentPath, ".steamcmd-prepare-")
        try {
            val archivePath = workingDirectory.resolve("steamcmd.zip")
            val downloadedArchive = archiveDownloader.download(archivePath)
            val extractedPath = workingDirectory.resolve("extracted")
            archiveExtractor.extract(downloadedArchive.path, extractedPath)

            val extractedExecutable = extractedPath.resolve("steamcmd.exe")
            require(Files.isRegularFile(extractedExecutable)) {
                "SteamCMD ZIP内にsteamcmd.exeが見つかりません"
            }

            Files.createDirectories(installPath)
            val installedExecutable = installPath.resolve("steamcmd.exe")
            Files.copy(extractedExecutable, installedExecutable, StandardCopyOption.REPLACE_EXISTING)

            return SteamCmdPreparationReport(
                completed = true,
                sourceUrl = downloadedArchive.sourceUrl,
                installPath = installPath.toString(),
                executablePath = installedExecutable.toString(),
                downloadedBytes = downloadedArchive.sizeBytes,
            )
        } finally {
            deleteRecursively(workingDirectory)
        }
    }

    private fun deleteRecursively(path: Path) {
        if (!Files.exists(path)) {
            return
        }

        Files.walk(path).use { paths ->
            paths.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
        }
    }

    data class Command(
        val installPath: String,
    )
}
