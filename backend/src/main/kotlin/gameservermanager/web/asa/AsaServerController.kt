package gameservermanager.web.asa

import gameservermanager.application.asa.GetAsaServerStatus
import gameservermanager.application.asa.RestartAsaServer
import gameservermanager.application.asa.StartAsaServer
import gameservermanager.application.asa.StopAsaServer
import gameservermanager.application.asa.DeleteAsaServer
import gameservermanager.application.server.GameServerRegistrationStore
import gameservermanager.domain.asa.AsaServerStatus
import gameservermanager.domain.asa.AsaServerState
import gameservermanager.domain.server.GameServerRegistration
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

// 登録済みASAサーバーの状態確認とプロセス操作APIを公開する。
@RestController
@RequestMapping("/api/asa/server")
class AsaServerController(
    private val getStatus: GetAsaServerStatus,
    private val startServer: StartAsaServer,
    private val stopServer: StopAsaServer,
    private val restartServer: RestartAsaServer,
    private val deleteServer: DeleteAsaServer,
    private val registrationStore: GameServerRegistrationStore,
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
