package gameservermanager.web.configuration

import gameservermanager.configuration.FeatureProperties
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/configuration/features")
class FeatureConfigurationController(private val properties: FeatureProperties) {
    @GetMapping
    fun get() = FeatureConfigurationResponse(properties.demoEnabled)
}

data class FeatureConfigurationResponse(val demoEnabled: Boolean)
