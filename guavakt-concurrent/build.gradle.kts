@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

val kotlinxCoroutinesVersion = project.property("kotlinxCoroutinesVersion") as String

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
            api(project(":guavakt-base"))
            api("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxCoroutinesVersion")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$kotlinxCoroutinesVersion")
        }
    }
}
