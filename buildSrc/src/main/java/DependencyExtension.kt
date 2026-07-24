import org.gradle.api.artifacts.dsl.DependencyHandler

private fun DependencyHandler.implementation(depName: String) {
    add("implementation", depName)
}

private fun DependencyHandler.testImplementation(depName: String) {
    add("testImplementation", depName)
}

private fun DependencyHandler.androidTestImplementation(depName: String) {
    add("androidTestImplementation", depName)
}

private fun DependencyHandler.debugImplementation(depName: String) {
    add("debugImplementation", depName)
}

private fun DependencyHandler.ksp(depName: String) {
    add("ksp", depName)
}

private fun DependencyHandler.implementationPlatform(depName: String) {
    add("implementation", platform(depName))
}

private fun DependencyHandler.androidTestImplementationPlatform(depName: String) {
    add("androidTestImplementation", platform(depName))
}

fun DependencyHandler.coreAndroidX() {
    implementation(Dependencies.coreKtx)
    implementation(Dependencies.lifecycleRuntimeKtx)
    implementation(Dependencies.activityCompose)
}

fun DependencyHandler.compose() {
    implementationPlatform(Dependencies.composeBom)
    implementation(Dependencies.composeUi)
    implementation(Dependencies.composeUiGraphics)
    implementation(Dependencies.composeUiToolingPreview)
    implementation(Dependencies.composeMaterial3)
    implementation(Dependencies.composeMaterialIconsExtended)
}

fun DependencyHandler.composeDebug() {
    debugImplementation(Dependencies.composeUiTooling)
    debugImplementation(Dependencies.composeUiTestManifest)
}

fun DependencyHandler.composeAndroidTest() {
    androidTestImplementationPlatform(Dependencies.composeBom)
    androidTestImplementation(Dependencies.composeUiTestJunit4)
}

fun DependencyHandler.hilt() {
    implementation(Dependencies.hiltAndroid)
    ksp(Dependencies.hiltCompiler)
}

fun DependencyHandler.navigationCompose() {
    implementation(Dependencies.navigationCompose)
    implementation(Dependencies.hiltNavigationCompose)
    implementation(Dependencies.lifecycleViewmodelCompose)
    implementation(Dependencies.lifecycleRuntimeCompose)
}

fun DependencyHandler.room() {
    implementation(Dependencies.roomRuntime)
    implementation(Dependencies.roomKtx)
    ksp(Dependencies.roomCompiler)
}

fun DependencyHandler.workManager() {
    implementation(Dependencies.workRuntimeKtx)
    implementation(Dependencies.hiltWork)
    ksp(Dependencies.hiltWorkCompiler)
}

fun DependencyHandler.coil() {
    implementation(Dependencies.coilCompose)
    implementation(Dependencies.coilGif)
}

fun DependencyHandler.dataStorePreferences() {
    implementation(Dependencies.datastorePreferences)
}

fun DependencyHandler.kotlinSerialization() {
    implementation(Dependencies.kotlinxSerializationJson)
}

fun DependencyHandler.jUnit() {
    testImplementation(TestDependencies.jUnit)
}

fun DependencyHandler.androidxTest() {
    androidTestImplementation(TestDependencies.testExtJUnit)
    androidTestImplementation(TestDependencies.testEspressoCore)
}
