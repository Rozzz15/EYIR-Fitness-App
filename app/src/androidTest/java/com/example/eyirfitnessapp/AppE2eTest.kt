package com.example.eyirfitnessapp

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.eyirfitnessapp.data.repository.AppRepository
import com.example.eyirfitnessapp.domain.model.MeasurementPart
import com.example.eyirfitnessapp.ui.theme.EYIRFitnessAppTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * E2E walk-through on the real data layer (spec §32 E2E):
 * fresh device → onboarding → add breakfast → add exercise → add water →
 * add weight → verify progress. The F features (measurements, habits,
 * routines) ride the same flow after the classic steps.
 */
@RunWith(AndroidJUnit4::class)
class AppE2eTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val repo get() = AppRepository.get(context)

    @Before
    fun reset() = runBlocking {
        repo.wipeAllData()
    }

    @Test
    fun fullTrackingFlowFromOnboardingToProgress() {
        composeRule.setContent {
            EYIRFitnessAppTheme {
                RootApp(repo)
            }
        }

        // ── Onboarding (fresh device starts in the wizard) ──
        // Splash shows first (spec §21) and hands off after ~1.1s, so wait for
        // the hand-off instead of asserting in the same frame as setContent.
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("Welcome!").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Welcome!").assertIsDisplayed()
        composeRule.onNodeWithText("Continue").performClick()
        composeRule.onNodeWithText("Your name").performTextInput("E2E User")
        composeRule.onNodeWithText("Continue").performClick()
        composeRule.onNodeWithText("Your body metrics").assertIsDisplayed()
        composeRule.onNodeWithText("Continue").performClick()
        composeRule.onNodeWithText("Your goal").assertIsDisplayed()
        composeRule.onNodeWithText("Continue").performClick()
        composeRule.onNodeWithText("Start tracking").performClick()

        // ── Today is the hub ──
        // The header renders the profile name on its own (TodayScreen's
        // DashboardHeader), falling back to "Welcome back" until the profile
        // flow emits — so wait for the name rather than asserting one frame
        // after navigation. ("Today" also matches the nav tab, hence the name.)
        composeRule.waitForIdle()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("E2E User").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("E2E User").assertIsDisplayed()

        // ── Add breakfast (seeded food: Banana) ──
        openQuickAdd("Add Food")
        composeRule.onNodeWithText("Search foods").performTextInput("Banana")
        composeRule.waitUntil(timeoutMillis = 5_000) {
            // Match the merged result row ("Banana" + "… kcal") — the search
            // field's EditableText also contains "Banana" and must not match.
            composeRule.onAllNodes(hasText("Banana") and hasText("kcal", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodes(hasText("Banana") and hasText("kcal", substring = true))[0].performClick()
        composeRule.onNodeWithText("Add to diary").performClick()

        // ── Add exercise (the screen stays open after logging — go back explicitly) ──
        openQuickAdd("Log Exercise")
        // The exercise list is alphabetical + lazy — Walking sits below the fold.
        scrollToText("Walking (moderate)")
        composeRule.onNodeWithText("Walking (moderate)").performClick()
        composeRule.onNodeWithText("Log cardio").performClick()
        composeRule.onNodeWithContentDescription("Back").performClick()

        // ── Add water ──
        openQuickAdd("Log Water")
        composeRule.onNodeWithText("+250 ml").performClick()
        composeRule.onNodeWithContentDescription("Back").performClick()

        // ── Add weight ──
        openQuickAdd("Log Weight")
        composeRule.onNodeWithText("Weight (kg)").performTextInput("76.5")
        composeRule.onNodeWithText("Save weight").performClick()
        composeRule.onNodeWithContentDescription("Back").performClick()

        // ── Everything landed in the repository ──
        val today = repo.today()
        val foodLogs = runBlocking { repo.logsForDate(today) }
        assertTrue("food log should exist", foodLogs.isNotEmpty())
        val exerciseLogs = runBlocking { repo.observeExerciseLogs(today).first() }
        assertTrue("exercise log should exist", exerciseLogs.isNotEmpty())
        val water = runBlocking { repo.waterBetween(today, today) }
        assertEquals(250, water.sumOf { it.amountMl })
        val weight = runBlocking { repo.observeWeightLogs().first() }
        assertTrue("weight log should exist", weight.isNotEmpty())

        // Back on a tab screen — the bottom navigation is visible again.

        // ── Progress tab reflects the data ──
        // The nav icon's contentDescription lives in the unmerged tree.
        composeRule.onNodeWithContentDescription("Progress", useUnmergedTree = true).performClick()
        composeRule.onNodeWithText("Range").assertIsDisplayed()

        // ── F features: measurements ──
        scrollToText("Body measurements")
        composeRule.onNodeWithText("Body measurements").performClick()
        composeRule.onNodeWithText("New measurement").assertIsDisplayed()
        // The body map pushes the form further down the LazyColumn than before.
        composeRule.onNodeWithText("Size (cm) — Waist").performScrollTo().performTextInput("84")
        composeRule.onNodeWithText("Save measurement").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runBlocking { repo.observeMeasurements().first() }
                .any { it.part == MeasurementPart.WAIST }
        }

        // ── F features: habits ──
        composeRule.onNodeWithContentDescription("Back").performClick()
        scrollToText("Healthy habits")
        composeRule.onNodeWithText("Healthy habits").performClick()
        composeRule.onNodeWithText("Add a habit").assertIsDisplayed()
        composeRule.onNodeWithText("Water").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runBlocking { repo.observeHabits().first() }.any { it.key == "WATER" }
        }

        // ── F features: routines via the Exercise screen ──
        composeRule.onNodeWithContentDescription("Back").performClick()
        openQuickAdd("Log Exercise")
        composeRule.onNodeWithText("Workout routines").performClick()
        scrollToText("New routine")
        composeRule.onNodeWithText("New routine").performClick() // opens the draft editor
        composeRule.onNodeWithText("Routine name (e.g. Push Day)").performTextInput("Push Day")
        // A routine line is only valid with a name AND sets/reps (RoutineLogic),
        // so fill a complete strength entry before saving.
        composeRule.onAllNodesWithText("Name")[0].performTextInput("Bench Press")
        composeRule.onAllNodesWithText("Sets")[0].performTextInput("3")
        composeRule.onAllNodesWithText("Reps")[0].performTextInput("10")
        scrollToText("Save routine")
        composeRule.onNodeWithText("Save routine").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runBlocking { repo.observeRoutines().first() }.any { it.name == "Push Day" }
        }
    }

    /** Opens the FAB quick-add sheet and picks an action. */
    private fun openQuickAdd(label: String) {
        composeRule.onNodeWithContentDescription("Quick add").performClick()
        composeRule.onNodeWithText(label).performClick()
    }

    /** Scrolls the first vertical-scrollable container until [text] is composed. */
    private fun scrollToText(text: String) {
        composeRule.onAllNodes(hasScrollAction())[0]
            .performScrollToNode(hasText(text, substring = true))
    }
}
