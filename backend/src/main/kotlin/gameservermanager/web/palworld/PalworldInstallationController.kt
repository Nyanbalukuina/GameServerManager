package gameservermanager.web.palworld

import gameservermanager.application.palworld.InstallPalworldServer
import gameservermanager.domain.palworld.PalworldInstallationReport
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/palworld/installations")
class PalworldInstallationController(
    private val installPalworldServer: InstallPalworldServer,
) {
    @PostMapping
    fun install(@Valid @RequestBody request: InstallPalworldRequest): PalworldInstallationReport {
        val command = InstallPalworldServer.Command(
            steamCmdPath = request.steamCmdPath,
            installPath = request.installPath,
        )

        return installPalworldServer.execute(command)
    }
}
