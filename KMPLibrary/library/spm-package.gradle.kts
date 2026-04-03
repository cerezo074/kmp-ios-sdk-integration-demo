import org.gradle.api.GradleException
import org.gradle.api.Project

// Populated from `library/build.gradle.kts` before this script is applied (Kotlin binary / XCFramework only).
@Suppress("UNCHECKED_CAST")
private val notesFrameworkBaseName: String =
    project.extra["notesFrameworkBaseName"] as? String
        ?: error("Missing extra \"notesFrameworkBaseName\"; set it in build.gradle.kts before apply(from = \"spm-package.gradle.kts\")")

/** SwiftPM `Package(name:)` and product name — not the framework module you `import` in Swift (`notes_module`). */
private val spmWrapperPackageName = "spm_wrapper"

private val assembleNotesModuleDebugXcframework: String =
    "assemble${notesFrameworkBaseName.replaceFirstChar { it.titlecase() }}DebugXCFramework"

private val assembleNotesModuleReleaseXcframework: String =
    "assemble${notesFrameworkBaseName.replaceFirstChar { it.titlecase() }}ReleaseXCFramework"

/**
 * SPM package root: `build/XCFrameworks/spm_wrapper/Package.swift` points at the sibling Kotlin output:
 * `../debug/notes_module.xcframework` or `../release/notes_module.xcframework` (no duplicate copy).
 */
private val spmWrapperOutputDirRelative = "XCFrameworks/$spmWrapperPackageName"

private fun Project.registerSpmWrapperPipeline(
    variantTaskSuffix: String,
    assembleXcframeworkTask: String,
    kotlinXcframeworksSubdir: String,
) {
    val cleanThenTask = "cleanThenAssembleSpmWrapper${variantTaskSuffix}Xcframework"
    val workTask = "buildSpmWrapperPackageWork$variantTaskSuffix"

    tasks.register(cleanThenTask) {
        description =
            "For $spmWrapperPackageName ($variantTaskSuffix): clean, then $assembleXcframeworkTask (finalizer)."
        dependsOn(tasks.named("clean"))
        finalizedBy(tasks.named(assembleXcframeworkTask))
    }

    tasks.register(workTask) {
        description =
            "Internal ($variantTaskSuffix): clean → $assembleXcframeworkTask → Package.swift under build/$spmWrapperOutputDirRelative/ (binary at ../$kotlinXcframeworksSubdir/)."

        notCompatibleWithConfigurationCache(
            "Ad-hoc task uses Kotlin build script closures tied to Project; safer to opt out than to cache.",
        )

        dependsOn(tasks.named(cleanThenTask))

        doLast {
            val notesModuleXcframeworkBundle = "${notesFrameworkBaseName}.xcframework"
            val buildRoot = layout.buildDirectory.asFile.get()
            val notesModuleXcframeworkDir =
                buildRoot.resolve("XCFrameworks/$kotlinXcframeworksSubdir/$notesModuleXcframeworkBundle")
            val spmWrapperPackageDir = buildRoot.resolve(spmWrapperOutputDirRelative)
            val binaryTargetPath = "../$kotlinXcframeworksSubdir/$notesModuleXcframeworkBundle"

            if (!notesModuleXcframeworkDir.exists()) {
                throw GradleException(
                    "XCFramework not found at $notesModuleXcframeworkDir. Run :${project.name}:$assembleXcframeworkTask " +
                        "or fix notesFrameworkBaseName.",
                )
            }

            if (spmWrapperPackageDir.exists()) {
                spmWrapperPackageDir.deleteRecursively()
            }
            spmWrapperPackageDir.mkdirs()

            spmWrapperPackageDir.resolve("Package.swift").writeText(
                """
                // swift-tools-version:5.6
                import PackageDescription

                let package = Package(
                    name: "$spmWrapperPackageName",
                    platforms: [
                        .iOS(.v13)
                    ],
                    products: [
                        .library(
                            name: "$spmWrapperPackageName",
                            targets: ["$notesFrameworkBaseName"]
                        ),
                    ],
                    targets: [
                        .binaryTarget(
                            name: "$notesFrameworkBaseName",
                            path: "$binaryTargetPath"
                        )
                    ]
                )
                """.trimIndent(),
            )
        }
    }
}

registerSpmWrapperPipeline(
    variantTaskSuffix = "Debug",
    assembleXcframeworkTask = assembleNotesModuleDebugXcframework,
    kotlinXcframeworksSubdir = "debug",
)

registerSpmWrapperPipeline(
    variantTaskSuffix = "Release",
    assembleXcframeworkTask = assembleNotesModuleReleaseXcframework,
    kotlinXcframeworksSubdir = "release",
)
