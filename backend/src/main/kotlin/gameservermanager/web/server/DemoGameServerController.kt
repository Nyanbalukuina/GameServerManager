package gameservermanager.web.server

import gameservermanager.application.server.ManageDemoGameServer
import gameservermanager.domain.server.GameServerRegistration
import jakarta.validation.Valid
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/servers/palworld")
@ConditionalOnProperty(
    prefix = "game-server-manager.features",
    name = ["demo-enabled"],
    havingValue = "true",
)
class DemoGameServerController(private val manageDemoGameServer: ManageDemoGameServer) {
    @PostMapping("/demo-operation")
    fun operate(@Valid @RequestBody request: DemoServerOperationRequest): GameServerRegistration =
        manageDemoGameServer.operate(request.action)

    @GetMapping("/demo-settings")
    fun getSettings(): DemoPalworldSettingsResponse = manageDemoGameServer.getSettings()

    @PutMapping("/demo-settings")
    fun updateSettings(@Valid @RequestBody request: UpdateDemoPalworldSettingsRequest): DemoPalworldSettingsResponse =
        manageDemoGameServer.updateSettings(request)

    @DeleteMapping("/demo")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@RequestParam confirmation: String) {
        manageDemoGameServer.deletePalworld(confirmation)
    }
}
