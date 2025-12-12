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

package com.android.settings.accessibility.screenmagnification.ui

import android.app.settings.SettingsEnums
import android.content.ComponentName
import android.platform.test.annotations.EnableFlags
import android.platform.test.annotations.RequiresFlagsDisabled
import android.platform.test.flag.junit.CheckFlagsRule
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import android.platform.test.flag.junit.SetFlagsRule
import android.view.InputDevice
import androidx.fragment.app.FragmentFactory
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.FragmentScenario.FragmentAction
import androidx.lifecycle.Lifecycle
import androidx.preference.Preference
import com.android.internal.accessibility.AccessibilityShortcutController
import com.android.settings.R
import com.android.settings.accessibility.BaseShortcutInteractionsTestCases
import com.android.settings.accessibility.Flags
import com.android.settings.accessibility.MagnificationCapabilities
import com.android.settings.accessibility.MagnificationCapabilities.MagnificationMode
import com.android.settings.accessibility.ShortcutPreference
import com.android.settings.accessibility.screenmagnification.dialogs.CursorFollowingModeChooser
import com.android.settings.accessibility.screenmagnification.dialogs.MagnificationModeChooser
import com.android.settings.testutils.AccessibilityTestUtils.assertDialogShown
import com.android.settings.testutils.inflateViewHolder
import com.android.settings.testutils.shadow.ShadowInputDevice
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@Config(shadows = [ShadowInputDevice::class])
@RunWith(RobolectricTestRunner::class)
class MagnificationPreferenceFragmentTest :
    BaseShortcutInteractionsTestCases<MagnificationPreferenceFragment>() {
    @get:Rule val checkFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()
    @get:Rule val setFlagsRule = SetFlagsRule()
    private var fragScenario: FragmentScenario<MagnificationPreferenceFragment>? = null
    private var fragment: MagnificationPreferenceFragment? = null

    @After
    fun cleanUp() {
        fragScenario?.close()
        ShadowInputDevice.reset()
    }

    @Test
    fun clickModePreference_showModeChooserDialog() {
        val fragment = launchFragment()
        val modePref: Preference? = fragment.findPreference(MAGNIFICATION_MODE_PREF_KEY)
        requireNotNull(modePref)

        modePref.inflateViewHolder().itemView.performClick()
        ShadowLooper.idleMainLooper()

        assertDialogShown(fragment, MagnificationModeChooser::class.java)
    }

    @EnableFlags(Flags.FLAG_ENABLE_MAGNIFICATION_CURSOR_FOLLOWING_DIALOG)
    @Test
    fun clickCursorFollowingModePreference_showDedicatedDialog() {
        // Setup cursor following mode enabled
        val device =
            ShadowInputDevice.makeInputDevicebyIdWithSources(/* id= */ 1, InputDevice.SOURCE_MOUSE)
        ShadowInputDevice.addDevice(device.id, device)
        MagnificationCapabilities.setCapabilities(context, MagnificationMode.FULLSCREEN)

        val fragment = launchFragment()
        val modePref: Preference? = fragment.findPreference(CURSOR_FOLLOWING_MODE_PREF_KEY)
        requireNotNull(modePref)

        modePref.inflateViewHolder().itemView.performClick()
        ShadowLooper.idleMainLooper()

        assertDialogShown(fragment, CursorFollowingModeChooser::class.java)
    }

    @Test
    fun getMetricsCategory_returnsCorrectCategory() {
        val fragment = launchFragment()

        assertThat(fragment.metricsCategory)
            .isEqualTo(SettingsEnums.ACCESSIBILITY_TOGGLE_SCREEN_MAGNIFICATION)
    }

    @Test
    fun getHelpResource_returnsCorrectHelpResource() {
        val fragment = launchFragment()

        assertThat(fragment.helpResource).isEqualTo(R.string.help_url_magnification)
    }

    @RequiresFlagsDisabled(Flags.FLAG_CATALYST_MAGNIFICATION)
    @Test
    fun getSearchIndexDataProvider_verifyXmlResourcesToIndex() {
        val searchIndexableResource =
            MagnificationPreferenceFragment.SEARCH_INDEX_DATA_PROVIDER.getXmlResourcesToIndex(
                context,
                /* enabled= */ true,
            )
        assertThat(searchIndexableResource.first().xmlResId)
            .isEqualTo(R.xml.accessibility_magnification_screen)
    }

    override fun getShortcutToggle(): ShortcutPreference? {
        return fragment?.findPreference(SHORTCUT_PREF_KEY)
    }

    override fun launchFragment(): MagnificationPreferenceFragment {
        fragScenario =
            FragmentScenario.launch(
                    MagnificationPreferenceFragment::class.java,
                    /* fragmentArgs= */ null,
                    androidx.appcompat.R.style.Theme_AppCompat,
                    null as FragmentFactory?,
                )
                .moveToState(Lifecycle.State.RESUMED)
        fragScenario!!.onFragment(
            FragmentAction { frag: MagnificationPreferenceFragment? -> fragment = frag }
        )
        return fragment!!
    }

    override fun getFeatureComponent(): ComponentName {
        return AccessibilityShortcutController.MAGNIFICATION_COMPONENT_NAME
    }

    override fun getFeatureComponentString(): String {
        return AccessibilityShortcutController.MAGNIFICATION_CONTROLLER_NAME
    }

    companion object {
        private const val SHORTCUT_PREF_KEY = "magnification_shortcut_preference"
        private const val MAGNIFICATION_MODE_PREF_KEY = "accessibility_magnification_capability"
        private const val CURSOR_FOLLOWING_MODE_PREF_KEY =
            "accessibility_magnification_cursor_following_mode"
    }
}
