package gameservermanager.shared.construction

data class SteamCmdInstallCommand(
    val steamCmdPath: String,
    val installPath: String,
    val appId: Int,
) {
    fun arguments(): List<String> {
        return listOf(
            "+force_install_dir",
            installPath,
            "+login",
            "anonymous",
            "+app_update",
            appId.toString(),
            "validate",
            "+quit",
        )
    }
}
