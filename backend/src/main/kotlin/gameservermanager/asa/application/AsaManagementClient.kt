package gameservermanager.asa.application

// ASAへRCON管理コマンドを送信する。
interface AsaManagementClient {
    fun execute(port: Int, password: String, command: String): String
}
