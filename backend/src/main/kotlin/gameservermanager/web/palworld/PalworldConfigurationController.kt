package gameservermanager.web.palworld

import gameservermanager.application.palworld.ConfigurePalworldServer
import gameservermanager.domain.palworld.PalworldConfigurationReport
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/palworld/configuration")
class PalworldConfigurationController(
    private val configurePalworldServer: ConfigurePalworldServer,
) {
    @PostMapping
    fun configure(@Valid @RequestBody request: ConfigurePalworldRequest): PalworldConfigurationReport {
        val command = ConfigurePalworldServer.Command(
            serverName = request.serverName,
            installPath = request.installPath,
            gamePort = requireNotNull(request.gamePort),
            rconPort = requireNotNull(request.rconPort),
            maxPlayers = requireNotNull(request.maxPlayers),
            serverPassword = request.serverPassword,
            adminPassword = request.adminPassword,
        )

        return configurePalworldServer.execute(command)
    }
}
