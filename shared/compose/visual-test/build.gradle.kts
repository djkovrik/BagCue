import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.testing.Test
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

abstract class GenerateBagCuePreviewTestsTask : DefaultTask() {
    @get:Input abstract val previewPackages: ListProperty<String>
    @get:Input abstract val testPackageName: Property<String>
    @get:Input abstract val testClassName: Property<String>
    @get:Input abstract val compileSdkVersion: Property<Int>
    @get:OutputDirectory abstract val outputDirectory: DirectoryProperty

    @TaskAction fun generate() {
        val packageName = testPackageName.get()
        val className = testClassName.get()
        val outputFile = outputDirectory.file("${packageName.replace('.', '/')}/$className.kt").get().asFile
        val packageLiterals = previewPackages.get().joinToString(", ") { "\"$it\"" }
        val packagePrefix = previewPackages.get().first()
        outputFile.parentFile.mkdirs()
        outputFile.writeText(
            """
            package $packageName

            import app.cash.paparazzi.Paparazzi
            import com.sedsoftware.bagcue.compose.visual.*
            import org.junit.Assert.assertEquals
            import org.junit.Rule
            import org.junit.Test
            import org.junit.runner.RunWith
            import org.junit.runners.Parameterized
            import sergio.sastre.composable.preview.scanner.android.AndroidPreviewInfo
            import sergio.sastre.composable.preview.scanner.android.screenshotid.AndroidPreviewScreenshotIdBuilder
            import sergio.sastre.composable.preview.scanner.core.preview.ComposablePreview

            @RunWith(Parameterized::class)
            class $className(private val preview: ComposablePreview<AndroidPreviewInfo>) {
                companion object {
                    private val cachedPreviews by lazy { discoverBagCuePreviews(listOf($packageLiterals)) }
                    @JvmStatic @Parameterized.Parameters(name = "{0}")
                    fun previews(): List<ComposablePreview<AndroidPreviewInfo>> = cachedPreviews.also {
                        check(it.size == 122) { "Expected 122 BagCue previews, found ${'$'}{it.size}" }
                        writeCoverageInventory(it, System.getProperty("bagcue.preview.coverage.file"), "$packagePrefix.")
                    }
                }

                @get:Rule val paparazzi: Paparazzi = BagCuePaparazziPreviewRule.createFor(preview, ${compileSdkVersion.get()})

                @Test fun snapshot() {
                    paparazzi.configureComposeResources()
                    val id = AndroidPreviewScreenshotIdBuilder(preview)
                        .doNotIgnoreMethodParametersType().encodeUnsafeCharacters().build()
                        .replace("$packagePrefix.", "")
                    paparazzi.snapshot(name = id) { BagCuePreviewContent(preview.previewInfo) { preview() } }
                }
            }

            class BagCuePreviewDiscoveryTest {
                @Test fun discoversExactRequiredPreviewMatrix() =
                    assertEquals(122, discoverBagCuePreviews(listOf($packageLiterals)).size)
            }
            """.trimIndent(),
        )
    }
}

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.paparazzi)
}

val composeProject = project(":shared:compose")
val generatedPreviewTests = layout.buildDirectory.dir("generated/source/paparazziPreviews/test/kotlin")
val composeAndroidAssets = composeProject.layout.buildDirectory.dir(
    "generated/assets/copyAndroidMainComposeResourcesToAndroidAssets",
)
val generateBagCuePreviewTests by tasks.registering(GenerateBagCuePreviewTestsTask::class) {
    group = "verification"
    previewPackages.set(listOf("com.sedsoftware.bagcue.compose.preview"))
    testPackageName.set("com.sedsoftware.bagcue.compose.visual.generated")
    testClassName.set("GeneratedBagCuePreviewPaparazziTest")
    compileSdkVersion.set(37)
    outputDirectory.set(generatedPreviewTests)
}

android {
    namespace = "com.sedsoftware.bagcue.compose.visualtest"
    compileSdk = 37
    defaultConfig { minSdk = 23 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    testOptions.unitTests.isIncludeAndroidResources = true
    sourceSets.getByName("test").kotlin.srcDir(generatedPreviewTests.get().asFile)
    // The Android-KMP project dependency exposes production bytecode but its
    // generated Compose assets are not merged into a conventional test host.
    // Register the producer output as main variant assets because Paparazzi's
    // projectAssetDirs follows variant-local/module assets, not unit-test assets.
    sourceSets.getByName("main").assets.srcDir(composeAndroidAssets.get().asFile)
}

kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }

dependencies {
    // Paparazzi builds its resource resolver from the Android main variant.
    // Keep the production UI on that graph so transitive Compose/Material
    // dialog styles are present as well as classes on the test runtime.
    implementation(project(":shared:compose"))
    testImplementation(libs.compose.runtime)
    testImplementation(libs.compose.ui)
    testImplementation(libs.compose.foundation)
    testImplementation(libs.compose.resources)
    testImplementation(libs.test.junit4)
    testImplementation(libs.test.composable.preview.scanner.android)
}

tasks.matching { it.name == "mergeDebugAssets" }.configureEach {
    dependsOn(":shared:compose:copyAndroidMainComposeResourcesToAndroidAssets")
}

tasks.matching {
    it.name == "compileDebugUnitTestKotlin" || it.name == "compileReleaseUnitTestKotlin" ||
        it.name == "testDebugUnitTest" || it.name == "testReleaseUnitTest" ||
        it.name.startsWith("recordPaparazzi") || it.name.startsWith("verifyPaparazzi")
}.configureEach {
    dependsOn(generateBagCuePreviewTests)
    dependsOn(":shared:compose:compileAndroidMain")
    dependsOn(":shared:compose:copyAndroidMainComposeResourcesToAndroidAssets")
}

tasks.withType<Test>().configureEach {
    reports.html.required.set(false)
    jvmArgs("-Xmx4g")
    workingDir(composeProject.projectDir)
    systemProperty("bagcue.preview.classpath.root", composeProject.layout.buildDirectory.get().asFile.absolutePath)
    systemProperty("bagcue.preview.coverage.file",
         layout.buildDirectory.file("reports/paparazzi/preview-coverage.json").get().asFile.absolutePath)
}
