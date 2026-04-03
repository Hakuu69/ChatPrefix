plugins {
    id("java-library")
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

group = "com.Hakuuu"
version = "2.0.0"

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
    from("src/main/resources") {
        include("plugin.yml")
        include("config.yml")
    }
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}