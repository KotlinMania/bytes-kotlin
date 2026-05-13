pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins { kotlin("multiplatform") version "2.3.21" }
}

plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0" }

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "bytes-kotlin"

val serdeLocal = file("../serde-kotlin")
if (serdeLocal.exists()) {
    includeBuild(serdeLocal) {
        dependencySubstitution {
            substitute(module("io.github.kotlinmania:serde-kotlin")).using(project(":"))
        }
    }
}

