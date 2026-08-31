package gameservermanager.shared.server

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import gameservermanager.shared.configuration.StorageProperties
import gameservermanager.shared.server.GameServerRegistration
import gameservermanager.shared.error.GameServerAlreadyExistsException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.time.Instant

class JsonGameServerRegistrationStoreTests {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `タイトルごとに1件だけJSONへ登録する`() {
        val mapper = jacksonObjectMapper().registerModule(JavaTimeModule())
        val store = JsonGameServerRegistrationStore(StorageProperties(tempDir.toString()), mapper)
        val registration = GameServerRegistration(
            "PALWORLD", "palworld-main", "DEMO", "STOPPED", "Demo",
            "install", "workspace", 8211, 25575, Instant.parse("2026-08-10T00:00:00Z"),
        )

        store.create(registration)

        assertThat(store.findByGame("PALWORLD")).isEqualTo(registration)
        assertThatThrownBy { store.create(registration) }
            .isInstanceOf(GameServerAlreadyExistsException::class.java)
    }
}
