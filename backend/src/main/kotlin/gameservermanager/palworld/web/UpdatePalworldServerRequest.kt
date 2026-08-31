package gameservermanager.palworld.web

import jakarta.validation.constraints.NotBlank

data class UpdatePalworldServerRequest(
    @field:NotBlank val steamCmdPath: String,
    @field:NotBlank val installPath: String,
)
