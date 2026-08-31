package gameservermanager.shared.steamcmd

import gameservermanager.shared.steamcmd.DownloadedSteamCmdArchive
import gameservermanager.shared.steamcmd.SteamCmdArchiveDownloader
import org.springframework.stereotype.Component
import java.io.IOException
import java.net.InetSocketAddress
import java.net.ProxySelector
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.Duration

@Component
class OfficialSteamCmdArchiveDownloader : SteamCmdArchiveDownloader {
    private val httpClient = createHttpClient()

    override fun download(destination: Path): DownloadedSteamCmdArchive {
        val request = HttpRequest.newBuilder(OFFICIAL_DOWNLOAD_URI)
            .timeout(Duration.ofMinutes(5))
            .header("User-Agent", "GameServerManager/0.0.1")
            .GET()
            .build()
        val response = try {
            httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream())
        } catch (exception: InterruptedException) {
            Thread.currentThread().interrupt()
            throw IOException("SteamCMDのダウンロードが中断されました", exception)
        }

        require(response.statusCode() == 200) {
            "SteamCMDのダウンロードに失敗しました: HTTP ${response.statusCode()}"
        }

        val contentLength = response.headers().firstValueAsLong("Content-Length").orElse(-1)
        require(contentLength <= MAXIMUM_DOWNLOAD_BYTES) {
            "SteamCMDのダウンロードサイズが上限を超えています"
        }

        Files.createDirectories(requireNotNull(destination.parent))
        var downloadedBytes = 0L
        response.body().use { input ->
            Files.newOutputStream(
                destination,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE,
            ).use { output ->
                val buffer = ByteArray(BUFFER_SIZE)
                while (true) {
                    val readBytes = input.read(buffer)
                    if (readBytes < 0) {
                        break
                    }
                    downloadedBytes += readBytes
                    if (downloadedBytes > MAXIMUM_DOWNLOAD_BYTES) {
                        throw IOException("SteamCMDのダウンロードサイズが上限を超えています")
                    }
                    output.write(buffer, 0, readBytes)
                }
            }
        }

        require(downloadedBytes > 0) {
            "SteamCMDのダウンロード結果が空です"
        }

        return DownloadedSteamCmdArchive(
            path = destination,
            sourceUrl = OFFICIAL_DOWNLOAD_URI.toString(),
            sizeBytes = downloadedBytes,
        )
    }

    private fun createHttpClient(): HttpClient {
        val builder = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .followRedirects(HttpClient.Redirect.NORMAL)
        val proxyUrl = System.getenv("HTTPS_PROXY") ?: System.getenv("https_proxy")

        if (!proxyUrl.isNullOrBlank()) {
            val proxyUri = URI.create(proxyUrl)
            val proxyPort = if (proxyUri.port >= 0) proxyUri.port else 80
            builder.proxy(ProxySelector.of(InetSocketAddress(proxyUri.host, proxyPort)))
        }

        return builder.build()
    }

    companion object {
        const val OFFICIAL_DOWNLOAD_URL =
            "https://steamcdn-a.akamaihd.net/client/installer/steamcmd.zip"
        private val OFFICIAL_DOWNLOAD_URI = URI.create(OFFICIAL_DOWNLOAD_URL)
        private const val MAXIMUM_DOWNLOAD_BYTES = 100L * 1024L * 1024L
        private const val BUFFER_SIZE = 8192
    }
}
