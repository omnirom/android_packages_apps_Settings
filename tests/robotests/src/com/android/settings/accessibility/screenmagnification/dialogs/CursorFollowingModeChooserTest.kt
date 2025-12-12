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
import android.provider.Settings.Secure.ACCESSIBILITY_MAGNIFICATION_CURSOR_FOLLOWING_MODE_CENTER
import android.provider.Settings.Secure.ACCESSIBILITY_MAGNIFICATION_CURSOR_FOLLOWING_MODE_CONTINUOUS
import android.provider.Settings.Secure.ACCESSIBILITY_MAGNIFICATION_CURSOR_FOLLOWING_MODE_EDGE
import android.provider.Settings.Secure.AccessibilityMagnificationCursorFollowingMode
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
import com.android.settings.accessibility.screenmagnification.dialogs.CursorFollowingModeChooser.Companion.getCheckedModeFromResult
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

private const val SETTING_KEY = Settings.Secure.ACCESSIBILITY_MAGNIFICATION_CURSOR_FOLLOWING_MODE

/** Tests for [CursorFollowingModeChooser] */
@RunWith(RobolectricTestParameterInjector::class)
class CursorFollowingModeChooserTest {
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
            .isEqualTo(
                context.getString(R.string.accessibility_magnification_cursor_following_title)
            )
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
                context.getString(R.string.accessibility_magnification_cursor_following_header)
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

        val modeContinuous = adapter.getItem(headerCounts) as CursorFollowingModeInfo
        val modeCenter = adapter.getItem(headerCounts + 1) as CursorFollowingModeInfo
        val modeEdge = adapter.getItem(headerCounts + 2) as CursorFollowingModeInfo

