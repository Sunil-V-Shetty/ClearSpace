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
}

fun DependencyHandler.composeDebug() {
    debugImplementation(Dependencies.composeUiTooling)
    debugImplementation(Dependencies.composeUiTestManifest)
}

fun DependencyHandler.composeAndroidTest() {
    androidTestImplementationPlatform(Dependencies.composeBom)
    androidTestImplementation(Dependencies.composeUiTestJunit4)
}

fun DependencyHandler.jUnit() {
    testImplementation(TestDependencies.jUnit)
}

fun DependencyHandler.androidxTest() {
    androidTestImplementation(TestDependencies.testExtJUnit)
    androidTestImplementation(TestDependencies.testEspressoCore)
}
