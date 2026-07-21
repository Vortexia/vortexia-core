plugins {
    id("com.gradleup.shadow") version "9.4.1" apply false
}


val refName = System.getenv("GITHUB_REF_NAME") ?: "local"
val refType = System.getenv("GITHUB_REF_TYPE") ?: "branch"
val runNumber = System.getenv("GITHUB_RUN_NUMBER")
val versionNumber = project.findProperty("projectVersion") as String? ?: "0.3.0"

val calculatedVersion = if (refType == "tag") {
    refName.replaceFirst("v", "")
} else if (refName == "master" || refName == "main") {
    versionNumber
} else if (refName == "development") {
    if (runNumber != null) "$versionNumber-alpha-b.$runNumber" else "$versionNumber-alpha"
} else {
    if (runNumber != null) "$versionNumber-${refName.uppercase()}-$runNumber" else "$versionNumber-${refName.uppercase()}"
}

subprojects {
    apply(plugin = "java")

    group = "me.alikuxac.vortexia"
    version = calculatedVersion

    repositories {
        mavenLocal()
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.spongepowered.org/repository/maven-public/")
        maven("https://repo.codemc.io/repository/maven-public/")
        maven("https://repo.codemc.io/repository/maven-releases/")
        maven("https://repo.codemc.io/repository/maven-snapshots/")
        maven("https://repo.panda-lang.org/releases")
        maven("https://jitpack.io")
        maven("https://mvn.wesjd.net/")
    }

    configure<org.gradle.api.plugins.JavaPluginExtension> {
        toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(21)
        options.compilerArgs.add("-Xlint:all")
    }
}
