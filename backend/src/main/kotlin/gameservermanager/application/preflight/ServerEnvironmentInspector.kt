package gameservermanager.application.preflight

interface ServerEnvironmentInspector {
    fun inspectPath(path: String, executableName: String? = null): PathInspection

    fun isUdpPortAvailable(port: Int): Boolean

    fun isTcpPortAvailable(port: Int): Boolean
}

data class PathInspection(
    val valid: Boolean,
    val absolute: Boolean,
    val root: Boolean,
    val exists: Boolean,
    val directory: Boolean,
    val writable: Boolean,
    val usableSpaceBytes: Long?,
    val executableExists: Boolean,
)

