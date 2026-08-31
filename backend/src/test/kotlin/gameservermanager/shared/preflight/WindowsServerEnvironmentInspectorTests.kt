package gameservermanager.shared.preflight

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class WindowsServerEnvironmentInspectorTests {
    private val inspector = WindowsServerEnvironmentInspector()

    @Test
    fun `存在する書き込み可能なフォルダーを検出する`(@TempDir tempDir: Path) {
        val inspection = inspector.inspectPath(tempDir.toString())

        assertThat(inspection.valid).isTrue()
        assertThat(inspection.absolute).isTrue()
        assertThat(inspection.exists).isTrue()
        assertThat(inspection.directory).isTrue()
        assertThat(inspection.writable).isTrue()
        assertThat(inspection.usableSpaceBytes).isPositive()
    }

    @Test
    fun `相対パスは絶対パスとして扱わない`() {
        val inspection = inspector.inspectPath("GameServers\\Palworld")

        assertThat(inspection.valid).isTrue()
        assertThat(inspection.absolute).isFalse()
        assertThat(inspection.writable).isFalse()
    }
}

