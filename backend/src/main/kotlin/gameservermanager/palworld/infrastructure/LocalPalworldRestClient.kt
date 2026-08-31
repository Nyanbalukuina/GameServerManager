package gameservermanager.palworld.infrastructure

import gameservermanager.palworld.application.PalworldManagementClient
import org.springframework.stereotype.Component
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.Base64

@Component
class LocalPalworldRestClient : PalworldManagementClient {
    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build()

    override fun save(port: Int, adminPassword: String) {
        post(port, "/save", adminPassword, "")
    }

    override fun shutdown(
        port: Int,
        adminPassword: String,
        waitSeconds: Int,
        message: String,
    ) {
        val escapedMessage = message.replace("\\", "\\\\").replace("\"", "\\\"")
        val body = "{\"waittime\":$waitSeconds,\"message\":\"$escapedMessage\"}"
        post(port, "/shutdown", adminPassword, body)
    }

    private fun post(port: Int, path: String, adminPassword: String, body: String) {
        val credentials = Base64.getEncoder().encodeToString(
            "admin:$adminPassword".toByteArray(StandardCharsets.UTF_8),
        )
        val request = HttpRequest.newBuilder(
            URI.create("http://127.0.0.1:$port/v1/api$path"),
        )
            .timeout(Duration.ofSeconds(10))
            .header("Authorization", "Basic $credentials")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()
        val response = try {
            httpClient.send(request, HttpResponse.BodyHandlers.discarding())
        } catch (exception: InterruptedException) {
            Thread.currentThread().interrupt()
            throw IllegalStateException("Palworld REST API呼び出しが中断されました", exception)
        }
        check(response.statusCode() in 200..299) {
            "Palworld REST APIがHTTP ${response.statusCode()}を返しました"
        }
    }
}
