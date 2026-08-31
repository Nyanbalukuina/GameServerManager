package gameservermanager.asa.web

import gameservermanager.asa.application.GetAsaServerStatus
import gameservermanager.asa.application.RestartAsaServer
import gameservermanager.asa.application.StartAsaServer
import gameservermanager.asa.application.StopAsaServer
import gameservermanager.asa.application.DeleteAsaServer
import gameservermanager.asa.application.CheckAsaServerVersion
import gameservermanager.asa.application.UpdateAsaServer
import gameservermanager.asa.application.GetInstalledAsaServerVersion
import gameservermanager.shared.server.GameServerRegistrationStore
import gameservermanager.shared.configuration.FeatureProperties
import gameservermanager.shared.configuration.StorageProperties
import gameservermanager.asa.domain.AsaServerVersion
import gameservermanager.shared.steamcmd.InstalledSteamServerVersion
import gameservermanager.asa.domain.AsaServerStatus
import gameservermanager.asa.domain.AsaServerState
import gameservermanager.shared.server.GameServerRegistration
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.http.HttpStatus
import java.nio.file.Path

// 登録済みASAサーバーの状態確認とプロセス操作APIを公開する。
@RestController
@RequestMapping("/api/asa/server")
class AsaServerController(
    private val getStatus: GetAsaServerStatus,
    private val startServer: StartAsaServer,
    private val stopServer: StopAsaServer,
    private val restartServer: RestartAsaServer,
    private val deleteServer: DeleteAsaServer,
    private val checkVersion: CheckAsaServerVersion,
    private val updateServer: UpdateAsaServer,
    private val getInstalledVersion: GetInstalledAsaServerVersion,
    private val registrationStore: GameServerRegistrationStore,
    private val features: FeatureProperties,
    private val storageProperties: StorageProperties,
) {
    @GetMapping
    fun get(): GameServerRegistration = registration()

    @GetMapping("/status")
    fun status(): AsaServerStatus {
        val server = registration()
        return if (server.mode == "DEMO") demoStatus(server) else getStatus.execute()
    }

    @PostMapping("/start")
    fun start(): AsaServerStatus {
        val server = registration()
        if (server.mode == "DEMO") return updateDemo(server, "RUNNING", "デモASAサーバーを起動しました")
        val status = startServer.execute(startCommand(server))
        registrationStore.update(server.copy(state = "RUNNING"))
        return status
    }

    @PostMapping("/stop")
    fun stop(@Valid @RequestBody request: OperateAsaServerRequest): AsaServerStatus {
        val server = registration()
        if (server.mode == "DEMO") return updateDemo(server, "STOPPED", "デモASAサーバーを停止しました")
        require(request.adminPassword.length >= 8) { "管理者パスワードを入力してください" }
        val status = stopServer.execute(StopAsaServer.Command(server.rconPort, request.adminPassword))
        registrationStore.update(server.copy(state = "STOPPED"))
        return status
    }

    @PostMapping("/restart")
    fun restart(@Valid @RequestBody request: OperateAsaServerRequest): AsaServerStatus {
        val server = registration()
        if (server.mode == "DEMO") return updateDemo(server, "RUNNING", "デモASAサーバーを再起動しました")
        require(request.adminPassword.length >= 8) { "管理者パスワードを入力してください" }
        val status = restartServer.execute(
            RestartAsaServer.Command(
                server.installPath, requireNotNull(server.map), server.gamePort,
                requireNotNull(server.queryPort), requireNotNull(server.maxPlayers),
                server.rconPort, request.adminPassword,
            ),
        )
        registrationStore.update(server.copy(state = "RUNNING"))
        return status
    }

    @PostMapping("/version/check")
    fun checkVersion(): AsaServerVersion {
        val server = realRegistration()
        return checkVersion.execute(CheckAsaServerVersion.Command(steamCmdPath(server), server.installPath))
    }

    @GetMapping("/version/current")
    fun currentVersion(): InstalledSteamServerVersion {
        val server = realRegistration()
        return getInstalledVersion.execute(
            GetInstalledAsaServerVersion.Command(steamCmdPath(server), server.installPath),
        )
    }

    @PostMapping("/update")
    fun update(): AsaServerVersion {
        val server = realRegistration()
        return updateServer.execute(UpdateAsaServer.Command(steamCmdPath(server), server.installPath))
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@RequestParam confirmation: String) {
        deleteServer.execute(confirmation)
    }

    private fun startCommand(server: GameServerRegistration) = StartAsaServer.Command(
        server.installPath, requireNotNull(server.map), server.gamePort,
        requireNotNull(server.queryPort), requireNotNull(server.maxPlayers),
    )

    private fun registration() = requireNotNull(registrationStore.findByGame("ASA")) {
        "ASAサーバーは登録されていません"
    }.also { require(features.demoEnabled || it.mode != "DEMO") { "ASAサーバーは登録されていません" } }

    private fun realRegistration() = registration().also {
        require(it.mode == "REAL") { "デモASAサーバーではバージョン確認と更新を実行できません" }
    }

    private fun steamCmdPath(server: GameServerRegistration): String {
        return server.steamCmdPath ?: Path.of(storageProperties.root).resolve("tools/steamcmd").toString()
    }

    private fun updateDemo(server: GameServerRegistration, state: String, message: String): AsaServerStatus {
        val updated = server.copy(state = state)
        registrationStore.update(updated)
        return demoStatus(updated, message)
    }

    private fun demoStatus(server: GameServerRegistration, message: String = "デモASAサーバーの現在状態です") =
        AsaServerStatus(
            if (server.state == "RUNNING") AsaServerState.RUNNING else AsaServerState.STOPPED,
            null, server.gamePort, server.peerPort, server.queryPort, null, message,
        )
}
