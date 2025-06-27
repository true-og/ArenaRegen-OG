import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.apache.tools.ant.filters.ReplaceTokens

tasks.named<Delete>("clean") { delete("$rootDir/build/libs/") }

tasks.named<org.gradle.api.tasks.Copy>("processResources") {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    from(sourceSets.main.get().resources.srcDirs) {
        include("**/*.yml")
        filter<ReplaceTokens>("tokens" to mapOf("VERSION" to project.version.toString()))
    }
}

dependencies {
    compileOnly("org.purpurmc.purpur:purpur-api:1.19.4-R0.1-SNAPSHOT")
    compileOnly("org.projectlombok:lombok:1.18.22")
    annotationProcessor("org.projectlombok:lombok:1.18.22")
    implementation("com.github.Realizedd.Duels:duels-api:3.4.1")
    implementation(project(":nms"))
    implementation(project(":v1_8_R3"))
    implementation(project(":v1_12_R1"))
    implementation(project(":v1_14_R1"))
    implementation(project(":v1_15_R1"))
    implementation(project(":v1_16_R3"))
    implementation(project(":v1_17_R1"))
    implementation(project(":v1_18_R1"))
    implementation(project(":v1_18_R2"))
    implementation(project(":v1_19_R1"))
}

tasks.named<ShadowJar>("shadowJar") {
    destinationDirectory.set(layout.buildDirectory.dir("../../build/libs/"))
    archiveFileName.set("ArenaRegen-OG-${project.version}.jar")
    dependencies {
        include(project(":nms"))
        include(project(":v1_8_R3"))
        include(project(":v1_12_R1"))
        include(project(":v1_14_R1"))
        include(project(":v1_15_R1"))
        include(project(":v1_16_R3"))
        include(project(":v1_17_R1"))
        include(project(":v1_18_R1"))
        include(project(":v1_18_R2"))
        include(project(":v1_19_R1"))
    }
    minimize()
}

tasks.build {
    dependsOn(tasks.spotlessApply)
    dependsOn(tasks.shadowJar)
}
