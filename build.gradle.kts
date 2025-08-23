plugins {
    id("org.jetbrains.intellij.platform") version "2.7.2" // Gradle IntelliJ Plugin
    java
}

group = "org.genrym"
version = "1.0.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories() // Adds JetBrains repositories
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2025.1")
        bundledPlugin("Git4Idea")
    }
}

tasks {
    patchPluginXml {
        sinceBuild.set("251") // Compatible with 2025.1
        untilBuild.set("999.*")
    }

    buildSearchableOptions {
        enabled = false
    }
}
