package gameservermanager.infrastructure.windows

import gameservermanager.application.preflight.ManagedPathPolicy
import gameservermanager.configuration.StorageProperties
import org.springframework.stereotype.Component
import java.nio.file.InvalidPathException
import java.nio.file.Path

@Component
class WindowsManagedPathPolicy(
    storageProperties: StorageProperties,
) : ManagedPathPolicy {
    private val root = Path.of(storageProperties.root).toAbsolutePath().normalize()
    private val serversRoot = root.resolve("servers")
    private val toolsRoot = root.resolve("tools")

    override fun isServerPathAllowed(path: String): Boolean {
        return isBelow(path, serversRoot)
    }

    override fun isToolPathAllowed(path: String): Boolean {
        return isBelow(path, toolsRoot)
    }

    override fun managedRoot(): String {
        return root.toString()
    }

    private fun isBelow(path: String, allowedRoot: Path): Boolean {
        val target = try {
            Path.of(path.trim()).toAbsolutePath().normalize()
        } catch (_: InvalidPathException) {
            return false
        }

        return target != allowedRoot && target.startsWith(allowedRoot)
    }
}
