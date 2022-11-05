@file:Suppress("DSL_SCOPE_VIOLATION")

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  alias(libs.plugins.kotlin.jvm)
}

dependencies {
  implementation(libs.kotlin.stdlibCommon)
  implementation(projects.inikioCore)
  implementation(libs.ksp)
  implementation(libs.kotlinPoet)
  implementation(libs.kotlinPoet.ksp)
}

tasks.withType<KotlinCompile> {
  kotlinOptions.jvmTarget = "1.8"
}