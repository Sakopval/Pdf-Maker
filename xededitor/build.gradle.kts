#!/usr/bin/env kotlin

import java.util.Properties




plugins {
    alias(libs.plugins.android.baselineprofile)
    id ("com.android.library")
    alias(libs.plugins.ktfmt)
    id ("androidx.room3")
    id ("com.google.dagger.hilt.android")
    id ("com.google.devtools.ksp")
}

val distributionDimension = "distribution"
val communityFlavor = "community"
val playStoreFlavor = "playstore"

android {
    namespace = "com.rk.application"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
//        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // versioning
        vectorDrawables { useSupportLibrary = true }
    }
    room3 {
        schemaDirectory ("$projectDir/schemas")
    }

    flavorDimensions += distributionDimension

    productFlavors {
        // Default, fully featured distribution (GitHub, F-Droid, IzzyOnDroid).
        create(communityFlavor) {
            dimension = distributionDimension
            isDefault = true
            // Published at runtime as com.rk.app.AppFlavour via AppFlavour.init(BuildConfig.FLAVOUR).
            buildConfigField("String", "FLAVOUR", "\"$communityFlavor\"")
        }

        // Google Play distribution. Identical to community except that it does not bundle
        // the `:features:extensions` module.
//        create(playStoreFlavor) {
//            dimension = distributionDimension
//            // using this id because I don't want to go through 14 day testing again
//            buildConfigField("String", "FLAVOUR", "\"$playStoreFlavor\"")
//        }
    }

    buildFeatures {
        viewBinding = true
        compose = false
        buildConfig = true
    }


    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    packaging {
        jniLibs { useLegacyPackaging = true }
        resources.excludes += "plugin.properties"
    }

    signingConfigs {
        create("release") {
            val isGitHubActions = System.getenv("GITHUB_ACTIONS") == "true"

            val propertiesFilePath =
                if (isGitHubActions) {
                    "/tmp/signing.properties"
                } else {
                    "/home/rohit/Android/xed-signing/signing.properties"
                }

            val propertiesFile = File(propertiesFilePath)
            if (propertiesFile.exists()) {
                val properties = Properties()
                properties.load(propertiesFile.inputStream())
                keyAlias = properties["keyAlias"] as String?
                keyPassword = properties["keyPassword"] as String?
                storeFile =
                    if (isGitHubActions) {
                        File("/tmp/xed.keystore")
                    } else {
                        (properties["storeFile"] as String?)?.let { File(it) }
                    }

                storePassword = properties["storePassword"] as String?
            } else {
                println("Signing properties file not found at $propertiesFilePath")
            }
        }
        getByName("debug") {
            storeFile = file(layout.buildDirectory.dir("../testkey.keystore"))
            storePassword = "testkey"
            keyAlias = "testkey"
            keyPassword = "testkey"
        }
    }

    buildTypes {
        release {
            // Community keeps minification off; the playstore flavor enables it below.
            isMinifyEnabled = false
            isShrinkResources = false

            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")

//            signingConfig = signingConfigs.getByName("release")
        }



        create("benchmark") {
            initWith(buildTypes.getByName("release"))
            matchingFallbacks += listOf("release")
        }
    }
}

// Play Store releases are minified; community releases stay unminified so extension work
// play store requires r8 to be enabled
androidComponents {
    beforeVariants(selector().withFlavor(distributionDimension, playStoreFlavor).withBuildType("release")) {
        it.isMinifyEnabled = true
    }
}

kotlin { jvmToolchain(21) }

dependencies {
    androidTestImplementation(libs.androidx.runner)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.uiautomator)

    implementation(libs.androidx.profileinstaller)
    coreLibraryDesugaring(libs.desugar)
            implementation (libs.androidx.sqlite.framework)
            implementation (libs.material)
            implementation (libs.androidx.viewpager2)
            implementation (libs.androidx.core.splashscreen)
            implementation (libs.androidx.documentfile)
            implementation (libs.commons.compress)
            implementation (libs.xz)
            implementation (libs.androidx.dynamicanimation)
            testImplementation (libs.junit)
            implementation (libs.androidx.room.runtime)
            ksp (libs.androidx.room.compiler)
            implementation (libs.hilt.android)
            ksp (libs.hilt.compiler)
            implementation (libs.androidx.lifecycle.livedata)

    baselineProfile(project(":baselineprofile"))
    implementation(project(":xedcore:main"))
    implementation(project(path = ":core"))
    implementation(project(":xedcore:resources"))
    implementation(libs.androidx.appcompat)
//    implementation(project(":features:runner"))
    "communityImplementation"(project(":features:extensions"))

}
