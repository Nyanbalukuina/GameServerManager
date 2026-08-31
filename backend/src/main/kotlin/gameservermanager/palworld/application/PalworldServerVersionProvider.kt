package gameservermanager.palworld.application

interface PalworldServerVersionProvider {
    fun currentBuildId(steamCmdPath: String, installPath: String): String
    fun latestBuildId(steamCmdPath: String, installPath: String): String
}
