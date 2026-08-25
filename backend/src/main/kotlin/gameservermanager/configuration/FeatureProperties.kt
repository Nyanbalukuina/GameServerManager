package gameservermanager.configuration

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("game-server-manager.features")
data class FeatureProperties(
    val demoEnabled: Boolean = true,
)
