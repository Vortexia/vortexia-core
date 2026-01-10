plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.vortexia"
val branch = System.getenv("GITHUB_REF_NAME") ?: "local"
version = if (branch == "master" || branch == "main") "0.0.1" else if (branch == "development") "0.0.1-DEV" else "0.0.1-${branch.uppercase()}"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release.set(21)
    }

    processResources {
        inputs.property("version", project.version)
        filesMatching("plugin.yml") {
            expand("version" to project.version)
        }
    }
}
