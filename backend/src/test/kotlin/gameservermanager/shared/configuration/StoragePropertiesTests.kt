package gameservermanager.shared.configuration

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import java.nio.file.Path

class StoragePropertiesTests {
    @Test
    fun `既定の管理ルートはユーザーのLocalAppData配下`() {
        val localAppData = System.getenv("LOCALAPPDATA")
            ?.takeIf { it.isNotBlank() }
            ?.let(Path::of)
            ?: Path.of(System.getProperty("user.home"), "AppData", "Local")

        assertEquals(
            localAppData.resolve("GameServerManager").toString(),
            StorageProperties().root,
        )
    }
}
