package gameservermanager.infrastructure.windows

import gameservermanager.application.preflight.PathInspection
import gameservermanager.application.preflight.ServerEnvironmentInspector
import org.springframework.stereotype.Component
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Path

@Component
class WindowsServerEnvironmentInspector : ServerEnvironmentInspector {
    override fun inspectPath(path: String, executableName: String?): PathInspection {
        val target = try {
            Path.of(path.trim()).normalize()
        } catch (_: InvalidPathException) {
            return invalidPath()
        }

        val absolute = target.isAbsolute
        val root = absolute && target.parent == null
        val exists = Files.exists(target)
        val directory = !exists || Files.isDirectory(target)
        val writableBase = findNearestExistingPath(target)
        val writable = absolute && writableBase != null && Files.isWritable(writableBase)
        val usableSpace = writableBase?.let {
            runCatching { Files.getFileStore(it).usableSpace }.getOrNull()
        }
        val executableExists = executableName != null &&
            exists &&
            Files.isRegularFile(target.resolve(executableName))

        return PathInspection(
            valid = true,
            absolute = absolute,
            root = root,
            exists = exists,
            directory = directory,
            writable = writable,
            usableSpaceBytes = usableSpace,
            executableExists = executableExists,
        )
    }

    override fun isUdpPortAvailable(port: Int): Boolean {
        return runCatching {
            DatagramSocket(null).use { socket ->
                socket.reuseAddress = false
                socket.bind(InetSocketAddress(port))
            }
        }.isSuccess
    }

    override fun isTcpPortAvailable(port: Int): Boolean {
        return runCatching {
            ServerSocket().use { socket ->
                socket.reuseAddress = false
                socket.bind(InetSocketAddress(port))
            }
        }.isSuccess
    }

    private fun findNearestExistingPath(path: Path): Path? {
        var current: Path? = path
        while (current != null && !Files.exists(current)) {
            current = current.parent
        }
        return current
    }

    private fun invalidPath(): PathInspection {
        return PathInspection(
            valid = false,
            absolute = false,
            root = false,
            exists = false,
            directory = false,
            writable = false,
            usableSpaceBytes = null,
            executableExists = false,
        )
    }
}
