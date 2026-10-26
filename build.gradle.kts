plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

val selfTestRequested = project.hasProperty("selftest") ||
    gradle.startParameter.taskNames.any { it.substringAfter(':') == "selfTest" }
val selfTestDir = layout.buildDirectory.dir("selftest-server").get().asFile

if (selfTestRequested) {
    tasks.register("prepareSelfTestServer") {
        notCompatibleWithConfigurationCache("Self-test harness")
        doLast {
            selfTestDir.mkdirs()
            selfTestDir.resolve("eula.txt").writeText("eula=true\n")
            selfTestDir.resolve("server.properties").writeText(
                """
                level-name=selftest
                level-type=FLAT
                generator-settings=
                online-mode=false
                allow-nether=false
                generate-structures=false
                spawn-protection=0
                max-tick-time=-1
                view-distance=4
                server-port=25566
                server-ip=127.0.0.1
                enable-query=false
                enable-rcon=false
                spawn-monsters=false
                spawn-animals=false
                spawn-npcs=false
                pvp=false
                difficulty=0
                """.trimIndent() + "\n"
            )
            selfTestDir.resolve("selftest").deleteRecursively()
        }
    }

    tasks.named<com.gtnewhorizons.retrofuturagradle.minecraft.RunMinecraftTask>("runServer") {
        dependsOn("prepareSelfTestServer")
        notCompatibleWithConfigurationCache("Self-test harness")
        workingDir = selfTestDir
        extraJvmArgs.add("-Dadditionalpipes.selftest=true")
        setIgnoreExitValue(true)
        doLast {
            // FML's log is fml-server-latest.log; latest.log is the vanilla log and does not contain mod log lines.
            val log = selfTestDir.resolve("logs/fml-server-latest.log")
            if (!log.exists()) {
                throw GradleException("Self-test log missing: $log")
            }
            val text = log.readText()
            if ("[SELFTEST] FAIL" in text) {
                throw GradleException("Self-test reported failures. See $log")
            }
            if ("[SELFTEST] DONE failures=0" !in text) {
                throw GradleException("Self-test did not finish cleanly. See $log")
            }
        }
    }
}

tasks.register("selfTest") {
    dependsOn("runServer")
}
