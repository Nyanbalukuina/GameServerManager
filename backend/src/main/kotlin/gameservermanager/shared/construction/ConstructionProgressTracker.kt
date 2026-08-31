package gameservermanager.shared.construction

import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

@Component
class ConstructionProgressTracker {
    private val progress = ConcurrentHashMap<String, ConstructionProgressSnapshot>()

    fun start(game: String, steps: List<ConstructionProgressStepDefinition>) {
        progress[game] = ConstructionProgressSnapshot(
            game = game,
            status = ConstructionProgressStatus.RUNNING,
            steps = steps.map { ConstructionProgressStep(it.id, it.label, ConstructionProgressStepStatus.PENDING, "待機中") },
            updatedAt = Clock.systemUTC().instant(),
        )
    }

    fun running(game: String, id: String) = updateStep(game, id, ConstructionProgressStepStatus.RUNNING, "処理中")

    fun completed(game: String, id: String, message: String) =
        updateStep(game, id, ConstructionProgressStepStatus.COMPLETED, message)

    fun failed(game: String, id: String, message: String) {
        updateStep(game, id, ConstructionProgressStepStatus.ERROR, message)
        progress.computeIfPresent(game) { _, current -> current.copy(status = ConstructionProgressStatus.ERROR) }
    }

    fun finish(game: String) {
        progress.computeIfPresent(game) { _, current ->
            current.copy(status = ConstructionProgressStatus.COMPLETED, updatedAt = Clock.systemUTC().instant())
        }
    }

    fun get(game: String): ConstructionProgressSnapshot? = progress[game]

    private fun updateStep(game: String, id: String, status: ConstructionProgressStepStatus, message: String) {
        progress.computeIfPresent(game) { _, current ->
            current.copy(
                steps = current.steps.map { if (it.id == id) it.copy(status = status, message = message) else it },
                updatedAt = Clock.systemUTC().instant(),
            )
        }
    }
}

data class ConstructionProgressStepDefinition(val id: String, val label: String)

data class ConstructionProgressSnapshot(
    val game: String,
    val status: ConstructionProgressStatus,
    val steps: List<ConstructionProgressStep>,
    val updatedAt: Instant,
)

data class ConstructionProgressStep(
    val id: String,
    val label: String,
    val status: ConstructionProgressStepStatus,
    val message: String,
)

enum class ConstructionProgressStatus { RUNNING, COMPLETED, ERROR }

enum class ConstructionProgressStepStatus { PENDING, RUNNING, COMPLETED, ERROR }
