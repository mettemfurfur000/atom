plugins {
    java
    id("com.gradleup.shadow") version "8.3.5"
    id("xyz.jpenilla.run-paper") version "2.3.1"
    id("org.jetbrains.kotlin.jvm") version "2.2.21"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.19"
    kotlin("plugin.lombok") version "2.2.21"
    id("io.freefair.lombok") version "8.14.2"
}

group = "org.shotrush.atom"
version = "5.0-ALPHA"

repositories {
    mavenCentral()
    maven {
        name = "papermc-repo"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven {
        name = "aikar"
        url = uri("https://repo.aikar.co/content/groups/aikar/")
    }
    maven {
        name = "dmulloy2-repo"
        url = uri("https://repo.dmulloy2.net/repository/public/")
    }
    maven("https://repo.momirealms.net/releases/")
    maven("https://repo.triumphteam.dev/snapshots")
    maven("https://maven.nostal.ink/repository/maven-public/")
}

dependencies {
    paperweight.foliaDevBundle("1.21.8-R0.1-SNAPSHOT")
    implementation("co.aikar:acf-paper:0.5.1-SNAPSHOT")
    implementation("org.reflections:reflections:0.10.2")
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.xerial:sqlite-jdbc:3.46.1.0")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
    implementation("org.yaml:snakeyaml:2.2")
    implementation("com.google.code.gson:gson:2.10.1")

    compileOnly("org.projectlombok:lombok:1.18.38")
    annotationProcessor("org.projectlombok:lombok:1.18.38")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testImplementation("org.mockito:mockito-core:5.8.0")

    implementation("org.jetbrains.kotlin:kotlin-stdlib")

    compileOnly("net.dmulloy2:ProtocolLib:5.4.0")
    compileOnly("net.momirealms:craft-engine-core:0.0.65")
    compileOnly("net.momirealms:craft-engine-bukkit:0.0.65")
    compileOnly("net.momirealms:craft-engine-nms-helper:1.0.127")
    compileOnly("io.github.toxicity188:bettermodel:1.14.1")
    implementation("dev.triumphteam:triumph-gui-paper-kotlin:4.0.0-SNAPSHOT")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("com.github.shynixn.mccoroutine:mccoroutine-folia-api:2.22.0")
    implementation("com.github.shynixn.mccoroutine:mccoroutine-folia-core:2.22.0")
    implementation("dev.jorel:commandapi-paper-shade:11.0.0")
    implementation("dev.jorel:commandapi-kotlin-paper:11.0.0")
    implementation("plutoproject.adventurekt:core:2.1.1")
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
        modrinth("terra", "6.6.6-BETA-bukkit")
//        hangar("BetterModel", "1.14.2-SNAPSHOT-427")
//        modrinth("betterhud2", "OUzj5ALL")
    }

    // Configure run-paper
    runPaper.folia.registerTask {
        minecraftVersion("1.21.8")
        downloadPlugins.from(spec)
    }

    runServer {
        minecraftVersion("1.21.8")
        downloadPlugins.from(spec)
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