package gameservermanager.palworld.web

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank

data class RestartPalworldServerRequest(
    @field:NotBlank val installPath: String,
    @field:Min(1) @field:Max(65535) val gamePort: Int?,
    @field:Min(1) @field:Max(32) val maxPlayers: Int?,
    @field:Min(1) @field:Max(65535) val restApiPort: Int?,
    @field:NotBlank val adminPassword: String,
)
