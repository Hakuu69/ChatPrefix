plugins {
    id("java-library")
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

group = "com.Hakuuu"
// Update this version here, and it will apply to your plugin.yml automatically
version = "2.3.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://repo.lucko.me/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.12.2")
    compileOnly("net.luckperms:api:5.4")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

tasks.processResources {
    // This part handles the "Search and Replace" for the version tag
    val props = mapOf("version" to project.version)
    inputs.properties(props)
    filteringCharset = "UTF-8"

    filesMatching("plugin.yml") {
        expand(props)
    }

    from("src/main/resources") {
        include("plugin.yml")
        include("config.yml")
    }
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}