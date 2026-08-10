package gameservermanager.domain.palworld

data class PalworldAutomationSettings(
    val enabled: Boolean,
    val shutdownTime: String,
    val startupTime: String,
    val backupAfterShutdown: Boolean,
    val gamePort: Int,
    val maxPlayers: Int,
)

data class PalworldAutomationRuntimeState(
    val stoppedBySchedule: Boolean = false,
    val lastShutdownCycle: String? = null,
    val lastBackupCycle: String? = null,
    val lastStartupCycle: String? = null,
    val lastError: String? = null,
)

data class PalworldAutomationStatus(
    val settings: PalworldAutomationSettings,
    val runtime: PalworldAutomationRuntimeState,
    val serverInDowntime: Boolean,
    val nextAction: String,
)
