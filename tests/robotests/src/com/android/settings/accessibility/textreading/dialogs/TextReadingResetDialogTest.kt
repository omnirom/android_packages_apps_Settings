/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.accessibility.textreading.dialogs

import android.app.settings.SettingsEnums
import android.content.Context
import android.content.DialogInterface
import android.provider.Settings
import androidx.appcompat.R
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragment
import androidx.test.core.app.ApplicationProvider
import com.android.settings.accessibility.shared.dialogs.DisableAccessibilityServiceDialogFragment
import com.android.settings.accessibility.textreading.data.BoldTextDataStore
import com.android.settings.accessibility.textreading.data.DisplaySizeDataStore
import com.android.settings.accessibility.textreading.data.FontSizeDataStore
import com.android.settings.accessibility.textreading.data.OutlineTextDataStore
import com.android.settings.accessibility.textreading.ui.BoldTextPreference
import com.android.settings.accessibility.textreading.ui.DisplaySizePreference
import com.android.settings.accessibility.textreading.ui.FontSizePreference
import com.android.settings.accessibility.textreading.ui.OutlineTextPreference
import com.android.settings.testutils.SettingsStoreRule
import com.android.settings.testutils.shadow.ShadowAlertDialogCompat
import com.google.common.truth.Truth.assertThat
import kotlin.math.abs
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog
import org.robolectric.shadows.ShadowLooper

/** Tests for [TextReadingResetDialog]. */
@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShadowAlertDialogCompat::class])
class TextReadingResetDialogTest {
    @get:Rule val settingsStoreRule = SettingsStoreRule()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var fragmentScenario: FragmentScenario<Fragment>
    private lateinit var fragment: Fragment

    @Before
    fun setUp() {
        fragmentScenario = launchFragment(themeResId = R.style.Theme_AppCompat)
        fragmentScenario.onFragment { frag -> fragment = frag }
    }

    @After
    fun cleanUp() {
        fragmentScenario.close()
    }

    @Test
    fun showDialog_verifyUI() {
        val alertDialog = launchDialog()
        val shadowAlertDialog = ShadowAlertDialogCompat.shadowOf(alertDialog)

        assertThat(shadowAlertDialog.title.toString())
            .isEqualTo(
                context.getString(
                    com.android.settings.R.string.accessibility_text_reading_confirm_dialog_title
                )
            )
        assertThat(shadowAlertDialog.message.toString())
            .isEqualTo(
                context.getString(
                    com.android.settings.R.string.accessibility_text_reading_confirm_dialog_message
                )
            )
        assertThat(alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).text.toString())
            .isEqualTo(
                context.getString(
                    com.android.settings.R.string
                        .accessibility_text_reading_confirm_dialog_reset_button
                )
            )
        assertThat(alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).text.toString())
            .isEqualTo(context.getString(com.android.settings.R.string.cancel))
    }

    @Test
    fun clickPositiveButton_settingsReset() {
        val fontSizeDataStore = FontSizeDataStore(context)
        val displaySizeDataStore = DisplaySizeDataStore(context)
        val boldTextDataStore = BoldTextDataStore(context)
        val outlineTextDataStore = OutlineTextDataStore(context)
        fontSizeDataStore.setInt(
            FontSizePreference.KEY,
            fontSizeDataStore.fontSizeData.value.values.size - 1,
        )
        displaySizeDataStore.setInt(
            DisplaySizePreference.KEY,
            displaySizeDataStore.displaySizeData.value.values.size - 1,
        )
        boldTextDataStore.setBoolean(BoldTextPreference.KEY, true)
        outlineTextDataStore.setBoolean(OutlineTextPreference.KEY, true)

        val alertDialog = launchDialog()

        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick()
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        assertThat(Settings.System.getString(context.contentResolver, Settings.System.FONT_SCALE))
            .isNull()
        assertThat(
                Settings.Secure.getInt(
                    context.contentResolver,
                    Settings.Secure.FONT_WEIGHT_ADJUSTMENT,
                )
            )
            .isEqualTo(0)
        assertThat(
                Settings.Secure.getInt(
                    context.contentResolver,
                    Settings.Secure.ACCESSIBILITY_HIGH_TEXT_CONTRAST_ENABLED,
                )
            )
            .isEqualTo(0)
    }

    @Test
    fun clickNegativeButton_settingsUnchanged() {
        val fontSizeDataStore = FontSizeDataStore(context)
        val displaySizeDataStore = DisplaySizeDataStore(context)
        val boldTextDataStore = BoldTextDataStore(context)
        val outlineTextDataStore = OutlineTextDataStore(context)
        fontSizeDataStore.setInt(
            FontSizePreference.KEY,
            fontSizeDataStore.fontSizeData.value.values.size - 1,
        )
        displaySizeDataStore.setInt(
            DisplaySizePreference.KEY,
            displaySizeDataStore.displaySizeData.value.values.size - 1,
        )
        boldTextDataStore.setBoolean(BoldTextPreference.KEY, true)
        outlineTextDataStore.setBoolean(OutlineTextPreference.KEY, true)

        val alertDialog = launchDialog()

        alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE).performClick()
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        assertThat(fontSizeDataStore.getInt(FontSizePreference.KEY))
            .isEqualTo(fontSizeDataStore.fontSizeData.value.values.size - 1)
        assertThat(displaySizeDataStore.getInt(DisplaySizePreference.KEY))
            .isEqualTo(displaySizeDataStore.displaySizeData.value.values.size - 1)
        assertThat(boldTextDataStore.getBoolean(BoldTextPreference.KEY)).isTrue()
        assertThat(outlineTextDataStore.getBoolean(OutlineTextPreference.KEY)).isTrue()
    }

    @Test
    fun getMetricsCategory() {
        assertThat(DisableAccessibilityServiceDialogFragment().metricsCategory)
            .isEqualTo(SettingsEnums.DIALOG_ACCESSIBILITY_SERVICE_DISABLE)
    }

    private fun launchDialog(): AlertDialog {
        TextReadingResetDialog.showDialog(fragmentManager = fragment.childFragmentManager)
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        return ShadowDialog.getLatestDialog() as AlertDialog
    }

    private fun FloatArray.indexOf(targetValue: Float): Int {
        if (this.isEmpty()) {
            return -1
        }

        var closestIndex = 0
        var minDifference = abs(this[0] - targetValue)

        for (i in 1 until this.size) {
            val currentDifference = abs(this[i] - targetValue)
            if (currentDifference < minDifference) {
                minDifference = currentDifference
                closestIndex = i
            }
        }

        return closestIndex
    }
}
