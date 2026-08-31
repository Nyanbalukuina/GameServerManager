package gameservermanager.shared.security

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@SpringBootTest
@AutoConfigureMockMvc
class LocalSecurityIntegrationTests @Autowired constructor(
    private val mockMvc: MockMvc,
) {
    @Test
    fun `ログインなしで管理APIを利用できる`() {
        mockMvc.get("/api/configuration/features")
            .andExpect { status { isOk() } }
    }

    @Test
    fun `CSRFトークンなしでは状態変更を拒否する`() {
        mockMvc.post("/api/asa/server/start")
            .andExpect { status { isForbidden() } }
    }
}
