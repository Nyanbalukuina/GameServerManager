package gameservermanager.domain.palworld

data class PalworldServerStatus(
    val state: PalworldServerState,
    val processId: Long?,
    val gamePort: Int?,
    val logPath: String?,
    val message: String,
)

enum class PalworldServerState {
    NOT_STARTED,
    STARTING,
    RUNNING,
    STOPPED,
    FAILED,
}
