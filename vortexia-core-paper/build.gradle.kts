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
        implementation("com.github.Vortexia:vortexia-api:v1.3.1")
    }

    compileOnly("io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT")
    implementation("dev.jorel:commandapi-paper-shade:11.1.0")
    compileOnly("dev.jorel:commandapi-annotations:11.1.0")
    annotationProcessor("dev.jorel:commandapi-annotations:11.1.0")

    compileOnly("fr.xephi:authme-core:6.0.0-SNAPSHOT")
    
    // SQLite/MySQL/HikariCP/Caffeine
    implementation("com.zaxxer:HikariCP:5.1.0")
    compileOnly("org.xerial:sqlite-jdbc:3.47.1.0")
    compileOnly("com.mysql:mysql-connector-j:9.1.0")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
    
    implementation("com.github.retrooper:packetevents-spigot:2.12.1")
}

val backendCommonPath = if (findProject(":vortexia-core-backend-common") != null) ":vortexia-core-backend-common" else ":vortexia-core:vortexia-core-backend-common"
val bungeePath = if (findProject(":vortexia-core-bungee") != null) ":vortexia-core-bungee" else ":vortexia-core:vortexia-core-bungee"
val spongePath = if (findProject(":vortexia-core-sponge") != null) ":vortexia-core-sponge" else ":vortexia-core:vortexia-core-sponge"
val velocityPath = if (findProject(":vortexia-core-velocity") != null) ":vortexia-core-velocity" else ":vortexia-core:vortexia-core-velocity"

evaluationDependsOn(backendCommonPath)
evaluationDependsOn(bungeePath)
evaluationDependsOn(spongePath)
evaluationDependsOn(velocityPath)

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

    build {
        dependsOn(shadowJar)
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
val versionNumber = project.findProperty("projectVersion") as? String ?: "0.3.0"

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
val modrinthProjectIdProp = project.findProperty("modrinthProjectID") as? String ?: "VxHFxXAM"
val hangarProjectIdProp = project.findProperty("hangarProjectID") as? String ?: "vortexia-core"
val gitChangelog = System.getenv("COMMIT_MESSAGE") ?: "No changelog provided."

hangarPublish {
    publications.register("plugin") {
        version.set(rawVersion)
        id.set(hangarProjectIdProp)
        channel.set(hangarChannel)
        changelog.set(gitChangelog)
        apiKey.set(System.getenv("HANGAR_API_TOKEN"))

        platforms {
            register(io.papermc.hangarpublishplugin.model.Platforms.PAPER) {
                jar.set(tasks.shadowJar.flatMap { it.archiveFile })
                platformVersions.set(parsedMcVersions)
            }
            register(io.papermc.hangarpublishplugin.model.Platforms.VELOCITY) {
                jar.set(project(velocityPath).tasks.named<org.gradle.api.tasks.bundling.AbstractArchiveTask>("shadowJar").flatMap { it.archiveFile })
                platformVersions.set(listOf(project.findProperty("velocityVersion") as? String ?: "3.4-3.5"))
            }
        }
    }
}

modrinth {
    token.set(System.getenv("MODRINTH_TOKEN"))
    projectId.set(modrinthProjectIdProp)
    versionNumber.set(rawVersion)
    versionName.set(finalVersionName)
    versionType.set(modrinthVersionType)
    changelog.set(gitChangelog)
    uploadFile.set(tasks.shadowJar.flatMap { it.archiveFile })
    additionalFiles.set(listOf(
        project(bungeePath).tasks.named<org.gradle.api.tasks.bundling.AbstractArchiveTask>("shadowJar").flatMap { it.archiveFile },
        project(spongePath).tasks.named<org.gradle.api.tasks.bundling.AbstractArchiveTask>("shadowJar").flatMap { it.archiveFile },
        project(velocityPath).tasks.named<org.gradle.api.tasks.bundling.AbstractArchiveTask>("shadowJar").flatMap { it.archiveFile }
    ))
    gameVersions.set(parsedMcVersions)
    loaders.set(listOf("paper", "bungeecord", "sponge", "velocity"))
}
