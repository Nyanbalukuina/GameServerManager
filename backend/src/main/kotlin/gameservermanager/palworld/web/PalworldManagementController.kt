package gameservermanager.palworld.web

import gameservermanager.palworld.application.StopPalworldServer
import gameservermanager.palworld.application.RestartPalworldServer
import gameservermanager.palworld.application.UpdatePalworldServer
import gameservermanager.palworld.application.PalworldOperationHistoryStore
import gameservermanager.palworld.domain.PalworldInstallationReport
import gameservermanager.palworld.domain.PalworldOperationReport
import gameservermanager.palworld.domain.PalworldOperationHistoryEntry
import gameservermanager.palworld.domain.PalworldServerStatus
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/palworld/server")
class PalworldManagementController(
    private val stopPalworldServer: StopPalworldServer,
    private val restartPalworldServer: RestartPalworldServer,
    private val updatePalworldServer: UpdatePalworldServer,
    private val historyStore: PalworldOperationHistoryStore,
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

    @GetMapping("/history")
    fun history(
        @RequestParam(defaultValue = "50") limit: Int,
    ): List<PalworldOperationHistoryEntry> {
        return historyStore.latest(limit)
    }
}
