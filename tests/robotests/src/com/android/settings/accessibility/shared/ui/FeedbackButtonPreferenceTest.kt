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

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.testing.EmptyFragmentActivity
import androidx.fragment.app.testing.launchFragment
import androidx.preference.PreferenceManager
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.android.server.accessibility.Flags
import com.android.settings.R
import com.android.settings.accessibility.FeedbackManager
import com.android.settings.testutils.inflateViewHolder
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.preference.createAndBindWidget
import com.android.settingslib.widget.ButtonPreference
import com.google.android.setupcompat.util.WizardManagerHelper
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController

@RunWith(RobolectricTestRunner::class)
class FeedbackButtonPreferenceTest {
    @get:Rule val setFlagsRule = SetFlagsRule()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val shadowPackageManager = shadowOf(context.packageManager)
    private var activityScenario: ActivityScenario<EmptyFragmentActivity>? = null
    private val preferenceManager = PreferenceManager(context)
    private val preferenceScreen = preferenceManager.createPreferenceScreen(context)
    private val preference = FeedbackButtonPreference {
        FeedbackManager(PACKAGE_NAME, CATEGORY_TAG, TRIGGER_ID)
    }

    @After
    fun tearDown() {
        activityScenario?.close()
    }

    @Test
    fun getKey() {
        assertThat(preference.key).isEqualTo(FeedbackButtonPreference.Companion.KEY)
    }

    @Test
    fun getIcon() {
        assertThat(preference.icon).isEqualTo(R.drawable.ic_feedback)
    }

    @Test
    fun getTitle() {
        assertThat(preference.title).isEqualTo(R.string.accessibility_send_feedback_title)
    }

    @Test
    fun createWidget() {
        val widget = preference.createWidget(context)
        assertThat(widget).isInstanceOf(ButtonPreference::class.java)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_LOW_VISION_GENERIC_FEEDBACK)
    fun isAvailable_whenInSetupWizard_returnFalse() {
        var activityController: ActivityController<ComponentActivity>? = null
        try {
            activityController =
                ActivityController.of(
                        ComponentActivity(),
                        Intent().apply { putExtra(WizardManagerHelper.EXTRA_IS_SETUP_FLOW, true) },
                    )
                    .create()
            assertThat(preference.isAvailable(activityController.get())).isFalse()
        } finally {
            activityController?.destroy()
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_LOW_VISION_GENERIC_FEEDBACK)
    fun isAvailable_whenNotInSetupWizard_returnTrue() {
        assertThat(preference.isAvailable(context)).isTrue()
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_LOW_VISION_GENERIC_FEEDBACK)
    fun isAvailable_disableLowVisionGeneric_returnFalse() {
        assertThat(preference.isAvailable(context)).isFalse()
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_LOW_VISION_GENERIC_FEEDBACK)
    fun performClick_shouldStartBugReportIntent() {
        val fragmentScenario =
            launchFragment<Fragment>(themeResId = androidx.appcompat.R.style.Theme_AppCompat)
        var fragment: Fragment? = null
        fragmentScenario.onFragment { fragment = it }
        assertThat(fragment).isNotNull()
        try {
            val buttonPreference =
                preference.createAndBindWidget<ButtonPreference>(createContext(), preferenceScreen)
            preferenceScreen.addPreference(buttonPreference)

            val preferenceLifecycleContext: PreferenceLifecycleContext = mock {
                on { findPreference<ButtonPreference>(FeedbackButtonPreference.Companion.KEY) }
                    .thenReturn(buttonPreference)
                on { childFragmentManager }.thenReturn(fragment!!.childFragmentManager)
            }

            preference.onCreate(preferenceLifecycleContext)
            buttonPreference.inflateViewHolder()
            buttonPreference.button.performClick()

            val intent: Intent = Shadows.shadowOf(context as Application?).nextStartedActivity
            assertThat(intent.action).isEqualTo(Intent.ACTION_BUG_REPORT)
        } finally {
            fragmentScenario.close()
        }
    }

    private fun createContext(): Context {
        shadowPackageManager.addActivityIfNotPresent(
            ComponentName(context, EmptyFragmentActivity::class.java)
        )
        var startedActivity: Context? = null
        val intent = Intent(context, EmptyFragmentActivity::class.java)
        activityScenario = ActivityScenario.launch(intent)
        activityScenario!!.onActivity { activity -> startedActivity = activity }
        return startedActivity!!
    }

    companion object {
        private const val PACKAGE_NAME = "android"
        private const val CATEGORY_TAG = "category tag"
        private const val TRIGGER_ID = "trigger id"
    }
}
