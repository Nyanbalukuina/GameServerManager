package gameservermanager.web.asa

import jakarta.validation.constraints.NotBlank

// ASAインストールAPIから受け取る入力値を保持する。
data class InstallAsaRequest(
    // 共通のSteamCMDが保存されているフォルダーを受け取る。
    @field:NotBlank(message = "SteamCMDの保存先を入力してください") val steamCmdPath: String,

    // ASA専用サーバーをインストールするフォルダーを受け取る。
    @field:NotBlank(message = "ASAインストール先を入力してください") val installPath: String,
)