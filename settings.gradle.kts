rootProject.name = "LogicGates"

plugins { id("dev.scaffoldit") version "0.2.16" }

// Exact target is declared once, in the production manifest.
val targetManifest = file("src/main/resources/manifest.json").readText()
val targetHytale = Regex("\"ServerVersion\"\\s*:\\s*\"=([^\"]+)\"")
    .find(targetManifest)?.groupValues?.get(1) ?: error("An exact ServerVersion is required")

hytale {
    usePatchline("release")
    useVersion(targetHytale)
    manifest {
        Group = "sisso"
        Name = "LogicGates"
        Main = "sisso.logicsim.LogicSimPlugin"
    }
}
