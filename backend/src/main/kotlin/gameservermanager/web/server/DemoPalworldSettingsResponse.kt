package gameservermanager.web.server

data class DemoPalworldSettingsResponse(
    val serverName: String,
    val serverDescription: String,
    val maxPlayers: Int,
    val serverPassword: String,
    val adminPassword: String,
    val expRate: Double,
    val palCaptureRate: Double,
    val palSpawnRate: Double,
    val enemyDropRate: Double,
    val eggHatchingTime: Double,
    val deathPenalty: String,
    val pvpEnabled: Boolean,
    val friendlyFireEnabled: Boolean,
    val baseCampMaxNum: Int,
    val baseCampWorkerMaxNum: Int,
)
