package gameservermanager.web.palworld

import gameservermanager.application.palworld.StopPalworldServer
import gameservermanager.application.palworld.RestartPalworldServer
import gameservermanager.application.palworld.UpdatePalworldServer
import gameservermanager.application.palworld.PalworldOperationHistoryStore
import gameservermanager.domain.palworld.PalworldInstallationReport
import gameservermanager.domain.palworld.PalworldOperationReport
import gameservermanager.domain.palworld.PalworldOperationHistoryEntry
import gameservermanager.domain.palworld.PalworldServerStatus
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
