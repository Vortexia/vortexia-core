plugins {
    id("com.gradleup.shadow")
    `maven-publish`
}

dependencies {
    compileOnly("org.spongepowered:spongeapi:10.0.0")
}

tasks {
    processResources {
        val projectVersion = project.version
        inputs.property("version", projectVersion)
        filesMatching("**/sponge_plugins.json") {
            expand("version" to projectVersion)
        }
    }

    shadowJar {
        archiveClassifier.set("")
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
