// Developed by alikuxac - Project Vortexia
plugins {
    id("com.gradleup.shadow")
    id("com.modrinth.minotaur") version "2.9.0"
    id("io.papermc.hangar-publish-plugin") version "0.1.4"
}

val paperPath = if (findProject(":vortexia-core-paper") != null) ":vortexia-core-paper" else ":vortexia-core:vortexia-core-paper"
val bungeePath = if (findProject(":vortexia-core-bungee") != null) ":vortexia-core-bungee" else ":vortexia-core:vortexia-core-bungee"
val spongePath = if (findProject(":vortexia-core-sponge") != null) ":vortexia-core-sponge" else ":vortexia-core:vortexia-core-sponge"
val velocityPath = if (findProject(":vortexia-core-velocity") != null) ":vortexia-core-velocity" else ":vortexia-core:vortexia-core-velocity"

evaluationDependsOn(paperPath)
evaluationDependsOn(bungeePath)
evaluationDependsOn(spongePath)
evaluationDependsOn(velocityPath)

tasks {
    shadowJar {
        archiveBaseName.set("VortexiaCore-Universal")
        archiveClassifier.set("")

        val platformProjects = listOf(paperPath, bungeePath, spongePath, velocityPath)
        val buildTasks = platformProjects.map { path ->
            project(path).tasks.named("shadowJar")
        }
        dependsOn(buildTasks)

        platformProjects.forEach { path ->
            val task = project(path).tasks.named<org.gradle.jvm.tasks.Jar>("shadowJar").get()
            from(zipTree(task.archiveFile))
        }

        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    jar {
        enabled = false
    }

    build {
        dependsOn(shadowJar)
    }
}

val rawVersion = project.version.toString()
val refType = System.getenv("GITHUB_REF_TYPE") ?: "branch"
val refName = System.getenv("GITHUB_REF_NAME") ?: "local"
val runNumber = System.getenv("GITHUB_RUN_NUMBER") ?: "0"
val projectVerStr = project.findProperty("projectVersion") as? String ?: "0.3.0"

val isMasterRelease = refType == "tag"
val isBetaRelease = isMasterRelease && refName.contains("beta", ignoreCase = true)

val modrinthVersionType = when {
    isBetaRelease -> "beta"
    isMasterRelease -> "release"
    else -> "alpha"
}

val finalVersionName = when {
    isBetaRelease -> refName
    isMasterRelease -> refName
    else -> "v$projectVerStr-alpha (build #$runNumber)"
}

val mcVersionsProp = project.findProperty("mcVersions") as? String ?: "1.21"
val parsedMcVersions = mcVersionsProp.split(",").map { it.trim() }
val modrinthProjectIdProp = project.findProperty("modrinthProjectID") as? String ?: "VxHFxXAM"

val githubReleaseUrl = "https://github.com/Vortexia/vortexia-core/releases/tag/$refName"
val changelogText = "See full release notes on GitHub: $githubReleaseUrl"

modrinth {
    token.set(System.getenv("MODRINTH_TOKEN"))
    projectId.set(modrinthProjectIdProp)
    versionNumber.set(rawVersion)
    versionName.set(finalVersionName)
    versionType.set(modrinthVersionType)
    changelog.set(changelogText)
    uploadFile.set(tasks.shadowJar.flatMap { it.archiveFile })
    gameVersions.set(parsedMcVersions)
    loaders.set(listOf("paper", "spigot", "folia", "bungeecord", "velocity", "sponge"))
}

val hangarProjectIdProp = project.findProperty("hangarProjectID") as? String ?: "vortexia-core"
val hangarChannel = when {
    isBetaRelease -> "Snapshot"
    isMasterRelease -> "Release"
    else -> "Snapshot"
}

hangarPublish {
    publications.register("hangar") {
        version.set(rawVersion)
        id.set(hangarProjectIdProp)
        channel.set(hangarChannel)
        changelog.set(changelogText)
        apiKey.set(System.getenv("HANGAR_API_TOKEN"))

        platforms {
            register(io.papermc.hangarpublishplugin.model.Platforms.PAPER) {
                jar.set(tasks.shadowJar.flatMap { it.archiveFile })
                platformVersions.set(parsedMcVersions)
            }
            register(io.papermc.hangarpublishplugin.model.Platforms.VELOCITY) {
                jar.set(tasks.shadowJar.flatMap { it.archiveFile })
                platformVersions.set(listOf(project.findProperty("velocityVersion") as? String ?: "3.4-3.5"))
            }
        }
    }
}
