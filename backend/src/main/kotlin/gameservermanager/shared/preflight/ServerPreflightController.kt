package gameservermanager.shared.preflight

import gameservermanager.shared.preflight.RunServerPreflight
import gameservermanager.shared.preflight.ServerPreflightReport
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

// このクラスをHTTPリクエストを受け付けるREST APIとしてSpring Bootへ登録する。
@RestController
// このControllerが受け付けるAPIの共通URLを指定する。
@RequestMapping("/api/server-construction-preflight")
class ServerPreflightController(
    // Spring Bootから事前検証のビジネスロジックを受け取る。
    private val runServerPreflight: RunServerPreflight,
) {
    // POST /api/server-construction-preflight を受け付ける。
    @PostMapping
    // JSONのリクエストボディをServerPreflightRequestへ変換し、入力値を検証する。
    fun run(@Valid @RequestBody request: ServerPreflightRequest): ServerPreflightReport {
        // API用の入力データをユースケース用のCommandへ詰め替え、事前検証を実行する。
        val command = RunServerPreflight.Command(
            installPath = request.installPath,
            steamCmdPath = request.steamCmdPath,
            // バリデーション通過後のため、nullではないことを確認してIntとして渡す。
            gamePort = requireNotNull(request.gamePort),
            rconPort = requireNotNull(request.rconPort),
        )

        return runServerPreflight.execute(command)
    }
}
