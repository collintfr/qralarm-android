package com.sweak.qralarm.features.add_edit_alarm

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasAnySibling
import androidx.compose.ui.test.isSelectable
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sweak.qralarm.core.designsystem.theme.QRAlarmTheme
import com.sweak.qralarm.core.domain.alarm.DismissalMethod
import com.sweak.qralarm.features.add_edit_alarm.components.DismissalSettings
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DismissalSettingsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun selectsOneObjectFromTheSearchableCatalogAndSwitchesMethods() {
        val method = mutableStateOf(DismissalMethod.CODE)
        val category = mutableStateOf<String?>(null)
        compose.setContent {
            QRAlarmTheme {
                DismissalSettings(method.value, category.value, false,
                    onMethodSelected = { method.value = it },
                    onObjectSelected = { category.value = it })
            }
        }
        compose.onNodeWithText("Recognize an object").performClick()
        compose.onNodeWithText("Search objects").assertIsDisplayed()
        compose.onNodeWithText("Search objects").performTextInput("tooth")
        compose.onNodeWithText("Toothbrush").performClick()
        compose.onNodeWithText("Toothbrush").assertIsDisplayed()
        compose.runOnIdle { assertEquals("toothbrush", category.value) }
        // Tapping the selected method again must reopen the picker to change the target.
        compose.onNodeWithText("Recognize an object").performClick()
        compose.onNodeWithText("Search objects").performTextInput("sink")
        compose.onNodeWithText("Sink").performClick()
        compose.runOnIdle { assertEquals("sink", category.value) }
        compose.onNodeWithText("Stop button").performClick()
        compose.onNodeWithText("Sink").assertDoesNotExist()
        compose.runOnIdle { assertEquals(DismissalMethod.NONE, method.value) }
    }

    @Test fun tappingTheRadioButtonOpensTheObjectPicker() {
        val method = mutableStateOf(DismissalMethod.CODE)
        compose.setContent {
            QRAlarmTheme {
                DismissalSettings(method.value, null, false, { method.value = it }, {})
            }
        }
        compose.onNode(
            isSelectable() and hasAnySibling(hasText("Recognize an object")),
            useUnmergedTree = true
        ).performClick()
        compose.onNodeWithText("Search objects").assertIsDisplayed()
        compose.runOnIdle { assertEquals(DismissalMethod.OBJECT, method.value) }
    }

    @Test fun missingObjectExplainsWhySavingIsBlocked() {
        compose.setContent {
            QRAlarmTheme {
                DismissalSettings(DismissalMethod.OBJECT, null, true, {}, {})
            }
        }
        compose.onNodeWithText("Choose an object before saving this alarm.").assertIsDisplayed()
    }
}
