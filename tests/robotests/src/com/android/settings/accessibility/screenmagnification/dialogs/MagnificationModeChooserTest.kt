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
import android.app.settings.SettingsEnums
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.provider.Settings
import android.widget.AdapterView
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentResultListener
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragment
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.android.settings.accessibility.AccessibilityUtil
import com.android.settings.accessibility.MagnificationCapabilities
import com.android.settings.accessibility.MagnificationCapabilities.MagnificationMode
import com.android.settings.accessibility.screenmagnification.dialogs.MagnificationModeChooser.Companion.getCheckedModeFromResult
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameters
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
import org.robolectric.RobolectricTestParameterInjector
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowDialog
import org.robolectric.shadows.ShadowListView
import org.robolectric.shadows.ShadowLooper

/** Tests for Magnification mode chooser dialog */
@RunWith(RobolectricTestParameterInjector::class)
class MagnificationModeChooserTest {
    @get:Rule val mockito = MockitoJUnit.rule()
    private val requestKey = "requestFromTest"
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var fragmentScenario: FragmentScenario<Fragment>
    private lateinit var fragment: Fragment
    @Mock private lateinit var mockFragResultListener: FragmentResultListener
    @Captor lateinit var responseCaptor: ArgumentCaptor<Bundle>

    @Before
    fun setUp() {
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
    fun launchDialog_verifyTitle() {
        val alertDialog = shadowOf(launchDialog())

        assertThat(alertDialog.title.toString())
            .isEqualTo(context.getString(R.string.accessibility_magnification_mode_dialog_title))
    }

    @Test
    fun launchDialog_verifySummary() {
        val alertDialog = launchDialog()
        val listView: ShadowListView = shadowOf(getListViewInDialog(alertDialog))

        assertThat(listView.headerViews.count()).isEqualTo(1)
        val summaryView: TextView =
            listView.headerViews[0].requireViewById(R.id.accessibility_dialog_header_text_view)
        assertThat(summaryView.text.toString())
            .isEqualTo(
                context.getString(R.string.accessibility_magnification_area_settings_message)
            )
    }

    @Test
    fun launchDialog_verifyListAdapterData() {
        val alertDialog = launchDialog()
        val listView: ListView = getListViewInDialog(alertDialog)

        val adapter = listView.adapter
        val headerCounts = listView.headerViewsCount
        val optionsCount = 3

        assertThat(adapter.count).isEqualTo(headerCounts + optionsCount)

        val modeFullScreen = adapter.getItem(headerCounts) as MagnificationModeInfo
        val modeWindow = adapter.getItem(headerCounts + 1) as MagnificationModeInfo
        val modeAll = adapter.getItem(headerCounts + 2) as MagnificationModeInfo

        assertFullScreenModeInfo(modeFullScreen)
        assertWindowModeInfo(modeWindow)
        assertAllModeInfo(modeAll)
    }

    private fun assertAllModeInfo(info: MagnificationModeInfo) {
        assertThat(info.mTitle.toString())
            .isEqualTo(
                context.getString(R.string.accessibility_magnification_mode_dialog_option_switch)
            )
        assertThat(info.mSummary.toString())
            .isEqualTo(
                context.getString(
                    R.string.accessibility_magnification_area_settings_mode_switch_summary
                )
            )
        assertThat(info.mDrawableId).isEqualTo(R.drawable.accessibility_magnification_mode_switch)
        assertThat(info.mode).isEqualTo(MagnificationMode.ALL)
    }

    private fun assertWindowModeInfo(info: MagnificationModeInfo) {
        assertThat(info.mTitle.toString())
            .isEqualTo(
                context.getString(R.string.accessibility_magnification_mode_dialog_option_window)
            )
        assertThat(info.mSummary).isNull()
        assertThat(info.mDrawableId).isEqualTo(R.drawable.accessibility_magnification_mode_window)
        assertThat(info.mode).isEqualTo(MagnificationMode.WINDOW)
    }

    private fun assertFullScreenModeInfo(info: MagnificationModeInfo) {
        assertThat(info.mTitle.toString())
            .isEqualTo(
                context.getString(
                    R.string.accessibility_magnification_mode_dialog_option_full_screen
                )
            )
        assertThat(info.mSummary).isNull()
        assertThat(info.mDrawableId)
            .isEqualTo(R.drawable.accessibility_magnification_mode_fullscreen)
        assertThat(info.mode).isEqualTo(MagnificationMode.FULLSCREEN)
    }

    @Test
    fun launchDialog_verifyButtonsText() {
        val alertDialog = launchDialog() as AlertDialog
        val positiveBtn = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE)
        val negativeBtn = alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE)

