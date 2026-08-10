package gameservermanager.infrastructure.palworld

import gameservermanager.application.palworld.PalworldBackupCreator
import gameservermanager.application.palworld.PalworldBackupResult
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import java.time.Clock
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@Component
class ZipPalworldBackupCreator() : PalworldBackupCreator {
    private var clock: Clock = Clock.systemUTC()

    constructor(clock: Clock) : this() {
        this.clock = clock
    }

    override fun create(source: Path, backupDirectory: Path): PalworldBackupResult {
        Files.createDirectories(backupDirectory)
        val timestamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS")
            .withZone(clock.zone)
            .format(clock.instant())
        val backupPath = backupDirectory.resolve("palworld-world-$timestamp.zip")
        var fileCount = 0

        ZipOutputStream(Files.newOutputStream(backupPath)).use { zip ->
            Files.walk(source).use { paths ->
                paths.filter { Files.isRegularFile(it) }.forEach { file ->
                    val relativePath = source.relativize(file).toString().replace('\\', '/')
                    zip.putNextEntry(ZipEntry(relativePath))
                    Files.copy(file, zip)
                    zip.closeEntry()
                    fileCount += 1
                }
            }
        }

        return PalworldBackupResult(
            backupPath = backupPath.toString(),
            sizeBytes = Files.size(backupPath),
            fileCount = fileCount,
        )
    }
}
