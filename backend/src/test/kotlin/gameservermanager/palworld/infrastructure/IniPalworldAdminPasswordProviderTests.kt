package gameservermanager.palworld.infrastructure

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class IniPalworldAdminPasswordProviderTests {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `PalWorldSettingsから管理者パスワードを読み取る`() {
        val settingsPath = tempDir.resolve("Pal/Saved/Config/WindowsServer/PalWorldSettings.ini")
        Files.createDirectories(settingsPath.parent)
        Files.writeString(
            settingsPath,
            "[/Script/Pal.PalGameWorldSettings]\nOptionSettings=(AdminPassword=\"secret\\\"value\",PublicPort=8211)",
        )

        val password = IniPalworldAdminPasswordProvider().read(tempDir.toString())

        assertThat(password).isEqualTo("secret\"value")
    }
}
