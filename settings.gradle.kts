@file:Suppress("UnstableApiUsage")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "miniplaceholders-parent"

pluginManagement {
    includeBuild("build-logic")
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.quiltmc.org/repository/release/")
        maven("https://repo.jpenilla.xyz/snapshots/")
    }
}

dependencyResolutionManagement {
    repositories {
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.maven.apache.org/maven2/")
        maven("https://repo.spongepowered.org/repository/")
        maven("https://jitpack.io")
        maven("https://repo.jpenilla.xyz/snapshots/")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("com.vanniktech.maven.publish") version "0.36.0" apply false
    kotlin("jvm") version "2.3.20" apply false
}

// BTC fork scope: Paper (BTC-CORE) and Velocity (BTCVelocity) only.
// The minestom/fabric/sponge sources stay on disk so upstream merges apply cleanly, but they are
// excluded from the build — we ship none of them, and fabric additionally fails at configuration
// time upstream because net.kyori:adventure-platform-fabric:6.9.0-SNAPSHOT is not published.
arrayOf(
    "connect",
    "api",
    "kotlin-ext",
    "common",
    "paper",
    "velocity",
    // Not shipped, but the runServer/runVelocity dev tasks load it as a sample expansion
    // (see build-logic/miniplaceholders.runtask.gradle.kts).
    "example-expansion-provider"
).forEach {
    include("miniplaceholders-$it")
    project(":miniplaceholders-$it").projectDir = file(it)
}