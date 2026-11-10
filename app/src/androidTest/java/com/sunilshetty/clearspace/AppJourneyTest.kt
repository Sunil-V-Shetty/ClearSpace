package com.sunilshetty.clearspace

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppJourneyTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()
    @Before fun awaitHome() {
        ui.waitUntil(15_000) {
            runCatching { ui.onAllNodesWithTag("nav_tools").fetchSemanticsNodes().isNotEmpty() }.getOrDefault(false)
        }
        ui.waitUntil(15_000) {
            ui.onAllNodesWithText("Clear Space").fetchSemanticsNodes().isNotEmpty()
        }
    }
    private fun openTool(name: String) {
        ui.onNodeWithTag("nav_tools").performClick()
        ui.onNodeWithText(name, ignoreCase = true).performScrollTo().performClick()
    }
    private fun back() { ui.onNodeWithContentDescription("Back").performClick() }

    @Test fun everyToolOpensAndReturnsWithoutPermissionRedirects() {
        listOf("Device diagnostics", "Insights", "Reclaimable", "Junk files", "Duplicates",
            "Unused apps", "Large files", "Trash").forEach { name ->
            openTool(name)
            ui.onNodeWithContentDescription("Back").assertExists()
            back()
        }
    }
    @Test fun touchscreenCheckRequiresAllTilesAndRetainsResult() {
        openTool("Device diagnostics")
        ui.onNodeWithTag("test_Touchscreen").performScrollTo().performClick()
        ui.onNodeWithText("Passed").assertIsNotEnabled()
        repeat(9) { ui.onNodeWithTag("touch_$it").performClick() }
        ui.onNodeWithText("Passed").assertIsEnabled().performClick()
        ui.onNodeWithText("Passed by you").assertExists()
        ui.activityRule.scenario.recreate()
        ui.onNodeWithText("Passed by you").assertExists()
    }
    @Test fun displayCheckAndSettingsThemeSurviveRecreation() {
        openTool("Device diagnostics")
        ui.onNodeWithTag("test_Display").performScrollTo().performClick()
        ui.onNodeWithText("Passed").assertIsNotEnabled()
        repeat(4) { ui.onNodeWithText("Next color", substring = true).performClick() }
        ui.onNodeWithText("Passed").performClick()
        back()
        ui.onNodeWithContentDescription("Settings").performClick()
        val toggle = ui.onNode(isToggleable())
        val original = toggle.fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.ToggleableState]
        toggle.performClick()
        val checked = if (original == androidx.compose.ui.state.ToggleableState.On) androidx.compose.ui.state.ToggleableState.Off else androidx.compose.ui.state.ToggleableState.On
        ui.waitUntil(5_000) { toggle.fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.ToggleableState] == checked }
        ui.activityRule.scenario.recreate()
        ui.waitUntil(5_000) { runCatching { ui.onNode(isToggleable()).fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.ToggleableState] == checked }.getOrDefault(false) }
        ui.onNode(isToggleable()).assert(androidx.compose.ui.test.SemanticsMatcher.expectValue(
            androidx.compose.ui.semantics.SemanticsProperties.ToggleableState, checked))
    }
    @Test fun quickScanFinishesAndReturnsToHome() {
        ui.onAllNodesWithText("Quick Scan")[0].performScrollTo().performClick()
        ui.onAllNodesWithText("Quick Scan").onLast().performClick()
        ui.waitUntil(20_000) { runCatching { ui.onAllNodesWithText("Scan complete").fetchSemanticsNodes().isNotEmpty() }.getOrDefault(false) }
        ui.onNodeWithText("Continue").performClick()
        back()
        ui.onNodeWithText("Clear Space").assertExists()
    }

    @Test fun redesignedHomeLinksAndArtworkLicenseWork() {
        ui.onNodeWithText("Free up space").assertIsDisplayed()
        ui.onNodeWithText("Storage details").performScrollTo().performClick()
        ui.onNodeWithContentDescription("Back").assertExists()
        back()
        ui.onNodeWithText("Device diagnostics").performScrollTo().performClick()
        ui.onNodeWithText("Hardware checks").assertIsDisplayed()
        back()
        ui.onNodeWithContentDescription("Settings").performClick()
        ui.onNodeWithText("Open-source artwork").performScrollTo().performClick()
        ui.onNodeWithText("Open-source licenses").assertIsDisplayed()
        ui.onNodeWithText("Material Design icons by Google", substring = true).assertExists()
        ui.onNodeWithText("Close", substring = false).performClick()
    }

    @Test fun storageDetailsHaveSummaryAndUsefulNextSteps() {
        listOf("Storage details", "Free space").forEach { link ->
            ui.onNodeWithText(link).performScrollTo().performClick()
            ui.onNodeWithText("Total capacity:", substring = true).assertExists()
            ui.onNodeWithText("Available:", substring = true).assertExists()
            ui.onNodeWithText("Review cleanup").performScrollTo().assertIsDisplayed()
            ui.onNodeWithText("Media overview").performScrollTo().performClick()
            ui.onNodeWithText("Media").assertExists()
            back()
            back()
        }
    }

    @Test fun mediaOverviewAndAllFileCategoriesHaveContentWithoutAccess() {
        ui.onNodeWithText("Media overview").performScrollTo().performClick()
        ui.onNodeWithText("Media").assertIsDisplayed()
        ui.onNodeWithText("Scan media").assertExists()
        listOf("Photos", "Videos", "Downloads").forEach { category ->
            ui.onNodeWithText(category, ignoreCase = true).performScrollTo().performClick()
            ui.onNodeWithText(category, ignoreCase = true).assertExists()
            ui.onNodeWithContentDescription("Back").assertExists()
            back()
        }
        ui.onNodeWithText("Scan media").performScrollTo().performClick()
        ui.onAllNodesWithText("Quick Scan").onLast().assertExists()
    }

    @Test fun speakerAndVibrationControlsSupportIssueReporting() {
        openTool("Device diagnostics")
        listOf("Speaker" to "Play tone", "Vibration" to "Start vibration").forEachIndexed { index, (test, action) ->
            ui.onNodeWithTag("test_$test").performScrollTo().performClick()
            ui.onNodeWithText("Passed").assertIsNotEnabled()
            ui.onNodeWithText(action).performClick()
            ui.onNodeWithText("Found an issue").performClick()
            ui.onAllNodesWithText("Issue reported by you").assertCountEquals(index + 1)
        }
    }

    @Test fun cleanupReviewIsProminentAndShortcutsOpenTheirTools() {
        ui.onNodeWithText("Review cleanup").assertIsDisplayed().performClick()
        ui.onNodeWithText("Review your cleanup").assertIsDisplayed()
        back()
        listOf("Junk files", "Duplicates", "Large files").forEach { label ->
            ui.onNodeWithText(label).performScrollTo().performClick()
            ui.onNodeWithContentDescription("Back").assertExists()
            back()
        }
    }
}
