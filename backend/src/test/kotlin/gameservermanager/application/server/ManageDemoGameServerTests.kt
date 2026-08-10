package gameservermanager.application.server

import gameservermanager.domain.server.GameServerRegistration
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant

class ManageDemoGameServerTests {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `デモ操作で状態を更新する`() {
        val demoRoot = tempDir.resolve("GameServerManagerDemo")
        val store = MemoryStore(registration(demoRoot))
        val manager = ManageDemoGameServer(store, demoRoot)

        assertThat(manager.operate("START").state).isEqualTo("RUNNING")
        assertThat(manager.operate("RESTART").state).isEqualTo("RUNNING")
        assertThat(manager.operate("STOP").state).isEqualTo("STOPPED")
    }

    @Test
    fun `確認文字列が一致した場合だけデモ領域と登録を削除する`() {
        val demoRoot = tempDir.resolve("GameServerManagerDemo")
        Files.createDirectories(demoRoot)
        Files.writeString(demoRoot.resolve("demo.txt"), "demo")
        val store = MemoryStore(registration(demoRoot))
        val manager = ManageDemoGameServer(store, demoRoot)

        assertThatThrownBy { manager.deletePalworld("palworld") }
            .isInstanceOf(IllegalArgumentException::class.java)
        assertThat(demoRoot).exists()

        manager.deletePalworld("PALWORLD")

        assertThat(demoRoot).doesNotExist()
        assertThat(store.findByGame("PALWORLD")).isNull()
    }

    private fun registration(workspace: Path): GameServerRegistration {
        return GameServerRegistration(
            "PALWORLD",
            "palworld-main",
            "DEMO",
            "STOPPED",
            "Demo",
            "C:\\GameServerManager\\servers\\palworld\\main\\runtime",
            workspace.toString(),
            8211,
            25575,
            Instant.parse("2026-08-10T00:00:00Z"),
        )
    }

    private class MemoryStore(initial: GameServerRegistration) : GameServerRegistrationStore {
        private var registration: GameServerRegistration? = initial
        override fun findAll(): List<GameServerRegistration> = listOfNotNull(registration)
        override fun findByGame(game: String): GameServerRegistration? = registration?.takeIf { it.game == game }
        override fun create(registration: GameServerRegistration) {
            this.registration = registration
        }
        override fun update(registration: GameServerRegistration) {
            this.registration = registration
        }
        override fun delete(game: String) {
            if (registration?.game == game) registration = null
        }
    }
}
