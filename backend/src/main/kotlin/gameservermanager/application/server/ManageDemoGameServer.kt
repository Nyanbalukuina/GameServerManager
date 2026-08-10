package gameservermanager.application.server

import gameservermanager.domain.server.GameServerRegistration
import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Autowired
import java.nio.file.Files
import java.nio.file.Path
import java.util.Comparator

@Service
class ManageDemoGameServer @Autowired constructor(
    private val store: GameServerRegistrationStore,
) {
    private var demoRoot: Path = Path.of(System.getProperty("java.io.tmpdir"))
        .resolve("GameServerManagerDemo")
        .toAbsolutePath()
        .normalize()

    constructor(store: GameServerRegistrationStore, demoRoot: Path) : this(store) {
        this.demoRoot = demoRoot.toAbsolutePath().normalize()
    }

    fun list(): List<GameServerRegistration> {
        return store.findAll()
    }

    fun getPalworld(): GameServerRegistration {
        return requireNotNull(store.findByGame("PALWORLD")) {
            "Palworldサーバーは作成されていません"
        }
    }

    fun operate(action: String): GameServerRegistration {
        val current = getPalworld()
        require(current.mode == "DEMO") {
            "実サーバーにはデモ操作を実行できません"
        }
        val nextState = when (action) {
            "START" -> "RUNNING"
            "STOP" -> "STOPPED"
            "RESTART" -> "RUNNING"
            else -> throw IllegalArgumentException("許可されていないデモ操作です")
        }
        val updated = current.copy(state = nextState)
        store.update(updated)
        return updated
    }

    fun deletePalworld(confirmation: String) {
        require(confirmation == "PALWORLD") {
            "削除確認にはPALWORLDと入力してください"
        }
        val current = getPalworld()
        require(current.mode == "DEMO") {
            "実サーバーはデモ削除できません"
        }
        val workspace = Path.of(current.workspacePath).toAbsolutePath().normalize()
        require(workspace == demoRoot) {
            "デモ領域以外は削除できません"
        }
        if (Files.exists(workspace)) {
            Files.walk(workspace).use { paths ->
                paths.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
            }
        }
        store.delete("PALWORLD")
    }
}
