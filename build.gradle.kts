import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.ClasspathNormalizer
import org.gradle.api.tasks.testing.AbstractTestTask
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnRootEnvSpec

plugins {
    kotlin("multiplatform") version "2.3.21"
    kotlin("plugin.serialization") version "2.3.21"
    id("com.android.kotlin.multiplatform.library") version "9.2.1"
    id("com.vanniktech.maven.publish") version "0.36.0"
}

group = "io.github.kotlinmania"
version = "0.2.1"

val androidSdkDir: String? =
    providers.environmentVariable("ANDROID_SDK_ROOT").orNull
        ?: providers.environmentVariable("ANDROID_HOME").orNull

if (androidSdkDir != null && file(androidSdkDir).exists()) {
    val localProperties = rootProject.file("local.properties")
    if (!localProperties.exists()) {
        val sdkDirPropertyValue = file(androidSdkDir).absolutePath.replace("\\", "/")
        localProperties.writeText("sdk.dir=$sdkDirPropertyValue")
    }
}

kotlin {
    applyDefaultHierarchyTemplate()

    sourceSets.all {
        languageSettings.optIn("kotlin.time.ExperimentalTime")
        languageSettings.optIn("kotlin.concurrent.atomics.ExperimentalAtomicApi")
    }

    compilerOptions {
        allWarningsAsErrors.set(true)
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    val xcf = XCFramework("Bytes")

    macosArm64 {
        binaries.framework { baseName = "Bytes"; xcf.add(this) }
    }
    iosArm64 {
        binaries.framework { baseName = "Bytes"; xcf.add(this) }
    }
    iosSimulatorArm64 {
        binaries.framework { baseName = "Bytes"; xcf.add(this) }
    }
    iosX64 {
        binaries.framework { baseName = "Bytes"; xcf.add(this) }
    }

    tvosArm64 {
        binaries.framework { baseName = "Bytes"; xcf.add(this) }
    }
    tvosSimulatorArm64 {
        binaries.framework { baseName = "Bytes"; xcf.add(this) }
    }

    watchosArm32 {
        binaries.framework { baseName = "Bytes"; xcf.add(this) }
    }
    watchosArm64 {
        binaries.framework { baseName = "Bytes"; xcf.add(this) }
    }
    watchosDeviceArm64 {
        binaries.framework { baseName = "Bytes"; xcf.add(this) }
    }
    watchosSimulatorArm64 {
        binaries.framework { baseName = "Bytes"; xcf.add(this) }
    }

    linuxX64()
    linuxArm64()
    mingwX64()

    androidNativeArm32()
    androidNativeArm64()
    androidNativeX86()
    androidNativeX64()

    js {
        browser()
        nodejs()
    }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        nodejs()
    }
    @OptIn(ExperimentalWasmDsl::class)
    wasmWasi {
        nodejs()
    }

    swiftExport {
        moduleName = "Bytes"
        flattenPackage = "io.github.kotlinmania.bytes"
    }

    android {
        namespace = "io.github.kotlinmania.bytes"
        compileSdk = 34
        minSdk = 24
        withHostTestBuilder {}.configure {}
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.11.0")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.8.0")
                implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.4.0")
                implementation("io.github.kotlinmania:serde-kotlin:0.1.1")
            }
        }
        val commonTest by getting { dependencies { implementation(kotlin("test")) } }
    }
    jvmToolchain(21)
}

tasks.withType<AbstractTestTask>().configureEach {
    testLogging {
        events(
            TestLogEvent.STARTED,
            TestLogEvent.PASSED,
            TestLogEvent.SKIPPED,
            TestLogEvent.FAILED,
            TestLogEvent.STANDARD_OUT,
            TestLogEvent.STANDARD_ERROR,
        )
        exceptionFormat = TestExceptionFormat.FULL
        showCauses = true
        showExceptions = true
        showStackTraces = true
        showStandardStreams = true
    }
}

rootProject.extensions.configure<NodeJsEnvSpec>("kotlinNodeJsSpec") {
    version.set("24.15.0")
}

rootProject.extensions.configure<WasmNodeJsEnvSpec>("kotlinWasmNodeJsSpec") {
    version.set("24.15.0")
}

rootProject.extensions.configure<YarnRootEnvSpec>("kotlinYarnSpec") {
    version.set("1.22.22")
}

rootProject.extensions.configure<WasmYarnRootEnvSpec>("kotlinWasmYarnSpec") {
    version.set("1.22.22")
}

