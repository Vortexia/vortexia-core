plugins {
    `java-library`
}

dependencies {
    if (findProject(":vortexia-api") != null) {
        api(project(":vortexia-api"))
    } else {
        api("com.github.Vortexia:vortexia-api:v1.3.1")
    }

    // Storage & Caching Libraries
    api("com.zaxxer:HikariCP:5.1.0")
    compileOnly("org.xerial:sqlite-jdbc:3.47.1.0")
    compileOnly("com.mysql:mysql-connector-j:9.1.0")
    api("com.github.ben-manes.caffeine:caffeine:3.1.8")
}
