package gameservermanager.palworld.web

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank

data class PalworldAutomationRequest(
    val enabled: Boolean,
    @field:NotBlank val shutdownTime: String,
    @field:NotBlank val startupTime: String,
    @field:Min(1) @field:Max(65535) val gamePort: Int?,
    @field:Min(1) @field:Max(32) val maxPlayers: Int?,
)
