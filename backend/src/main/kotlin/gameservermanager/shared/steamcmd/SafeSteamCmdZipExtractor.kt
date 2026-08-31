package gameservermanager.shared.steamcmd

import gameservermanager.shared.steamcmd.SteamCmdArchiveExtractor
import org.springframework.stereotype.Component
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.zip.ZipInputStream

@Component
class SafeSteamCmdZipExtractor : SteamCmdArchiveExtractor {
    override fun extract(archive: Path, destination: Path) {
        Files.createDirectories(destination)
        val normalizedDestination = destination.toAbsolutePath().normalize()
        var entryCount = 0
        var extractedBytes = 0L

        Files.newInputStream(archive).use { fileInput ->
            ZipInputStream(fileInput).use { zipInput ->
                while (true) {
                    val entry = zipInput.nextEntry ?: break
                    entryCount += 1
                    require(entryCount <= MAXIMUM_ENTRY_COUNT) {
                        "SteamCMD ZIPのファイル数が上限を超えています"
                    }

                    val outputPath = normalizedDestination.resolve(entry.name).normalize()
                    require(outputPath.startsWith(normalizedDestination)) {
                        "SteamCMD ZIPに不正なパスが含まれています"
                    }

                    if (entry.isDirectory) {
                        Files.createDirectories(outputPath)
                    } else {
                        Files.createDirectories(requireNotNull(outputPath.parent))
                        Files.newOutputStream(
                            outputPath,
                            StandardOpenOption.CREATE,
                            StandardOpenOption.TRUNCATE_EXISTING,
                            StandardOpenOption.WRITE,
                        ).use { output ->
                            val buffer = ByteArray(BUFFER_SIZE)
                            while (true) {
                                val readBytes = zipInput.read(buffer)
                                if (readBytes < 0) {
                                    break
                                }
                                extractedBytes += readBytes
                                if (extractedBytes > MAXIMUM_EXTRACTED_BYTES) {
                                    throw IOException("SteamCMD ZIPの展開サイズが上限を超えています")
                                }
                                output.write(buffer, 0, readBytes)
                            }
                        }
                    }
                    zipInput.closeEntry()
                }
            }
        }

        require(entryCount > 0) {
            "SteamCMD ZIPが空です"
        }
    }

    companion object {
        private const val MAXIMUM_ENTRY_COUNT = 1000
        private const val MAXIMUM_EXTRACTED_BYTES = 200L * 1024L * 1024L
        private const val BUFFER_SIZE = 8192
    }
}
