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

package com.android.settings.accessibility

import android.content.Context
import android.platform.test.annotations.EnableFlags
import android.provider.Settings
import android.view.View
import android.view.accessibility.AccessibilityManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragment
import androidx.lifecycle.Lifecycle.State.CREATED
import androidx.lifecycle.Lifecycle.State.DESTROYED
import androidx.lifecycle.Lifecycle.State.INITIALIZED
import androidx.preference.PreferenceManager
import androidx.preference.PreferenceViewHolder
import androidx.test.core.app.ApplicationProvider
import com.android.hardware.input.Flags
import com.android.internal.accessibility.AccessibilityShortcutController.AUTOCLICK_COMPONENT_NAME
import com.android.internal.accessibility.common.ShortcutConstants
import com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.DEFAULT
import com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.GESTURE
import com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.HARDWARE
import com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.KEY_GESTURE
import com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.SOFTWARE
import com.android.internal.accessibility.util.ShortcutUtils
import com.android.settings.R
import com.android.settings.testutils.AccessibilityTestUtils
import com.android.settings.testutils.shadow.ShadowAccessibilityManager
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowLooper

private const val PREFERENCE_KEY = "prefKey"
private const val PREFERENCE_TITLE = "prefTitle"

/** Tests for [ToggleShortcutPreferenceController] */
@RunWith(RobolectricTestRunner::class)
class ToggleShortcutPreferenceControllerTest {
    @get:Rule val mockitoRule: MockitoRule = MockitoJUnit.rule()
    private lateinit var shortcutPreference: ShortcutPreference
    private lateinit var controller: ToggleShortcutPreferenceController
    private lateinit var fragmentScenario: FragmentScenario<Fragment>
    private lateinit var viewHolder: PreferenceViewHolder
    private val testComponentString = AUTOCLICK_COMPONENT_NAME.flattenToString()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val a11yManager: ShadowAccessibilityManager =
        Shadow.extract(context.getSystemService(AccessibilityManager::class.java))
    private val preferenceManager = PreferenceManager(context)

    @Before
    fun setUp() {
        fragmentScenario = launchFragment<Fragment>(initialState = INITIALIZED)
        fragmentScenario.onFragment { fragment ->
            controller =
                ToggleShortcutPreferenceController(fragment.requireContext(), PREFERENCE_KEY)

            fragment.lifecycle.addObserver(controller)
            controller.initialize(
                AUTOCLICK_COMPONENT_NAME,
                fragment.childFragmentManager,
                PREFERENCE_TITLE,
                0,
            )
        }
        shortcutPreference = ShortcutPreference(context, null)
        shortcutPreference.key = PREFERENCE_KEY
        shortcutPreference.title = PREFERENCE_TITLE
        val preferenceScreen = preferenceManager.createPreferenceScreen(context)
        preferenceScreen.addPreference(shortcutPreference)
        preferenceManager.setPreferences(preferenceScreen)
        viewHolder =
            AccessibilityTestUtils.inflateShortcutPreferenceView(context, shortcutPreference)
    }

    @After
    fun cleanUp() {
        fragmentScenario.close()
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
    }

    @Test
    fun onCreate_verifyContentObserverRegistered() {
        fragmentScenario.moveToState(CREATED)

        // verify observers are registered
        val contentResolver = shadowOf(context.contentResolver)
        for (settingKey in ShortcutConstants.GENERAL_SHORTCUT_SETTINGS) {
            val observers =
                contentResolver.getContentObservers(Settings.Secure.getUriFor(settingKey))
            assertThat(observers).isNotEmpty()
            assertThat(observers).contains(controller.getContentObserverForTesting())
        }
    }

    @Test
    fun onDestroy_verifyContentObserverUnregistered() {
        onCreate_verifyContentObserverRegistered()
        fragmentScenario.moveToState(DESTROYED)

        // verify no observers
        val contentResolver = shadowOf(context.contentResolver)
        for (settingKey in ShortcutConstants.GENERAL_SHORTCUT_SETTINGS) {
            assertThat(contentResolver.getContentObservers(Settings.Secure.getUriFor(settingKey)))
                .isEmpty()
        }
    }

    @Test
    fun onCreate_hasShortcuts_updateUserPreferredShortcuts() {
        PreferredShortcuts.saveUserShortcutType(
            context,
            PreferredShortcut(testComponentString, SOFTWARE or HARDWARE),
        )
        a11yManager.setAccessibilityShortcutTargets(HARDWARE, listOf<String>(testComponentString))

        fragmentScenario.moveToState(CREATED)

        assertThat(
                PreferredShortcuts.retrieveUserShortcutType(context, testComponentString, SOFTWARE)
            )
            .isEqualTo(HARDWARE)
    }

