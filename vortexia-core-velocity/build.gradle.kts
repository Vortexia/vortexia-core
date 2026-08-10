plugins {
    id("com.gradleup.shadow")
    `maven-publish`
}

dependencies {
    implementation(project(":vortexia-core-proxy-common"))
    compileOnly("com.velocitypowered:velocity-api:3.3.0-SNAPSHOT")
}

tasks {
    processResources {
        val projectVersion = project.version
        inputs.property("version", projectVersion)
        filesMatching("**/velocity-plugin.json") {
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
