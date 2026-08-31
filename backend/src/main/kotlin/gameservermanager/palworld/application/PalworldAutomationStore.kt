package gameservermanager.palworld.application

import gameservermanager.palworld.domain.PalworldAutomationRuntimeState
import gameservermanager.palworld.domain.PalworldAutomationSettings

interface PalworldAutomationStore {
    fun loadSettings(): PalworldAutomationSettings?

    fun saveSettings(settings: PalworldAutomationSettings)

    fun loadRuntime(): PalworldAutomationRuntimeState

    fun saveRuntime(runtime: PalworldAutomationRuntimeState)
}
