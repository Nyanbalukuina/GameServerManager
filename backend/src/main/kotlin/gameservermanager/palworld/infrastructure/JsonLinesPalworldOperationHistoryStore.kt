package gameservermanager.palworld.infrastructure

import com.fasterxml.jackson.databind.ObjectMapper
import gameservermanager.palworld.application.PalworldOperationHistoryStore
import gameservermanager.shared.configuration.StorageProperties
import gameservermanager.palworld.domain.PalworldOperationHistoryEntry
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

@Component
class JsonLinesPalworldOperationHistoryStore(
    storageProperties: StorageProperties,
    private val objectMapper: ObjectMapper,
) : PalworldOperationHistoryStore {
    private val historyPath = Path.of(storageProperties.root)
        .toAbsolutePath()
        .normalize()
        .resolve("logs/palworld-operation-history.jsonl")

    @Synchronized
    override fun append(entry: PalworldOperationHistoryEntry) {
        Files.createDirectories(requireNotNull(historyPath.parent))
        val line = objectMapper.writeValueAsString(entry) + System.lineSeparator()
        Files.writeString(
            historyPath,
            line,
            StandardCharsets.UTF_8,
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND,
        )
    }

    @Synchronized
    override fun latest(limit: Int): List<PalworldOperationHistoryEntry> {
        require(limit in 1..100) {
            "履歴件数は1から100の範囲で指定してください"
        }
        if (!Files.isRegularFile(historyPath)) {
            return emptyList()
        }

        return Files.readAllLines(historyPath, StandardCharsets.UTF_8)
            .asReversed()
            .asSequence()
            .filter(String::isNotBlank)
            .map { objectMapper.readValue(it, PalworldOperationHistoryEntry::class.java) }
            .take(limit)
            .toList()
    }
}
