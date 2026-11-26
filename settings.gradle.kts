pluginManagement {
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
