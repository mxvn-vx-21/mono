package com.mono.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.mono.app.ui.navigation.FloatingNavBar
import com.mono.app.ui.theme.MonoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class NavBarTest {
    @get:Rule val rule = createComposeRule()

    @Test fun showsFourDestinationsAndCentreAction() {
        var centre = 0
        rule.setContent { MonoTheme { FloatingNavBar(null, false, {}, { centre++ }) } }
        listOf("Today", "Done", "Memo", "Settings", "Quick add").forEach { rule.onNodeWithContentDescription(it).assertIsDisplayed() }
        rule.onNodeWithContentDescription("Quick add").performClick()
        assertEquals(1, centre)
    }
}
