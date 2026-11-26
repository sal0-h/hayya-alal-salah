pluginManagement {
    resolutionStrategy {
        eachPlugin {
            // The JVM marker has a POM only. Resolve its verified implementation
            // directly so a missing marker JAR probe cannot disable Maven Central.
            if (requested.id.id == "org.jetbrains.kotlin.jvm") {
                useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:${requested.version}")
            }
        }
    }
    repositories {
        google { content { excludeGroupByRegex("org\\.jetbrains\\.kotlin.*") } }
        // Kotlin marker POMs differ on the plugin portal. Use the verified Central bytes.
        mavenCentral()
        gradlePluginPortal { content { excludeGroupByRegex("org\\.jetbrains\\.kotlin.*") } }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "HayyaAlalSalah"
include(":app", ":core", ":praytimes", ":benchmark")
