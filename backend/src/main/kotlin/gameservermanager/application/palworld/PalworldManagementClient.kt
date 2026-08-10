package gameservermanager.application.palworld

interface PalworldManagementClient {
    fun save(port: Int, adminPassword: String)

    fun shutdown(port: Int, adminPassword: String, waitSeconds: Int, message: String)
}
