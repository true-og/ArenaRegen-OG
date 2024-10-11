import org.apache.tools.ant.filters.ReplaceTokens

plugins {
    id("com.gradleup.shadow") version "8.3.2" // Import shadow API.
    java // Tell gradle this is a java project.
    eclipse // Import eclipse plugin for IDE integration.
    kotlin("jvm") version "2.0.20" // Import kotlin jvm plugin for kotlin/java integration.
}

java {
    // Declare java version.
    sourceCompatibility = JavaVersion.VERSION_17
}

group = "me.realized.de"
version = "1.4"
val apiVersion = "1.19"

tasks.named<ProcessResources>("processResources") {
    val props = mapOf(
        "version" to version,
        "apiVersion" to apiVersion
    )

    inputs.properties(props) // Indicates to rerun if version changes.

    filesMatching("plugin.yml") {
        expand(props)
    }

    from(sourceSets.main.get().resources.srcDirs) {
        include("**/*.yml")
        filter<ReplaceTokens>("tokens" to mapOf("VERSION" to project.version.toString()))
    }
}

repositories {
    mavenCentral()
    gradlePluginPortal()
    maven {
        url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    }
    
    maven {
        url = uri("https://repo.purpurmc.org/snapshots")
    }
    
    maven {
        url = uri("https://jitpack.io")
    }
    
    maven {
        url = uri("https://maven.enginehub.org/repo/")
    }
    
    maven {
        url = uri("file://${System.getProperty("user.home")}/.m2/repository")
    }
}

dependencies {
    compileOnly("org.purpurmc.purpur:purpur-api:1.19.4-R0.1-SNAPSHOT")
    compileOnly("org.spigotmc:spigot-api:1.19.4-R0.1-SNAPSHOT")
    compileOnly("org.spigotmc:spigot:1.19.4-R0.1-SNAPSHOT")
    compileOnly("io.github.miniplaceholders:miniplaceholders-api:2.2.3")
    implementation("com.github.Realizedd.Duels:duels-api:3.4.1") // Import/Shade Duels API.
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.8") // Import WorldGuard API.
    compileOnly("io.github.miniplaceholders:miniplaceholders-kotlin-ext:2.2.3") // Import MiniPlaceholders API helper.

    // Shade remapped APIs into final jar.
    implementation("org.spigotmc:spigot-api:1.19.4-R0.1-SNAPSHOT")
    implementation("org.spigotmc:spigot:1.19.4-R0.1-SNAPSHOT")
    
    implementation(project(":libs:Utilities-OG"))
    implementation(project(":libs:GxUI-OG"))
    implementation(project(":libs:DiamondBank-OG"))
}

tasks.withType<AbstractArchiveTask>().configureEach { // Ensure reproducible builds.
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

tasks.shadowJar {
    archiveClassifier.set("") // Use empty string instead of null
    from("LICENSE") {
        into("/")
    }
    exclude("io.github.miniplaceholders.*") // Exclude the MiniPlaceholders package from being shadowed.
    minimize()
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.jar {
    archiveClassifier.set("part")
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-parameters")
    options.compilerArgs.add("-Xlint:deprecation") // Triggers deprecation warning messages.
    options.encoding = "UTF-8"
    options.isFork = true
}

kotlin {
    jvmToolchain(17)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
        vendor = JvmVendorSpec.GRAAL_VM
    }
}

