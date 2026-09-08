import dev.detekt.gradle.Detekt
import dev.detekt.gradle.extensions.FailOnSeverity
import java.math.RoundingMode
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction

abstract class PrintKoverLineCoverage : DefaultTask() {
    @get:InputFile
    abstract val reportFile: RegularFileProperty

    @TaskAction
    fun printCoverage() {
        val report = reportFile.get().asFile
        require(report.isFile) { "Missing Kover XML report: ${report.absolutePath}" }
        val counter = Regex("""<counter type="LINE" missed="(\d+)" covered="(\d+)"/>""")
            .findAll(report.readText())
            .lastOrNull()
            ?: error("Kover XML has no total LINE counter")
        val missed = counter.groupValues[1].toLong()
        val covered = counter.groupValues[2].toLong()
        val total = missed + covered
        require(total > 0) { "Kover filtered line total is zero" }
        val percent = covered.toBigDecimal()
            .multiply(100.toBigDecimal())
            .divide(total.toBigDecimal(), 2, RoundingMode.HALF_UP)
        println(percent.setScale(2, RoundingMode.HALF_UP).toPlainString())
    }
}

plugins {
    base
    alias(libs.plugins.kotlin.multiplatform).apply(false)
    alias(libs.plugins.compose.compiler).apply(false)
    alias(libs.plugins.compose.multiplatform).apply(false)
    alias(libs.plugins.android.application).apply(false)
    alias(libs.plugins.android.library).apply(false)
    alias(libs.plugins.android.kmp.library).apply(false)
    alias(libs.plugins.kotlinx.serialization).apply(false)
    alias(libs.plugins.sqldelight).apply(false)
    alias(libs.plugins.paparazzi).apply(false)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kover)
    alias(libs.plugins.google.services).apply(false)
    alias(libs.plugins.kotlin.cocoapods).apply(false)
}

detekt {
    toolVersion = libs.versions.detekt.get()
    source.setFrom(
        fileTree(rootDir) {
            include("**/*.kt", "**/*.kts")
            exclude(
                ".gradle/**",
                ".vibe/**",
                "**/build/**",
                "**/generated/**",
                "**/src/*Test/**",
                "**/src/test/**",
            )
        },
    )
    config.setFrom(file("detekt/base-config.yml"))
    buildUponDefaultConfig = false
    parallel = true
    ignoreFailures = false
    failOnSeverity = FailOnSeverity.Warning
    basePath.set(rootDir)
}

tasks.withType<Detekt>().configureEach {
    exclude("**/build/**", "**/generated/**")
    reports {
        checkstyle.required.set(true)
        html.required.set(true)
        sarif.required.set(true)
    }
}

dependencies {
    kover(project(":shared:domain"))
    kover(project(":shared:component:catalog"))
    kover(project(":shared:component:templates"))
    kover(project(":shared:component:session"))
    kover(project(":shared:component:history"))
    kover(project(":shared:component:settings"))
    kover(project(":shared:root"))
}

val bagCueCoverageMinimum = providers.gradleProperty("bagcue.kover.minimum").map(String::toInt).orElse(0)

kover {
    reports {
        filters {
            includes {
                packages(
                    "com.sedsoftware.bagcue.domain",
                    "com.sedsoftware.bagcue.catalog",
                    "com.sedsoftware.bagcue.templates",
                    "com.sedsoftware.bagcue.session",
                    "com.sedsoftware.bagcue.history",
                    "com.sedsoftware.bagcue.settings",
                    "com.sedsoftware.bagcue.root",
                )
            }
            excludes {
                classes("*Preview*", "*Factory*", "*Module*", "*.BuildConfig", "*.Res", "*.Res$*")
            }
        }
        verify {
            rule("BagCue domain and production Decompose line coverage") {
                minBound(bagCueCoverageMinimum)
            }
        }
    }
}

tasks.register<PrintKoverLineCoverage>("printKoverLineCoverage") {
    group = "verification"
    description = "Prints only the filtered total line coverage percentage."
    dependsOn("koverXmlReport")
    reportFile.set(layout.buildDirectory.file("reports/kover/report.xml"))
}

tasks.named("check") {
    dependsOn("detekt", "koverVerify")
}
