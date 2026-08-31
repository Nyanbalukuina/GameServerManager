package gameservermanager.shared.construction

import com.fasterxml.jackson.databind.ObjectMapper
import gameservermanager.shared.construction.DemoServerConfigurationCommand
import gameservermanager.shared.construction.DemoServerConfigurator
import gameservermanager.palworld.application.PalworldSettingsIni
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

@Component
class FileSystemDemoServerConfigurator(
    private val objectMapper: ObjectMapper,
) : DemoServerConfigurator {
    override fun configure(command: DemoServerConfigurationCommand) {
        val workspace = Path.of(command.workspacePath).toAbsolutePath().normalize()
        val runtimeDirectory = workspace.resolve("servers/palworld/main/runtime")
        val configurationDirectory = runtimeDirectory.resolve(
            "Pal/Saved/Config/WindowsServer",
        )
        Files.createDirectories(configurationDirectory)
        Files.writeString(
            runtimeDirectory.resolve("DefaultPalWorldSettings.ini"),
            defaultSettings(),
            StandardCharsets.UTF_8,
        )
        Files.writeString(
            configurationDirectory.resolve("PalWorldSettings.ini"),
            demoSettings(command),
            StandardCharsets.UTF_8,
        )

        val automationPath = workspace.resolve("config/palworld-main-automation.demo.json")
        Files.createDirectories(requireNotNull(automationPath.parent))
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(
            automationPath.toFile(),
            linkedMapOf(
                "enabled" to command.automationEnabled,
                "shutdownTime" to command.shutdownTime,
                "startupTime" to command.startupTime,
                "gamePort" to command.gamePort,
                "maxPlayers" to command.maxPlayers,
                "gamePortRemoteAddresses" to command.gamePortAccess.remoteAddresses(),
            ),
        )
    }

    private fun demoSettings(command: DemoServerConfigurationCommand): String {
        return PalworldSettingsIni.update(
            defaultSettings(),
            linkedMapOf(
                "ServerName" to PalworldSettingsIni.quoted(command.serverName),
                "PublicPort" to command.gamePort.toString(),
                "RCONPort" to command.rconPort.toString(),
                "ServerPlayerMaxNum" to command.maxPlayers.toString(),
                "ServerPassword" to PalworldSettingsIni.quoted(command.serverPassword),
                "AdminPassword" to PalworldSettingsIni.quoted(command.adminPassword),
                "ServerDescription" to PalworldSettingsIni.quoted(command.serverDescription),
                "ExpRate" to command.expRate.toString(),
                "PalCaptureRate" to command.palCaptureRate.toString(),
                "PalSpawnNumRate" to command.palSpawnRate.toString(),
                "EnemyDropItemRate" to command.enemyDropRate.toString(),
                "PalEggDefaultHatchingTime" to command.eggHatchingTime.toString(),
                "DeathPenalty" to command.deathPenalty,
                "bIsPvP" to command.pvpEnabled.toString().replaceFirstChar(Char::uppercase),
                "bEnableFriendlyFire" to command.friendlyFireEnabled.toString().replaceFirstChar(Char::uppercase),
                "BaseCampMaxNum" to command.baseCampMaxNum.toString(),
                "BaseCampWorkerMaxNum" to command.baseCampWorkerMaxNum.toString(),
            ),
        )
    }

    private fun defaultSettings(): String {
        return """
            [/Script/Pal.PalGameWorldSettings]
            OptionSettings=(Difficulty=None,ServerName="Default Palworld Server",ServerDescription="",ServerPlayerMaxNum=32,ServerPassword="",AdminPassword="",ExpRate=1.000000,PalCaptureRate=1.000000,PalSpawnNumRate=1.000000,EnemyDropItemRate=1.000000,PalEggDefaultHatchingTime=2.000000,DeathPenalty=All,bIsPvP=False,bEnableFriendlyFire=False,BaseCampMaxNum=128,BaseCampWorkerMaxNum=15,PublicPort=8211,RCONEnabled=True,RCONPort=25575,RESTAPIEnabled=True,RESTAPIPort=8212,bIsUseBackupSaveData=True,BanListURL="https://api.palworldgame.com/api/banlist.txt")
        """.trimIndent()
    }
}
