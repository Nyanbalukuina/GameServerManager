package gameservermanager.shared.server

import gameservermanager.shared.server.GameServerRegistration
import gameservermanager.shared.server.UpdateDemoPalworldSettingsRequest
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

    @Test
    fun `停止中は設定を更新して変更前ファイルをバックアップする`() {
        val demoRoot = tempDir.resolve("GameServerManagerDemo")
        writeSettings(demoRoot)
        val store = MemoryStore(registration(demoRoot))
        val manager = ManageDemoGameServer(store, demoRoot)

        val updated = manager.updateSettings(updateRequest())

        assertThat(updated.serverName).isEqualTo("Updated Demo")
        assertThat(updated.adminPassword).isEqualTo("new-admin-password")
        assertThat(updated.serverPassword).isEmpty()
        assertThat(store.findByGame("PALWORLD")?.serverName).isEqualTo("Updated Demo")
        assertThat(Files.list(demoRoot.resolve("backups/palworld-main/config")).use { it.count() }).isEqualTo(1)
        assertThat(Files.readString(settingsPath(demoRoot)))
            .contains("ExpRate=2.0", "AdminPassword=\"new-admin-password\"", "BanListURL=\"https://api.palworldgame.com/api/banlist.txt\"")
    }

    @Test
    fun `起動中は設定変更を拒否する`() {
        val demoRoot = tempDir.resolve("GameServerManagerDemo")
        writeSettings(demoRoot)
        val running = registration(demoRoot).copy(state = "RUNNING")
        val manager = ManageDemoGameServer(MemoryStore(running), demoRoot)

        assertThatThrownBy { manager.updateSettings(updateRequest()) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("設定を変更する前にサーバーを停止してください")
    }

    private fun writeSettings(demoRoot: Path) {
        val path = settingsPath(demoRoot)
        Files.createDirectories(path.parent)
        Files.writeString(
            path,
            """
                [/Script/Pal.PalGameWorldSettings]
                OptionSettings=(ServerName="Demo",ServerDescription="",ServerPlayerMaxNum=3,ServerPassword="",AdminPassword="admin-password",ExpRate=1.0,PalCaptureRate=1.0,PalSpawnNumRate=1.0,EnemyDropItemRate=1.0,PalEggDefaultHatchingTime=2.0,DeathPenalty=All,bIsPvP=False,bEnableFriendlyFire=False,BaseCampMaxNum=128,BaseCampWorkerMaxNum=15,BanListURL="https://api.palworldgame.com/api/banlist.txt")
            """.trimIndent(),
        )
    }

    private fun settingsPath(demoRoot: Path): Path = demoRoot.resolve(
        "servers/palworld/main/runtime/Pal/Saved/Config/WindowsServer/PalWorldSettings.ini",
    )

    private fun updateRequest() = UpdateDemoPalworldSettingsRequest(
        serverName = "Updated Demo",
        serverDescription = "Description",
        maxPlayers = 8,
        adminPassword = "new-admin-password",
        expRate = 2.0,
        palCaptureRate = 1.5,
        palSpawnRate = 1.0,
        enemyDropRate = 2.0,
        eggHatchingTime = 1.0,
        deathPenalty = "Item",
        pvpEnabled = false,
        friendlyFireEnabled = false,
        baseCampMaxNum = 64,
        baseCampWorkerMaxNum = 20,
    )

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
