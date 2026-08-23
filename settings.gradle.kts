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
    id("com.vanniktech.maven.publish") version "0.37.0" apply false
    kotlin("jvm") version "2.4.10" apply false
}

// BTC fork scope: Paper (BTC-CORE) only.
// The minestom/fabric/sponge sources stay on disk so upstream merges apply cleanly, but they are
// excluded from the build — we ship none of them, and fabric additionally fails at configuration
// time upstream because net.kyori:adventure-platform-fabric:6.9.0-SNAPSHOT is not published.
//
// Velocity is excluded for the same underlying reason, and it is a real limitation rather than a
// preference: Velocity 3.5 pins Adventure 4.26.1 through its BOM while Paper 26.2 is on 5.2.0, and
// the two are binary-incompatible. `api` and `common` are compiled once and shaded into every
// platform jar, so a single build cannot serve both — whichever version they target, the other
// platform gets a jar that dies on NoSuchMethodError at startup. This fork targets 5.2.0 because
// BTC-CORE is what we run. Re-including velocity means either waiting for it to move to Adventure
// 5, or compiling api+common twice. Nothing on BTCVelocity uses MiniPlaceholders today; it only
// hosts the Maven repo the artifacts are published to.
arrayOf(
    "connect",
    "api",
    "kotlin-ext",
    "common",
    "paper",
    // Not shipped, but the runServer dev task loads it as a sample expansion
    // (see build-logic/miniplaceholders.runtask.gradle.kts).
    "example-expansion-provider"
).forEach {
    include("miniplaceholders-$it")
    project(":miniplaceholders-$it").projectDir = file(it)
}