rootProject.extensions.configure<YarnRootExtension>("kotlinYarn") {
    resolution("diff", "8.0.3")
    resolution("**/diff", "8.0.3")
    resolution("serialize-javascript", "7.0.5")
    resolution("**/serialize-javascript", "7.0.5")
    resolution("webpack", "5.106.2")
    resolution("**/webpack", "5.106.2")
    resolution("follow-redirects", "1.16.0")
    resolution("**/follow-redirects", "1.16.0")
    resolution("lodash", "4.18.1")
    resolution("**/lodash", "4.18.1")
    resolution("ajv", "8.20.0")
    resolution("**/ajv", "8.20.0")
    resolution("brace-expansion", "5.0.5")
    resolution("**/brace-expansion", "5.0.5")
    resolution("flatted", "3.4.2")
    resolution("**/flatted", "3.4.2")
    resolution("minimatch", "10.2.5")
    resolution("**/minimatch", "10.2.5")
    resolution("picomatch", "4.0.4")
    resolution("**/picomatch", "4.0.4")
    resolution("qs", "6.15.1")
    resolution("**/qs", "6.15.1")
    resolution("socket.io-parser", "4.2.6")
    resolution("**/socket.io-parser", "4.2.6")
}


val patchedKarmaWebpackPackage = rootProject.layout.projectDirectory.dir("gradle/npm/karma-webpack").asFile.absolutePath.replace("\\", "/")

rootProject.extensions.configure<NodeJsRootExtension>("kotlinNodeJs") {
    versions.webpack.version = "5.106.2"
    versions.webpackCli.version = "7.0.2"
    versions.karma.version = "npm:karma-maintained@6.4.7"
    versions.karmaWebpack.version = "file:$patchedKarmaWebpackPackage"
    versions.mocha.version = "12.0.0-beta-10"
    versions.kotlinWebHelpers.version = "3.1.0"
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()

    coordinates(group.toString(), "bytes-kotlin", version.toString())

    pom {
        name.set("bytes-kotlin")
        description.set("Kotlin Multiplatform port of tokio-rs/bytes - Types and traits for working with bytes")
        inceptionYear.set("2026")
        url.set("https://github.com/KotlinMania/bytes-kotlin")

        licenses {
            license {
                name.set("MIT")
                url.set("https://opensource.org/licenses/MIT")
                distribution.set("repo")
            }
        }

        developers {
            developer {
                id.set("sydneyrenee")
                name.set("Sydney Renee")
                email.set("sydney@solace.ofharmony.ai")
                url.set("https://github.com/sydneyrenee")
            }
        }

        scm {
            url.set("https://github.com/KotlinMania/bytes-kotlin")
            connection.set("scm:git:git://github.com/KotlinMania/bytes-kotlin.git")
            developerConnection.set("scm:git:ssh://github.com/KotlinMania/bytes-kotlin.git")
        }
    }
}

// ---------------------------------------------------------------------------
// CodeQL Java/Kotlin extraction task
//
// .github/workflows/codeql.yml invokes `./gradlew codeqlCompileJvm` to feed
// kotlinc-compiled commonMain through the CodeQL Java agent.
val codeqlKotlinc: Configuration by configurations.creating {
    description = "Kotlin compiler (CodeQL extraction target only — not published)"
    isCanBeResolved = true
    isCanBeConsumed = false
}

val codeqlSourceClasspath: Configuration by configurations.creating {
    description = "Runtime classpath for CodeQL extraction of commonMain sources"
    isCanBeResolved = true
    isCanBeConsumed = false
}

dependencies {
    codeqlKotlinc("org.jetbrains.kotlin:kotlin-compiler-embeddable:2.3.21")
    codeqlSourceClasspath("org.jetbrains.kotlin:kotlin-stdlib:2.3.21")
    codeqlSourceClasspath("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.11.0")
    codeqlSourceClasspath("org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:1.11.0")
    codeqlSourceClasspath("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.11.0")
    codeqlSourceClasspath("org.jetbrains.kotlinx:kotlinx-datetime-jvm:0.8.0")
    codeqlSourceClasspath("org.jetbrains.kotlinx:kotlinx-collections-immutable-jvm:0.4.0")
    // serde-kotlin: once published to Maven Central, add the -jvm or -android variant here
}

