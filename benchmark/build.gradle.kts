plugins { alias(libs.plugins.android.test) }
android {
    namespace = "org.hayya.salah.benchmarktests"
    compileSdk = 37
    buildToolsVersion = "36.0.0"
    targetProjectPath = ":app"
    experimentalProperties["android.experimental.self-instrumenting"] = true
    defaultConfig { minSdk = 28; targetSdk = 37; testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" }
    buildTypes {
        create("performance") { isDebuggable = true; signingConfig = signingConfigs.getByName("debug") }
        create("profilegen") { isDebuggable = true; signingConfig = signingConfigs.getByName("debug") }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
androidComponents { beforeVariants(selector().withBuildType("debug")) { it.enable = false } }
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
dependencies {
    implementation(libs.benchmark.macro)
    implementation(libs.android.test.runner)
    implementation(libs.android.test.junit)
}
