package gameservermanager.shared.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import java.nio.file.Path

@ConfigurationProperties("game-server-manager.storage")
data class StorageProperties(
    var root: String = defaultRoot(),
) {
    companion object {
        fun defaultRoot(): String {
            val localAppData = System.getenv("LOCALAPPDATA")
                ?.takeIf { it.isNotBlank() }
                ?.let(Path::of)
                ?: Path.of(System.getProperty("user.home"), "AppData", "Local")
            return localAppData.resolve("GameServerManager").toString()
        }
    }
}
