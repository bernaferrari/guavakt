rootProject.name = "guavakt"

pluginManagement {
    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
    }
    plugins {
        kotlin("multiplatform") version "2.4.10"
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
    }
}

include(
    ":guavakt-annotations",
    ":guavakt-base",
    ":guavakt-primitives",
    ":guavakt-math",
    ":guavakt-collect",
    ":guavakt-escape",
    ":guavakt-hash",
    ":guavakt-graph",
    ":guavakt-cache",
    ":guavakt-io",
    ":guavakt-net",
    ":guavakt-concurrent",
    ":guavakt",
    ":guavakt-parity",
)
