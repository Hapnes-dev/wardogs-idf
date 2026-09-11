package dev.hapnes.wardogsidf

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

/**
 * Drives the real app on a device or emulator and writes a screenshot of each
 * state to the app's internal files directory. CI collects them with `run-as`,
 * which works on a debuggable build; scoped storage blocks adb from reading
 * `Android/data` on API 30 and above, so the external directory is no use here.
 */
@RunWith(AndroidJUnit4::class)
class IdfCalculatorUiTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun anEmptyFormAsksForCoordinates() {
        rule.onNodeWithText("WAITING FOR COORDINATES").assertExists()
        rule.onNodeWithText("ENTER ALL FOUR COORDINATES").assertExists()
        capture("01-waiting")
    }

    @Test
    fun anL81SolutionMatchesTheWebCalculator() {
        enterCoordinates(firing = "0" to "0", target = "3" to "4")

        rule.onNodeWithTag("solution").performScrollTo()
        rule.onNodeWithText("READY").assertExists()
        rule.onNodeWithText("500").assertExists()
        rule.onNodeWithText("037").assertExists()
        rule.onNodeWithText("NE").assertExists()
        rule.onNodeWithText("465").assertExists()

        capture("02-l81-ready")
    }

    @Test
    fun aTargetInsideTheMinimumRangeIsRefused() {
        enterCoordinates(firing = "0" to "0", target = "0" to "1")

        rule.onNodeWithTag("solution").performScrollTo()
        rule.onNodeWithText("OUT OF RANGE").assertExists()
        rule.onNodeWithText("TARGET TOO CLOSE", substring = true).assertExists()
        rule.onNodeWithText("—").assertExists()

        capture("03-too-close")
    }

    @Test
    fun theSph2ReachesWhatTheL81Cannot() {
        rule.onNodeWithText("SPH-2 Artillery").performClick()
        enterCoordinates(firing = "0" to "0", target = "0" to "10")

        rule.onNodeWithTag("solution").performScrollTo()
        rule.onNodeWithText("READY").assertExists()
        rule.onNodeWithText("1,000").assertExists()
        rule.onNodeWithText("000").assertExists()
        rule.onNodeWithText("N").assertExists()
        rule.onNodeWithText("SPH-2").assertExists()

        capture("04-sph2-ready")
    }

    private fun enterCoordinates(firing: Pair<String, String>, target: Pair<String, String>) {
        rule.onNodeWithTag("firing-x").performTextInput(firing.first)
        rule.onNodeWithTag("firing-y").performTextInput(firing.second)
        rule.onNodeWithTag("target-x").performTextInput(target.first)
        rule.onNodeWithTag("target-y").performTextInput(target.second)
        runCatching { Espresso.closeSoftKeyboard() }
        rule.waitForIdle()
        Thread.sleep(500)
    }

    private fun capture(name: String) {
        val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.filesDir, "screenshots").apply { mkdirs() }
        FileOutputStream(File(directory, "$name.png")).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }
}
