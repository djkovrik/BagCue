package com.sedsoftware.bagcue.compose.visual

import android.content.res.Configuration.UI_MODE_NIGHT_MASK
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.HtmlReportWriter
import app.cash.paparazzi.Paparazzi
import app.cash.paparazzi.Snapshot
import app.cash.paparazzi.SnapshotHandler
import app.cash.paparazzi.SnapshotVerifier
import app.cash.paparazzi.TestName
import app.cash.paparazzi.detectEnvironment
import com.android.ide.common.rendering.api.SessionParams
import com.android.resources.Density
import com.android.resources.NightMode
import com.android.resources.ScreenRatio
import com.android.resources.ScreenRound
import com.android.resources.ScreenSize
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.setResourceReaderAndroidContext
import sergio.sastre.composable.preview.scanner.android.AndroidComposablePreviewScanner
import sergio.sastre.composable.preview.scanner.android.AndroidPreviewInfo
import sergio.sastre.composable.preview.scanner.android.device.DevicePreviewInfoParser
import sergio.sastre.composable.preview.scanner.android.screenshotid.AndroidPreviewScreenshotIdBuilder
import sergio.sastre.composable.preview.scanner.core.preview.ComposablePreview
import sergio.sastre.composable.preview.scanner.core.scanner.config.classpath.Classpath
import java.io.File
import kotlin.math.ceil

internal fun discoverBagCuePreviews(packages: List<String>): List<ComposablePreview<AndroidPreviewInfo>> {
    val root = checkNotNull(System.getProperty("bagcue.preview.classpath.root"))
    val target = File(root, "classes/kotlin/android/main")
    val known = File(target, "com/sedsoftware/bagcue/compose/preview/BagCuePreviewMatrixKt.class")
    check(known.isFile) { "Missing production preview bytecode: ${known.absolutePath}" }
    return AndroidComposablePreviewScanner()
        .setTargetSourceSet(
            Classpath("classes/kotlin/android/main", root),
            packageTreesOfCrossModuleCustomPreviews = packages,
        )
        .scanPackageTrees(include = packages, exclude = emptyList())
        .includePrivatePreviews()
        .getPreviews()
        .sortedBy { it.toString() }
}

internal object BagCuePaparazziPreviewRule {
    fun createFor(preview: ComposablePreview<AndroidPreviewInfo>, compileSdkVersion: Int): Paparazzi = Paparazzi(
        environment = detectEnvironment().copy(compileSdkVersion = compileSdkVersion),
        deviceConfig = deviceConfig(preview.previewInfo),
        renderingMode = if (preview.previewInfo.widthDp > 0 && preview.previewInfo.heightDp > 0) SessionParams.RenderingMode.FULL_EXPAND else SessionParams.RenderingMode.SHRINK,
        supportsRtl = true,
        showSystemUi = preview.previewInfo.showSystemUi,
        maxPercentDifference = 0.0,
        snapshotHandler = snapshotHandler(),
    )
}

@Composable
internal fun BagCuePreviewContent(info: AndroidPreviewInfo, content: @Composable () -> Unit) {
    val resize = when {
        info.widthDp > 0 && info.heightDp > 0 -> Modifier.size(info.widthDp.dp, info.heightDp.dp)
        info.widthDp > 0 -> Modifier.width(info.widthDp.dp)
        info.heightDp > 0 -> Modifier.height(info.heightDp.dp)
        else -> Modifier
    }
    Box(resize) {
        if (info.showBackground) {
            val color = if (info.backgroundColor == 0L) Color.White else Color(info.backgroundColor)
            Box(Modifier.background(color)) { content() }
        } else content()
    }
}

@OptIn(ExperimentalResourceApi::class)
internal fun Paparazzi.configureComposeResources() = setResourceReaderAndroidContext(context)

internal fun writeCoverageInventory(previews: List<ComposablePreview<AndroidPreviewInfo>>, outputPath: String?, packagePrefix: String) {
    if (outputPath.isNullOrBlank()) return
    val entries = previews.map { preview ->
        val info = preview.previewInfo
        val snapshot = AndroidPreviewScreenshotIdBuilder(preview).doNotIgnoreMethodParametersType().encodeUnsafeCharacters().build().replace(packagePrefix, "")
        val screen = Regex("SCREEN[_-](\\d{3})", RegexOption.IGNORE_CASE).find(snapshot)?.groupValues?.get(1)?.let { "SCREEN-$it" } ?: "UNKNOWN"
        val theme = if (info.uiMode and UI_MODE_NIGHT_MASK == UI_MODE_NIGHT_YES) "dark" else "light"
        """{"screen":"$screen","statePreview":${preview.toString().json()},"theme":"$theme","locale":"${info.locale.ifBlank { "en" }}","fontScale":${info.fontScale},"widthDp":${info.widthDp},"heightDp":${info.heightDp},"device":${info.device.json()},"snapshotId":${snapshot.json()}}"""
    }
    File(outputPath).apply { parentFile.mkdirs() }.writeText("{\n  \"schemaVersion\": \"1.0\",\n  \"previewCount\": ${entries.size},\n  \"entries\": [\n    ${entries.joinToString(",\n    ")}\n  ]\n}\n")
}

private fun String.json(): String = "\"" + replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\""

private fun deviceConfig(info: AndroidPreviewInfo): DeviceConfig {
    val parsed = DevicePreviewInfoParser.parse(info.device)
    val base = if (parsed == null) DeviceConfig.PIXEL_5 else DeviceConfig(
        screenHeight = parsed.dimensions.height.toInt(), screenWidth = parsed.dimensions.width.toInt(),
        xdpi = parsed.densityDpi, ydpi = parsed.densityDpi,
        ratio = ScreenRatio.valueOf(parsed.screenRatio.name), size = ScreenSize.valueOf(parsed.screenSize.name),
        density = Density(parsed.densityDpi), screenRound = ScreenRound.valueOf(parsed.shape.name),
    )
    val factor = base.density.dpiValue / 160f
    return base.copy(
        screenWidth = if (info.widthDp > 0) ceil(info.widthDp * factor).toInt() else base.screenWidth,
        screenHeight = if (info.heightDp > 0) ceil(info.heightDp * factor).toInt() else base.screenHeight,
        locale = info.locale.ifBlank { "en" }, fontScale = info.fontScale,
        nightMode = if (info.uiMode and UI_MODE_NIGHT_MASK == UI_MODE_NIGHT_YES) NightMode.NIGHT else NightMode.NOTNIGHT,
    )
}

private val stableName = TestName("Paparazzi", "Preview", "Test")
private fun snapshotHandler(): SnapshotHandler = if (System.getProperty("paparazzi.test.verify")?.toBoolean() == true) StableVerifier() else StableReport()
private class StableVerifier : SnapshotHandler {
    private val delegate = SnapshotVerifier(0.0)
    override fun newFrameHandler(snapshot: Snapshot, frameCount: Int, fps: Int) = delegate.newFrameHandler(snapshot.copy(testName = stableName), frameCount, fps)
    override fun close() = delegate.close()
}
private class StableReport : SnapshotHandler {
    private val delegate = HtmlReportWriter(maxPercentDifference = 0.0)
    override fun newFrameHandler(snapshot: Snapshot, frameCount: Int, fps: Int) = delegate.newFrameHandler(snapshot.copy(testName = stableName), frameCount, fps)
    override fun close() = delegate.close()
}
