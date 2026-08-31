package gameservermanager.shared.construction

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ConstructionProgressTrackerTests {
    @Test
    fun `工程の開始完了失敗を保持する`() {
        val tracker = ConstructionProgressTracker()
        tracker.start(
            "ASA",
            listOf(
                ConstructionProgressStepDefinition("steamcmd", "SteamCMDの準備"),
                ConstructionProgressStepDefinition("install", "ASAのダウンロード"),
            ),
        )

        tracker.running("ASA", "steamcmd")
        tracker.completed("ASA", "steamcmd", "完了しました")
        tracker.running("ASA", "install")
        tracker.failed("ASA", "install", "プロキシへ接続できません")

        val progress = tracker.get("ASA")
        assertThat(progress?.status).isEqualTo(ConstructionProgressStatus.ERROR)
        assertThat(progress?.steps).containsExactly(
            ConstructionProgressStep(
                "steamcmd", "SteamCMDの準備", ConstructionProgressStepStatus.COMPLETED, "完了しました",
            ),
            ConstructionProgressStep(
                "install", "ASAのダウンロード", ConstructionProgressStepStatus.ERROR, "プロキシへ接続できません",
            ),
        )
    }
}
