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
            api(project(":guavakt-collect"))
            api("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxCoroutinesVersion")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$kotlinxCoroutinesVersion")
        }
        jvmTest.dependencies {
            implementation("org.openjdk.jmh:jmh-core:1.37")
        }
    }
}

dependencies {
    add("jvmTestAnnotationProcessor", "org.openjdk.jmh:jmh-generator-annprocess:1.37")
}

val jmhResult = layout.buildDirectory.file("jmh-coroutine-cache.json")
val jvmTestTask = tasks.named<Test>("jvmTest")

tasks.register<JavaExec>("jmhCoroutineCache") {
    group = "benchmark"
    description = "Runs reproducible JMH cache workloads; not part of the correctness test suite."
    dependsOn("jvmTestClasses")
    classpath = jvmTestTask.get().classpath
    mainClass.set("org.openjdk.jmh.Main")
    // Separate modules have separate result files, so simultaneous explicit benchmark tasks do
    // not share mutable state. JMH's global temp-dir lock would otherwise reject that useful run.
    jvmArgs("-Djmh.ignoreLock=true")
    inputs.property("jmhWarmupIterations", providers.gradleProperty("jmhWarmupIterations").orElse("3"))
    inputs.property("jmhMeasurementIterations", providers.gradleProperty("jmhMeasurementIterations").orElse("5"))
    inputs.property("jmhForks", providers.gradleProperty("jmhForks").orElse("2"))
    outputs.file(jmhResult)
    args(
        "dev.guavakt.cache.CoroutineLoadingCacheBenchmark",
        "-wi", providers.gradleProperty("jmhWarmupIterations").orElse("3").get(),
        "-i", providers.gradleProperty("jmhMeasurementIterations").orElse("5").get(),
        "-f", providers.gradleProperty("jmhForks").orElse("2").get(),
        "-bm", "thrpt",
        "-tu", "ms",
        "-rf", "json",
        "-rff", jmhResult.get().asFile.absolutePath,
    )
}
