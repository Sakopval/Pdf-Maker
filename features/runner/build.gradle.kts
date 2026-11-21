import org.gradle.kotlin.dsl.dependencies

plugins {
    id ("com.android.library")
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ktfmt)
}

android {
    namespace = "com.rk.runner"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
//        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        viewBinding = true
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlin { jvmToolchain(21) }
}

dependencies {
    implementation(project(":xedcore:main"))
    implementation(project(":xedcore:components"))
    implementation(project(":xedcore:resources"))

    // Editor dependencies for code runner settings and markdown rendering
    implementation(libs.sorax.editor)
    implementation(libs.sorax.editor.lsp)

    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.utilcode)
    implementation(libs.okhttp)
    implementation(libs.nanohttpd)
    implementation(libs.androidx.browser)
//    implementation(libs.gson)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.material.icons.core)
}
