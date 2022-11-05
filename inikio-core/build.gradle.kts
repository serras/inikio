@file:Suppress("DSL_SCOPE_VIOLATION")

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.dokka)
}

kotlin {
  jvm()
  js {
    browser()
    nodejs()
  }
}

kotlin {
  explicitApi()
  sourceSets {
    commonMain {
      dependencies {
        implementation(libs.kotlin.stdlibCommon)
      }
    }
  }
}

tasks.withType<KotlinCompile> {
  kotlinOptions.jvmTarget = "1.8"
}

tasks.dokkaHtml.configure {
  outputDirectory.set(rootDir.resolve("docs"))
  dokkaSourceSets {
    named("commonMain") {
      includes.from("README.md")
    }
  }
}