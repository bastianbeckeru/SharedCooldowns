plugins {
    java
    id("com.gradleup.shadow") version "9.4.3"
}

group = "com.sharedcooldowns"
version = "1.0.1"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    compileOnly("com.github.Zrips:CMI-API:9.8.6.4")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(26))
}

tasks {
    shadowJar {
        archiveClassifier.set("")
        minimize()
    }

    build {
        dependsOn(shadowJar)
    }

    processResources {
        val props = mapOf("version" to version)
        inputs.properties(props)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}


