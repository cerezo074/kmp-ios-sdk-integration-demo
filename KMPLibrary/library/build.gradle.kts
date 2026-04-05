import org.gradle.api.publish.maven.tasks.PublishToMavenLocal
import org.gradle.plugins.signing.SigningExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.vanniktech.mavenPublish)
}

group = "com.eli.examples.kmp.notes_module"
version = "1.0.0"

// XCFramework name + `binaries.framework` baseName (must match; see Kotlin SPM export docs).
val notesFrameworkBaseName = "notes_module"

kotlin {
    androidLibrary {
        namespace = "com.eli.examples.kmp.notes_module"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilations.configureEach {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_11)
                }
            }
        }
    }

    val xcf = XCFramework(notesFrameworkBaseName)
    val iosTargets = listOf(iosX64(), iosArm64(), iosSimulatorArm64())

    iosTargets.forEach {
        it.binaries.framework {
            baseName = notesFrameworkBaseName
            binaryOption("bundleId", "com.eli.examples.kmp.notes_module")
            isStatic = true
            xcf.add(this)
        }
    }

    sourceSets {
        commonMain.dependencies {
            //put your multiplatform dependencies here
        }
    }
}

mavenPublishing {
    publishToMavenCentral()

    signAllPublications()

    coordinates(group.toString(), "notes_module", version.toString())

    pom {
        name = "Notes Module"
        description = "A library for managing notes."
        inceptionYear = "2026"
        url = "https://github.com/kotlin/multiplatform-library-template/"
        licenses {
            license {
                name = "XXX"
                url = "YYY"
                distribution = "ZZZ"
            }
        }
        developers {
            developer {
                id = "XXX"
                name = "YYY"
                url = "ZZZ"
            }
        }
        scm {
            url = "XXX"
            connection = "YYY"
            developerConnection = "ZZZ"
        }
    }
}

afterEvaluate {
    extensions.configure<SigningExtension>("signing") {
        setRequired {
            val isSnapshot: Boolean = version.toString().endsWith("-SNAPSHOT")
            val publishesToCentral: Boolean = gradle.taskGraph.allTasks.any { task ->
                val n: String = task.name
                n.endsWith("ToMavenCentralRepository", ignoreCase = true) ||
                    n.equals("publishToMavenCentral", ignoreCase = true) ||
                    n.equals("publishAndReleaseToMavenCentral", ignoreCase = true)
            }
            publishesToCentral && !isSnapshot
        }
    }
}

tasks.withType<PublishToMavenLocal>().configureEach {
    onlyIf {
        !name.startsWith("publishIos")
    }
}

extra["notesFrameworkBaseName"] = notesFrameworkBaseName
apply(from = "spm-package.gradle.kts")

// SPM wrapper lives at `build/XCFrameworks/spm_wrapper/` (same path for Xcode); pick debug vs release binary below.
tasks.register("buildDebugSpmPackage") {
    group = "build"
    description =
        "Clean, assemble debug XCFramework, write build/XCFrameworks/spm_wrapper/Package.swift → ../debug/*.xcframework."
    dependsOn(tasks.named("buildSpmWrapperPackageWorkDebug"))
}

tasks.register("buildReleaseSpmPackage") {
    group = "build"
    description =
        "Clean, assemble release XCFramework, write build/XCFrameworks/spm_wrapper/Package.swift → ../release/*.xcframework."
    dependsOn(tasks.named("buildSpmWrapperPackageWorkRelease"))
}
