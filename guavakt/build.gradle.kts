@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()
    js {
        browser()
        nodejs()
    }
    wasmJs {
        browser()
        nodejs()
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    macosX64()
    macosArm64()
    linuxX64()
    mingwX64()

    sourceSets {
        commonMain.dependencies {
            api(project(":guavakt-annotations"))
            api(project(":guavakt-base"))
            api(project(":guavakt-primitives"))
            api(project(":guavakt-math"))
            api(project(":guavakt-collect"))
            api(project(":guavakt-escape"))
            api(project(":guavakt-hash"))
            api(project(":guavakt-graph"))
            api(project(":guavakt-cache"))
            api(project(":guavakt-io"))
            api(project(":guavakt-net"))
            api(project(":guavakt-eventbus"))
            api(project(":guavakt-concurrent"))
            api(project(":guavakt-reflect"))
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
