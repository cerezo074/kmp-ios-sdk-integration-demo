# KMP notes — monorepo

**Open this folder in Android Studio** (`custom_library_cocoapods`), not `NotesApp-android` or `KMPLibrary` alone.

### One Gradle project (root)

- **`settings.gradle.kts`** — includes `:library` → `KMPLibrary/library`, `:app` → `NotesApp-android/app`
- **`gradle/libs.versions.toml`** — single catalog for app + library (no duplicates under subfolders)
- **`gradlew`** — only at repo root

### Commands (from repo root)

| Goal | Command |
|------|---------|
| Debug Android app | `./gradlew :app:assembleDebug` |
| Debug XCFramework + SPM wrapper | `./gradlew :library:buildDebugSpmPackage` |
| Release XCFramework + SPM | `./gradlew :library:buildReleaseSpmPackage` |

### Android SDK path

Gradle expects **`local.properties`** at the **repo root** with `sdk.dir=...`. Android Studio usually creates it when you open the root project; or copy `sdk.dir` from an old `NotesApp-android/local.properties`.

### iOS app

Xcode still points at `KMPLibrary/library/build/XCFrameworks/spm_wrapper`. After `./gradlew :library:buildDebugSpmPackage`, use *File → Packages → Resolve Package Versions*.

**Out of scope:** CI details beyond `.github/workflows`, Maven Central usage (see `:library:publishToMavenCentral`).
