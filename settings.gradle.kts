rootProject.name = "ArenaRegen-OG"

// Execute bootstrap.sh
exec {
    workingDir(rootDir)
    commandLine("sh", "bootstrap.sh")
}

include("libs:Utilities-OG")
