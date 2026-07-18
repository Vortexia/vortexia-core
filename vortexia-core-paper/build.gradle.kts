plugins {
    id("com.gradleup.shadow")
    `maven-publish`
}

dependencies {
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
    compileOnly("com.zaxxer:HikariCP:5.1.0")
    compileOnly("org.xerial:sqlite-jdbc:3.47.1.0")
    compileOnly("com.mysql:mysql-connector-j:9.1.0")
    compileOnly("com.github.ben-manes.caffeine:caffeine:3.1.8")
    
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
