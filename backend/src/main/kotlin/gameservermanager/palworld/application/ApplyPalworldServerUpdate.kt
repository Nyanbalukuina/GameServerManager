package gameservermanager.palworld.application

import gameservermanager.palworld.domain.PalworldServerVersion
import org.springframework.stereotype.Service

@Service
class ApplyPalworldServerUpdate(
    private val updateServer: UpdatePalworldServer,
    private val checkVersion: CheckPalworldServerVersion,
) {
    fun execute(command: Command): PalworldServerVersion {
        val before = checkVersion.execute(CheckPalworldServerVersion.Command(command.steamCmdPath, command.installPath))
        require(before.updateAvailable) { "Palworldサーバーは既に最新です" }
        updateServer.execute(UpdatePalworldServer.Command(command.steamCmdPath, command.installPath))
        val after = checkVersion.execute(CheckPalworldServerVersion.Command(command.steamCmdPath, command.installPath))
        check(!after.updateAvailable) { "更新処理後もBuild IDが最新バージョンと一致しません" }
        return after.copy(message = "Palworldサーバーを最新バージョンへ更新しました")
    }

    data class Command(val steamCmdPath: String, val installPath: String)
}
