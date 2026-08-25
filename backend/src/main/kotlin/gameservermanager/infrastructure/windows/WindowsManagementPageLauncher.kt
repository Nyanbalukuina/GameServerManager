package gameservermanager.infrastructure.windows

import gameservermanager.application.startup.ManagementPageLauncher
import org.springframework.stereotype.Component

@Component
class WindowsManagementPageLauncher : ManagementPageLauncher {
    override fun open(url: String) {
        require(url.startsWith("http://localhost:")) { "管理画面URLが不正です" }
        ProcessBuilder("rundll32.exe", "url.dll,FileProtocolHandler", url).start()
    }
}
