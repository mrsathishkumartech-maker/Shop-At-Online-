pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "App-AndroidGames"

include(":core")

// Every folder under games/ that has a build.gradle.kts becomes its own app module,
// e.g. games/arrowescape -> :games:arrowescape. Add a game = add a folder.
file("games").listFiles()
    ?.filter { it.isDirectory && file("${it.path}/build.gradle.kts").exists() }
    ?.sortedBy { it.name }
    ?.forEach { include(":games:${it.name}") }
