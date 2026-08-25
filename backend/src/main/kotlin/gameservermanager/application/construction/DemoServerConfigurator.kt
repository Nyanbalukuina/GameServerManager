package gameservermanager.application.construction

import gameservermanager.domain.server.GamePortAccess

data class DemoServerConfigurationCommand(
    val workspacePath: String,
    val serverName: String,
    val gamePort: Int,
    val rconPort: Int,
    val maxPlayers: Int,
    val serverPassword: String,
    val adminPassword: String,
    val automationEnabled: Boolean,
    val shutdownTime: String,
    val startupTime: String,
    val gamePortAccess: GamePortAccess,
    val serverDescription: String = "",
    val expRate: Double = 1.0,
    val palCaptureRate: Double = 1.0,
    val palSpawnRate: Double = 1.0,
    val enemyDropRate: Double = 1.0,
    val eggHatchingTime: Double = 2.0,
    val deathPenalty: String = "All",
    val pvpEnabled: Boolean = false,
    val friendlyFireEnabled: Boolean = false,
    val baseCampMaxNum: Int = 128,
    val baseCampWorkerMaxNum: Int = 15,
)

interface DemoServerConfigurator {
    fun configure(command: DemoServerConfigurationCommand)
}
