import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.apache.tools.ant.filters.ReplaceTokens

plugins {
    id("base")
    id("eclipse")
    id("java-library")
    id("com.gradleup.shadow") version "8.3.9" apply false
}

allprojects {
    group = "me.realized.de"
    version = "3.4.1"

    repositories {
        mavenLocal()
        mavenCentral()
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots") { name = "spigot-repo" }
        maven("https://oss.sonatype.org/content/repositories/snapshots") { name = "bungeecord-repo" }
        maven("https://jitpack.io") { name = "jitpack-repo" }
        maven("https://repo.codemc.org/repository/nms/") { name = "codemc-repo" }
        maven("https://repo.papermc.io/repository/maven-public/") { name = "paper-repo" }
    }
}

subprojects {
    apply(plugin = "eclipse")
    apply(plugin = "java-library")

    java {
        toolchain.languageVersion.set(JavaLanguageVersion.of(17))
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(17)
    }
}

project(":nms") {
    dependencies {
        compileOnly("org.spigotmc:spigot-api:1.13.2-R0.1-SNAPSHOT")
    }
}

project(":v1_8_R3") {
    dependencies {
        compileOnly("org.spigotmc:spigot:1.8.8-R0.1-SNAPSHOT")
        compileOnly(project(":nms"))
    }
}

project(":v1_12_R1") {
    dependencies {
        compileOnly("org.spigotmc:spigot:1.12.2-R0.1-SNAPSHOT")
        compileOnly(project(":nms"))
    }
}

project(":v1_14_R1") {
    dependencies {
        compileOnly("org.spigotmc:spigot:1.14.4-R0.1-SNAPSHOT")
        compileOnly(project(":nms"))
    }
}

project(":v1_15_R1") {
    dependencies {
        compileOnly("org.spigotmc:spigot:1.15.2-R0.1-SNAPSHOT")
        compileOnly(project(":nms"))
    }
}

project(":v1_16_R3") {
    dependencies {
        compileOnly("org.spigotmc:spigot:1.16.5-R0.1-SNAPSHOT")
        compileOnly(project(":nms"))
    }
}

project(":v1_17_R1") {
    dependencies {
        compileOnly("org.spigotmc:spigot:1.17.1-R0.1-SNAPSHOT")
        compileOnly(project(":nms"))
    }
}

project(":v1_18_R1") {
    dependencies {
        compileOnly("org.spigotmc:spigot:1.18.1-R0.1-SNAPSHOT")
        compileOnly(project(":nms"))
    }
}

project(":v1_18_R2") {
    dependencies {
        compileOnly("org.spigotmc:spigot:1.18.2-R0.1-SNAPSHOT")
        compileOnly(project(":nms"))
    }
}

project(":v1_19_R1") {
    dependencies {
        compileOnly("org.spigotmc:spigot:1.19.2-R0.1-SNAPSHOT")
        compileOnly(project(":nms"))
    }
}

project(":arenaregen") {
    apply(plugin = "com.gradleup.shadow")

    val shade by configurations.creating

    dependencies {
        compileOnly("org.projectlombok:lombok:1.18.22")
        annotationProcessor("org.projectlombok:lombok:1.18.22")

        compileOnly("org.spigotmc:spigot-api:1.13-R0.1-SNAPSHOT")
        compileOnly("com.github.Realizedd.Duels:duels-api:3.4.1")

        compileOnly(project(":nms"))
        compileOnly(project(":v1_8_R3"))
        compileOnly(project(":v1_12_R1"))
        compileOnly(project(":v1_14_R1"))
        compileOnly(project(":v1_15_R1"))
        compileOnly(project(":v1_16_R3"))
        compileOnly(project(":v1_17_R1"))
        compileOnly(project(":v1_18_R1"))
        compileOnly(project(":v1_18_R2"))
        compileOnly(project(":v1_19_R1"))

        shade(project(":nms")) { isTransitive = false }
        shade(project(":v1_8_R3")) { isTransitive = false }
        shade(project(":v1_12_R1")) { isTransitive = false }
        shade(project(":v1_14_R1")) { isTransitive = false }
        shade(project(":v1_15_R1")) { isTransitive = false }
        shade(project(":v1_16_R3")) { isTransitive = false }
        shade(project(":v1_17_R1")) { isTransitive = false }
        shade(project(":v1_18_R1")) { isTransitive = false }
        shade(project(":v1_18_R2")) { isTransitive = false }
        shade(project(":v1_19_R1")) { isTransitive = false }
    }

    tasks.processResources {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE

        from(sourceSets.main.get().resources.srcDirs) {
            include("**/*.yml")
            filter(
                ReplaceTokens::class,
                mapOf("tokens" to mapOf("VERSION" to project.version.toString()))
            )
        }
    }

    tasks.named<ShadowJar>("shadowJar") {
        dependsOn(tasks.processResources)

        destinationDirectory.set(rootProject.layout.buildDirectory.dir("libs"))

        archiveBaseName.set("ArenaRegen-OG")
        archiveVersion.set(project.version.toString())
        archiveClassifier.set("")

        from(sourceSets.main.get().output)

        configurations = listOf(shade)
    }

    tasks.named<Jar>("jar") {
        enabled = false
    }
}

tasks.named("build") {
    dependsOn(":arenaregen:shadowJar")
}
