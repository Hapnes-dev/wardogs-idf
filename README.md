# WARDOGS IDF

An offline Android port of the WARDOGS indirect-fire calculator published at
<https://schaulers.com/tools/wardogs-mortar-calculator>. Enter the gun's map
coordinates and the target's; the app reports range, bearing, compass direction
and — for the L81 — the estimated sight elevation in mils.

Native Kotlin and Jetpack Compose, no network permission, no analytics, no
dependencies beyond AndroidX. The app works with the phone in flight mode.

## What it computes

One full grid square counts as 100 meters, so with the gun at `(fx, fy)` and the
target at `(tx, ty)`:

```
east    = (tx - fx) * 100
north   = (ty - fy) * 100
range   = hypot(east, north)
bearing = (atan2(east, north) in degrees + 360) mod 360
```

The compass direction is the bearing snapped to the nearest of the eight points.
L81 elevation is linear interpolation over the published sight markings, rounded
to the nearest 5 mils. Ranges are checked against the weapon's effective
envelope — 120–700 m for the L81, 735–2,630 m for the SPH-2 — and a target
outside it produces an explicit error instead of a firing solution.

`app/src/test/java/dev/hapnes/wardogsidf/FiringSolutionTest.kt` pins these
results to the values the web calculator produces for the same inputs.

Elevation is an estimate from the current in-game sight markings. Fire a
spotting round before sustained fire, and expect the numbers to drift as the
game's ballistics change.

## Building

There is no Gradle wrapper in the repository, deliberately — no binary jar to
review. Use CI, or supply your own Gradle.

**GitHub Actions.** Every push to `main` runs the unit tests and builds a debug
APK. Download it from the run's *Artifacts* section as `wardogs-idf-debug`, then
install it with `adb install` or by opening the file on the phone with install
from unknown sources enabled for your file manager.

**Locally.** Requires JDK 17 and the Android SDK with platform 35.

```bash
gradle assembleDebug
```

Android Studio opens the project directly and offers to generate a wrapper on
first sync; accept it if you prefer `./gradlew`.

## Release builds

`assembleRelease` is unsigned. Signing config is intentionally absent so no
keystore or secret lives in this repository.

## Credit

The formulas, sight-marking table and weapon range envelopes come from the SC
Haulers web tool linked above. This is an independent, offline reimplementation
for personal use; it ships none of that site's markup, styling or artwork.
