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

import android.content.ComponentName
import android.content.Context
import android.view.accessibility.AccessibilityManager
import androidx.preference.PreferenceFragmentCompat
import androidx.test.core.app.ApplicationProvider
import com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType
import com.android.settings.accessibility.data.AccessibilityRepositoryProvider
import com.android.settings.testutils.AccessibilityTestUtils
import com.android.settings.testutils.SettingsStoreRule
import com.android.settings.testutils.shadow.ShadowAccessibilityManager
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowDialog
import org.robolectric.shadows.ShadowLooper

/** Base test cases for fragment class that that provides accessibility shortcut toggle. */
abstract class BaseShortcutInteractionsTestCases<T : PreferenceFragmentCompat> {
    @get:Rule val settingStoreRule = SettingsStoreRule()

    protected val context: Context = ApplicationProvider.getApplicationContext()
    protected val a11yManager: ShadowAccessibilityManager =
        Shadow.extract(context.getSystemService(AccessibilityManager::class.java))

    @After
    fun clearAccessibilityRepository() {
        AccessibilityRepositoryProvider.resetInstanceForTesting()
    }

    @Test
    fun clickShortcutToggle_shortcutWasOff_turnOnShortcutAndShowShortcutTutorial() {
        a11yManager.enableShortcutsForTargets(
            /* enable= */ false,
            UserShortcutType.ALL,
            setOf(getFeatureComponentString()),
            context.userId,
        )
        val fragment = launchFragment()

        val pref: ShortcutPreference? = getShortcutToggle()
        assertThat(pref).isNotNull()
        assertThat(pref!!.isChecked).isFalse()
        val viewHolder =
            AccessibilityTestUtils.inflateShortcutPreferenceView(fragment.requireContext(), pref)

        val widget = viewHolder.findViewById(pref.switchResId)
        assertThat(widget).isNotNull()
        widget!!.performClick()
        ShadowLooper.idleMainLooper()

        assertThat(pref.isChecked).isTrue()
        AccessibilityTestUtils.assertShortcutsTutorialDialogShown(fragment)
    }

    @Test
    fun clickShortcutToggle_shortcutWasOn_turnOffShortcutAndNoTutorialShown() {
        a11yManager.enableShortcutsForTargets(
            /* enable= */ true,
            UserShortcutType.HARDWARE,
            setOf(getFeatureComponentString()),
            context.userId,
        )
        val fragment = launchFragment()

        val pref: ShortcutPreference? = getShortcutToggle()
        assertThat(pref).isNotNull()
        assertThat(pref!!.isChecked).isTrue()
        val viewHolder =
            AccessibilityTestUtils.inflateShortcutPreferenceView(fragment.requireContext(), pref)

        val widget = viewHolder.findViewById(pref.switchResId)
        assertThat(widget).isNotNull()
        widget!!.performClick()
        ShadowLooper.idleMainLooper()

        assertThat(pref.isChecked).isFalse()
        assertThat(a11yManager.getAccessibilityShortcutTargets(UserShortcutType.HARDWARE)).isEmpty()
        assertThat(ShadowDialog.getLatestDialog()).isNull()
    }

    @Test
    fun clickShortcutSettings_showEditShortcutsScreenWithoutChangingShortcutToggleState() {
        val fragment = launchFragment()

        val pref: ShortcutPreference? = getShortcutToggle()
        assertThat(pref).isNotNull()
        val shortcutToggleState = pref!!.isChecked
        pref.performClick()
        ShadowLooper.idleMainLooper()

        AccessibilityTestUtils.assertEditShortcutsScreenShown(fragment)
        assertThat(pref.isChecked).isEqualTo(shortcutToggleState)
    }

    abstract fun getShortcutToggle(): ShortcutPreference?

    abstract fun launchFragment(): T

    abstract fun getFeatureComponent(): ComponentName

    open fun getFeatureComponentString(): String {
        return getFeatureComponent().flattenToString()
    }
}
