package gameservermanager.palworld.application

interface PalworldAdminPasswordProvider {
    fun read(installPath: String): String
}
