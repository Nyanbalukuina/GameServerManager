package gameservermanager.shared.server

import gameservermanager.shared.server.ManageDemoGameServer
import gameservermanager.shared.configuration.FeatureProperties
import gameservermanager.shared.server.GameServerRegistration
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
