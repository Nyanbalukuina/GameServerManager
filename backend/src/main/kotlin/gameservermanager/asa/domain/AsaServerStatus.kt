package gameservermanager.asa.domain

// ASAサーバーの現在の状態を保持する。
data class AsaServerStatus(
    val state: AsaServerState,
    val processId: Long?,
    val gamePort: Int?,
    val peerPort: Int?,
    val queryPort: Int?,
    val logPath: String?,
    val message: String,
)

enum class AsaServerState {
    NOT_STARTED,
    STARTING,
    RUNNING,
    STOPPED,
    FAILED,
}
