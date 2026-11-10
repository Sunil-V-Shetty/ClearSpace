plugins {
    id(Plugin.androidLibrary)
    id(Plugin.kotlinCompose)
}

android {
    namespace = "com.sunilshetty.clearspace.design"
    compileSdk = Apps.compileSdk

    defaultConfig {
        minSdk = Apps.minSdk
        testInstrumentationRunner = TestDependencies.instrumentationRunner
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":core"))
    coreAndroidX()
    compose()
    composeDebug()
    jUnit()
    androidxTest()
    composeAndroidTest()
}
