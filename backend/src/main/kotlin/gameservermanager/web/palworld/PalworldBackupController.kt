package gameservermanager.web.palworld

import gameservermanager.application.palworld.BackupPalworldServer
import gameservermanager.domain.palworld.PalworldBackupReport
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/palworld/backups")
class PalworldBackupController(
    private val backupPalworldServer: BackupPalworldServer,
) {
    @PostMapping
    fun create(@Valid @RequestBody request: BackupPalworldRequest): PalworldBackupReport {
        return backupPalworldServer.execute(BackupPalworldServer.Command(request.installPath))
    }
}