    @Test
    fun displayPreferenceAndUpdateState_softwareOrGestureShortcut_updateCheckStateAndSummary() {
        a11yManager.enableShortcutsForTargets(
            /* enable= */ true,
            SOFTWARE or GESTURE,
            setOf(testComponentString),
            context.userId,
        )

        fragmentScenario.moveToState(CREATED)
        controller.displayPreference(shortcutPreference.preferenceManager.preferenceScreen)
        controller.updateState(shortcutPreference)

        assertThat(shortcutPreference.isChecked).isTrue()
        assertThat(shortcutPreference.summary)
            .isEqualTo(AccessibilityUtil.getShortcutSummaryList(context, SOFTWARE or GESTURE))
    }

    @Test
    fun displayPreferenceAndUpdateState_defaultShortcut_updateCheckStateAndSummary() {
        a11yManager.enableShortcutsForTargets(
            /* enable= */ true,
            DEFAULT,
            setOf(testComponentString),
            context.userId,
        )

        fragmentScenario.moveToState(CREATED)
        controller.displayPreference(shortcutPreference.preferenceManager.preferenceScreen)
        controller.updateState(shortcutPreference)

        assertThat(shortcutPreference.isChecked).isFalse()
        assertThat(shortcutPreference.summary)
            .isEqualTo(context.getText(R.string.accessibility_shortcut_state_off))
    }

    @EnableFlags(Flags.FLAG_ENABLE_TALKBACK_AND_MAGNIFIER_KEY_GESTURES)
    @Test
    fun displayPreferenceAndUpdateState_keyGestureShortcut_updateCheckStateAndSummary() {
        a11yManager.enableShortcutsForTargets(
            /* enable= */ true,
            KEY_GESTURE,
            setOf(testComponentString),
            context.userId,
        )

        fragmentScenario.moveToState(CREATED)
        controller.displayPreference(shortcutPreference.preferenceManager.preferenceScreen)
        controller.updateState(shortcutPreference)

        assertThat(shortcutPreference.isChecked).isFalse()
        assertThat(shortcutPreference.summary)
            .isEqualTo(context.getText(R.string.accessibility_shortcut_state_off))
    }

    @Test
    fun clickSetting_showEditShortcutsScreenWithoutChangingShortcutToggleState() {
        controller.displayPreference(shortcutPreference.preferenceManager.preferenceScreen)
        viewHolder.itemView.performClick()

        fragmentScenario.onFragment { fragment ->
            AccessibilityTestUtils.assertEditShortcutsScreenShown(fragment)
            assertThat(shortcutPreference.isChecked).isFalse()
        }
    }

    @Test
    fun clickToggleToTurnOnShortcut_triggerShowDialog() {
        controller.displayPreference(shortcutPreference.preferenceManager.preferenceScreen)
        assertThat(shortcutPreference.isChecked).isFalse()

        viewHolder.itemView.findViewById<View>(shortcutPreference.switchResId).performClick()

        fragmentScenario.onFragment { fragment ->
            assertThat(shortcutPreference.isChecked).isTrue()
            AccessibilityTestUtils.assertShortcutsTutorialDialogShown(fragment)
            assertThat(ShortcutUtils.getEnabledShortcutTypes(context, testComponentString))
                .isNotEqualTo(DEFAULT)
        }
    }

    @Test
    fun contentObserver_onChange_updatePreferenceDataAndState() {
        onCreate_verifyContentObserverRegistered()
        controller.displayPreference(shortcutPreference.preferenceManager.preferenceScreen)
        controller.updateState(shortcutPreference)
        assertThat(shortcutPreference.isChecked).isFalse()
        assertThat(
                PreferredShortcuts.retrieveUserShortcutType(context, testComponentString, DEFAULT)
            )
            .isEqualTo(DEFAULT)

        a11yManager.enableShortcutsForTargets(
            /* enable=*/ true,
            GESTURE,
            setOf(testComponentString),
            context.userId,
        )
        controller
            .getContentObserverForTesting()
            .onChange(
                /* selfChange= */ false,
                Settings.Secure.getUriFor(Settings.Secure.ACCESSIBILITY_GESTURE_TARGETS),
            )
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        assertThat(shortcutPreference.isChecked).isTrue()
        assertThat(shortcutPreference.summary)
            .isEqualTo(AccessibilityUtil.getShortcutSummaryList(context, GESTURE))
        assertThat(
                PreferredShortcuts.retrieveUserShortcutType(context, testComponentString, DEFAULT)
            )
            .isEqualTo(GESTURE)
    }
}
