package com.rk.application

import com.rk.App
import com.rk.ExtensionFeature
import com.rk.app.AppFlavour
import com.rk.feature.FeatureRegistry
import com.rk.xededitor.BuildConfig

/**
 * Application entry point for the `community` distribution.
 *
 * Registers every feature, including `ExtensionFeature` provided by the bundled `:features:extensions` module.
 */
class XedApplication : App() {
    override fun onCreate() {
        // Publish the compile-time flavour (see app/build.gradle.kts) before any shared code runs.
        AppFlavour.init("community")

        super.onCreate()

        // Register pluggable features
        FeatureRegistry.register(ExtensionFeature())

        // Initialize core features
        FeatureRegistry.initFeatures(this)
    }
}
