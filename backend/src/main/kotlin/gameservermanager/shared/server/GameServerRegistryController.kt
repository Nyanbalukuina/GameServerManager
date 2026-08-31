package gameservermanager.shared.server

import gameservermanager.shared.configuration.FeatureProperties
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/servers")
class GameServerRegistryController(
    private val store: GameServerRegistrationStore,
    private val features: FeatureProperties,
) {
    @GetMapping
    fun list(): List<GameServerRegistration> {
        return store.findAll().filter { features.demoEnabled || it.mode != "DEMO" }
    }
}
