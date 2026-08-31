package gameservermanager.shared.configuration

import gameservermanager.shared.configuration.StorageProperties
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.mockito.BDDMockito.given

@WebMvcTest(StorageConfigurationController::class)
@AutoConfigureMockMvc(addFilters = false)
class StorageConfigurationControllerTests @Autowired constructor(
    private val mockMvc: MockMvc,
) {
    @MockitoBean
    private lateinit var storageProperties: StorageProperties

    @Test
    fun `設定された管理ルートから各保存先を返す`() {
        given(storageProperties.root).willReturn("C:\\GameServerManagerDev")

        mockMvc.get("/api/configuration/storage").andExpect {
            status { isOk() }
            jsonPath("$.root") { value("C:\\GameServerManagerDev") }
            jsonPath("$.palworldInstallPath") {
                value("C:\\GameServerManagerDev\\servers\\palworld\\main\\runtime")
            }
            jsonPath("$.steamCmdPath") { value("C:\\GameServerManagerDev\\tools\\steamcmd") }
        }
    }
}
