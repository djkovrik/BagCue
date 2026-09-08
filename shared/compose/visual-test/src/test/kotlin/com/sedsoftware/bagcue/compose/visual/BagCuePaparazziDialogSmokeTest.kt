package com.sedsoftware.bagcue.compose.visual

import app.cash.paparazzi.Paparazzi
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

@Ignore("Focused dialog harness smoke; generated matrix owns committed baselines")
class BagCuePaparazziDialogSmokeTest {
    private val preview = discoverBagCuePreviews(listOf("com.sedsoftware.bagcue.compose.preview")).first {
        it.toString().contains("SCREEN_004_ConsentRequired") && it.previewInfo.name == "light_compact_en-100"
    }

    @get:Rule val paparazzi: Paparazzi = BagCuePaparazziPreviewRule.createFor(preview, 37)

    @Test fun rendersProductionConsentDialog() {
        paparazzi.configureComposeResources()
        paparazzi.snapshot(name = "SCREEN-004_consent-required_light_compact_en-100_dialog-smoke") {
            BagCuePreviewContent(preview.previewInfo) { preview() }
        }
    }
}
