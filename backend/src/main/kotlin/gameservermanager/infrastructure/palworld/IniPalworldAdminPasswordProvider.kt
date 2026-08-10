package gameservermanager.infrastructure.palworld

import gameservermanager.application.palworld.PalworldAdminPasswordProvider
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

@Component
class IniPalworldAdminPasswordProvider : PalworldAdminPasswordProvider {
    override fun read(installPath: String): String {
        val settingsPath = Path.of(installPath)
            .resolve("Pal/Saved/Config/WindowsServer/PalWorldSettings.ini")
        require(Files.isRegularFile(settingsPath)) {
            "PalWorldSettings.iniが見つかりません"
        }
        val content = Files.readString(settingsPath, StandardCharsets.UTF_8)
        val encoded = requireNotNull(ADMIN_PASSWORD.find(content)?.groupValues?.get(1)) {
            "Palworld管理者パスワードが設定されていません"
        }
        return unescape(encoded)
    }

    private fun unescape(value: String): String {
        val result = StringBuilder()
        var escaped = false
        value.forEach { character ->
            if (escaped) {
                result.append(character)
                escaped = false
            } else if (character == '\\') {
                escaped = true
            } else {
                result.append(character)
            }
        }
        if (escaped) {
            result.append('\\')
        }
        return result.toString()
    }

    companion object {
        private val ADMIN_PASSWORD = Regex("(?:^|[,\\(])AdminPassword=\"((?:\\\\.|[^\"])*)\"")
    }
}
