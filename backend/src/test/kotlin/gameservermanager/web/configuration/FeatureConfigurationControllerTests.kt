package gameservermanager.web.configuration

import gameservermanager.configuration.FeatureProperties
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class FeatureConfigurationControllerTests {
    @Test
    fun `デモ機能の有効状態を返す`() {
        val mockMvc = MockMvcBuilders
            .standaloneSetup(FeatureConfigurationController(FeatureProperties(demoEnabled = false)))
            .build()

        mockMvc.get("/api/configuration/features").andExpect {
            status { isOk() }
            jsonPath("$.demoEnabled") { value(false) }
        }
    }
}
