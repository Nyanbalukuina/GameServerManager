package gameservermanager.palworld.application

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PalworldSettingsIniTests {
    @Test
    fun `本番形式から引用符とカンマを含む文字列を読み取る`() {
        val content = """
            [/Script/Pal.PalGameWorldSettings]
            OptionSettings=(ServerName="Tokyo, Server",ServerDescription="A \"quoted\" server",AdminPassword="admin\\pass",ExpRate=1.500000)
        """.trimIndent()

        val values = PalworldSettingsIni.read(
            content,
            listOf("ServerName", "ServerDescription", "AdminPassword", "ExpRate"),
        )

        assertThat(values["ServerName"]).isEqualTo("Tokyo, Server")
        assertThat(values["ServerDescription"]).isEqualTo("A \"quoted\" server")
        assertThat(values["AdminPassword"]).isEqualTo("admin\\pass")
        assertThat(values["ExpRate"]).isEqualTo("1.500000")
    }

    @Test
    fun `対象キーだけを更新して未知の設定を保持する`() {
        val content = """
            [/Script/Pal.PalGameWorldSettings]
            OptionSettings=(ServerName="Before",ExpRate=1.000000,FutureSetting="keep-me")
        """.trimIndent()

        val updated = PalworldSettingsIni.update(
            content,
            linkedMapOf("ServerName" to PalworldSettingsIni.quoted("After"), "ExpRate" to "2.0"),
        )

        assertThat(updated).contains(
            "ServerName=\"After\"",
            "ExpRate=2.0",
            "FutureSetting=\"keep-me\"",
        )
    }
}
