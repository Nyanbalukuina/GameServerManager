package gameservermanager.asa.web

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

// ASA実構築APIの入力値を保持する。
data class ConstructAsaRequest(
    @field:NotBlank @field:Size(max = 100) val serverName: String = "",
    @field:NotBlank @field:Size(max = 500) val installPath: String = "",
    @field:NotBlank @field:Size(max = 500) val steamCmdPath: String = "",
    @field:NotBlank val map: String = "TheIsland_WP",
    @field:NotNull @field:Min(1) @field:Max(65534) val gamePort: Int? = null,
    @field:NotNull @field:Min(1) @field:Max(65535) val queryPort: Int? = null,
    @field:NotNull @field:Min(1) @field:Max(65535) val rconPort: Int? = null,
    @field:NotNull @field:Min(1) @field:Max(70) val maxPlayers: Int? = null,
    @field:Size(max = 64) val serverPassword: String = "",
    @field:NotBlank @field:Size(min = 8, max = 64) val adminPassword: String = "",
    val allowLocalSubnet: Boolean = true,
    val allowTailscale: Boolean = true,
    @field:Size(max = 2000) val customRemoteAddresses: String = "",
    val allowAnyRemoteAddress: Boolean = false,
    @field:Min(0) @field:Max(100) val xpMultiplier: Double = 1.0,
    @field:Min(0) @field:Max(100) val tamingSpeedMultiplier: Double = 1.0,
    @field:Min(0) @field:Max(100) val harvestAmountMultiplier: Double = 1.0,
    val pveEnabled: Boolean = true,
    @field:Min(0) @field:Max(100) val eggHatchSpeedMultiplier: Double = 1.0,
    @field:Min(0) @field:Max(100) val babyMatureSpeedMultiplier: Double = 1.0,
    val useSingleplayerSettings: Boolean = false,
)
