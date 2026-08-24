package gameservermanager.web.steamcmd

import gameservermanager.application.steamcmd.PrepareSteamCmd
import gameservermanager.domain.server.steamcmd.SteamCmdPreparationReport
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/steamcmd")
class SteamCmdController(
    private val prepareSteamCmd: PrepareSteamCmd,
) {
    @PostMapping("/prepare")
    fun prepare(@Valid @RequestBody request: PrepareSteamCmdRequest): SteamCmdPreparationReport {
        val command = PrepareSteamCmd.Command(
            installPath = request.installPath,
        )

        return prepareSteamCmd.execute(command)
    }
}
