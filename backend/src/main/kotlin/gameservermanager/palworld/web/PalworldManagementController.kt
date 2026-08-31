package gameservermanager.palworld.web

import gameservermanager.palworld.application.StopPalworldServer
import gameservermanager.palworld.application.RestartPalworldServer
import gameservermanager.palworld.application.UpdatePalworldServer
import gameservermanager.palworld.application.PalworldOperationHistoryStore
import gameservermanager.palworld.application.ApplyPalworldServerUpdate
import gameservermanager.palworld.application.CheckPalworldServerVersion
import gameservermanager.palworld.application.GetInstalledPalworldServerVersion
import gameservermanager.palworld.domain.PalworldInstallationReport
import gameservermanager.palworld.domain.PalworldOperationReport
import gameservermanager.palworld.domain.PalworldOperationHistoryEntry
import gameservermanager.palworld.domain.PalworldServerStatus
import gameservermanager.palworld.domain.PalworldServerVersion
import gameservermanager.shared.configuration.StorageProperties
import gameservermanager.shared.server.GameServerRegistration
import gameservermanager.shared.server.GameServerRegistrationStore
import gameservermanager.shared.steamcmd.InstalledSteamServerVersion
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.nio.file.Path

@RestController
@RequestMapping("/api/palworld/server")
class PalworldManagementController(
    private val stopPalworldServer: StopPalworldServer,
    private val restartPalworldServer: RestartPalworldServer,
    private val updatePalworldServer: UpdatePalworldServer,
    private val checkVersion: CheckPalworldServerVersion,
    private val applyUpdate: ApplyPalworldServerUpdate,
    private val getInstalledVersion: GetInstalledPalworldServerVersion,
    private val historyStore: PalworldOperationHistoryStore,
    private val registrationStore: GameServerRegistrationStore,
    private val storageProperties: StorageProperties,
) {
    @PostMapping("/stop")
    fun stop(@Valid @RequestBody request: StopPalworldServerRequest): PalworldOperationReport {
        return stopPalworldServer.execute(
            StopPalworldServer.Command(
                restApiPort = requireNotNull(request.restApiPort),
                adminPassword = request.adminPassword,
            ),
        )
    }

    @PostMapping("/restart")
    fun restart(@Valid @RequestBody request: RestartPalworldServerRequest): PalworldServerStatus {
        return restartPalworldServer.execute(
            RestartPalworldServer.Command(
                installPath = request.installPath,
                gamePort = requireNotNull(request.gamePort),
                maxPlayers = requireNotNull(request.maxPlayers),
                restApiPort = requireNotNull(request.restApiPort),
                adminPassword = request.adminPassword,
            ),
        )
    }

    @PostMapping("/update")
    fun update(@Valid @RequestBody request: UpdatePalworldServerRequest): PalworldInstallationReport {
        return updatePalworldServer.execute(
            UpdatePalworldServer.Command(request.steamCmdPath, request.installPath),
        )
    }

    @PostMapping("/version/check")
    fun checkVersion(): PalworldServerVersion {
        val server = realRegistration()
        return checkVersion.execute(
            CheckPalworldServerVersion.Command(steamCmdPath(server), server.installPath),
        )
    }

    @GetMapping("/version/current")
    fun currentVersion(): InstalledSteamServerVersion {
        val server = realRegistration()
        return getInstalledVersion.execute(
            GetInstalledPalworldServerVersion.Command(steamCmdPath(server), server.installPath),
        )
    }

    @PostMapping("/version/update")
    fun applyUpdate(): PalworldServerVersion {
        val server = realRegistration()
        return applyUpdate.execute(
            ApplyPalworldServerUpdate.Command(steamCmdPath(server), server.installPath),
        )
    }

    @GetMapping("/history")
    fun history(
        @RequestParam(defaultValue = "50") limit: Int,
    ): List<PalworldOperationHistoryEntry> {
        return historyStore.latest(limit)
    }

    private fun realRegistration(): GameServerRegistration {
        return requireNotNull(registrationStore.findByGame("PALWORLD")) {
            "Palworldサーバーは登録されていません"
        }.also {
            require(it.mode == "REAL") { "デモPalworldサーバーではバージョン確認と更新を実行できません" }
        }
    }

    private fun steamCmdPath(server: GameServerRegistration): String {
        return server.steamCmdPath ?: Path.of(storageProperties.root).resolve("tools/steamcmd").toString()
    }
}
