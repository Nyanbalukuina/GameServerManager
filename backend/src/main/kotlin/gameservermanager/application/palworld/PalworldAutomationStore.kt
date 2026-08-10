package gameservermanager.application.palworld

import gameservermanager.domain.palworld.PalworldAutomationRuntimeState
import gameservermanager.domain.palworld.PalworldAutomationSettings

interface PalworldAutomationStore {
    fun loadSettings(): PalworldAutomationSettings?

    fun saveSettings(settings: PalworldAutomationSettings)

    fun loadRuntime(): PalworldAutomationRuntimeState

    fun saveRuntime(runtime: PalworldAutomationRuntimeState)
}
