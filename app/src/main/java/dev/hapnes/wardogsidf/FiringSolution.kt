package dev.hapnes.wardogsidf

import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.hypot

/** One full grid square on the WARDOGS map is treated as 100 meters. */
const val METERS_PER_GRID = 100.0

private val COMPASS = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")

/**
 * Rounds half-up towards positive infinity, matching JavaScript's `Math.round`.
 * Kotlin's `round` rounds half away from zero, which disagrees on negative
 * halves, so the original web calculator's output is reproduced explicitly.
 */
private fun jsRound(value: Double): Long = floor(value + 0.5).toLong()

enum class Weapon(
    val displayName: String,
    val shortName: String,
    val minRange: Double,
    val maxRange: Double,
    /** True when the weapon has a published sight-marking elevation table. */
    val hasElevation: Boolean,
) {
    L81("L81 Mortar", "L81", minRange = 120.0, maxRange = 700.0, hasElevation = true),
    SPH2("SPH-2 Artillery", "SPH-2", minRange = 735.0, maxRange = 2630.0, hasElevation = false),
}

/** A map grid coordinate pair, in grid squares rather than meters. */
data class GridPoint(val x: Double, val y: Double)

data class FiringSolution(
    /** Easting offset in meters; negative means the target is west of the gun. */
    val east: Double,
    /** Northing offset in meters; negative means the target is south of the gun. */
    val north: Double,
    val rangeMeters: Double,
    val bearingDegrees: Double,
    val direction: String,
    val elevationMils: Int,
    /** Empty when the target is inside the weapon's effective range. */
    val error: String,
) {
    val isInRange: Boolean get() = error.isEmpty()
}

private data class SightMark(val range: Double, val mil: Double)

/** L81 sight markings as read off the in-game sight, range in meters to mils. */
private val SIGHT_TABLE = listOf(
    SightMark(80.0, 950.0),
    SightMark(110.0, 900.0),
    SightMark(132.0, 850.0),
    SightMark(187.0, 800.0),
    SightMark(240.0, 750.0),
    SightMark(290.0, 700.0),
    SightMark(340.0, 650.0),
    SightMark(385.0, 600.0),
    SightMark(430.0, 550.0),
    SightMark(470.0, 500.0),
    SightMark(510.0, 450.0),
    SightMark(545.0, 400.0),
    SightMark(578.0, 350.0),
    SightMark(609.0, 300.0),
    SightMark(637.0, 250.0),
    SightMark(661.0, 200.0),
    SightMark(684.0, 150.0),
    SightMark(700.0, 120.0),
)

/**
 * Interpolates mortar elevation from the sight table and rounds to the nearest
 * 5 mils. Ranges outside the table are extrapolated from its first segment,
 * which only happens when the range check has already flagged an error.
 */
fun elevationMilsFor(rangeMeters: Double): Int {
    var lower = SIGHT_TABLE[0]
    var upper = SIGHT_TABLE[1]
    for (i in 0 until SIGHT_TABLE.size - 1) {
        if (rangeMeters >= SIGHT_TABLE[i].range && rangeMeters <= SIGHT_TABLE[i + 1].range) {
            lower = SIGHT_TABLE[i]
            upper = SIGHT_TABLE[i + 1]
            break
        }
    }
    val fraction = (rangeMeters - lower.range) / (upper.range - lower.range)
    val mils = lower.mil + fraction * (upper.mil - lower.mil)
    return (jsRound(mils / 5.0) * 5L).toInt()
}

fun rangeError(rangeMeters: Double, minRange: Double, maxRange: Double): String = when {
    rangeMeters < minRange ->
        "TARGET TOO CLOSE — MOVE AT LEAST ${ceil(minRange - rangeMeters).toInt()} M FARTHER AWAY"

    rangeMeters > maxRange ->
        "TARGET OUT OF RANGE — ${ceil(rangeMeters - maxRange).toInt()} M BEYOND MAXIMUM"

    else -> ""
}

fun solve(firing: GridPoint, target: GridPoint, weapon: Weapon): FiringSolution {
    val east = (target.x - firing.x) * METERS_PER_GRID
    val north = (target.y - firing.y) * METERS_PER_GRID
    val range = hypot(east, north)
    val bearing = (Math.toDegrees(atan2(east, north)) + 360.0) % 360.0
    return FiringSolution(
        east = east,
        north = north,
        rangeMeters = range,
        bearingDegrees = bearing,
        direction = COMPASS[(jsRound(bearing / 45.0) % 8L).toInt()],
        elevationMils = elevationMilsFor(range),
        error = rangeError(range, weapon.minRange, weapon.maxRange),
    )
}

/**
 * Parses one coordinate field. Returns null for blank or non-numeric input, so
 * that an incomplete form shows no solution at all. A comma is accepted as the
 * decimal separator because Android decimal keyboards emit one in most locales.
 */
fun parseCoordinate(raw: String): Double? {
    val trimmed = raw.trim().replace(',', '.')
    if (trimmed.isEmpty()) return null
    val value = trimmed.toDoubleOrNull() ?: return null
    return if (value.isFinite()) value else null
}
