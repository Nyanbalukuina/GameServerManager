package gameservermanager.palworld.infrastructure

import com.fasterxml.jackson.databind.ObjectMapper
import gameservermanager.palworld.application.PalworldAutomationStore
import gameservermanager.shared.configuration.StorageProperties
import gameservermanager.palworld.domain.PalworldAutomationRuntimeState
import gameservermanager.palworld.domain.PalworldAutomationSettings
import org.springframework.stereotype.Component
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

@Component
class JsonPalworldAutomationStore(
    storageProperties: StorageProperties,
    private val objectMapper: ObjectMapper,
) : PalworldAutomationStore {
    private val settingsPath = Path.of(storageProperties.root).toAbsolutePath().normalize()
        .resolve("config/palworld-main-automation.json")
    private val runtimePath = Path.of(storageProperties.root).toAbsolutePath().normalize()
        .resolve("config/palworld-main-automation-runtime.json")

    @Synchronized
    override fun loadSettings(): PalworldAutomationSettings? {
        return read(settingsPath, PalworldAutomationSettings::class.java)
    }

    @Synchronized
    override fun saveSettings(settings: PalworldAutomationSettings) {
        write(settingsPath, settings)
    }

    @Synchronized
    override fun loadRuntime(): PalworldAutomationRuntimeState {
        return read(runtimePath, PalworldAutomationRuntimeState::class.java)
            ?: PalworldAutomationRuntimeState()
    }

    @Synchronized
    override fun saveRuntime(runtime: PalworldAutomationRuntimeState) {
        write(runtimePath, runtime)
    }

    private fun <T> read(path: Path, type: Class<T>): T? {
        return if (Files.isRegularFile(path)) objectMapper.readValue(path.toFile(), type) else null
    }

    private fun write(path: Path, value: Any) {
        Files.createDirectories(requireNotNull(path.parent))
        val temporary = Files.createTempFile(path.parent, ".${path.fileName}-", ".tmp")
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(temporary.toFile(), value)
            try {
                Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING)
            }
        } finally {
            Files.deleteIfExists(temporary)
        }
    }
}
