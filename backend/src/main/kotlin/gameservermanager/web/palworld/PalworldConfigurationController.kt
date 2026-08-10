package gameservermanager.web.palworld

import gameservermanager.application.palworld.ConfigurePalworldServer
import gameservermanager.application.palworld.ConfigurePalworldAutomation
import gameservermanager.application.server.GameServerRegistrationStore
import gameservermanager.domain.server.GameServerRegistration
import gameservermanager.web.error.GameServerAlreadyExistsException
import gameservermanager.domain.palworld.PalworldConfigurationReport
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Clock

@RestController
@RequestMapping("/api/palworld/configuration")
class PalworldConfigurationController(
    private val configurePalworldServer: ConfigurePalworldServer,
    private val configurePalworldAutomation: ConfigurePalworldAutomation,
    private val registrationStore: GameServerRegistrationStore,
) {
    @PostMapping
    fun configure(@Valid @RequestBody request: ConfigurePalworldRequest): PalworldConfigurationReport {
        if (registrationStore.findByGame("PALWORLD") != null) {
            throw GameServerAlreadyExistsException("PALWORLD")
        }
        val command = ConfigurePalworldServer.Command(
            serverName = request.serverName,
            installPath = request.installPath,
            gamePort = requireNotNull(request.gamePort),
            rconPort = requireNotNull(request.rconPort),
            maxPlayers = requireNotNull(request.maxPlayers),
            serverPassword = request.serverPassword,
            adminPassword = request.adminPassword,
        )

        val report = configurePalworldServer.execute(command)
        configurePalworldAutomation.save(
            ConfigurePalworldAutomation.Command(
                enabled = request.automationEnabled,
                shutdownTime = request.shutdownTime,
                startupTime = request.startupTime,
                backupAfterShutdown = request.backupAfterShutdown,
                gamePort = requireNotNull(request.gamePort),
                maxPlayers = requireNotNull(request.maxPlayers),
            ),
        )
        registrationStore.create(
            GameServerRegistration(
                game = "PALWORLD",
                serverId = "palworld-main",
                mode = "REAL",
                state = "STOPPED",
                serverName = request.serverName,
                installPath = request.installPath,
                workspacePath = "",
                gamePort = requireNotNull(request.gamePort),
                rconPort = requireNotNull(request.rconPort),
                createdAt = Clock.systemUTC().instant(),
            ),
        )
        return report
    }
}
