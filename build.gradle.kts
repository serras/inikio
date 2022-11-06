@file:Suppress("DSL_SCOPE_VIOLATION")

allprojects {
    group = "com.github.serras.inikio"
    version = "0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.jvm) apply false
}