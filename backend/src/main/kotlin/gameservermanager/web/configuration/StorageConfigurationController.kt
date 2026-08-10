package gameservermanager.web.configuration

import gameservermanager.configuration.StorageProperties
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.nio.file.Path

@RestController
@RequestMapping("/api/configuration/storage")
class StorageConfigurationController(
    private val storageProperties: StorageProperties,
) {
    @GetMapping
    fun get(): StoragePathsResponse {
        val root = Path.of(storageProperties.root).toAbsolutePath().normalize()
        return StoragePathsResponse(
            root = root.toString(),
            palworldInstallPath = root.resolve("servers/palworld/main/runtime").toString(),
            steamCmdPath = root.resolve("tools/steamcmd").toString(),
        )
    }
}
