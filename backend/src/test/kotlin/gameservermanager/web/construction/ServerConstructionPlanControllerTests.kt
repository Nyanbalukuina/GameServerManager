package gameservermanager.web.construction

import gameservermanager.application.construction.CreateServerConstructionPlan
import gameservermanager.web.error.ApiExceptionHandler
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post

@WebMvcTest(ServerConstructionPlanController::class)
@AutoConfigureMockMvc(addFilters = false)
@Import(CreateServerConstructionPlan::class, ApiExceptionHandler::class)
class ServerConstructionPlanControllerTests @Autowired constructor(
    private val mockMvc: MockMvc,
) {
    @Test
    fun `有効な入力から構築計画を返す`() {
        mockMvc.post("/api/server-construction-plans") {
            contentType = MediaType.APPLICATION_JSON
            content = validRequest()
        }.andExpect {
            status { isOk() }
            jsonPath("$.serverName") { value("Palworld Server") }
            jsonPath("$.gamePort") { value(8211) }
            jsonPath("$.rconPort") { value(25575) }
            jsonPath("$.serverPasswordConfigured") { value(false) }
            jsonPath("$.adminPasswordConfigured") { value(true) }
            jsonPath("$.automationEnabled") { value(true) }
            jsonPath("$.shutdownTime") { value("04:00") }
            jsonPath("$.startupTime") { value("09:00") }
            jsonPath("$.backupAfterShutdown") { value(true) }
            jsonPath("$.backupRetentionCount") { value(3) }
            jsonPath("$.adminPassword") { doesNotExist() }
        }
    }

    @Test
    fun `必須項目がない場合は日本語のエラーを返す`() {
        mockMvc.post("/api/server-construction-plans") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "serverName": "",
                  "installPath": "",
                  "steamCmdPath": "",
                  "gamePort": null,
                  "rconPort": null,
                  "maxPlayers": null,
                  "serverPassword": "",
                  "adminPassword": ""
                }
            """.trimIndent()
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.errors.serverName") { value("サーバー名を入力してください") }
            jsonPath("$.errors.gamePort") { value("ゲームポートを入力してください") }
            jsonPath("$.errors.rconPort") { value("RCONポートを入力してください") }
            jsonPath("$.errors.maxPlayers") { value("最大プレイヤー数を入力してください") }
        }
    }

    @Test
    fun `ゲームポートとRCONポートの重複を拒否する`() {
        mockMvc.post("/api/server-construction-plans") {
            contentType = MediaType.APPLICATION_JSON
            content = validRequest().replace("\"rconPort\": 25575", "\"rconPort\": 8211")
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.errors.rconPort") {
                value("RCONポートはゲームポートと異なる値にしてください")
            }
        }
    }

    private fun validRequest(): String {
        return """
            {
              "serverName": "Palworld Server",
              "installPath": "C:\\GameServers\\Palworld",
              "steamCmdPath": "C:\\GameServers\\SteamCMD",
              "gamePort": 8211,
              "rconPort": 25575,
              "maxPlayers": 3,
              "serverPassword": "",
              "adminPassword": "admin-password",
              "automationEnabled": true,
              "shutdownTime": "04:00",
              "startupTime": "09:00",
              "backupAfterShutdown": true
            }
        """.trimIndent()
    }
}
