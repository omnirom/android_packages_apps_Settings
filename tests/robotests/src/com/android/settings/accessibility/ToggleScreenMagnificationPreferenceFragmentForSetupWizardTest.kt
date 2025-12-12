/*
 * Copyright (C) 2022 The Android Open Source Project
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

import android.app.settings.SettingsEnums
import android.content.ComponentName
import android.os.Bundle
import com.android.internal.accessibility.AccessibilityShortcutController.MAGNIFICATION_COMPONENT_NAME
import com.android.internal.accessibility.AccessibilityShortcutController.MAGNIFICATION_CONTROLLER_NAME
import com.android.settings.R
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Tests for [ToggleScreenMagnificationPreferenceFragmentForSetupWizard]. */
@RunWith(RobolectricTestRunner::class)
class ToggleScreenMagnificationPreferenceFragmentForSetupWizardTest :
    BaseShortcutInteractionsInSuwTestCases<
        ToggleScreenMagnificationPreferenceFragmentForSetupWizard
    >() {

    @Test
    fun getMetricsCategory_returnsCorrectCategory() {
        assertThat(ToggleScreenMagnificationPreferenceFragmentForSetupWizard().getMetricsCategory())
            .isEqualTo(SettingsEnums.SUW_ACCESSIBILITY_TOGGLE_SCREEN_MAGNIFICATION)
    }

    @Test
    fun getHelpResource_shouldNotHaveHelpResource() {
        assertThat(ToggleScreenMagnificationPreferenceFragmentForSetupWizard().getHelpResource())
            .isEqualTo(0)
    }

    override fun getSetupWizardTitle(): String {
        return context.getString(R.string.accessibility_screen_magnification_title)
    }

    override fun getSetupWizardDescription(): String {
        return context.getString(R.string.accessibility_screen_magnification_intro_text)
    }

    override fun getFragmentClazz() =
        ToggleScreenMagnificationPreferenceFragmentForSetupWizard::class.java

    override fun getFragmentArgs(): Bundle? {
        return null
    }

    override fun getShortcutToggle(): ShortcutPreference? {
        return fragment?.findPreference(SHORTCUT_PREF_KEY)
    }

    override fun getFeatureComponent(): ComponentName {
        return MAGNIFICATION_COMPONENT_NAME
    }

    override fun getFeatureComponentString(): String {
        return MAGNIFICATION_CONTROLLER_NAME
    }

    companion object {
        private const val SHORTCUT_PREF_KEY = "magnification_shortcut_preference"
    }
}
