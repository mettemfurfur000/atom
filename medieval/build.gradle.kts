plugins {
    id("com.gradleup.shadow")
    id("xyz.jpenilla.run-paper")
    kotlin("plugin.serialization")
    id("io.papermc.paperweight.userdev")
}

group = "org.shotrush.atom"
version = "0.0.1 INDEV"

dependencies {
    paperweight.foliaDevBundle("1.21.8-R0.1-SNAPSHOT")
    implementation(project(":core"))

    compileOnly("org.projectlombok:lombok:1.18.38")
    annotationProcessor("org.projectlombok:lombok:1.18.38")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testImplementation("org.mockito:mockito-core:5.8.0")

    compileOnly("net.dmulloy2:ProtocolLib:5.4.0")
    compileOnly("net.momirealms:craft-engine-core:0.0.65")
    compileOnly("net.momirealms:craft-engine-bukkit:0.0.65")
    compileOnly("net.momirealms:craft-engine-nms-helper:1.0.127")
    compileOnly("io.github.toxicity188:bettermodel:1.14.1")
    compileOnly("io.github.toxicity188:BetterHud-bukkit-api:1.14.0")
    compileOnly("me.clip:placeholderapi:2.11.7")
}

tasks.withType(xyz.jpenilla.runtask.task.AbstractRun::class) {
    javaLauncher = javaToolchains.launcherFor {
        vendor = JvmVendorSpec.JETBRAINS
        languageVersion = JavaLanguageVersion.of(21)
    }
    jvmArgs("-XX:+AllowEnhancedClassRedefinition")
}

tasks {
    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        // target Java 21
        options.release.set(21)
    }

    var spec = runPaper.downloadPluginsSpec {
        url("https://github.com/dmulloy2/ProtocolLib/releases/download/5.4.0/ProtocolLib.jar")
//        modrinth("terra", "6.6.6-BETA-bukkit")
        hangar("BetterModel", "1.14.2-SNAPSHOT-427")
        modrinth("betterhud2", "OUzj5ALL")
        hangar("PlaceholderAPI", "2.11.7")
    }

    // Configure run-paper
    runPaper.folia.registerTask {
        minecraftVersion("1.21.8")
        downloadPlugins.from(spec)
    }

    runServer {
        minecraftVersion("1.21.8")
        downloadPlugins.from(spec)

        this.jvmArgs("-DIKnowThereAreNoNMSBindingsForv1_21_8ButIWillProceedAnyway=true")
    }


    processResources {
        val props = mapOf("version" to version)
        inputs.properties(props)
        filteringCharset = "UTF-8"
        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    shadowJar {
        archiveClassifier.set("")

//        relocate("co.aikar.commands", "org.shotrush.atom.acf")
//        relocate("co.aikar.locales", "org.shotrush.atom.locales")
//        relocate("org.reflections", "org.shotrush.atom.reflections")
//        relocate("com.zaxxer.hikari", "org.shotrush.atom.hikari")
//        relocate("com.github.benmanes.caffeine", "org.shotrush.atom.caffeine")
//        relocate("com.google.gson", "org.shotrush.atom.gson")
    }

    build {
        dependsOn(named("shadowJar"))
    }

    test {
        useJUnitPlatform()
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

kotlin {
    jvmToolchain(21)
}