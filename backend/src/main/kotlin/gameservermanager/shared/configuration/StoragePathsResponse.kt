package gameservermanager.shared.configuration

data class StoragePathsResponse(
    val root: String,
    val palworldInstallPath: String,
    val asaInstallPath: String,
    val steamCmdPath: String,
)
