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

package com.android.settings.accessibility.textreading.ui

import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.testing.launchFragment
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.android.settings.accessibility.TextReadingPreferenceFragment.EntryPoint
import com.android.settings.accessibility.textreading.dialogs.TextReadingResetDialog
import com.android.settings.testutils.AccessibilityTestUtils.assertDialogShown
import com.android.settings.testutils.inflateViewHolder
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.preference.createAndBindWidget
import com.android.settingslib.widget.ButtonPreference
import com.google.android.setupcompat.util.WizardManagerHelper.EXTRA_IS_SETUP_FLOW
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController

@RunWith(RobolectricTestRunner::class)
class ResetPreferenceTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val preference = ResetPreference(entryPoint = EntryPoint.UNKNOWN_ENTRY)
    private val preferenceManager = PreferenceManager(context)
    private val preferenceScreen = preferenceManager.createPreferenceScreen(context)

    @Test
    fun getKey() {
        assertThat(preference.key).isEqualTo(ResetPreference.KEY)
    }

    @Test
    fun getIcon() {
        assertThat(preference.icon).isEqualTo(R.drawable.ic_history)
    }

    @Test
    fun getTitle() {
        assertThat(preference.title)
            .isEqualTo(R.string.accessibility_text_reading_reset_button_title)
    }

    @Test
    fun createWidget() {
        val widget = preference.createWidget(context)
        assertThat(widget).isInstanceOf(ButtonPreference::class.java)
    }

    @Test
    fun isAvailable_inSetupWizard_returnFalse() {
        var activityController: ActivityController<ComponentActivity>? = null
        try {
            activityController =
                ActivityController.of(
                        ComponentActivity(),
                        Intent().apply { putExtra(EXTRA_IS_SETUP_FLOW, true) },
                    )
                    .create()
            assertThat(preference.isAvailable(activityController.get())).isFalse()
        } finally {
            activityController?.destroy()
        }
    }

    @Test
    fun isAvailable_notInSetupWizard_returnTrue() {
        assertThat(preference.isAvailable(context)).isTrue()
    }

    @Test
    fun onCreate_setupClickListenerThatLaunchesDialog() {
        val fragmentScenario =
            launchFragment<Fragment>(themeResId = androidx.appcompat.R.style.Theme_AppCompat)
        var fragment: Fragment? = null
        fragmentScenario.onFragment { fragment = it }
        assertThat(fragment).isNotNull()
        try {
            val buttonPreference =
                preference.createAndBindWidget<ButtonPreference>(context, preferenceScreen)
            preferenceScreen.addPreference(buttonPreference)
            val preferenceLifecycleContext: PreferenceLifecycleContext = mock {
                on { findPreference<ButtonPreference>(ResetPreference.KEY) }
                    .thenReturn(buttonPreference)
                on { childFragmentManager }.thenReturn(fragment!!.childFragmentManager)
            }

            preference.onCreate(preferenceLifecycleContext)
            buttonPreference.inflateViewHolder()
            buttonPreference.button.performClick()

            assertDialogShown(fragment, TextReadingResetDialog::class.java)
        } finally {
            fragmentScenario.close()
        }
    }
}
