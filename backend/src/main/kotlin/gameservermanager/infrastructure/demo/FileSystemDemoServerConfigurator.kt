package gameservermanager.infrastructure.demo

import com.fasterxml.jackson.databind.ObjectMapper
import gameservermanager.application.construction.DemoServerConfigurationCommand
import gameservermanager.application.construction.DemoServerConfigurator
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
        val configurationDirectory = workspace.resolve(
            "servers/palworld/main/runtime/Pal/Saved/Config/WindowsServer",
        )
        Files.createDirectories(configurationDirectory)
        Files.writeString(
            configurationDirectory.resolve("PalWorldSettings.demo.ini"),
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
                "backupAfterShutdown" to command.backupAfterShutdown,
                "backupRetentionCount" to 3,
                "gamePort" to command.gamePort,
                "maxPlayers" to command.maxPlayers,
            ),
        )
    }

    private fun demoSettings(command: DemoServerConfigurationCommand): String {
        return listOf(
            "ServerName=${command.serverName}",
            "PublicPort=${command.gamePort}",
            "RCONPort=${command.rconPort}",
            "ServerPlayerMaxNum=${command.maxPlayers}",
            "ServerPassword=${if (command.serverPasswordConfigured) "<configured>" else "<not-configured>"}",
            "AdminPassword=${if (command.adminPasswordConfigured) "<configured>" else "<not-configured>"}",
        ).joinToString(System.lineSeparator())
    }
}
