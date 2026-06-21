import com.android.build.api.dsl.ApplicationExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val appVersionName = "0.1.3-pre-release"
val distributionArtifactName = "hermes-webui-v$appVersionName"

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Use the product name, not the repository name, in generated APK/AAB artifacts.
base {
    archivesName.set(distributionArtifactName)
}

extensions.configure<ApplicationExtension>("android") {
    namespace = "com.hermeswebui.android"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.hermeswebui.android"
        minSdk = 26
        targetSdk = 37
        versionCode = 4
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material)
    implementation(libs.google.material)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.webkit)
    implementation(libs.androidx.security.crypto)

    testImplementation(libs.junit)
    testImplementation(libs.truth)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

fun copyFirstExistingArtifact(candidates: List<File>, target: File) {
    val source = candidates.firstOrNull { it.exists() }
        ?: error("No artifact found. Checked: ${candidates.joinToString { it.path }}")
    target.parentFile.mkdirs()
    source.copyTo(target, overwrite = true)
    logger.lifecycle("Wrote ${target.path}")
}

tasks.register("stageGithubReleaseApk") {
    group = "distribution"
    description = "Builds the release APK and stages it as release/$distributionArtifactName.apk."
    dependsOn("assembleRelease")

    doLast {
        copyFirstExistingArtifact(
            candidates = listOf(
                layout.buildDirectory.file("outputs/apk/release/$distributionArtifactName-release.apk").get().asFile,
                layout.buildDirectory.file("outputs/apk/release/$distributionArtifactName-release-unsigned.apk").get().asFile
            ),
            target = rootProject.layout.projectDirectory.file("release/$distributionArtifactName.apk").asFile
        )
    }
}

tasks.register("stagePlayReleaseBundle") {
    group = "distribution"
    description = "Builds the release app bundle and stages it as release/$distributionArtifactName.aab."
    dependsOn("bundleRelease")

    doLast {
        copyFirstExistingArtifact(
            candidates = listOf(
                layout.buildDirectory.file("outputs/bundle/release/$distributionArtifactName-release.aab").get().asFile,
                layout.buildDirectory.file("outputs/bundle/release/app-release.aab").get().asFile
            ),
            target = rootProject.layout.projectDirectory.file("release/$distributionArtifactName.aab").asFile
        )
    }
}

tasks.register("stageReleaseArtifacts") {
    group = "distribution"
    description = "Builds and stages release APK/AAB artifacts with product/version filenames."
    dependsOn("stageGithubReleaseApk", "stagePlayReleaseBundle")
}
