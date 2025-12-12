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

package com.android.settings.accessibility.shared.ui

import android.content.ComponentName
import android.content.Context
import android.view.View
import android.view.accessibility.AccessibilityManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragment
import androidx.lifecycle.Lifecycle
import androidx.preference.PreferenceViewHolder
import androidx.test.core.app.ApplicationProvider
import com.android.internal.accessibility.AccessibilityShortcutController.ACCESSIBILITY_HEARING_AIDS_COMPONENT_NAME
import com.android.internal.accessibility.common.ShortcutConstants
import com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.HARDWARE
import com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.QUICK_SETTINGS
import com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.SOFTWARE
import com.android.settings.R
import com.android.settings.accessibility.AccessibilityUtil
import com.android.settings.accessibility.PreferredShortcut
import com.android.settings.accessibility.PreferredShortcuts
import com.android.settings.accessibility.ShortcutPreference
import com.android.settings.testutils.AccessibilityTestUtils
import com.android.settings.testutils.inflateViewHolder
import com.android.settings.testutils.shadow.ShadowAccessibilityManager
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.preference.createAndBindWidget
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.TestScope
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadow.api.Shadow

const val TEST_KEY = "testKey"
const val TEST_TITLE_RES = 0
const val TEST_FEATURE_NAME_RES = 0

@RunWith(RobolectricTestRunner::class)
class AccessibilityShortcutPreferenceTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val testComponentName: ComponentName = ACCESSIBILITY_HEARING_AIDS_COMPONENT_NAME
    private var testScope = TestScope()
    private val shortcutPreference = TestableAccessibilityShortcutPreference()
    private val a11yManager: ShadowAccessibilityManager =
        Shadow.extract(context.getSystemService(AccessibilityManager::class.java))
    private lateinit var fragmentScenario: FragmentScenario<Fragment>
    private lateinit var shortcutWidget: ShortcutPreference
    private lateinit var shortcutWidgetViewHolder: PreferenceViewHolder
    private val mockPreferenceLifecycleContext = mock<PreferenceLifecycleContext>()

    @Before
    fun setUp() {
        fragmentScenario = launchFragment<Fragment>(initialState = Lifecycle.State.INITIALIZED)
        fragmentScenario.onFragment { fragment ->
            shortcutWidget = shortcutPreference.createAndBindWidget(fragment.requireContext())
            shortcutWidgetViewHolder = shortcutWidget.inflateViewHolder()
            mockPreferenceLifecycleContext.stub {
                on { requirePreference<ShortcutPreference>(TEST_KEY) } doReturn shortcutWidget
                on { fragmentManager } doReturn fragment.childFragmentManager
                on { childFragmentManager } doReturn fragment.childFragmentManager
            }
        }
    }

    @After
    fun tearDown() {
        testScope.cancel()
    }

    @Test
    fun getSummary_settingsNotEditable_summaryCorrect() {
        shortcutPreference.setSettingsEditable(false)

        assertThat(shortcutPreference.getSummary(context))
            .isEqualTo(
                context.getString(R.string.accessibility_shortcut_edit_dialog_title_hardware)
            )
    }

    @Test
    fun getSummary_shortcutsDisabled_summaryCorrect() {
        assertThat(shortcutPreference.getSummary(context))
            .isEqualTo(context.getString(R.string.accessibility_shortcut_state_off))
    }

    @Test
    fun getSummary_shortcutsEnabled_summaryCorrect() {
        enableShortcuts(SOFTWARE or HARDWARE)

        assertThat(shortcutPreference.getSummary(context))
            .isEqualTo(AccessibilityUtil.getShortcutSummaryList(context, SOFTWARE or HARDWARE))
    }

    @Test
    fun bind_shortcutsDisabled_toggleIsOff() {
        shortcutPreference.bind(shortcutWidget, shortcutPreference)

        assertThat(shortcutWidget.isChecked).isFalse()
    }

    @Test
    fun bind_shortcutsEnabled_toggleIsOn() {
        enableShortcuts(SOFTWARE or QUICK_SETTINGS)

        shortcutPreference.bind(shortcutWidget, shortcutPreference)

        assertThat(shortcutWidget.isChecked).isTrue()
    }

    @Test
    fun bind_settingsEditable() {
        shortcutPreference.setSettingsEditable(true)

        shortcutPreference.bind(shortcutWidget, shortcutPreference)

        assertThat(shortcutWidget.isSettingsEditable).isTrue()
    }

    @Test
    fun bind_settingsNotEditable() {
        shortcutPreference.setSettingsEditable(false)

        shortcutPreference.bind(shortcutWidget, shortcutPreference)

        assertThat(shortcutWidget.isSettingsEditable).isFalse()
    }

    @Test
    fun clickToggle_tutorialDialogShownAndToggleChanged() {
        fragmentScenario.onFragment { fragment ->
            assertThat(shortcutWidget.isChecked).isFalse()

            shortcutPreference.onCreate(mockPreferenceLifecycleContext)
            shortcutWidgetViewHolder.itemView
                .findViewById<View>(shortcutWidget.switchResId)
                .performClick()

            AccessibilityTestUtils.assertShortcutsTutorialDialogShown(fragment)
            assertThat(shortcutWidget.isChecked).isTrue()
        }
    }

    @Test
    fun clickSettings_editShortcutsScreenShownAndToggleNotChanged() {
        fragmentScenario.onFragment { fragment ->
            assertThat(shortcutWidget.isChecked).isFalse()

            shortcutPreference.onCreate(mockPreferenceLifecycleContext)
            shortcutWidgetViewHolder.itemView.performClick()

            AccessibilityTestUtils.assertEditShortcutsScreenShown(fragment)
            assertThat(shortcutWidget.isChecked).isFalse()
        }
    }

    private fun enableShortcuts(types: Int) {
        val componentString = testComponentName.flattenToString()
        a11yManager.enableShortcutsForTargets(true, types, setOf(componentString), context.userId)
        if (types != ShortcutConstants.UserShortcutType.DEFAULT) {
            PreferredShortcuts.saveUserShortcutType(
                context,
                PreferredShortcut(componentString, types),
            )
        }
    }

    inner class TestableAccessibilityShortcutPreference :
        AccessibilityShortcutPreference(
            context,
            TEST_KEY,
            TEST_TITLE_RES,
            testComponentName,
            TEST_FEATURE_NAME_RES,
        ) {
        private var isSettingsEditable: Boolean = true

        fun setSettingsEditable(editable: Boolean) {
            isSettingsEditable = editable
        }

        override fun getSettingsEditable(context: Context): Boolean = isSettingsEditable
    }
}
