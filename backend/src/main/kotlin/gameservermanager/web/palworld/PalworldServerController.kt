package gameservermanager.web.palworld

import gameservermanager.application.palworld.GetPalworldServerStatus
import gameservermanager.application.palworld.StartPalworldServer
import gameservermanager.domain.palworld.PalworldServerStatus
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/palworld/server")
class PalworldServerController(
    private val startPalworldServer: StartPalworldServer,
    private val getPalworldServerStatus: GetPalworldServerStatus,
) {
    @PostMapping("/start")
    fun start(@Valid @RequestBody request: StartPalworldServerRequest): PalworldServerStatus {
        return startPalworldServer.execute(
            StartPalworldServer.Command(
                installPath = request.installPath,
                gamePort = requireNotNull(request.gamePort),
                maxPlayers = requireNotNull(request.maxPlayers),
            ),
        )
    }

    @GetMapping("/status")
    fun status(): PalworldServerStatus {
        return getPalworldServerStatus.execute()
    }
}
