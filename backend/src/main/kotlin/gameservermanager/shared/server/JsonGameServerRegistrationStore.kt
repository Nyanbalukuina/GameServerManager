package gameservermanager.shared.server

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import gameservermanager.shared.server.GameServerRegistrationStore
import gameservermanager.shared.configuration.StorageProperties
import gameservermanager.shared.server.GameServerRegistration
import gameservermanager.shared.error.GameServerAlreadyExistsException
import org.springframework.stereotype.Component
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

@Component
class JsonGameServerRegistrationStore(
    storageProperties: StorageProperties,
    private val objectMapper: ObjectMapper,
) : GameServerRegistrationStore {
    private val path = Path.of(storageProperties.root).toAbsolutePath().normalize()
        .resolve("config/servers.json")

    @Synchronized
    override fun findAll(): List<GameServerRegistration> {
        return read()
    }

    @Synchronized
    override fun findByGame(game: String): GameServerRegistration? {
        return read().singleOrNull { it.game == game }
    }

    @Synchronized
    override fun create(registration: GameServerRegistration) {
        val registrations = read()
        if (registrations.any { it.game == registration.game }) {
            throw GameServerAlreadyExistsException(registration.game)
        }
        write(registrations + registration)
    }

    @Synchronized
    override fun update(registration: GameServerRegistration) {
        val registrations = read()
        require(registrations.any { it.game == registration.game }) {
            "${registration.game}サーバーは登録されていません"
        }
        write(registrations.map { if (it.game == registration.game) registration else it })
    }

    @Synchronized
    override fun delete(game: String) {
        write(read().filterNot { it.game == game })
    }

    private fun read(): List<GameServerRegistration> {
        return if (Files.isRegularFile(path)) objectMapper.readValue(path.toFile()) else emptyList()
    }

    private fun write(registrations: List<GameServerRegistration>) {
        Files.createDirectories(requireNotNull(path.parent))
        val temporary = Files.createTempFile(path.parent, ".servers-", ".tmp")
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(temporary.toFile(), registrations)
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
