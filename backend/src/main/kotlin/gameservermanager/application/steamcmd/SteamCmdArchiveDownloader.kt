package gameservermanager.application.steamcmd

import java.nio.file.Path

interface SteamCmdArchiveDownloader {
    fun download(destination: Path): DownloadedSteamCmdArchive
}

data class DownloadedSteamCmdArchive(
    val path: Path,
    val sourceUrl: String,
    val sizeBytes: Long,
)
