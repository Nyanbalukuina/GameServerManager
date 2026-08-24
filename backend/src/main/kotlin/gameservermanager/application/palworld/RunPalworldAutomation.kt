package gameservermanager.application.palworld

import gameservermanager.application.preflight.ManagedPathPolicy
import gameservermanager.application.preflight.ServerEnvironmentInspector
import gameservermanager.domain.palworld.PalworldAutomationRuntimeState
import gameservermanager.domain.palworld.PalworldAutomationStatus
import gameservermanager.domain.palworld.PalworldOperationHistoryEntry
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Service
class RunPalworldAutomation(
    private val configureAutomation: ConfigurePalworldAutomation,
    private val store: PalworldAutomationStore,
    private val managedPathPolicy: ManagedPathPolicy,
    private val environmentInspector: ServerEnvironmentInspector,
    private val processManager: PalworldServerProcessManager,
    private val adminPasswordProvider: PalworldAdminPasswordProvider,
    private val stopServer: StopPalworldServer,
    private val startServer: StartPalworldServer,
    private val historyStore: PalworldOperationHistoryStore,
) {
    private var clock: Clock = Clock.systemDefaultZone()

    @Scheduled(fixedDelayString = "${'$'}{game-server-manager.automation.poll-delay:30000}")
    @Synchronized
    fun runScheduled() {
        execute(LocalDateTime.now(clock))
    }

    @Synchronized
    fun execute(now: LocalDateTime = LocalDateTime.now(clock)): PalworldAutomationStatus {
        val settings = configureAutomation.get()
        var runtime = store.loadRuntime()
        if (!settings.enabled) {
            return status(settings, runtime, now, "自動運転は無効です")
        }

        val installPath = managedInstallPath()
        val shutdownTime = LocalTime.parse(settings.shutdownTime)
        val startupTime = LocalTime.parse(settings.startupTime)
        val downtime = isDowntime(now.toLocalTime(), shutdownTime, startupTime)
        val cycle = cycleDate(now, shutdownTime, startupTime).toString()

        try {
            if (downtime) {
                if (isServerRunning(settings.gamePort)) {
                    val password = adminPasswordProvider.read(installPath)
                    stopServer.execute(
                        StopPalworldServer.Command(
                            restApiPort = ConfigurePalworldServer.REST_API_PORT,
                            adminPassword = password,
                        ),
                    )
                    runtime = runtime.copy(
                        stoppedBySchedule = true,
                        lastShutdownCycle = cycle,
                    )
                    store.saveRuntime(runtime)
                }
            } else if (runtime.stoppedBySchedule && !isServerRunning(settings.gamePort)) {
                startServer.execute(
                    StartPalworldServer.Command(
                        installPath = installPath,
                        gamePort = settings.gamePort,
                        maxPlayers = settings.maxPlayers,
                    ),
                )
                runtime = runtime.copy(
                    stoppedBySchedule = false,
                    lastStartupCycle = now.toLocalDate().toString(),
                    lastError = null,
                )
                store.saveRuntime(runtime)
            }
        } catch (exception: RuntimeException) {
            val errorMessage = exception.message ?: "自動運転に失敗しました"
            val shouldRecordError = runtime.lastError != errorMessage
            runtime = runtime.copy(lastError = errorMessage)
            store.saveRuntime(runtime)
            if (shouldRecordError) {
                historyStore.append(
                    PalworldOperationHistoryEntry(
                        clock.instant(),
                        "AUTOMATION",
                        "ERROR",
                        errorMessage,
                    ),
                )
            }
        }

        val nextAction = when {
            runtime.lastError != null -> "エラーを確認してください"
            downtime -> "起動時刻まで停止します"
            runtime.stoppedBySchedule -> "Palworldサーバーを起動します"
            else -> "停止時刻まで稼働します"
        }
        return status(settings, runtime, now, nextAction)
    }

    fun status(now: LocalDateTime = LocalDateTime.now(clock)): PalworldAutomationStatus {
        val settings = configureAutomation.get()
        val runtime = store.loadRuntime()
        return status(settings, runtime, now, if (settings.enabled) "次回の自動処理を待機しています" else "自動運転は無効です")
    }

    private fun status(
        settings: gameservermanager.domain.palworld.PalworldAutomationSettings,
        runtime: PalworldAutomationRuntimeState,
        now: LocalDateTime,
        nextAction: String,
    ): PalworldAutomationStatus {
        val downtime = isDowntime(
            now.toLocalTime(),
            LocalTime.parse(settings.shutdownTime),
            LocalTime.parse(settings.startupTime),
        )
        return PalworldAutomationStatus(settings, runtime, downtime, nextAction)
    }

    private fun managedInstallPath(): String {
        return java.nio.file.Path.of(managedPathPolicy.managedRoot())
            .resolve("servers/palworld/main/runtime")
            .toString()
    }

    private fun isServerRunning(gamePort: Int): Boolean {
        return processManager.current()?.alive == true || !environmentInspector.isUdpPortAvailable(gamePort)
    }

    private fun isDowntime(time: LocalTime, shutdown: LocalTime, startup: LocalTime): Boolean {
        return if (shutdown < startup) {
            time >= shutdown && time < startup
        } else {
            time >= shutdown || time < startup
        }
    }

    private fun cycleDate(now: LocalDateTime, shutdown: LocalTime, startup: LocalTime): LocalDate {
        return if (shutdown < startup || now.toLocalTime() >= shutdown) {
            now.toLocalDate()
        } else {
            now.toLocalDate().minusDays(1)
        }
    }
}
