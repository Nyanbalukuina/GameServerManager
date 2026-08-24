package gameservermanager.application.asa

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

// ASAのINI設定が正しく更新されることを確認する。
class AsaGameUserSettingsIniTests {
    @Test
    fun `空の設定へ必要なセクションと設定を追加する`() {
        // ASAの最小設定を準備する。
        val sections = linkedMapOf(
            "SessionSettings" to linkedMapOf(
                "SessionName" to "GSM ASA Server",
            ),
            "ServerSettings" to linkedMapOf(
                "ServerPassword" to "",
                "ServerAdminPassword" to "admin-secret",
                "RCONEnabled" to "True",
                "RCONPort" to "27020",
            ),
        )

        // 空のINIへ設定を追加する。
        val updated = AsaGameUserSettingsIni.update("", sections)

        // 必要なセクションと設定が生成されたことを確認する。
        assertThat(updated).isEqualTo(
            """
                [SessionSettings]
                SessionName=GSM ASA Server

                [ServerSettings]
                ServerPassword=
                ServerAdminPassword=admin-secret
                RCONEnabled=True
                RCONPort=27020
            """.trimIndent() + "\n",
        )
    }

    @Test
    fun `既存設定を残して指定した設定だけ更新する`() {
        // ASAが生成済みの設定ファイルを再現する。
        val original = """
            [SessionSettings]
            SessionName=Old Server

            [ServerSettings]
            ServerPassword=old-password
            ServerCrosshair=True
            RCONPort=12345
        """.trimIndent()

        val sections = linkedMapOf(
            "SessionSettings" to linkedMapOf(
                "SessionName" to "New Server",
            ),
            "ServerSettings" to linkedMapOf(
                "ServerPassword" to "new-password",
                "ServerAdminPassword" to "admin-secret",
                "RCONEnabled" to "True",
                "RCONPort" to "27020",
            ),
        )

        // 既存INIの対象設定を更新する。
        val updated = AsaGameUserSettingsIni.update(original, sections)

        // 既存設定を保持しながら新しい値が反映されたことを確認する。
        assertThat(updated).contains(
            "SessionName=New Server",
            "ServerPassword=new-password",
            "ServerAdminPassword=admin-secret",
            "RCONEnabled=True",
            "RCONPort=27020",
            "ServerCrosshair=True",
        )
        assertThat(updated).doesNotContain(
            "SessionName=Old Server",
            "ServerPassword=old-password",
            "RCONPort=12345",
        )
    }
}