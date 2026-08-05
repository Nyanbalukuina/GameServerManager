package com.github.gameservermanager.web.api.server

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class NewServerRequest(
    @field:NotBlank(message = "サーバー名を入力してください")
    @field:Size(max = 100, message = "サーバー名は100文字以内で入力してください")
    val serverName: String = "",

    @field:NotBlank(message = "インストール先を入力してください")
    @field:Size(max = 500, message = "インストール先は500文字以内で入力してください")
    val installPath: String = "",

    @field:NotBlank(message = "SteamCMDの保存先を入力してください")
    @field:Size(max = 500, message = "SteamCMDの保存先は500文字以内で入力してください")
    val steamCmdPath: String = "",

    @field:NotNull(message = "ゲームポートを入力してください")
    @field:Min(value = 1, message = "ゲームポートは1以上で入力してください")
    @field:Max(value = 65535, message = "ゲームポートは65535以下で入力してください")
    val gamePort: Int? = null,

    @field:NotNull(message = "RCONポートを入力してください")
    @field:Min(value = 1, message = "RCONポートは1以上で入力してください")
    @field:Max(value = 65535, message = "RCONポートは65535以下で入力してください")
    val rconPort: Int? = null,

    @field:NotNull(message = "最大プレイヤー数を入力してください")
    @field:Min(value = 1, message = "最大プレイヤー数は1人以上で入力してください")
    @field:Max(value = 32, message = "最大プレイヤー数は32人以下で入力してください")
    val maxPlayers: Int? = null,

    @field:Size(max = 64, message = "サーバーパスワードは64文字以内で入力してください")
    val serverPassword: String = "",

    @field:NotBlank(message = "管理者パスワードを入力してください")
    @field:Size(min = 8, max = 64, message = "管理者パスワードは8文字以上64文字以内で入力してください")
    val adminPassword: String = "",
)

