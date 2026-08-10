package gameservermanager.infrastructure.authentication

import com.fasterxml.jackson.databind.ObjectMapper
import gameservermanager.application.authentication.AdministratorCredentialStore
import gameservermanager.configuration.StorageProperties
import gameservermanager.domain.authentication.AdministratorCredentials
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

@Component
class JsonAdministratorCredentialStore(
    storageProperties: StorageProperties,
    private val objectMapper: ObjectMapper,
) : AdministratorCredentialStore {
    private val credentialsPath = Path.of(storageProperties.root)
        .toAbsolutePath()
        .normalize()
        .resolve("config/authentication.json")

    override fun exists(): Boolean {
        return Files.isRegularFile(credentialsPath)
    }

    override fun load(): AdministratorCredentials? {
        if (!exists()) {
            return null
        }
        return objectMapper.readValue(credentialsPath.toFile(), AdministratorCredentials::class.java)
    }

    @Synchronized
    override fun create(credentials: AdministratorCredentials) {
        if (Files.exists(credentialsPath)) {
            throw IllegalArgumentException("管理者はすでに設定されています")
        }

        Files.createDirectories(requireNotNull(credentialsPath.parent))
        val temporaryPath = Files.createTempFile(credentialsPath.parent, ".authentication-", ".tmp")
        try {
            Files.writeString(
                temporaryPath,
                objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(credentials),
                StandardCharsets.UTF_8,
            )
            moveWithoutReplacing(temporaryPath, credentialsPath)
        } finally {
            Files.deleteIfExists(temporaryPath)
        }
    }

    private fun moveWithoutReplacing(source: Path, destination: Path) {
        try {
            Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE)
        } catch (_: AtomicMoveNotSupportedException) {
            try {
                Files.move(source, destination)
            } catch (_: FileAlreadyExistsException) {
                throw IllegalArgumentException("管理者はすでに設定されています")
            }
        } catch (_: FileAlreadyExistsException) {
            throw IllegalArgumentException("管理者はすでに設定されています")
        }
    }
}
