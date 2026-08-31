package gameservermanager.palworld.application

import gameservermanager.palworld.domain.PalworldAutomationSettings
import org.springframework.stereotype.Service
import java.time.LocalTime

@Service
class ConfigurePalworldAutomation(
    private val store: PalworldAutomationStore,
) {
    fun get(): PalworldAutomationSettings {
        return store.loadSettings() ?: PalworldAutomationSettings(
            enabled = false,
            shutdownTime = "04:00",
            startupTime = "09:00",
            gamePort = 8211,
            maxPlayers = 3,
        )
    }

    fun save(command: Command): PalworldAutomationSettings {
        val shutdownTime = parseTime(command.shutdownTime, "停止時刻")
        val startupTime = parseTime(command.startupTime, "起動時刻")
        require(!command.enabled || shutdownTime != startupTime) {
            "停止時刻と起動時刻は別の時刻を指定してください"
        }
        require(command.gamePort in 1..65535) {
            "ゲームポートは1から65535の範囲で指定してください"
        }
        require(command.maxPlayers in 1..32) {
            "最大プレイヤー数は1から32の範囲で指定してください"
        }

        val settings = PalworldAutomationSettings(
            enabled = command.enabled,
            shutdownTime = shutdownTime.toString(),
            startupTime = startupTime.toString(),
            gamePort = command.gamePort,
            maxPlayers = command.maxPlayers,
        )
        store.saveSettings(settings)
        return settings
    }

    private fun parseTime(value: String, label: String): LocalTime {
        return try {
            LocalTime.parse(value)
        } catch (_: RuntimeException) {
            throw IllegalArgumentException("${label}をHH:mm形式で指定してください")
        }
    }

    data class Command(
        val enabled: Boolean,
        val shutdownTime: String,
        val startupTime: String,
        val gamePort: Int,
        val maxPlayers: Int,
    )
}
