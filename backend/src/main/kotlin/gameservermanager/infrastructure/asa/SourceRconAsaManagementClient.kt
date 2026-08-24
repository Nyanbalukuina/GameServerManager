package gameservermanager.infrastructure.asa

import gameservermanager.application.asa.AsaManagementClient
import org.springframework.stereotype.Component
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets

// Source RCONプロトコルでlocalhost上のASAへ管理コマンドを送信する。
@Component
class SourceRconAsaManagementClient : AsaManagementClient {
    override fun execute(port: Int, password: String, command: String): String {
        require(port in 1..65535) { "RCONポートが不正です" }
        require(password.isNotBlank()) { "管理者パスワードを入力してください" }
        require(command.isNotBlank()) { "RCONコマンドを入力してください" }

        Socket().use { socket ->
            socket.connect(InetSocketAddress("127.0.0.1", port), TIMEOUT_MILLIS)
            socket.soTimeout = TIMEOUT_MILLIS
            val input = DataInputStream(socket.getInputStream())
            val output = DataOutputStream(socket.getOutputStream())

            writePacket(output, AUTH_REQUEST_ID, AUTH_PACKET_TYPE, password)
            authenticate(input)
            writePacket(output, COMMAND_REQUEST_ID, COMMAND_PACKET_TYPE, command)
            val response = readPacket(input)
            check(response.requestId == COMMAND_REQUEST_ID) { "RCON応答IDが一致しません" }
            return response.body
        }
    }

    // 認証応答が届くまで読み取り、拒否された場合は失敗させる。
    private fun authenticate(input: DataInputStream) {
        repeat(2) {
            val response = readPacket(input)
            if (response.type == AUTH_RESPONSE_TYPE) {
                check(response.requestId != AUTH_FAILURE_ID) { "RCON認証に失敗しました" }
                check(response.requestId == AUTH_REQUEST_ID) { "RCON認証応答IDが一致しません" }
                return
            }
        }
        error("RCON認証応答を確認できませんでした")
    }

    // little-endian形式のRCONパケットを書き込む。
    private fun writePacket(output: DataOutputStream, requestId: Int, type: Int, body: String) {
        val bodyBytes = body.toByteArray(StandardCharsets.UTF_8)
        val payload = ByteBuffer.allocate(10 + bodyBytes.size).order(ByteOrder.LITTLE_ENDIAN)
            .putInt(requestId)
            .putInt(type)
            .put(bodyBytes)
            .put(0)
            .put(0)
            .array()
        val size = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(payload.size).array()
        output.write(size)
        output.write(payload)
        output.flush()
    }

    // RCONパケットを読み取り、応答情報へ変換する。
    private fun readPacket(input: DataInputStream): Packet {
        val sizeBytes = ByteArray(4)
        input.readFully(sizeBytes)
        val size = ByteBuffer.wrap(sizeBytes).order(ByteOrder.LITTLE_ENDIAN).int
        require(size in MIN_PACKET_SIZE..MAX_PACKET_SIZE) { "RCON応答サイズが不正です" }

        val payload = ByteArray(size)
        input.readFully(payload)
        val buffer = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN)
        val requestId = buffer.int
        val type = buffer.int
        val bodyBytes = ByteArray(size - MIN_PACKET_SIZE)
        buffer.get(bodyBytes)
        check(buffer.get().toInt() == 0 && buffer.get().toInt() == 0) { "RCON応答終端が不正です" }
        return Packet(requestId, type, String(bodyBytes, StandardCharsets.UTF_8))
    }

    private data class Packet(val requestId: Int, val type: Int, val body: String)

    companion object {
        private const val AUTH_REQUEST_ID = 1
        private const val COMMAND_REQUEST_ID = 2
        private const val AUTH_PACKET_TYPE = 3
        private const val COMMAND_PACKET_TYPE = 2
        private const val AUTH_RESPONSE_TYPE = 2
        private const val AUTH_FAILURE_ID = -1
        private const val MIN_PACKET_SIZE = 10
        private const val MAX_PACKET_SIZE = 4 * 1024 * 1024
        private const val TIMEOUT_MILLIS = 5_000
    }
}
