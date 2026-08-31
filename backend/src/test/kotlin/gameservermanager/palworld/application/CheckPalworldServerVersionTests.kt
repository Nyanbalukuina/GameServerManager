package gameservermanager.palworld.application

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CheckPalworldServerVersionTests {
    @Test
    fun `現在と最新のBuild IDが異なる場合は更新可能と判定する`() {
        val result = CheckPalworldServerVersion(FixedVersionProvider("200", "201")).execute(
            CheckPalworldServerVersion.Command("steamcmd", "palworld"),
        )

        assertThat(result.currentBuildId).isEqualTo("200")
        assertThat(result.latestBuildId).isEqualTo("201")
        assertThat(result.updateAvailable).isTrue()
    }

    @Test
    fun `現在と最新のBuild IDが同じ場合は最新と判定する`() {
        val result = CheckPalworldServerVersion(FixedVersionProvider("201", "201")).execute(
            CheckPalworldServerVersion.Command("steamcmd", "palworld"),
        )

        assertThat(result.updateAvailable).isFalse()
        assertThat(result.message).isEqualTo("サーバーは最新です")
    }

    private class FixedVersionProvider(
        private val current: String,
        private val latest: String,
    ) : PalworldServerVersionProvider {
        override fun currentBuildId(steamCmdPath: String, installPath: String) = current
        override fun latestBuildId(steamCmdPath: String, installPath: String) = latest
    }
}
