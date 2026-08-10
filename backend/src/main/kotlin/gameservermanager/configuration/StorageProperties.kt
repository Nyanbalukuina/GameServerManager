package gameservermanager.configuration

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("game-server-manager.storage")
data class StorageProperties(
    var root: String = "C:\\GameServerManager",
)
