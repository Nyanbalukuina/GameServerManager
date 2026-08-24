package gameservermanager.web.server

import gameservermanager.application.server.ManageDemoGameServer
import gameservermanager.domain.server.GameServerRegistration
import jakarta.validation.Valid
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
@RequestMapping("/api/servers")
class GameServerRegistryController(
    private val manageDemoGameServer: ManageDemoGameServer,
) {
    @GetMapping
    fun list(): List<GameServerRegistration> {
        return manageDemoGameServer.list()
    }

    @GetMapping("/palworld")
    fun getPalworld(): GameServerRegistration {
        return manageDemoGameServer.getPalworld()
    }

    @PostMapping("/palworld/demo-operation")
    fun operate(@Valid @RequestBody request: DemoServerOperationRequest): GameServerRegistration {
        return manageDemoGameServer.operate(request.action)
    }

    @GetMapping("/palworld/demo-settings")
    fun getDemoSettings(): DemoPalworldSettingsResponse {
        return manageDemoGameServer.getSettings()
    }

    @PutMapping("/palworld/demo-settings")
    fun updateDemoSettings(
        @Valid @RequestBody request: UpdateDemoPalworldSettingsRequest,
    ): DemoPalworldSettingsResponse {
        return manageDemoGameServer.updateSettings(request)
    }

    @DeleteMapping("/palworld/demo")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@RequestParam confirmation: String) {
        manageDemoGameServer.deletePalworld(confirmation)
    }
}
