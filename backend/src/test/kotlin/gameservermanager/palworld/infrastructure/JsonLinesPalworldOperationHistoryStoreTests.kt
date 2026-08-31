package gameservermanager.palworld.infrastructure

import com.fasterxml.jackson.databind.ObjectMapper
import gameservermanager.shared.configuration.StorageProperties
import gameservermanager.palworld.domain.PalworldOperationHistoryEntry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant

class JsonLinesPalworldOperationHistoryStoreTests {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `秘密情報を含まない履歴を新しい順で取得する`() {
        val store = JsonLinesPalworldOperationHistoryStore(
            StorageProperties(tempDir.toString()),
            ObjectMapper().findAndRegisterModules(),
        )
        store.append(entry("UPDATE", "更新しました", "2026-08-10T01:00:00Z"))
        store.append(entry("RESTART", "再起動しました", "2026-08-10T02:00:00Z"))

        val history = store.latest(1)

        assertThat(history).hasSize(1)
        assertThat(history.single().operation).isEqualTo("RESTART")
        val file = tempDir.resolve("logs/palworld-operation-history.jsonl")
        assertThat(Files.readAllLines(file)).hasSize(2)
        assertThat(Files.readString(file)).doesNotContain("password", "secret")
    }

    private fun entry(operation: String, message: String, timestamp: String): PalworldOperationHistoryEntry {
        return PalworldOperationHistoryEntry(
            timestamp = Instant.parse(timestamp),
            operation = operation,
            status = "COMPLETED",
            message = message,
        )
    }
}
