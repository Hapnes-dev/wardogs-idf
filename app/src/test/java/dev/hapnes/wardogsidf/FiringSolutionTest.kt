package dev.hapnes.wardogsidf

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The expected values here are the ones the schaulers.com web calculator
 * produces for the same inputs, so a regression in the port shows up as a
 * failing test rather than as a bad fire mission.
 */
class FiringSolutionTest {

    @Test
    fun `three four grid offset gives a 500 m north-east solution`() {
        val solution = solve(GridPoint(0.0, 0.0), GridPoint(3.0, 4.0), Weapon.L81)

        assertEquals(300.0, solution.east, 1e-9)
        assertEquals(400.0, solution.north, 1e-9)
        assertEquals(500.0, solution.rangeMeters, 1e-9)
        assertEquals(36.8698976, solution.bearingDegrees, 1e-6)
        assertEquals("NE", solution.direction)
        assertEquals(465, solution.elevationMils)
        assertTrue(solution.isInRange)
    }

    @Test
    fun `due west target reads bearing 270`() {
        val solution = solve(GridPoint(5.0, 5.0), GridPoint(1.0, 5.0), Weapon.SPH2)

        assertEquals(270.0, solution.bearingDegrees, 1e-9)
        assertEquals("W", solution.direction)
        assertEquals(400.0, solution.rangeMeters, 1e-9)
    }

    @Test
    fun `due south target reads bearing 180`() {
        val solution = solve(GridPoint(0.0, 0.0), GridPoint(0.0, -3.0), Weapon.L81)

        assertEquals(180.0, solution.bearingDegrees, 1e-9)
        assertEquals("S", solution.direction)
    }

    @Test
    fun `target inside the L81 minimum range reports how far to move`() {
        val solution = solve(GridPoint(0.0, 0.0), GridPoint(0.0, 1.0), Weapon.L81)

        assertEquals("TARGET TOO CLOSE — MOVE AT LEAST 20 M FARTHER AWAY", solution.error)
        assertTrue(!solution.isInRange)
    }

    @Test
    fun `target past the SPH-2 maximum reports the overshoot`() {
        val solution = solve(GridPoint(0.0, 0.0), GridPoint(0.0, 30.0), Weapon.SPH2)

        assertEquals("TARGET OUT OF RANGE — 370 M BEYOND MAXIMUM", solution.error)
    }

    @Test
    fun `the same geometry can be in range for one weapon and not the other`() {
        val firing = GridPoint(0.0, 0.0)
        val target = GridPoint(0.0, 5.0)

        assertTrue(solve(firing, target, Weapon.L81).isInRange)
        assertTrue(!solve(firing, target, Weapon.SPH2).isInRange)
    }

    @Test
    fun `elevation matches the sight table at its own marks`() {
        assertEquals(500, elevationMilsFor(470.0))
        assertEquals(950, elevationMilsFor(80.0))
        assertEquals(120, elevationMilsFor(700.0))
    }

    @Test
    fun `elevation interpolates and snaps to five mils`() {
        assertEquals(465, elevationMilsFor(500.0))
        assertEquals(875, elevationMilsFor(121.0))
    }

    @Test
    fun `coordinates accept a comma as the decimal separator`() {
        assertEquals(12.5, parseCoordinate("12,5")!!, 1e-9)
        assertEquals(12.5, parseCoordinate(" 12.5 ")!!, 1e-9)
        assertEquals(-3.0, parseCoordinate("-3")!!, 1e-9)
    }

    @Test
    fun `blank and junk coordinates parse to null`() {
        assertNull(parseCoordinate(""))
        assertNull(parseCoordinate("   "))
        assertNull(parseCoordinate("abc"))
        assertNull(parseCoordinate("-"))
    }
}
