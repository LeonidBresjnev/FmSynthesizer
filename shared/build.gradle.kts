import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

val generateTokenConfig = tasks.register("generateTokenConfig") {
    val token = System.getenv("MUSICXML_TOKEN") ?: System.getenv("musicxmlToken") ?: ""
    val outputDir = layout.buildDirectory.dir("generated/source/tokenConfig/commonMain/com/sputnik/fmsynthesizer/model")
    outputs.dir(outputDir)
    doLast {
        val configFile = outputDir.get().file("DefaultTokenConfig.kt").asFile
        configFile.parentFile.mkdirs()
        configFile.writeText(
            """
            package com.sputnik.fmsynthesizer.model

            object DefaultTokenConfig {
                const val DEFAULT_TOKEN: String = "${token.trim()}"
            }
            """.trimIndent()
        )
    }
}

kotlin {
    jvm()
    
    js {
        browser()
    }
    
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }
    
    android {
       namespace = "com.sputnik.fmsynthesizer.shared"
       compileSdk = libs.versions.android.compileSdk.get().toInt()
       minSdk = libs.versions.android.minSdk.get().toInt()
    
       compilerOptions {
           jvmTarget = JvmTarget.JVM_11
       }
       androidResources {
           enable = true
       }
       withHostTest {
           isIncludeAndroidResources = true
       }
       withDeviceTestBuilder {
           sourceSetTreeName = "test"
       }.configure {
           instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
       }
    }
    
    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.uiTooling)
            implementation("io.ktor:ktor-client-cio:3.1.1")
        }
        jvmMain.dependencies {
            implementation("io.ktor:ktor-client-cio:3.1.1")
            runtimeOnly("ch.qos.logback:logback-classic:1.6.4")
        }
        commonMain {
            kotlin.srcDir(generateTokenConfig)
            dependencies {
                implementation(libs.compose.runtime)
                implementation(libs.compose.foundation)
                implementation(libs.compose.material3)
                implementation(libs.compose.ui)
                implementation(libs.compose.components.resources)
                implementation(libs.compose.uiToolingPreview)
                implementation(libs.androidx.lifecycle.viewmodelCompose)
                implementation(libs.androidx.lifecycle.runtimeCompose)
                implementation("io.ktor:ktor-client-core:3.1.1")
            }
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jsMain.dependencies {
            implementation(libs.wrappers.browser)
            implementation("io.ktor:ktor-client-js:3.1.1")
        }
        wasmJsMain.dependencies {
            implementation("io.ktor:ktor-client-js:3.1.1")
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}

abstract class BuildDesktopNativeTask @Inject constructor(
    private val execOperations: ExecOperations
) : DefaultTask() {

    @get:InputDirectory
    abstract val cmakeSourceDir: DirectoryProperty

    @get:OutputDirectory
    abstract val outputResourceDir: DirectoryProperty

    @get:Internal
    abstract val buildDirectory: DirectoryProperty

    @TaskAction
    fun build() {
        val cmakeBuildDir = buildDirectory.dir("cmake/desktop").get().asFile
        val outputDir = outputResourceDir.get().asFile
        cmakeBuildDir.mkdirs()
        outputDir.mkdirs()

        val sourceDir = cmakeSourceDir.get().asFile

        execOperations.exec {
            commandLine("cmake", "-B", cmakeBuildDir.absolutePath, "-S", sourceDir.absolutePath)
        }
        execOperations.exec {
            commandLine("cmake", "--build", cmakeBuildDir.absolutePath, "--config", "Release")
        }

        val osName = System.getProperty("os.name").lowercase()
        val ext = when {
            osName.contains("win") -> "dll"
            osName.contains("mac") -> "dylib"
            else -> "so"
        }

        cmakeBuildDir.walkTopDown().filter { file ->
            file.isFile && file.extension == ext && file.name.contains("fmsynthesizer_desktop")
        }.forEach { builtFile ->
            builtFile.copyTo(File(outputDir, builtFile.name), overwrite = true)
        }
    }
}

val buildDesktopNative = tasks.register<BuildDesktopNativeTask>("buildDesktopNative") {
    cmakeSourceDir.set(layout.projectDirectory.dir("src/jvmMain/cpp"))
    outputResourceDir.set(layout.buildDirectory.dir("processedResources/jvm/main/native"))
    buildDirectory.set(layout.buildDirectory)
}

tasks.named("jvmProcessResources") {
    dependsOn(buildDesktopNative)
}
