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

package com.android.settings.accessibility.screenmagnification.dialogs

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.text.SpannableString
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentResultListener
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragment
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.android.settings.accessibility.MagnificationCapabilities
import com.android.settings.accessibility.MagnificationCapabilities.MagnificationMode
import com.android.settings.accessibility.screenmagnification.dialogs.MagnificationModeChooser.Companion.getCheckedModeFromResult
import com.android.settings.testutils.AccessibilityTestUtils
import com.android.settings.utils.AnnotationSpan
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.junit.MockitoJUnit
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowDialog
import org.robolectric.shadows.ShadowLooper

/** Tests for [TripleTapWarningDialog] */
@RunWith(RobolectricTestRunner::class)
class TripleTapWarningDialogTest {
    @get:Rule val mockito = MockitoJUnit.rule()
    private val requestKey = "requestFromTest"
    private val initialModeInSetting = MagnificationMode.FULLSCREEN
    private val selectedMode = MagnificationMode.WINDOW
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var fragmentScenario: FragmentScenario<Fragment>
    private lateinit var fragment: Fragment
    @Mock private lateinit var mockFragResultListener: FragmentResultListener
    @Captor lateinit var responseCaptor: ArgumentCaptor<Bundle>

    @Before
    fun setUp() {
        MagnificationCapabilities.setCapabilities(context, initialModeInSetting)
        fragmentScenario = launchFragment(themeResId = androidx.appcompat.R.style.Theme_AppCompat)
        fragmentScenario.onFragment { frag ->
            fragment = frag
            fragment.childFragmentManager.setFragmentResultListener(
                requestKey,
                fragment,
                mockFragResultListener,
            )
        }
    }

    @After
    fun cleanUp() {
        fragmentScenario.close()
    }

    @Test
    fun launchDialog_verifyTitleText() {
        val alertDialog = shadowOf(launchDialog())
        assertThat(alertDialog.title.toString())
            .isEqualTo(
                context.getString(R.string.accessibility_magnification_triple_tap_warning_title)
            )
    }

    @Test
    fun launchDialog_verifyButtonsText() {
        val alertDialog = launchDialog() as AlertDialog
        val positiveBtn = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE)
        val negativeBtn = alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE)

        assertThat(positiveBtn.text.toString())
            .isEqualTo(
                context.getString(
                    R.string.accessibility_magnification_triple_tap_warning_positive_button
                )
            )
        assertThat(negativeBtn.text.toString())
            .isEqualTo(
                context.getString(
                    R.string.accessibility_magnification_triple_tap_warning_negative_button
                )
            )
    }

    @Test
    fun launchDialog_verifyContentText() {
        val alertDialog = launchDialog() as AlertDialog
        val messageView: TextView = alertDialog.requireViewById(R.id.message)
        assertThat(messageView.text.toString())
            .isEqualTo(
                context.getString(R.string.accessibility_magnification_triple_tap_warning_message)
            )
    }

    @Test
    fun clickPositiveButton_saveSelectedModeSetting() {
        val alertDialog = launchDialog() as AlertDialog

        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick()
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        assertThat(MagnificationCapabilities.getCapabilities(context)).isEqualTo(selectedMode)
        verify(mockFragResultListener).onFragmentResult(eq(requestKey), responseCaptor.capture())
        val response = responseCaptor.value
        assertThat(getCheckedModeFromResult(response)).isEqualTo(selectedMode)
    }

    @Test
    fun clickNegativeButton_selectedModeSettingUnchanged() {
        val alertDialog = launchDialog() as AlertDialog

        alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE).performClick()
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        assertThat(MagnificationCapabilities.getCapabilities(context))
            .isEqualTo(initialModeInSetting)
        verify(mockFragResultListener, never()).onFragmentResult(eq(requestKey), any())
    }

    @Test
    fun clickNegativeButton_showMagnificationModeChooser() {
        val alertDialog = launchDialog() as AlertDialog

        alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE).performClick()
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        val modeDialogTitle =
            context.getString(R.string.accessibility_magnification_mode_dialog_title)
        val dialog = ShadowDialog.getLatestDialog()
        assertThat(dialog).isNotNull()
        assertThat(shadowOf(dialog).title).isEqualTo(modeDialogTitle)
    }

    @Test
    fun clickChangeShortcuts_showEditShortcutScreen() {
        val alertDialog = launchDialog() as AlertDialog
        val messageView: TextView = alertDialog.requireViewById(R.id.message)
        val message = messageView.text
        assertThat(message).isInstanceOf(SpannableString::class.java)
        val spans =
            (message as SpannableString).getSpans<AnnotationSpan>(
                0,
                message.length,
                AnnotationSpan::class.java,
            )

        spans[0].onClick(messageView)

        AccessibilityTestUtils.assertEditShortcutsScreenShown(fragment)
    }

    private fun launchDialog(): Dialog {
        TripleTapWarningDialog.showDialog(
            fragmentManager = fragment.childFragmentManager,
            requestKey = requestKey,
            selectedMagnificationMode = selectedMode,
        )
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        return ShadowDialog.getLatestDialog()
    }
}
