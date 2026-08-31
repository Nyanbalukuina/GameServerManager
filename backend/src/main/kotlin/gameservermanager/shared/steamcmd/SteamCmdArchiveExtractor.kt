package gameservermanager.shared.steamcmd

import java.nio.file.Path

interface SteamCmdArchiveExtractor {
    fun extract(archive: Path, destination: Path)
}
