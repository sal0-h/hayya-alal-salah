plugins { alias(libs.plugins.kotlin.jvm) }
kotlin { jvmToolchain(21); compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
dependencies {
    implementation(project(":praytimes"))
    implementation(libs.adhan)
    testImplementation(libs.kotlin.test)
}

java { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
