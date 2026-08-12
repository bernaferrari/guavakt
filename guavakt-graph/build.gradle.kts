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
            api(project(":guavakt-base"))
            api(project(":guavakt-collect"))
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        jvmTest.dependencies {
            implementation("org.openjdk.jmh:jmh-core:1.37")
        }
    }
}

dependencies {
    add("jvmTestAnnotationProcessor", "org.openjdk.jmh:jmh-generator-annprocess:1.37")
}

val jmhResult = layout.buildDirectory.file("jmh-graph.json")
val jvmTestTask = tasks.named<Test>("jvmTest")

tasks.register<JavaExec>("jmhGraph") {
    group = "benchmark"
    description = "Runs reproducible JMH graph workloads; not part of the correctness test suite."
    dependsOn("jvmTestClasses")
    classpath = jvmTestTask.get().classpath
    mainClass.set("org.openjdk.jmh.Main")
    jvmArgs("-Djmh.ignoreLock=true")
    inputs.property("jmhWarmupIterations", providers.gradleProperty("jmhWarmupIterations").orElse("3"))
    inputs.property("jmhMeasurementIterations", providers.gradleProperty("jmhMeasurementIterations").orElse("5"))
    inputs.property("jmhForks", providers.gradleProperty("jmhForks").orElse("2"))
    outputs.file(jmhResult)
    args(
        "com.bernaferrari.guavakt.graph.GraphBenchmark",
        "-wi", providers.gradleProperty("jmhWarmupIterations").orElse("3").get(),
        "-i", providers.gradleProperty("jmhMeasurementIterations").orElse("5").get(),
        "-f", providers.gradleProperty("jmhForks").orElse("2").get(),
        "-bm", "thrpt",
        "-tu", "ms",
        "-rf", "json",
        "-rff", jmhResult.get().asFile.absolutePath,
    )
}
