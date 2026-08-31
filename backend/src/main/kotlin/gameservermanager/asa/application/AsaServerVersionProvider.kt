package gameservermanager.asa.application

interface AsaServerVersionProvider {
    fun currentBuildId(steamCmdPath: String, installPath: String): String
    fun latestBuildId(steamCmdPath: String, installPath: String): String
}
