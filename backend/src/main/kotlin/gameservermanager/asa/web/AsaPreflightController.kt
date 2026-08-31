package gameservermanager.asa.web

import gameservermanager.asa.application.RunAsaServerPreflight
import gameservermanager.shared.preflight.ServerPreflightReport
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

// ASA構築前の読み取り専用検証APIを公開する。
@RestController
@RequestMapping("/api/asa/preflight")
class AsaPreflightController(private val preflight: RunAsaServerPreflight) {
    @PostMapping
    fun run(@Valid @RequestBody request: ConstructAsaRequest): ServerPreflightReport {
        return preflight.execute(
            RunAsaServerPreflight.Command(
                request.installPath, request.steamCmdPath, requireNotNull(request.gamePort),
                requireNotNull(request.queryPort), requireNotNull(request.rconPort),
            ),
        )
    }
}
