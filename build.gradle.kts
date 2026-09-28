plugins {
    kotlin("jvm") version "1.9.23"
    id("org.jetbrains.compose") version "1.6.11"
    kotlin("plugin.serialization") version "1.9.23"
}

group = "d.d.meshenger.desktop"
version = "4.5.0-windows"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    
    // JSON Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.8.0")

    // Libsodium for Java / Desktop (LazySodium for E2E Crypto)
    implementation("com.goterl:lazysodium-java:5.1.1")
    implementation("net.java.dev.jna:jna:5.14.0")

    // Logging
    implementation("org.slf4j:slf4j-simple:2.0.12")

    // Unit Testing
    testImplementation(kotlin("test"))
}

compose.desktop {
    application {
        mainClass = "d.d.meshenger.desktop.MainKt"
        nativeDistributions {
            targetFormats(org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi, org.jetbrains.compose.desktop.application.dsl.TargetFormat.Exe)
            packageName = "MeshengerTR-Windows"
            packageVersion = "4.5.0"
            description = "MeshengerTR P2P Emergency Network & Disaster Beacon for Windows (ISO 22324 Compliant)"
            copyright = "© 2026 MeshengerTR Contributors. All rights reserved."
            vendor = "MeshengerTR Contributors"
            
            windows {
                menuGroup = "MeshengerTR"
                upgradeUuid = "39eecc3f-e8f1-440e-852c-494e3b3845a9"
            }
        }
    }
}
