import java.util.*

plugins {
    java
}

tasks {
    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(25)
    }
    jar {
        manifest {
            attributes(
                    "Specification-Version" to project.version,
                    "Specification-Vendor" to "MiniPlaceholders",
                    "Implementation-Build-Date" to Date()
            )
        }
    }
}

java{
    toolchain{
        // No vendor pin: the BTC toolchain is Oracle JDK 25. Forcing Azul here would make
        // every build download a second JDK for no benefit.
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}