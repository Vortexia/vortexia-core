plugins {
    id("com.gradleup.shadow")
    `maven-publish`
    id("io.papermc.hangar-publish-plugin") version "0.1.4"
    id("com.modrinth.minotaur") version "2.9.0"
}

dependencies {
    implementation(project(":vortexia-core-backend-common"))

    if (findProject(":vortexia-api") != null) {
        implementation(project(":vortexia-api"))
    } else {
        implementation("com.github.Vortexia:vortexia-api:v1.3.2")
    }

    compileOnly("io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT")
    implementation("dev.jorel:commandapi-paper-shade:11.1.0")
    compileOnly("dev.jorel:commandapi-annotations:11.1.0")
    annotationProcessor("dev.jorel:commandapi-annotations:11.1.0")

    compileOnly("fr.xephi:authme-core:6.0.0-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.6")
    
    // SQLite/MySQL/HikariCP/Caffeine
    implementation("com.zaxxer:HikariCP:5.1.0")
    compileOnly("org.xerial:sqlite-jdbc:3.47.1.0")
    compileOnly("com.mysql:mysql-connector-j:9.1.0")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
    
    implementation("com.github.retrooper:packetevents-spigot:2.12.1")
}

tasks {
    processResources {
        val projectVersion = project.version
        inputs.property("version", projectVersion)
        filesMatching("**/paper-plugin.yml") {
            expand("version" to projectVersion)
        }
        filesMatching("**/plugin.yml") {
            expand("version" to projectVersion)
        }
    }

    shadowJar {
        archiveClassifier.set("")
        relocate("dev.jorel.commandapi", "me.alikuxac.vortexia.core.libs.commandapi")
        relocate("com.github.retrooper.packetevents", "me.alikuxac.vortexia.core.libs.packetevents")
        relocate("io.github.retrooper.packetevents", "me.alikuxac.vortexia.core.libs.packetevents")
        relocate("com.zaxxer.hikari", "me.alikuxac.vortexia.core.libs.hikari")
        mergeServiceFiles()
    }


    jar {
        enabled = false
    }

    val generateResourcePack by registering(Zip::class) {
        group = "build"
        description = "Packages the Vortexia Resource Pack into a distributable zip"
        archiveFileName.set("vortexia-resourcepack.zip")
        destinationDirectory.set(layout.buildDirectory.dir("distributions"))
        from("src/main/resources/resourcepack")
    }

    build {
        dependsOn(shadowJar, generateResourcePack)
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifact(tasks.shadowJar)
        }
    }
}

val rawVersion = project.version.toString()
val refType = System.getenv("GITHUB_REF_TYPE") ?: "branch"
val refName = System.getenv("GITHUB_REF_NAME") ?: "local"
val runNumber = System.getenv("GITHUB_RUN_NUMBER") ?: "0"
val versionNumber = project.findProperty("projectVersion") as? String ?: "0.4.0"

val isMasterRelease = refType == "tag"
val isBetaRelease = isMasterRelease && refName.contains("beta", ignoreCase = true)

val hangarChannel = when {
    isBetaRelease -> "Snapshot"
    isMasterRelease -> "Release"
    else -> "Snapshot"
}

val modrinthVersionType = when {
    isBetaRelease -> "beta"
    isMasterRelease -> "release"
    else -> "alpha"
}

val finalVersionName = when {
    isBetaRelease -> refName // e.g., v0.3.0-beta.1
    isMasterRelease -> refName // e.g., v0.3.0
    else -> "v$versionNumber-alpha (build #$runNumber)"
}

val mcVersionsProp = project.findProperty("mcVersions") as? String ?: "1.21"
val parsedMcVersions = mcVersionsProp.split(",").map { it.trim() }

modrinth {
    token.set(System.getenv("MODRINTH_TOKEN"))
    projectId.set(project.findProperty("modrinthProjectID") as? String ?: "VxHFxXAM")
    versionNumber.set(rawVersion)
    versionName.set(finalVersionName)
    versionType.set(modrinthVersionType)
    changelog.set("See full release notes on GitHub: https://github.com/Vortexia/vortexia-core/releases/tag/$refName")
    uploadFile.set(tasks.shadowJar.flatMap { it.archiveFile })
    gameVersions.set(parsedMcVersions)
    loaders.set(listOf("paper", "spigot", "folia"))
}

hangarPublish {
    publications.register("hangar") {
        version.set(rawVersion)
        id.set(project.findProperty("hangarProjectID") as? String ?: "vortexia-core")
        channel.set(hangarChannel)
        changelog.set("See full release notes on GitHub: https://github.com/Vortexia/vortexia-core/releases/tag/$refName")
        apiKey.set(System.getenv("HANGAR_API_TOKEN"))

        platforms {
            register(io.papermc.hangarpublishplugin.model.Platforms.PAPER) {
                jar.set(tasks.shadowJar.flatMap { it.archiveFile })
                platformVersions.set(parsedMcVersions)
            }
        }
    }
}
