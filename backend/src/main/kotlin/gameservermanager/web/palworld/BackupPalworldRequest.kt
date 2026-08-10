package gameservermanager.web.palworld

import jakarta.validation.constraints.NotBlank

data class BackupPalworldRequest(
    @field:NotBlank(message = "インストール先を入力してください")
    val installPath: String,
)
