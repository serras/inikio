@file:Suppress("DSL_SCOPE_VIOLATION")

import org.jetbrains.kotlin.gradle.dsl.KotlinJsCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.ksp)
  idea
}

kotlin {
  jvm {
    jvmToolchain(8)
  }
  js {
    browser()
    nodejs()
  }
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        implementation(libs.kotlin.stdlibCommon)
        implementation(projects.inikioCore)
      }
      kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
    }
  }
}

dependencies {
  add("kspCommonMainMetadata", projects.inikioKsp)
}

tasks.withType<KotlinJsCompile> {
  dependsOn("kspCommonMainKotlinMetadata")
}

tasks.getByName("jsSourcesJar") {
  dependsOn("kspCommonMainKotlinMetadata")
}

tasks.withType<KotlinJvmCompile> {
  dependsOn("kspCommonMainKotlinMetadata")
}

idea {
  module {
    // Not using += due to https://github.com/gradle/gradle/issues/8749
    sourceDirs = sourceDirs + file("build/generated/ksp/metadata/commonMain/kotlin")
    generatedSourceDirs = generatedSourceDirs + file("build/generated/ksp/metadata/commonMain/kotlin")
  }
}
