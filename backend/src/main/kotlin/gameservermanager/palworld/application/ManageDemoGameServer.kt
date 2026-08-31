package gameservermanager.palworld.application

import gameservermanager.shared.server.GameServerRegistration
import gameservermanager.shared.server.GameServerProcessManager
import gameservermanager.shared.server.GameServerRegistrationStore
import gameservermanager.palworld.web.DemoPalworldSettingsResponse
import gameservermanager.palworld.web.UpdateDemoPalworldSettingsRequest
import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Autowired
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.AtomicMoveNotSupportedException
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

    fun getSettings(): DemoPalworldSettingsResponse {
        val current = requireDemoRegistration()
        val values = readSettings(current)
        return settingsResponse(values)
    }

    fun updateSettings(request: UpdateDemoPalworldSettingsRequest): DemoPalworldSettingsResponse {
        val current = requireDemoRegistration()
        require(current.state == "STOPPED") {
            "設定を変更する前にサーバーを停止してください"
        }
        val path = settingsPath(current)
        val values = linkedMapOf(
            "ServerName" to PalworldSettingsIni.quoted(request.serverName),
            "ServerDescription" to PalworldSettingsIni.quoted(request.serverDescription),
            "ServerPlayerMaxNum" to request.maxPlayers.toString(),
            "ServerPassword" to PalworldSettingsIni.quoted(request.serverPassword),
            "AdminPassword" to PalworldSettingsIni.quoted(request.adminPassword),
            "ExpRate" to request.expRate.toString(),
            "PalCaptureRate" to request.palCaptureRate.toString(),
            "PalSpawnNumRate" to request.palSpawnRate.toString(),
            "EnemyDropItemRate" to request.enemyDropRate.toString(),
            "PalEggDefaultHatchingTime" to request.eggHatchingTime.toString(),
            "DeathPenalty" to request.deathPenalty,
            "bIsPvP" to booleanValue(request.pvpEnabled),
            "bEnableFriendlyFire" to booleanValue(request.friendlyFireEnabled),
            "BaseCampMaxNum" to request.baseCampMaxNum.toString(),
            "BaseCampWorkerMaxNum" to request.baseCampWorkerMaxNum.toString(),
        )

        val backupDirectory = demoRoot.resolve("backups/palworld-main/config")
        Files.createDirectories(backupDirectory)
        Files.copy(
            path,
            Files.createTempFile(backupDirectory, "PalWorldSettings-", ".ini"),
            StandardCopyOption.REPLACE_EXISTING,
        )
        val updated = PalworldSettingsIni.update(Files.readString(path), values)
        val temporary = Files.createTempFile(path.parent, ".palworld-settings-", ".tmp")
        try {
            Files.writeString(temporary, updated)
            moveReplacing(temporary, path)
        } finally {
            Files.deleteIfExists(temporary)
        }
        store.update(current.copy(serverName = request.serverName))
        return settingsResponse(readSettings(current))
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

    private fun requireDemoRegistration(): GameServerRegistration {
        val current = getPalworld()
        require(current.mode == "DEMO") {
            "実サーバーにはデモ設定を使用できません"
        }
        val workspace = Path.of(current.workspacePath).toAbsolutePath().normalize()
        require(workspace == demoRoot) {
            "デモ領域以外の設定は操作できません"
        }
        return current
    }

    private fun settingsPath(registration: GameServerRegistration): Path {
        val configurationDirectory = Path.of(registration.workspacePath).toAbsolutePath().normalize().resolve(
            "servers/palworld/main/runtime/Pal/Saved/Config/WindowsServer",
        )
        val path = configurationDirectory.resolve("PalWorldSettings.ini")
        if (!Files.isRegularFile(path)) migrateLegacySettings(configurationDirectory)
        require(Files.isRegularFile(path)) {
            "デモ設定ファイルが見つかりません"
        }
        return path
    }

    private fun readSettings(registration: GameServerRegistration): LinkedHashMap<String, String> {
        return PalworldSettingsIni.read(Files.readString(settingsPath(registration)), SETTINGS_KEYS)
    }

    private fun settingsResponse(values: Map<String, String>): DemoPalworldSettingsResponse {
        return DemoPalworldSettingsResponse(
            serverName = values.required("ServerName"),
            serverDescription = values.required("ServerDescription"),
            maxPlayers = values.required("ServerPlayerMaxNum").toInt(),
            serverPassword = values.required("ServerPassword"),
            adminPassword = values.required("AdminPassword"),
            expRate = values.required("ExpRate").toDouble(),
            palCaptureRate = values.required("PalCaptureRate").toDouble(),
            palSpawnRate = values.required("PalSpawnNumRate").toDouble(),
            enemyDropRate = values.required("EnemyDropItemRate").toDouble(),
            eggHatchingTime = values.required("PalEggDefaultHatchingTime").toDouble(),
            deathPenalty = values.required("DeathPenalty"),
            pvpEnabled = values.required("bIsPvP").equals("True", ignoreCase = true),
            friendlyFireEnabled = values.required("bEnableFriendlyFire").equals("True", ignoreCase = true),
            baseCampMaxNum = values.required("BaseCampMaxNum").toInt(),
            baseCampWorkerMaxNum = values.required("BaseCampWorkerMaxNum").toInt(),
        )
    }

    private fun Map<String, String>.required(key: String): String {
        return requireNotNull(this[key]) { "デモ設定キー${key}が見つかりません" }
    }

    private fun booleanValue(value: Boolean): String = if (value) "True" else "False"

    private fun migrateLegacySettings(configurationDirectory: Path) {
        val legacyPath = configurationDirectory.resolve("PalWorldSettings.demo.ini")
        if (!Files.isRegularFile(legacyPath)) return
        val legacy = Files.readAllLines(legacyPath).associate { line ->
            val separator = line.indexOf('=')
            require(separator > 0) { "旧デモ設定ファイルの形式が不正です" }
            line.substring(0, separator) to line.substring(separator + 1)
        }
        val defaults = demoSettingsTemplate()
        val values = SETTINGS_KEYS.associateWith { key ->
            val value = legacy[key] ?: DEFAULT_VALUES.getValue(key)
            if (key in STRING_KEYS) PalworldSettingsIni.quoted(legacyPasswordValue(value)) else value
        }
        Files.writeString(configurationDirectory.resolve("PalWorldSettings.ini"), PalworldSettingsIni.update(defaults, values))
    }

    private fun legacyPasswordValue(value: String): String {
        return if (value == "<configured>" || value == "<not-configured>") "" else value
    }

    private fun moveReplacing(source: Path, destination: Path) {
        try {
            Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun demoSettingsTemplate(): String {
        return "[/Script/Pal.PalGameWorldSettings]${System.lineSeparator()}OptionSettings=(ServerName=\"\",ServerDescription=\"\",ServerPlayerMaxNum=32,ServerPassword=\"\",AdminPassword=\"\",ExpRate=1.0,PalCaptureRate=1.0,PalSpawnNumRate=1.0,EnemyDropItemRate=1.0,PalEggDefaultHatchingTime=2.0,DeathPenalty=All,bIsPvP=False,bEnableFriendlyFire=False,BaseCampMaxNum=128,BaseCampWorkerMaxNum=15)"
    }

    companion object {
        private val SETTINGS_KEYS = listOf("ServerName", "ServerDescription", "ServerPlayerMaxNum", "ServerPassword", "AdminPassword", "ExpRate", "PalCaptureRate", "PalSpawnNumRate", "EnemyDropItemRate", "PalEggDefaultHatchingTime", "DeathPenalty", "bIsPvP", "bEnableFriendlyFire", "BaseCampMaxNum", "BaseCampWorkerMaxNum")
        private val STRING_KEYS = setOf("ServerName", "ServerDescription", "ServerPassword", "AdminPassword")
        private val DEFAULT_VALUES = mapOf("ServerName" to "Demo", "ServerDescription" to "", "ServerPlayerMaxNum" to "3", "ServerPassword" to "", "AdminPassword" to "", "ExpRate" to "1.0", "PalCaptureRate" to "1.0", "PalSpawnNumRate" to "1.0", "EnemyDropItemRate" to "1.0", "PalEggDefaultHatchingTime" to "2.0", "DeathPenalty" to "All", "bIsPvP" to "False", "bEnableFriendlyFire" to "False", "BaseCampMaxNum" to "128", "BaseCampWorkerMaxNum" to "15")
    }
}
