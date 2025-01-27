@file:Suppress("DSL_SCOPE_VIOLATION")

plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.dokka)
  `maven-publish`
}

kotlin {
  jvmToolchain(8)
  jvm()

  js(IR) {
    browser()
    nodejs()
  }

  explicitApi()
  
  sourceSets {
    commonMain {
      dependencies {
        implementation(libs.kotlin.stdlibCommon)
      }
    }
  }
}

dokka {
  moduleName.set("Inikio")
  dokkaSourceSets.commonMain {
    includes.from("README.md")
  }
  dokkaPublications.html {
    outputDirectory.set(rootDir.resolve("docs"))
  }
}

val javadocJar by tasks.registering(Jar::class) {
  archiveClassifier.set("javadoc")
  from(tasks.dokkaGenerate)
}

publishing {
  publications.withType<MavenPublication>().configureEach {
    artifact(javadocJar)
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
