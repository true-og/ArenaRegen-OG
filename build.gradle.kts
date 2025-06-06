import org.apache.tools.ant.filters.ReplaceTokens

plugins {
    id("com.gradleup.shadow") version "8.3.6" // Import shadow API.
    java // Tell gradle this is a java project.
    eclipse // Import eclipse plugin for IDE integration.
    kotlin("jvm") version "2.1.21" // Import kotlin jvm plugin for kotlin/java integration.
}

java {
    // Declare java version.
    sourceCompatibility = JavaVersion.VERSION_17
}

group = "me.realized.de"
version = "1.4"
val apiVersion = "1.19"

tasks.named<ProcessResources>("processResources") {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE // Overwrite duplicate files
	val props = mapOf(
        "version" to version,
        "apiVersion" to apiVersion
    )

    inputs.properties(props) // Indicates to rerun if version changes.

    filesMatching("plugin.yml") {
        expand(props)
    }
    from("LICENSE") { // Bundle license into .jars.
        into("/")
    }
    from(sourceSets.main.get().resources.srcDirs) {
        include("**/*.yml")
        filter<ReplaceTokens>("tokens" to mapOf("VERSION" to project.version.toString()))
    }
}

repositories {
    mavenCentral()
    gradlePluginPortal()
    mavenLocal()
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
	val customMavenLocal = System.getProperty("SELF_MAVEN_LOCAL_REPO")
	if (customMavenLocal != null) {
		val mavenLocalDir = file(customMavenLocal)
		if (mavenLocalDir.isDirectory) {
		    println("Using SELF_MAVEN_LOCAL_REPO at: $customMavenLocal")
	        maven {
	            url = uri("file://${mavenLocalDir.absolutePath}")
	        }
		} else {
		    logger.error("TrueOG Bootstrap not found, defaulting to ~/.m2 for mavenLocal()")
		}
	} else {
		logger.error("TrueOG Bootstrap not found, defaulting to ~/.m2 for mavenLocal()")
	}
}

dependencies {
    compileOnly("org.purpurmc.purpur:purpur-api:1.19.4-R0.1-SNAPSHOT")
    compileOnly("org.spigotmc:spigot-api:1.19.4-R0.1-SNAPSHOT")
    compileOnly("org.spigotmc:spigot:1.19.4-R0.1-SNAPSHOT")
    compileOnly("io.github.miniplaceholders:miniplaceholders-api:2.2.3")
    implementation("com.github.Realizedd.Duels:duels-api:3.4.1") // Import/Shade Duels API.
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.8") // Import WorldGuard API.
    compileOnly("io.github.miniplaceholders:miniplaceholders-api:2.2.3") // Import MiniPlaceholders API.
    // Shade remapped APIs into final jar.
    implementation("org.spigotmc:spigot-api:1.19.4-R0.1-SNAPSHOT")
    implementation("org.spigotmc:spigot:1.19.4-R0.1-SNAPSHOT")
    compileOnly(project(":libs:Utilities-OG"))
}

tasks.withType<AbstractArchiveTask>().configureEach { // Ensure reproducible .jars
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

tasks.shadowJar {
    exclude("io.github.miniplaceholders.*") // Exclude the MiniPlaceholders package from being shadowed.
    archiveClassifier.set("") // Use empty string instead of null.
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