val codeqlCompileJvm = tasks.register<JavaExec>("codeqlCompileJvm") {
    description =
        "Compile commonMain Kotlin sources with kotlinc 2.3.21 for CodeQL Java/Kotlin extraction."
    group = "verification"

    classpath(codeqlKotlinc)
    mainClass.set("org.jetbrains.kotlin.cli.jvm.K2JVMCompiler")

    val outDir = layout.buildDirectory.dir("classes/kotlin/codeql-jvm")
    val sources = fileTree("src/commonMain/kotlin") { include("**/*.kt") }
    val sentinelDir = layout.buildDirectory.dir("generated/codeql-empty-source")
    inputs.files(sources).withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.files(codeqlSourceClasspath).withNormalizer(ClasspathNormalizer::class.java)
    outputs.dir(outDir)
    outputs.dir(sentinelDir)

    doFirst {
        outDir.get().asFile.mkdirs()
        val sourceFiles = sources.files.toMutableList()
        if (sourceFiles.isEmpty()) {
            val sentinelFile = sentinelDir.get().asFile.resolve("io/github/kotlinmania/codeql/_CodeqlEmptySource.kt")
            sentinelFile.parentFile.mkdirs()
            sentinelFile.writeText(
                """
                // Auto-generated. Present so codeqlCompileJvm has at least
                // one Kotlin source to feed kotlinc; replaced by real
                // commonMain content once porting begins.
                package io.github.kotlinmania.codeql

                private object _CodeqlEmptySource
                """.trimIndent(),
            )
            sourceFiles += sentinelFile
        }
        args = listOf(
            "-d", outDir.get().asFile.absolutePath,
            "-classpath", codeqlSourceClasspath.asPath,
            "-jvm-target", "21",
            "-no-stdlib",
            "-no-reflect",
            "-language-version", "2.3",
            "-api-version", "2.3",
            "-Xexpect-actual-classes",
            "-opt-in", "kotlin.time.ExperimentalTime",
            "-opt-in", "kotlin.concurrent.atomics.ExperimentalAtomicApi",
        ) + sourceFiles.map { it.absolutePath }
    }
}

tasks.register("test") {
    group = "verification"
    description =
        "Runs the host-portable test suite (macOS + JS + WasmJS + Android unit). " +
        "Non-host native targets (mingwX64, linuxX64) only run on their own host."

    val defaultTestTasks = listOf(
        "macosArm64Test",
        "jsNodeTest",
        "wasmJsNodeTest",
        "compileAndroidMain",
        "assembleUnitTest",
    )

    dependsOn(defaultTestTasks.mapNotNull { taskName -> tasks.findByName(taskName) })
}

