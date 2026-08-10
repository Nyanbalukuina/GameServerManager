package gameservermanager.web.authentication

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.nio.file.Files
import java.nio.file.Path
import java.util.Comparator

@SpringBootTest(
    properties = ["game-server-manager.storage.root=build/test-authentication-storage"],
)
@AutoConfigureMockMvc
class AuthenticationIntegrationTests @Autowired constructor(
    private val mockMvc: MockMvc,
) {
    private val storageRoot = Path.of("build/test-authentication-storage").toAbsolutePath()

    @BeforeEach
    fun prepare() {
        deleteStorage()
    }

    @AfterEach
    fun cleanup() {
        deleteStorage()
    }

    @Test
    fun `未認証では管理APIを利用できない`() {
        mockMvc.post("/api/server-construction-plans") {
            with(csrf())
            contentType = MediaType.APPLICATION_JSON
            content = "{}"
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `初回設定はリモート端末から実行できない`() {
        mockMvc.post("/api/auth/setup") {
            with(csrf())
            with { request -> request.remoteAddr = "192.168.1.20"; request }
            contentType = MediaType.APPLICATION_JSON
            content = setupRequest()
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.errors.request") { value("初回管理者設定はサーバーPC上で実行してください") }
        }
    }

    @Test
    fun `localhostで初回設定してCookieセッションへログインする`() {
        mockMvc.post("/api/auth/setup") {
            with(csrf())
            with { request -> request.remoteAddr = "127.0.0.1"; request }
            contentType = MediaType.APPLICATION_JSON
            content = setupRequest()
        }.andExpect {
            status { isNoContent() }
        }

        val credentialsPath = storageRoot.resolve("config/authentication.json")
        assertThat(credentialsPath).isRegularFile()
        assertThat(Files.readString(credentialsPath))
            .contains("{bcrypt}")
            .doesNotContain(TEST_PASSWORD)

        val loginResult = mockMvc.post("/api/auth/login") {
            with(csrf())
            param("username", "admin")
            param("password", TEST_PASSWORD)
        }.andExpect {
            status { isNoContent() }
        }.andReturn()

        mockMvc.get("/api/auth/status") {
            session = requireNotNull(loginResult.request.session as? org.springframework.mock.web.MockHttpSession)
        }.andExpect {
            status { isOk() }
            jsonPath("$.configured") { value(true) }
            jsonPath("$.authenticated") { value(true) }
            jsonPath("$.username") { value("admin") }
        }
    }

    @Test
    fun `CSRFトークンなしでは状態変更を拒否する`() {
        mockMvc.post("/api/auth/setup") {
            contentType = MediaType.APPLICATION_JSON
            content = setupRequest()
        }.andExpect {
            status { isForbidden() }
        }
    }

    private fun setupRequest(): String {
        return """
            {
              "password": "$TEST_PASSWORD",
              "passwordConfirmation": "$TEST_PASSWORD"
            }
        """.trimIndent()
    }

    private fun deleteStorage() {
        if (!Files.exists(storageRoot)) {
            return
        }
        Files.walk(storageRoot).use { paths ->
            paths.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
        }
    }

    companion object {
        private const val TEST_PASSWORD = "correct-horse-battery-staple"
    }
}
