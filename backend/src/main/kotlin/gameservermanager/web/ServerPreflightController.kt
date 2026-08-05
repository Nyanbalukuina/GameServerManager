package gameservermanager.web

import gameservermanager.application.preflight.RunServerPreflight
import gameservermanager.domain.preflight.ServerPreflightReport
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/server-construction-preflight")
class ServerPreflightController(
    private val runServerPreflight: RunServerPreflight,
) {
    @PostMapping
    fun run(@Valid @RequestBody request: ServerPreflightRequest): ServerPreflightReport =
        runServerPreflight.execute(
            RunServerPreflight.Command(
                installPath = request.installPath,
                steamCmdPath = request.steamCmdPath,
                gamePort = requireNotNull(request.gamePort),
                rconPort = requireNotNull(request.rconPort),
            ),
        )
}
