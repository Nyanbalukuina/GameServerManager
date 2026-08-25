package gameservermanager.application.startup

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class OpenManagementPageOnStartupTests {
    @Test
    fun `起動完了時にlocalhostの管理画面を開く`() {
        var openedUrl: String? = null
        val startup = OpenManagementPageOnStartup({ openedUrl = it }, 18080)

        startup.open()

        assertEquals("http://localhost:18080", openedUrl)
    }

    @Test
    fun `ブラウザ起動失敗をアプリ起動失敗にしない`() {
        val startup = OpenManagementPageOnStartup({ error("ブラウザを起動できません") }, 8080)

        startup.open()
    }
}
