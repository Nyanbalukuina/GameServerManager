package gameservermanager.web.server

import gameservermanager.application.server.ManageDemoGameServer
import gameservermanager.configuration.FeatureProperties
import gameservermanager.domain.server.GameServerRegistration
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/servers")
class GameServerRegistryController(
    private val manageDemoGameServer: ManageDemoGameServer,
    private val features: FeatureProperties,
) {
    @GetMapping
    fun list(): List<GameServerRegistration> {
        return manageDemoGameServer.list().filter { features.demoEnabled || it.mode != "DEMO" }
    }

    @GetMapping("/palworld")
    fun getPalworld(): GameServerRegistration {
        val server = manageDemoGameServer.getPalworld()
        require(features.demoEnabled || server.mode != "DEMO") { "Palworldサーバーは登録されていません" }
        return server
    }
}
