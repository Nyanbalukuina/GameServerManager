package gameservermanager.infrastructure.windows

import gameservermanager.configuration.StorageProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class WindowsManagedPathPolicyTests {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `管理ルート内のserversとtools配下だけを許可する`() {
        val policy = WindowsManagedPathPolicy(StorageProperties(tempDir.toString()))

        assertThat(policy.isServerPathAllowed(tempDir.resolve("servers/palworld/main/runtime").toString())).isTrue()
        assertThat(policy.isToolPathAllowed(tempDir.resolve("tools/steamcmd").toString())).isTrue()
        assertThat(policy.isServerPathAllowed(tempDir.resolve("tools/steamcmd").toString())).isFalse()
        assertThat(policy.isToolPathAllowed(tempDir.resolve("servers/palworld").toString())).isFalse()
        assertThat(policy.isServerPathAllowed(tempDir.resolve("outside").toString())).isFalse()
    }
}
