plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}
android {
    namespace = "org.hayya.salah"
    compileSdk = 37
    buildToolsVersion = "36.0.0"
    defaultConfig {
        applicationId = "org.hayya.salah"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        javaCompileOptions.annotationProcessorOptions.arguments["room.schemaLocation"] = "$projectDir/schemas"
        minSdk = 26
        targetSdk = 37
        versionCode = providers.environmentVariable("QA_VERSION_CODE").orElse("1").get().toInt()
        versionName = "0.1.0-internal"
    }
    buildTypes {
        debug { applicationIdSuffix = ".dev"; versionNameSuffix = "-dev" }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
        create("qa") {
            initWith(getByName("release"))
            applicationIdSuffix = ".qa"
            versionNameSuffix = "-qa"
            isDebuggable = false
            signingConfig = null // Signed separately by the trusted QA delivery job.
        }
    }
    bundle { language { enableSplit = false } } // All four languages stay available offline.
    buildFeatures { compose = true }
    sourceSets.getByName("main").assets.srcDir(rootProject.file("licenses"))
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    lint { abortOnError = true; warningsAsErrors = true }
}
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
dependencies {
    implementation(project(":core"))
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.tooling)
    implementation(libs.activity.compose)
    implementation(libs.datastore)
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)
    androidTestImplementation(libs.room.testing)
    implementation(libs.lifecycle.runtime.compose)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.test)
    androidTestImplementation(libs.compose.accessibility.test)
    androidTestImplementation(libs.android.test.runner)
    androidTestImplementation(libs.android.test.junit)
}
