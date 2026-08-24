package gameservermanager.application.asa

import gameservermanager.application.preflight.ManagedPathPolicy
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

// ASAの起動引数とポート計算を確認する。
class CreateAsaServerLaunchCommandTests {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `ASAの起動引数とPeerポートを作成する`() {
        val creator = CreateAsaServerLaunchCommand(TemporaryManagedPathPolicy(tempDir))

        // The Islandを標準ポートで起動する情報を作る。
        val launchCommand = creator.execute(command())

        // 実行ファイル、引数、各ポートが正しいことを確認する。
        assertThat(launchCommand.executable.toString()).endsWith(
            "ShooterGame\\Binaries\\Win64\\ArkAscendedServer.exe",
        )
        assertThat(launchCommand.arguments).containsExactly(
            "TheIsland_WP?QueryPort=27015",
            "-port=7777",
            "-WinLiveMaxPlayers=20",
        )
        assertThat(launchCommand.gamePort).isEqualTo(7777)
        assertThat(launchCommand.peerPort).isEqualTo(7778)
        assertThat(launchCommand.queryPort).isEqualTo(27015)
        assertThat(launchCommand.logPath.toString()).endsWith("logs\\asa-main.log")
    }

    @Test
    fun `QueryポートがPeerポートと同じなら拒否する`() {
        val creator = CreateAsaServerLaunchCommand(TemporaryManagedPathPolicy(tempDir))

        // ゲームポート7777のPeerポートである7778をQueryポートへ指定する。
        assertThatThrownBy {
            creator.execute(command().copy(queryPort = 7778))
        }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("QueryポートはゲームポートおよびPeerポートと分けてください")
    }

    @Test
    fun `未対応マップなら拒否する`() {
        val creator = CreateAsaServerLaunchCommand(TemporaryManagedPathPolicy(tempDir))

        // 最小構成で未対応のマップを指定する。
        assertThatThrownBy {
            creator.execute(command().copy(map = "ScorchedEarth_WP"))
        }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("現在対応しているマップはThe Islandだけです")
    }

    // 正常な起動コマンド入力を作る。
    private fun command(): CreateAsaServerLaunchCommand.Command {
        return CreateAsaServerLaunchCommand.Command(
            installPath = tempDir.resolve("servers/asa/main/runtime").toString(),
            map = CreateAsaServerLaunchCommand.THE_ISLAND_MAP,
            gamePort = 7777,
            queryPort = 27015,
            maxPlayers = 20,
        )
    }

    // テスト用ルート内だけを管理対象として許可する。
    private class TemporaryManagedPathPolicy(root: Path) : ManagedPathPolicy {
        private val normalizedRoot = root.toAbsolutePath().normalize()

        override fun isServerPathAllowed(path: String): Boolean {
            return Path.of(path)
                .toAbsolutePath()
                .normalize()
                .startsWith(normalizedRoot.resolve("servers"))
        }

        override fun isToolPathAllowed(path: String): Boolean {
            return Path.of(path)
                .toAbsolutePath()
                .normalize()
                .startsWith(normalizedRoot.resolve("tools"))
        }

        override fun managedRoot(): String {
            return normalizedRoot.toString()
        }
    }
}