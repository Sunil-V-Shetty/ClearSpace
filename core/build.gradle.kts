plugins {
    id(Plugin.androidLibrary)
    id(Plugin.hilt)
    id(Plugin.ksp)
}

android {
    namespace = "com.sunilshetty.clearspace.core"
    compileSdk = Apps.compileSdk

    defaultConfig {
        minSdk = Apps.minSdk
        testInstrumentationRunner = TestDependencies.instrumentationRunner
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
    }
}

dependencies {
    implementation(Dependencies.coreKtx)
    hilt()
    room()
    dataStorePreferences()
    jUnit()
    androidxTest()
}
