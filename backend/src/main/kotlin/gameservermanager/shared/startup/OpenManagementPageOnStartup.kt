package gameservermanager.shared.startup

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(
    prefix = "game-server-manager",
    name = ["open-browser"],
    havingValue = "true",
)
class OpenManagementPageOnStartup(
    private val launcher: ManagementPageLauncher,
    @Value("\${server.port:8080}") private val port: Int,
) {
    @EventListener(ApplicationReadyEvent::class)
    fun open() {
        runCatching { launcher.open("http://localhost:$port") }
    }
}
