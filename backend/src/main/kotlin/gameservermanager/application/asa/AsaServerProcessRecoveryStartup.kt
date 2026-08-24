package gameservermanager.application.asa

import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

// GSM起動完了時に既存ASAプロセスの再接続を試みる。
@Component
class AsaServerProcessRecoveryStartup(private val recovery: RecoverAsaServerProcess) {
    @EventListener(ApplicationReadyEvent::class)
    fun recover() {
        runCatching { recovery.execute() }
    }
}
