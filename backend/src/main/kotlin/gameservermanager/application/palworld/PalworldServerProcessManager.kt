package gameservermanager.application.palworld

import java.nio.file.Path

interface PalworldServerProcessManager {
    fun start(command: PalworldServerProcessCommand): PalworldServerProcessSnapshot

    fun current(): PalworldServerProcessSnapshot?

    fun stop()
}

data class PalworldServerProcessCommand(
    val executable: Path,
    val arguments: List<String>,
    val logPath: Path,
    val gamePort: Int,
)

data class PalworldServerProcessSnapshot(
    val processId: Long,
    val alive: Boolean,
    val exitCode: Int?,
    val gamePort: Int,
    val logPath: String,
)
