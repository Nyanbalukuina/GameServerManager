package gameservermanager.asa.application

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CheckAsaServerVersionTests {
    @Test
    fun `現在と最新のBuild IDが異なる場合は更新可能と判定する`() {
        val result = CheckAsaServerVersion(FixedVersionProvider("100", "101")).execute(
            CheckAsaServerVersion.Command("steamcmd", "asa"),
        )

        assertThat(result.currentBuildId).isEqualTo("100")
        assertThat(result.latestBuildId).isEqualTo("101")
        assertThat(result.updateAvailable).isTrue()
    }

    @Test
    fun `現在と最新のBuild IDが同じ場合は最新と判定する`() {
        val result = CheckAsaServerVersion(FixedVersionProvider("101", "101")).execute(
            CheckAsaServerVersion.Command("steamcmd", "asa"),
        )

        assertThat(result.updateAvailable).isFalse()
        assertThat(result.message).isEqualTo("サーバーは最新です")
    }

    private class FixedVersionProvider(
        private val current: String,
        private val latest: String,
    ) : AsaServerVersionProvider {
        override fun currentBuildId(steamCmdPath: String, installPath: String) = current
        override fun latestBuildId(steamCmdPath: String, installPath: String) = latest
    }
}
