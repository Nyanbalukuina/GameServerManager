package gameservermanager.web.asa

import gameservermanager.application.asa.InstallAsaServer
import gameservermanager.domain.asa.AsaInstallationReport
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

// ASA専用サーバーのインストールAPIを公開する。
@RestController
@RequestMapping("/api/asa/installations")
class AsaInstallationController(private val installAsaServer: InstallAsaServer, )
{
    // JSONで受け取った保存先をASAインストール処理へ渡す。
    @PostMapping
    fun install(@Valid @RequestBody request: InstallAsaRequest, ): AsaInstallationReport
    {
        val command = InstallAsaServer.Command(
            steamCmdPath = request.steamCmdPath,
            installPath = request.installPath,
        )

        return installAsaServer.execute(command)
    }
}
