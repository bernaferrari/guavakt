plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":guavakt"))
    testImplementation(kotlin("test"))
    // Collision-free dev.guavakt packages allow compile-time differential tests.
    testImplementation("com.google.guava:guava:33.6.0-jre")
}

tasks.test {
    useJUnitPlatform()
    // A substantial deterministic default keeps every regular parity run useful. Larger values
    // are intentionally opt-in for prolonged direct-Guava fuzzing.
    systemProperty("guavakt.fuzz.seeds", providers.gradleProperty("fuzzSeeds").orElse("12").get())
    systemProperty("guavakt.fuzz.cases", providers.gradleProperty("fuzzCases").orElse("96").get())
}
