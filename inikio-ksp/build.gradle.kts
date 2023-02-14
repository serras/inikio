@file:Suppress("DSL_SCOPE_VIOLATION")

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  alias(libs.plugins.kotlin.jvm)
  `maven-publish`
}

dependencies {
  implementation(libs.kotlin.stdlibCommon)
  implementation(projects.inikioCore)
  implementation(libs.ksp)
  implementation(libs.kotlinPoet)
  implementation(libs.kotlinPoet.ksp)
}

kotlin {
  jvmToolchain(8)
}

publishing {
  publications.withType<MavenPublication>().configureEach {
    pom {
      description.set("Better initial-style DSLs in Kotlin ")
      url.set("https://serranofp.com/inikio")
      name.set("Inikio")
      licenses {
        license {
          name.set("The Apache License, Version 2.0")
          url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
        }
      }
      developers {
        developer {
          id.set("inikio-authors")
          name.set("The Inikio authors")
        }
      }
      scm {
        connection.set("scm:git:git://github.com/serras/inikio.git")
        developerConnection.set("scm:git:ssh://git@github.com/serras/inikio.git")
        url.set("https://github.com/serras/inikio")
      }
    }
  }
}

plugins.withType<JavaPlugin>().configureEach {
  publishing {
    publications {
      register<MavenPublication>("maven") {
        from(components["java"])
      }
    }
  }
}
