package gameservermanager.asa.web

import gameservermanager.asa.application.ManageAsaGameplaySettings
import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/asa/server/gameplay-settings")
class AsaGameplaySettingsController(private val settings: ManageAsaGameplaySettings) {
    @GetMapping
    fun get() = settings.get()

    @PutMapping
    fun update(@Valid @RequestBody request: Request) = settings.update(request.toSettings())

    data class Request(
        @field:NotBlank @field:Size(max = 100) val serverName: String = "",
        @field:Min(1) @field:Max(70) val maxPlayers: Int = 20,
        val pveEnabled: Boolean = true,
        @field:DecimalMin("0.1") @field:DecimalMax("100.0") val xpMultiplier: Double = 1.0,
        @field:DecimalMin("0.1") @field:DecimalMax("100.0") val tamingSpeedMultiplier: Double = 1.0,
        @field:DecimalMin("0.1") @field:DecimalMax("100.0") val harvestAmountMultiplier: Double = 1.0,
        @field:DecimalMin("0.1") @field:DecimalMax("100.0") val eggHatchSpeedMultiplier: Double = 1.0,
        @field:DecimalMin("0.1") @field:DecimalMax("100.0") val babyMatureSpeedMultiplier: Double = 1.0,
    ) {
        fun toSettings() = ManageAsaGameplaySettings.Settings(
            serverName, maxPlayers, pveEnabled, xpMultiplier, tamingSpeedMultiplier, harvestAmountMultiplier,
            eggHatchSpeedMultiplier, babyMatureSpeedMultiplier,
        )
    }
}
