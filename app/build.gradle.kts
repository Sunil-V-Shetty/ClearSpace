plugins {
    id(Plugin.androidApplication)
    id(Plugin.kotlinCompose)
    id(Plugin.hilt)
    id(Plugin.ksp)
}

android {
    namespace = Apps.applicationId
    compileSdk = Apps.compileSdk

    defaultConfig {
        applicationId = Apps.applicationId
        minSdk = Apps.minSdk
        targetSdk = Apps.targetSdk
        versionCode = Apps.versionCode
        versionName = Apps.versionName
        testInstrumentationRunner = TestDependencies.instrumentationRunner
    }

    buildTypes {
        getByName(Debug.name) {
            isDebuggable = Debug.debuggable
            isShrinkResources = Debug.shrinkResources
            isMinifyEnabled = Debug.minifyEnabled
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        getByName(Release.name) {
            isDebuggable = Release.debuggable
            isShrinkResources = Release.shrinkResources
            isMinifyEnabled = Release.minifyEnabled
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
    }
}

dependencies {
    implementation(libs.androidx.core.splashscreen)
    coreAndroidX()
    compose()
    composeDebug()
    hilt()
    navigationCompose()
    room()
    workManager()
    coil()
    dataStorePreferences()
    jUnit()
    androidxTest()
    composeAndroidTest()
}
