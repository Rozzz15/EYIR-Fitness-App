package com.example.eyirfitnessapp

import android.Manifest
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.example.eyirfitnessapp.data.repository.AppRepository
import com.example.eyirfitnessapp.ui.theme.EYIRFitnessAppTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Covers the Quick Add entries that [AppE2eTest] doesn't reach: the barcode
 * screen (camera permission + lookup + log) and the Quick Calories sheet
 * (spec §3 Quick Add menu).
 */
@RunWith(AndroidJUnit4::class)
class QuickAddBarcodeTest {

    /** Pre-granted so the scanner opens straight into the camera, not the dialog. */
    @get:Rule
    val cameraPermission: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.CAMERA)

    @get:Rule
    val composeRule = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val repo get() = AppRepository.get(context)

    @Before
    fun reset() = runBlocking {
        repo.wipeAllData()
        repo.seedIfEmpty()
    }

    @Test
    fun barcodeScreenScansLooksUpAndLogsSeededProduct() {
        launchAndFinishOnboarding()

        composeRule.onNodeWithContentDescription("Quick add").performClick()
        composeRule.onNodeWithText("Scan Barcode").performClick()

        // Permission was pre-granted, so the camera must actually bind — the
        // fallback cards only appear when it can't.
        composeRule.onAllNodesWithText("Camera access is off").assertCountEquals(0)
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodes(hasText("Scanning…", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Enter barcode").assertIsDisplayed()

        composeRule.onNodeWithText("Barcode digits").performTextInput("1000000000015")
        composeRule.onNodeWithText("Look up").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Cheddar Crackers").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Log it").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            runBlocking { repo.logsForDate(repo.today()) }
                .any { it.name == "Cheddar Crackers" }
        }
    }

    @Test
    fun quickCaloriesLogsDirectlyAndConfirms() {
        launchAndFinishOnboarding()

        composeRule.onNodeWithContentDescription("Quick add").performClick()
        composeRule.onNodeWithText("Quick Calories").performClick()

        composeRule.onNodeWithText("Calories *").performTextInput("450")
        composeRule.onNodeWithText("Log 450 kcal").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            runBlocking { repo.logsForDate(repo.today()) }
                .any { it.name == "Quick add" && it.calories == 450.0 }
        }
        // Spec §6 Rule 5: the sheet closes on save, so the confirmation has to
        // surface somewhere the user can still see it.
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Added 450 kcal to Breakfast")
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** Fresh device → onboarding wizard → Today (the FAB only shows there). */
    private fun launchAndFinishOnboarding() {
        composeRule.setContent {
            EYIRFitnessAppTheme {
                RootApp(repo)
            }
        }
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("Welcome!").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Continue").performClick()
        composeRule.onNodeWithText("Your name").performTextInput("QA User")
        composeRule.onNodeWithText("Continue").performClick()
        composeRule.onNodeWithText("Your body metrics").assertIsDisplayed()
        composeRule.onNodeWithText("Continue").performClick()
        composeRule.onNodeWithText("Your goal").assertIsDisplayed()
        composeRule.onNodeWithText("Continue").performClick()
        composeRule.onNodeWithText("Start tracking").performClick()
        composeRule.waitForIdle()
    }
}