        assertThat(positiveBtn.text.toString()).isEqualTo(context.getString(R.string.save))
        assertThat(negativeBtn.text.toString()).isEqualTo(context.getString(R.string.cancel))
    }

    @Test
    @TestParameters(
        customName = "fullScreen",
        value = ["{initialMode: ${MagnificationMode.FULLSCREEN}}"],
    )
    @TestParameters(customName = "window", value = ["{initialMode: ${MagnificationMode.WINDOW}}"])
    @TestParameters(customName = "all", value = ["{initialMode: ${MagnificationMode.ALL}}"])
    fun launchDialog_verifyInitialModeSetCorrectly(@MagnificationMode initialMode: Int) {
        MagnificationCapabilities.setCapabilities(context, initialMode)

        val dialog = launchDialog()
        val listView = getListViewInDialog(dialog)

        assertThat(getCheckedMode(listView)).isEqualTo(initialMode)
    }

    @Test
    fun configurationChange_checkedItemUiPersists() {
        launchDialogAndChecked(
            initialMode = MagnificationMode.WINDOW,
            checkedMode = MagnificationMode.ALL,
        )
        ShadowDialog.reset()

        // configuration change
        fragmentScenario.recreate().moveToState(Lifecycle.State.RESUMED)
        val dialog = ShadowDialog.getLatestDialog()
        val listView = getListViewInDialog(dialog)

        assertThat(getCheckedMode(listView)).isEqualTo(MagnificationMode.ALL)
    }

    @Test
    fun configurationChange_magnificationModeSettingUnchanged() {
        launchDialogAndChecked(
            initialMode = MagnificationMode.WINDOW,
            checkedMode = MagnificationMode.ALL,
        )

        // configuration change
        fragmentScenario.recreate().moveToState(Lifecycle.State.RESUMED)

        assertThat(MagnificationCapabilities.getCapabilities(context))
            .isEqualTo(MagnificationMode.WINDOW)
    }

    @Test
    fun checkModeAndSave_modeSettingUpdates() {
        launchDialogAndChecked(
            initialMode = MagnificationMode.WINDOW,
            checkedMode = MagnificationMode.ALL,
        )

        val alertDialog = ShadowDialog.getLatestDialog() as AlertDialog
        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick()
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        assertThat(MagnificationCapabilities.getCapabilities(context))
            .isEqualTo(MagnificationMode.ALL)
        verify(mockFragResultListener).onFragmentResult(eq(requestKey), responseCaptor.capture())
        val response = responseCaptor.value
        assertThat(getCheckedModeFromResult(response)).isEqualTo(MagnificationMode.ALL)
    }

    @Test
    fun checkModeAndCancel_modeSettingUnchanged() {
        launchDialogAndChecked(
            initialMode = MagnificationMode.WINDOW,
            checkedMode = MagnificationMode.ALL,
        )

        val alertDialog = ShadowDialog.getLatestDialog() as AlertDialog
        alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE).performClick()
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        assertThat(MagnificationCapabilities.getCapabilities(context))
            .isEqualTo(MagnificationMode.WINDOW)
        verify(mockFragResultListener, never()).onFragmentResult(eq(requestKey), any())
    }

    @Test
    fun tripleTapShortcutEnabled_checkWindowModeAndSave_showWarningDialog() {
        Settings.Secure.putInt(
            context.contentResolver,
            Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_ENABLED,
            AccessibilityUtil.State.ON,
        )

        launchDialogAndChecked(
            initialMode = MagnificationMode.FULLSCREEN,
            checkedMode = MagnificationMode.WINDOW,
        )
        val alertDialog = ShadowDialog.getLatestDialog() as AlertDialog
        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick()
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        assertThat(isTripleTapWarningDialogShown()).isTrue()
    }

    @Test
    fun tripleTapShortcutDisabled_checkWindowModeAndSave_doNotShowWarningDialog() {
        Settings.Secure.putInt(
            context.contentResolver,
            Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_ENABLED,
            AccessibilityUtil.State.OFF,
        )

        launchDialogAndChecked(
            initialMode = MagnificationMode.FULLSCREEN,
            checkedMode = MagnificationMode.WINDOW,
        )
        val alertDialog = ShadowDialog.getLatestDialog() as AlertDialog
        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick()
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        assertThat(isTripleTapWarningDialogShown()).isFalse()
    }

    @Test
    fun getMetricsCategory() {
        assertThat(MagnificationModeChooser().metricsCategory)
            .isEqualTo(SettingsEnums.DIALOG_MAGNIFICATION_CAPABILITY)
    }

    private fun launchDialog(): Dialog {
        MagnificationModeChooser.showDialog(
            fragmentManager = fragment.childFragmentManager,
            requestKey = requestKey,
        )
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        return ShadowDialog.getLatestDialog()
    }

    private fun launchDialogAndChecked(
        @MagnificationMode initialMode: Int,
        @MagnificationMode checkedMode: Int,
    ) {
        MagnificationCapabilities.setCapabilities(context, initialMode)
        var dialog = launchDialog()
        var listView = getListViewInDialog(dialog)
        val adapter = listView.adapter

        for (i in 0 until listView.count) {
            (adapter.getItem(i) as? MagnificationModeInfo)?.run {
                if (this.mode == checkedMode) {
                    listView.setItemChecked(i, true)
                    return
                }
            }
        }
    }

    private fun getListViewInDialog(dialog: Dialog): ListView {
        return dialog.requireViewById<ListView>(android.R.id.list)
    }

    @MagnificationMode
    private fun getCheckedMode(listView: ListView): Int {
        return listView.checkedItemPosition.let {
            if (it == AdapterView.INVALID_POSITION) {
                    null
                } else {
                    listView.adapter.getItem(it) as? MagnificationModeInfo
                }
                ?.mode ?: MagnificationMode.NONE
        }
    }

    private fun isTripleTapWarningDialogShown(): Boolean {
        val tripleTapDialogTitle =
            context.getString(R.string.accessibility_magnification_triple_tap_warning_title)
        val dialog = ShadowDialog.getLatestDialog()
        if (dialog != null) {
            return shadowOf(dialog).title == tripleTapDialogTitle
        }
        return false
    }
}
