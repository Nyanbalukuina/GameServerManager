package gameservermanager.web.palworld

import gameservermanager.application.palworld.ConfigurePalworldAutomation
import gameservermanager.application.palworld.RunPalworldAutomation
import gameservermanager.domain.palworld.PalworldAutomationSettings
import gameservermanager.domain.palworld.PalworldAutomationStatus
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/palworld/automation")
class PalworldAutomationController(
    private val configureAutomation: ConfigurePalworldAutomation,
    private val runAutomation: RunPalworldAutomation,
) {
    @GetMapping
    fun get(): PalworldAutomationStatus {
        return runAutomation.status()
    }

    @PutMapping
    fun update(@Valid @RequestBody request: PalworldAutomationRequest): PalworldAutomationSettings {
        return configureAutomation.save(
            ConfigurePalworldAutomation.Command(
                enabled = request.enabled,
                shutdownTime = request.shutdownTime,
                startupTime = request.startupTime,
                backupAfterShutdown = request.backupAfterShutdown,
                gamePort = requireNotNull(request.gamePort),
                maxPlayers = requireNotNull(request.maxPlayers),
            ),
        )
    }
}
