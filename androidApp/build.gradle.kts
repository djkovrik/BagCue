import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction

abstract class VerifyAndroidReleaseConfiguration : DefaultTask() {
    @get:Input abstract val adUnitId: Property<String>
    @get:Input abstract val versionCodeValue: Property<Int>
    @get:Input abstract val versionNameValue: Property<String>

    @TaskAction
    fun verify() {
        require(adUnitId.get() == "R-M-19857241-1") { "Unexpected BagCue production Yandex ad unit ID" }
        require(versionCodeValue.get() in 1..2_100_000_000) { "Invalid Google Play versionCode" }
        require(Regex("""\d+\.\d+\.\d+""").matches(versionNameValue.get())) {
            "BAGCUE_VERSION_NAME must be plain SemVer without a v prefix"
        }
    }
}

abstract class VerifyReleaseSigningInputs : DefaultTask() {
    @get:Optional @get:InputFile abstract val keystoreFile: RegularFileProperty
    @get:Optional @get:Input abstract val keystorePassword: Property<String>
    @get:Optional @get:Input abstract val keyAliasValue: Property<String>
    @get:Optional @get:Input abstract val keyPasswordValue: Property<String>

    @TaskAction
    fun verify() {
        val missing = buildList {
            if (!keystoreFile.isPresent) add("ANDROID_KEYSTORE_FILE")
            if (!keystorePassword.isPresent || keystorePassword.get().isBlank()) add("ANDROID_KEYSTORE_PASSWORD")
            if (!keyAliasValue.isPresent || keyAliasValue.get().isBlank()) add("ANDROID_KEY_ALIAS")
            if (!keyPasswordValue.isPresent || keyPasswordValue.get().isBlank()) add("ANDROID_KEY_PASSWORD")
        }
        require(missing.isEmpty()) { "Missing release signing inputs: ${missing.joinToString()}" }
        require(keystoreFile.get().asFile.isFile) { "ANDROID_KEYSTORE_FILE does not exist" }
    }
}

plugins {
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.android.application)
}

if (file("google-services.json").isFile) {
    apply(plugin = "com.google.gms.google-services")
}

val productionYandexAdUnitId = providers.gradleProperty("bagcue.productionYandexAdUnitId")
val releaseVersionCode = providers.environmentVariable("BAGCUE_VERSION_CODE")
    .map { value -> value.toInt() }
    .orElse(1)
val releaseVersionName = providers.environmentVariable("BAGCUE_VERSION_NAME").orElse("1.0.0")
val releaseKeystorePath = providers.environmentVariable("ANDROID_KEYSTORE_FILE")
val releaseKeystorePassword = providers.environmentVariable("ANDROID_KEYSTORE_PASSWORD")
val releaseKeyAlias = providers.environmentVariable("ANDROID_KEY_ALIAS")
val releaseKeyPassword = providers.environmentVariable("ANDROID_KEY_PASSWORD")

android {
    namespace = "com.sedsoftware.bagcue"
    compileSdk = 37

    defaultConfig {
        minSdk = 23
        targetSdk = 37

        applicationId = "com.sedsoftware.bagcue"
        versionCode = releaseVersionCode.get()
        versionName = releaseVersionName.get()

        buildConfigField("String", "YANDEX_AD_UNIT_ID", "\"\"")
    }

    buildFeatures {
        buildConfig = true
    }

    signingConfigs {
        create("release") {
            if (releaseKeystorePath.isPresent) {
                storeFile = File(releaseKeystorePath.get())
                storePassword = releaseKeystorePassword.orNull
                keyAlias = releaseKeyAlias.orNull
                keyPassword = releaseKeyPassword.orNull
            }
        }
    }

    buildTypes {
        getByName("release") {
            buildConfigField("String", "YANDEX_AD_UNIT_ID", "\"${productionYandexAdUnitId.get()}\"")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

dependencies {
    implementation(project(":shared:compose"))
    implementation(project(":shared:root"))
    implementation(project(":shared:data"))
    implementation(project(":shared:platform"))
    implementation(project(":shared:network"))
    implementation(libs.androidx.activityCompose)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.decompose)
    implementation(libs.mvikotlin)
    implementation(libs.mvikotlin.main)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.datetime)
    implementation(libs.yandex.mobileads)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
}

tasks.register<VerifyAndroidReleaseConfiguration>("verifyAndroidReleaseConfiguration") {
    group = "verification"
    description = "Checks BagCue release-only monetization and version invariants."
    adUnitId.set(productionYandexAdUnitId)
    versionCodeValue.set(releaseVersionCode)
    versionNameValue.set(releaseVersionName)
}

tasks.register<VerifyReleaseSigningInputs>("verifyReleaseSigningInputs") {
    group = "verification"
    description = "Fails clearly unless all upload-key inputs are present."
    if (releaseKeystorePath.isPresent) {
        keystoreFile.set(layout.file(releaseKeystorePath.map(::File)))
    }
    keystorePassword.set(releaseKeystorePassword)
    keyAliasValue.set(releaseKeyAlias)
    keyPasswordValue.set(releaseKeyPassword)
}
