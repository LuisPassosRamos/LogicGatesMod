val sourceManifest = file("src/main/resources/manifest.json").readText()
fun manifestValue(key: String): String = Regex("\"$key\"\\s*:\\s*\"([^\"]+)\"")
    .find(sourceManifest)?.groupValues?.get(1) ?: error("Missing $key in manifest.json")

version = manifestValue("Version")
val targetHytale = manifestValue("ServerVersion").removePrefix("=")
val python = providers.gradleProperty("pythonExecutable").orElse("python")
val buildInfoDirectory = layout.buildDirectory.dir("generated/build-info")
val releaseDirectory = layout.buildDirectory.dir("release-metadata")

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.4")
}

// ScaffoldIt's older bundled devtools plugin rejects the target Hytale version.
configurations.named("runtimeClasspath") { exclude(group = "dev.scaffoldit", module = "devtools") }
// The checked-in manifest is authoritative. Builds must not rewrite source files.
tasks.named("generateManifest") { enabled = false }

val generateBuildInfo by tasks.registering(Exec::class) {
    description = "Generates version, revision and a deterministic source fingerprint."
    group = "build"
    workingDir(projectDir)
    outputs.upToDateWhen { false }
    commandLine(python.get(), "tools/build_info.py", "--resources", buildInfoDirectory.get().asFile.absolutePath)
}

tasks.processResources {
    dependsOn(generateBuildInfo)
    from(buildInfoDirectory)
    exclude("**/*.bbmodel", "**/*.bak", "**/.idea/**")
}

tasks.withType<Jar>().configureEach {
    archiveBaseName.set("LogicGates")
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}
tasks.named<Jar>("jar") {
    archiveFileName.set("LogicGates-${project.version}.jar")
    manifest.attributes("Implementation-Title" to "LogicGates", "Implementation-Version" to project.version.toString())
}
tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    systemProperty("java.util.logging.manager", "com.hypixel.hytale.logger.backend.HytaleLogManager")
}

fun registerVerification(name: String, mode: String, details: String) = tasks.register<Exec>(name) {
    description = details
    group = "verification"
    workingDir(projectDir)
    commandLine(python.get(), "tools/verify_project.py", mode)
}
val verifyProject = registerVerification("verifyProject", "repository", "Checks versions, documentation links and translation references.")
val verifyGeneratedAssets = registerVerification("verifyGeneratedAssets", "assets", "Checks generated assets and Blockbench projects without rewriting them.")
val testTools by tasks.registering(Exec::class) {
    description = "Tests build identity, distribution checks and isolated test-universe cleanup."
    group = "verification"
    workingDir(projectDir)
    commandLine(python.get(), "-m", "unittest", "discover", "-s", "tools/tests", "-v")
}
val productionJar = tasks.named<Jar>("jar").flatMap { it.archiveFile }
val verifyDistribution by tasks.registering(Exec::class) {
    description = "Checks the actual JAR's identity, runtime assets and packaging exclusions."
    group = "verification"
    dependsOn(tasks.jar)
    workingDir(projectDir)
    commandLine(python.get(), "tools/verify_project.py", "distribution", "--jar", productionJar.get().asFile.absolutePath)
}
tasks.check { dependsOn(verifyProject, verifyGeneratedAssets, verifyDistribution, testTools) }

tasks.register<Jar>("serverTestJar") {
    description = "Packages the isolated integration plugin matching the production version."
    group = "verification"
    dependsOn(tasks.testClasses)
    inputs.property("modVersion", project.version.toString())
    inputs.property("hytaleVersion", targetHytale)
    from(sourceSets.test.get().output) { include("sisso/logicsim/integration/**") }
    from("src/test/server") {
        include("manifest.json")
        expand("modVersion" to project.version.toString(), "hytaleVersion" to targetHytale)
    }
    archiveFileName.set("logicgates-integration-tests.jar")
    destinationDirectory.set(layout.buildDirectory.dir("server-tests"))
}

val releaseMetadata by tasks.registering(Exec::class) {
    description = "Writes release.json and the production JAR checksum."
    group = "distribution"
    dependsOn(tasks.jar)
    workingDir(projectDir)
    commandLine(python.get(), "tools/build_info.py", "--jar", productionJar.get().asFile.absolutePath,
        "--output", releaseDirectory.get().asFile.absolutePath)
}
val releaseZip by tasks.registering(Zip::class) {
    description = "Packages the verified JAR, checksum, build identity and changelog."
    group = "distribution"
    dependsOn(tasks.check, releaseMetadata)
    from(productionJar)
    from(productionJar.map { it.asFile.resolveSibling(it.asFile.name + ".sha256") })
    from(releaseDirectory)
    from("CHANGELOG.md", "README.md")
    from("docs") { into("docs") }
    from("assets") {
        include("**/*.md", "**/*.json", "**/*.png", "**/*.bbmodel", "**/*.blockymodel")
        into("assets")
    }
    archiveFileName.set("LogicGates-${project.version}.zip")
    destinationDirectory.set(layout.buildDirectory.dir("releases"))
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}
val verifyRelease by tasks.registering(Exec::class) {
    description = "Checks release ZIP contents, checksums and bundled documentation links."
    group = "verification"
    dependsOn(releaseZip)
    workingDir(projectDir)
    commandLine(python.get(), "tools/verify_project.py", "release", "--zip", releaseZip.get().archiveFile.get().asFile.absolutePath)
}
tasks.build { dependsOn(verifyRelease) }

val serverIntegrationTest by tasks.registering(Exec::class) {
    description = "Runs real Hytale create/restart checks; requires a local Assets.zip."
    group = "verification"
    dependsOn(tasks.build, "serverTestJar")
    workingDir(projectDir)
    val arguments = mutableListOf(python.get(), "tools/run_server_tests.py", "--production-jar", productionJar.get().asFile.absolutePath)
    providers.gradleProperty("hytaleAssets").orNull?.let { arguments.addAll(listOf("--assets", it)) }
    commandLine(arguments)
}
tasks.register("fullCheck") {
    description = "Runs build, tests, asset/documentation/package checks and real server integration."
    group = "verification"
    dependsOn(serverIntegrationTest)
}

tasks.register<JavaExec>("benchmarkCircuit") {
    description = "Measures evaluator median/p95 and allocations at 100, 1000 and 10000 components."
    group = "verification"
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("sisso.logicsim.benchmarks.CircuitBenchmark")
    maxHeapSize = "2g"
    args(layout.buildDirectory.file("reports/benchmarks/circuit.json").get().asFile.absolutePath)
}
tasks.named<JavaExec>("runServer") {
    workingDir(layout.projectDirectory.dir("devserver-074"))
    doFirst { workingDir.mkdirs() }
}
