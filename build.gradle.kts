plugins {
    kotlin("multiplatform") version "2.4.10" apply false
    kotlin("jvm") version "2.4.10" apply false
    id("org.jetbrains.dokka") version "2.2.0"
    id("com.vanniktech.maven.publish") version "0.32.0" apply false
    id("org.jetbrains.kotlinx.binary-compatibility-validator") version "0.18.1"
    `maven-publish`
}

allprojects {
    group = "dev.guavakt"
    version = "0.1.0"

    repositories {
        mavenCentral()
        google()
    }
}

subprojects {
    plugins.withId("org.jetbrains.kotlin.multiplatform") {
        apply(plugin = "org.jetbrains.dokka")
        apply(plugin = "com.vanniktech.maven.publish")
        extensions.configure<PublishingExtension>("publishing") {
            repositories {
                mavenLocal()
                maven {
                    name = "GitHubPackages"
                    url = uri("https://maven.pkg.github.com/bernaferrari/guavakt")
                    credentials {
                        username = findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
                        password = findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
                    }
                }
            }
        }
        // Configure POM for each software component after KMP registers publications
        afterEvaluate {
            extensions.findByType(PublishingExtension::class.java)?.publications?.withType(MavenPublication::class.java)?.configureEach {
                pom {
                    name.set(project.name)
                    description.set("Kotlin-first Kotlin Multiplatform primitives — ${project.name}")
                    url.set("https://github.com/bernaferrari/guavakt")
                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }
                    developers {
                        developer {
                            id.set("bernaferrari")
                            name.set("Bernardo Ferrari")
                        }
                    }
                    scm {
                        url.set("https://github.com/bernaferrari/guavakt")
                        connection.set("scm:git:https://github.com/bernaferrari/guavakt.git")
                        developerConnection.set("scm:git:ssh://git@github.com/bernaferrari/guavakt.git")
                    }
                }
            }
        }
    }
}

dependencies {
    subprojects
        .filter { it.name != "guavakt-parity" }
        .forEach { add("dokka", project(it.path)) }
}

apiValidation {
    ignoredProjects.add("guavakt-parity")
    @OptIn(kotlinx.validation.ExperimentalBCVApi::class)
    klib.enabled = true
}
