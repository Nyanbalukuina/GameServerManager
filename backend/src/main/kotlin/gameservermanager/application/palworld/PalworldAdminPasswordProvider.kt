package gameservermanager.application.palworld

interface PalworldAdminPasswordProvider {
    fun read(installPath: String): String
}