        assertContinuousModeInfo(modeContinuous)
        assertCenterModeInfo(modeCenter)
        assertEdgeModeInfo(modeEdge)
    }

    private fun assertContinuousModeInfo(info: CursorFollowingModeInfo) {
        assertThat(info.mTitle.toString())
            .isEqualTo(
                context.getString(R.string.accessibility_magnification_cursor_following_continuous)
            )
        assertThat(info.mode)
            .isEqualTo(ACCESSIBILITY_MAGNIFICATION_CURSOR_FOLLOWING_MODE_CONTINUOUS)
    }

    private fun assertCenterModeInfo(info: CursorFollowingModeInfo) {
        assertThat(info.mTitle.toString())
            .isEqualTo(
                context.getString(R.string.accessibility_magnification_cursor_following_center)
            )
        assertThat(info.mode).isEqualTo(ACCESSIBILITY_MAGNIFICATION_CURSOR_FOLLOWING_MODE_CENTER)
    }

    private fun assertEdgeModeInfo(info: CursorFollowingModeInfo) {
        assertThat(info.mTitle.toString())
            .isEqualTo(
                context.getString(R.string.accessibility_magnification_cursor_following_edge)
            )
        assertThat(info.mode).isEqualTo(ACCESSIBILITY_MAGNIFICATION_CURSOR_FOLLOWING_MODE_EDGE)
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
        customName = "continuous",
        value = ["{initialMode: ${ACCESSIBILITY_MAGNIFICATION_CURSOR_FOLLOWING_MODE_CONTINUOUS}}"],
    )
    @TestParameters(
        customName = "center",
        value = ["{initialMode: ${ACCESSIBILITY_MAGNIFICATION_CURSOR_FOLLOWING_MODE_CENTER}}"],
    )
    @TestParameters(
        customName = "edge",
        value = ["{initialMode: ${ACCESSIBILITY_MAGNIFICATION_CURSOR_FOLLOWING_MODE_EDGE}}"],
    )
    fun launchDialog_verifyInitialModeSetCorrectly(
        @AccessibilityMagnificationCursorFollowingMode initialMode: Int
    ) {
        val dialog = launchDialogAndChecked(
            initialMode = initialMode,
            checkedMode = null,
        )
        val listView = getListViewInDialog(dialog)

        assertThat(getCheckedMode(listView)).isEqualTo(initialMode)
    }

    @Test
    fun configurationChange_checkedItemUiPersists() {
        launchDialogAndChecked(
            initialMode = ACCESSIBILITY_MAGNIFICATION_CURSOR_FOLLOWING_MODE_CENTER,
            checkedMode = ACCESSIBILITY_MAGNIFICATION_CURSOR_FOLLOWING_MODE_EDGE,
        )
        ShadowDialog.reset()

        // configuration change
        fragmentScenario.recreate().moveToState(Lifecycle.State.RESUMED)
        val dialog = ShadowDialog.getLatestDialog()
        val listView = getListViewInDialog(dialog)

        assertThat(getCheckedMode(listView))
            .isEqualTo(ACCESSIBILITY_MAGNIFICATION_CURSOR_FOLLOWING_MODE_EDGE)
    }

    @Test
    fun configurationChange_magnificationModeSettingUnchanged() {
        launchDialogAndChecked(
            initialMode = ACCESSIBILITY_MAGNIFICATION_CURSOR_FOLLOWING_MODE_CENTER,
            checkedMode = ACCESSIBILITY_MAGNIFICATION_CURSOR_FOLLOWING_MODE_EDGE,
        )

        // configuration change
        fragmentScenario.recreate().moveToState(Lifecycle.State.RESUMED)

        assertThat(Settings.Secure.getInt(context.contentResolver, SETTING_KEY))
            .isEqualTo(ACCESSIBILITY_MAGNIFICATION_CURSOR_FOLLOWING_MODE_CENTER)
    }

    @Test
    fun checkModeAndSave_modeSettingUpdates() {
        launchDialogAndChecked(
            initialMode = ACCESSIBILITY_MAGNIFICATION_CURSOR_FOLLOWING_MODE_CENTER,
            checkedMode = ACCESSIBILITY_MAGNIFICATION_CURSOR_FOLLOWING_MODE_EDGE,
        )

        val alertDialog = ShadowDialog.getLatestDialog() as AlertDialog
        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick()
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        assertThat(Settings.Secure.getInt(context.contentResolver, SETTING_KEY))
            .isEqualTo(ACCESSIBILITY_MAGNIFICATION_CURSOR_FOLLOWING_MODE_EDGE)
        verify(mockFragResultListener).onFragmentResult(eq(requestKey), responseCaptor.capture())
        val response = responseCaptor.value
        assertThat(getCheckedModeFromResult(response))
            .isEqualTo(ACCESSIBILITY_MAGNIFICATION_CURSOR_FOLLOWING_MODE_EDGE)
    }

    @Test
    fun checkModeAndCancel_modeSettingUnchanged() {
        launchDialogAndChecked(
            initialMode = ACCESSIBILITY_MAGNIFICATION_CURSOR_FOLLOWING_MODE_CENTER,
            checkedMode = ACCESSIBILITY_MAGNIFICATION_CURSOR_FOLLOWING_MODE_EDGE,
        )

        val alertDialog = ShadowDialog.getLatestDialog() as AlertDialog
        alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE).performClick()
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        assertThat(Settings.Secure.getInt(context.contentResolver, SETTING_KEY))
            .isEqualTo(ACCESSIBILITY_MAGNIFICATION_CURSOR_FOLLOWING_MODE_CENTER)
        verify(mockFragResultListener, never()).onFragmentResult(eq(requestKey), any())
    }

    @Test
    fun getMetricsCategory() {
        assertThat(CursorFollowingModeChooser().metricsCategory)
            .isEqualTo(SettingsEnums.DIALOG_MAGNIFICATION_CURSOR_FOLLOWING)
    }

    private fun launchDialog(): Dialog {
        return launchDialogAndChecked(
            initialMode = ACCESSIBILITY_MAGNIFICATION_CURSOR_FOLLOWING_MODE_CONTINUOUS,
            checkedMode = null,
        )
    }

    private fun launchDialogAndChecked(
        @AccessibilityMagnificationCursorFollowingMode initialMode: Int,
        @AccessibilityMagnificationCursorFollowingMode checkedMode: Int?,
    ): Dialog {
        Settings.Secure.putInt(context.contentResolver, SETTING_KEY, initialMode)
        CursorFollowingModeChooser.showDialog(
            fragmentManager = fragment.childFragmentManager,
            requestKey = requestKey,
        )
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        var dialog = ShadowDialog.getLatestDialog()
        var listView = getListViewInDialog(dialog)
        val adapter = listView.adapter

        if (checkedMode != null) {
            for (i in 0 until listView.count) {
                if ((adapter.getItem(i) as? CursorFollowingModeInfo)?.mode == checkedMode) {
                    listView.setItemChecked(i, true)
                    break
                }
            }
        }

        return dialog
    }

    private fun getListViewInDialog(dialog: Dialog): ListView {
        return dialog.requireViewById<ListView>(android.R.id.list)
    }

    @AccessibilityMagnificationCursorFollowingMode
    private fun getCheckedMode(listView: ListView): Int? {
        val checkedPosition = listView.checkedItemPosition
        return if (checkedPosition != AdapterView.INVALID_POSITION) {
            (listView.adapter.getItem(checkedPosition) as? CursorFollowingModeInfo)?.mode
        } else {
            null
        }
    }
}
