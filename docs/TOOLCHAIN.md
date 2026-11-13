# Build toolchain

Pinned: Temurin JDK 21.0.13+9 (Java/Kotlin output 17), AGP 9.5.1, Gradle 9.6.0,
Kotlin/Compose compiler 2.4.21, Compose BOM 2026.09.00,
Android platform 37.0, build tools 36.0.0; min API 26, target API 37.

Compatibility sources checked 2026-11-13:
- https://developer.android.com/build/releases/agp-9-5-0-release-notes
- https://kotlinlang.org/docs/releases.html
- https://developer.android.com/develop/ui/compose/bom

AGP uses built-in Kotlin; the explicit JVM plugin serves only the isolated core and
PrayTimes modules. The benchmark module reserves the agreed module boundary; actual
Macrobenchmark journeys and Baseline Profiles are pending.

Install JDK 21, Android command-line tools and packages:
`platforms;android-37.0`, `build-tools;36.0.0`, `platform-tools`.
Set JAVA_HOME and ANDROID_HOME to those installations.
Run `./gradlew :core:test :praytimes:test :app:lintDebug :app:assembleDebug :app:assembleQa`.

The wrapper verifies the Gradle distribution SHA-256. Dependency locks and checksum
metadata are committed after a successful resolution. Review changes to either.
Temporary downloads on one machine are not project dependencies.

CI provisions Android command-line tools 15859902 explicitly and checks the published
SHA-256 before extraction, rather than relying on hosted runner PATH contents. Gradle
Actions uses its open-source `basic` cache provider.
