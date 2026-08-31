package gameservermanager.palworld.web

import gameservermanager.palworld.application.ConfigurePalworldServer
import gameservermanager.palworld.application.ConfigurePalworldAutomation
import gameservermanager.shared.server.GameServerRegistrationStore
import gameservermanager.shared.server.GameServerRegistration
import gameservermanager.shared.error.GameServerAlreadyExistsException
import gameservermanager.palworld.domain.PalworldConfigurationReport
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
