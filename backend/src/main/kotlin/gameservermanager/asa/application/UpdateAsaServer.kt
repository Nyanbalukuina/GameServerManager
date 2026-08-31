package gameservermanager.asa.application

import gameservermanager.asa.domain.AsaServerState
import gameservermanager.asa.domain.AsaServerVersion
import org.springframework.stereotype.Service

@Service
class UpdateAsaServer(
    private val getStatus: GetAsaServerStatus,
    private val installAsaServer: InstallAsaServer,
    private val checkVersion: CheckAsaServerVersion,
) {
    fun execute(command: Command): AsaServerVersion {
        require(getStatus.execute().state != AsaServerState.RUNNING) {
            "ASAサーバーを停止してから更新してください"
        }
        val before = checkVersion.execute(CheckAsaServerVersion.Command(command.steamCmdPath, command.installPath))
        require(before.updateAvailable) { "ASAサーバーは既に最新です" }
        installAsaServer.execute(InstallAsaServer.Command(command.steamCmdPath, command.installPath))
        val after = checkVersion.execute(CheckAsaServerVersion.Command(command.steamCmdPath, command.installPath))
        check(!after.updateAvailable) { "更新処理後もBuild IDが最新バージョンと一致しません" }
        return after.copy(message = "ASAサーバーを最新バージョンへ更新しました")
    }

    data class Command(
        val steamCmdPath: String,
        val installPath: String,
    )
}
