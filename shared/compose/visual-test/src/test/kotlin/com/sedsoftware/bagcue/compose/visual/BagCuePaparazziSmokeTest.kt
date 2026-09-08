package com.sedsoftware.bagcue.compose.visual

import app.cash.paparazzi.Paparazzi
import org.junit.Rule
import org.junit.Ignore
import org.junit.Test

@Ignore("Focused harness smoke; generated matrix owns the committed baselines")
class BagCuePaparazziSmokeTest {
    private val preview = discoverBagCuePreviews(listOf("com.sedsoftware.bagcue.compose.preview")).first {
        it.toString().contains("SCREEN_001_FirstRunStarter") && it.previewInfo.name == "light_compact_en-100"
    }

    @get:Rule val paparazzi: Paparazzi = BagCuePaparazziPreviewRule.createFor(preview, 37)

    @Test fun rendersProductionTodayFirstRun() {
        paparazzi.configureComposeResources()
        paparazzi.snapshot(name = "SCREEN-001_first-run_light_compact_en-100_smoke") {
            BagCuePreviewContent(preview.previewInfo) { preview() }
        }
    }
}
