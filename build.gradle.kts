plugins {
    java
    id("com.gradleup.shadow") version "9.4.1"
}

group = "me.alikuxac.vortexia"
val refName = System.getenv("GITHUB_REF_NAME") ?: "local"
val refType = System.getenv("GITHUB_REF_TYPE") ?: "branch"

version = if (refType == "tag") {
    refName.replaceFirst("v", "")
} else if (refName == "master" || refName == "main") {
    "0.3.0"
} else if (refName == "development") {
    "0.3.0-DEV"
} else {
    "0.3.0-${refName.uppercase()}"
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.codemc.io/repository/maven-public/")
    maven("https://repo.codemc.io/repository/maven-releases/")
    maven("https://repo.codemc.io/repository/maven-snapshots/")
    maven("https://repo.panda-lang.org/releases")
    maven("https://jitpack.io")
    maven("https://mvn.wesjd.net/")
}

dependencies {
    implementation("me.alikuxac.vortexia:vortexia-api:1.2.0")
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
    compileOnly("fr.xephi:authme-core:6.0.0-SNAPSHOT")
    implementation("com.github.retrooper:packetevents-spigot:2.12.1")
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
        val projectVersion = project.version
        inputs.property("version", projectVersion)
        filesMatching("**/paper-plugin.yml") {
            expand("version" to projectVersion)
        }
    }

    shadowJar {
        archiveClassifier.set("")
        relocate("dev.jorel.commandapi", "me.alikuxac.vortexia.core.libs.commandapi")
        relocate("com.zaxxer.hikari", "me.alikuxac.vortexia.core.libs.hikari")
        relocate("com.github.retrooper.packetevents", "me.alikuxac.vortexia.core.libs.packetevents")
        relocate("io.github.retrooper.packetevents", "me.alikuxac.vortexia.core.libs.packetevents")
        mergeServiceFiles()
    }

    jar {
        enabled = false
    }

    build {
        dependsOn(shadowJar)
    }
}
