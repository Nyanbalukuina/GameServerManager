package gameservermanager.web.asa

import jakarta.validation.constraints.Size

// ASAの安全な停止と再起動に必要な管理者パスワードを受け取る。
data class OperateAsaServerRequest(
    @field:Size(max = 64) val adminPassword: String = "",
)