val fullTargetBuildTaskNames = setOf(
    "compileAndroidMain",
    "compileAndroidHostTest",
    "compileAndroidDeviceTest",
    "assembleAndroidMain",
    "assembleAndroidHostTest",
    "assembleAndroidDeviceTest",
    "assembleUnitTest",
    "assembleAndroidTest",
    "testAndroidHostTest",
    "jsMainClasses",
    "jsTestClasses",
    "jsBrowserTest",
    "jsNodeTest",
    "jsTest",
    "wasmJsMainClasses",
    "wasmJsTestClasses",
    "wasmJsBrowserTest",
    "wasmJsNodeTest",
    "wasmJsTest",
    "wasmWasiMainClasses",
    "wasmWasiTestClasses",
    "wasmWasiNodeTest",
    "wasmWasiTest",
    "androidNativeArm32Binaries",
    "androidNativeArm32TestBinaries",
    "androidNativeArm64Binaries",
    "androidNativeArm64TestBinaries",
    "androidNativeX64Binaries",
    "androidNativeX64TestBinaries",
    "androidNativeX86Binaries",
    "androidNativeX86TestBinaries",
    "iosArm64Binaries",
    "iosArm64TestBinaries",
    "iosSimulatorArm64Binaries",
    "iosSimulatorArm64TestBinaries",
    "iosX64Binaries",
    "iosX64TestBinaries",
    "linuxArm64Binaries",
    "linuxArm64TestBinaries",
    "linuxX64Binaries",
    "linuxX64TestBinaries",
    "macosArm64Binaries",
    "macosArm64TestBinaries",
    "mingwX64Binaries",
    "mingwX64TestBinaries",
    "tvosArm64Binaries",
    "tvosArm64TestBinaries",
    "tvosSimulatorArm64Binaries",
    "tvosSimulatorArm64TestBinaries",
    "watchosArm32Binaries",
    "watchosArm32TestBinaries",
    "watchosArm64Binaries",
    "watchosArm64TestBinaries",
    "watchosDeviceArm64Binaries",
    "watchosDeviceArm64TestBinaries",
    "watchosSimulatorArm64Binaries",
    "watchosSimulatorArm64TestBinaries",
    "embedSwiftExportForXcode",
    "assembleBytesXCFramework",
    "assembleBytesDebugXCFramework",
    "assembleBytesReleaseXCFramework",
    "exportCommonSourceSetsMetadataLocationsForMetadataApiElements",
    "exportRootPublicationCoordinatesForMetadataApiElements",
    "exportCrossCompilationMetadataForAndroidNativeArm32ApiElements",
    "exportCrossCompilationMetadataForAndroidNativeArm64ApiElements",
    "exportCrossCompilationMetadataForAndroidNativeX64ApiElements",
    "exportCrossCompilationMetadataForAndroidNativeX86ApiElements",
    "exportCrossCompilationMetadataForIosArm64ApiElements",
    "exportCrossCompilationMetadataForIosSimulatorArm64ApiElements",
    "exportCrossCompilationMetadataForIosX64ApiElements",
    "exportCrossCompilationMetadataForLinuxArm64ApiElements",
    "exportCrossCompilationMetadataForLinuxX64ApiElements",
    "exportCrossCompilationMetadataForMacosArm64ApiElements",
    "exportCrossCompilationMetadataForMingwX64ApiElements",
    "exportCrossCompilationMetadataForTvosArm64ApiElements",
    "exportCrossCompilationMetadataForTvosSimulatorArm64ApiElements",
    "exportCrossCompilationMetadataForWatchosArm32ApiElements",
    "exportCrossCompilationMetadataForWatchosArm64ApiElements",
    "exportCrossCompilationMetadataForWatchosDeviceArm64ApiElements",
    "exportCrossCompilationMetadataForWatchosSimulatorArm64ApiElements",
    "exportTargetPublicationCoordinatesForAndroidApiElements",
    "exportTargetPublicationCoordinatesForAndroidNativeArm32ApiElements",
    "exportTargetPublicationCoordinatesForAndroidNativeArm64ApiElements",
    "exportTargetPublicationCoordinatesForAndroidNativeX64ApiElements",
    "exportTargetPublicationCoordinatesForAndroidNativeX86ApiElements",
    "exportTargetPublicationCoordinatesForAndroidRuntimeElements",
    "exportTargetPublicationCoordinatesForIosArm64ApiElements",
    "exportTargetPublicationCoordinatesForIosSimulatorArm64ApiElements",
    "exportTargetPublicationCoordinatesForIosX64ApiElements",
    "exportTargetPublicationCoordinatesForJsApiElements",
    "exportTargetPublicationCoordinatesForJsRuntimeElements",
    "exportTargetPublicationCoordinatesForLinuxArm64ApiElements",
    "exportTargetPublicationCoordinatesForLinuxX64ApiElements",
    "exportTargetPublicationCoordinatesForMacosArm64ApiElements",
    "exportTargetPublicationCoordinatesForMingwX64ApiElements",
    "exportTargetPublicationCoordinatesForTvosArm64ApiElements",
    "exportTargetPublicationCoordinatesForTvosSimulatorArm64ApiElements",
    "exportTargetPublicationCoordinatesForWasmJsApiElements",
    "exportTargetPublicationCoordinatesForWasmJsRuntimeElements",
    "exportTargetPublicationCoordinatesForWasmWasiApiElements",
    "exportTargetPublicationCoordinatesForWasmWasiRuntimeElements",
    "exportTargetPublicationCoordinatesForWatchosArm32ApiElements",
    "exportTargetPublicationCoordinatesForWatchosArm64ApiElements",
    "exportTargetPublicationCoordinatesForWatchosDeviceArm64ApiElements",
    "exportTargetPublicationCoordinatesForWatchosSimulatorArm64ApiElements",
)

tasks.named("build") {
    dependsOn(fullTargetBuildTaskNames)
}

afterEvaluate {
    tasks.named("build") {
        dependsOn(
            tasks.matching {
                name.endsWith("MainClasses") ||
                    name.endsWith("TestClasses") ||
                    name.endsWith("Binaries") ||
                    name.endsWith("XCFramework") ||
                    name == "embedSwiftExportForXcode" ||
                    name.startsWith("exportCommonSourceSetsMetadataLocationsFor") ||
                    name.startsWith("exportRootPublicationCoordinatesFor") ||
                    name.startsWith("exportCrossCompilationMetadataFor") ||
                    name.startsWith("exportTargetPublicationCoordinatesFor")
            },
        )
    }
}
