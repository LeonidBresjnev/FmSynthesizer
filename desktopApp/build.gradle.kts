import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(project(":shared"))

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)

    implementation(libs.compose.uiToolingPreview)
    implementation(compose.components.resources)
}

compose.desktop {
    application {
        mainClass = "com.sputnik.fmsynthesizer.MainKt"

        nativeDistributions {
            targetFormats(
                TargetFormat.Dmg,
                TargetFormat.Pkg,
                TargetFormat.Msi,
                TargetFormat.Exe,
                TargetFormat.Deb,
                TargetFormat.Rpm
            )
            packageName = "FMSynthesizer"
            packageVersion = "1.0.0"
            description = "FM Synthesizer Desktop Application"
            copyright = "© 2025 Sputnik. All rights reserved."
            vendor = "Sputnik"

            windows {
                menu = true
                shortcut = true
                dirChooser = true
                upgradeUuid = "8b417e4f-287f-4c5e-a9b1-561df6d38e0a"
                iconFile.set(project.file("src/main/resources/icon.ico"))
            }

            macOS {
                bundleID = "com.sputnik.fmsynthesizer"
                dockName = "FM Synthesizer"
                iconFile.set(project.file("src/main/resources/icon.icns"))
            }

            linux {
                shortcut = true
                menuGroup = "Audio"
                iconFile.set(project.file("src/main/resources/icon.png"))
            }
        }
    }
}

val generateDesktopIcons = tasks.register<JavaExec>("generateDesktopIcons") {
    mainClass.set("com.sputnik.fmsynthesizer.IconGeneratorKt")
    classpath = sourceSets["main"].runtimeClasspath
}

val runMidiDownloadTest = tasks.register<JavaExec>("runMidiDownloadTest") {
    mainClass.set("com.sputnik.fmsynthesizer.MidiDownloadTestKt")
    classpath = sourceSets["main"].runtimeClasspath
}
