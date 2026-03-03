plugins {
    java
    id("com.gradleup.shadow") version "8.3.5"
}

group = "me.alikuxac.vortexia"
val refName = System.getenv("GITHUB_REF_NAME") ?: "local"
val refType = System.getenv("GITHUB_REF_TYPE") ?: "branch"

version = if (refType == "tag") {
    refName.replaceFirst("v", "")
} else if (refName == "master" || refName == "main") {
    "0.0.1"
} else if (refName == "development") {
    "0.0.1-DEV"
} else {
    "0.0.1-${refName.uppercase()}"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.codemc.org/repository/maven-public/")
    maven("https://repo.panda-lang.org/releases")
    maven("https://jitpack.io")
    maven("https://repo.codemc.io/repository/maven-public/") // For AuthMe
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT")
    implementation("dev.jorel:commandapi-paper-shade:11.1.0")
    compileOnly("dev.jorel:commandapi-annotations:11.1.0")
    annotationProcessor("dev.jorel:commandapi-annotations:11.1.0")
    
    // Storage dependencies
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.xerial:sqlite-jdbc:3.47.1.0")
    implementation("com.mysql:mysql-connector-j:9.1.0")
    
    // Cache dependency
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")

    // Auth integration
    compileOnly("fr.xephi:authme:5.6.0-SNAPSHOT")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release.set(21)
        options.compilerArgs.add("-Xlint:all")
    }

    processResources {
        inputs.property("version", project.version)
        filesMatching("**/paper-plugin.yml") {
            expand("version" to project.version)
        }
    }

    shadowJar {
        archiveClassifier.set("")
        relocate("dev.jorel.commandapi", "me.alikuxac.vortexia.core.libs.commandapi")
        relocate("com.zaxxer.hikari", "me.alikuxac.vortexia.core.libs.hikari")
        mergeServiceFiles()
    }

    jar {
        enabled = false
    }

    build {
        dependsOn(shadowJar)
    }
}
