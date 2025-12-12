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

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.Intent.CATEGORY_BROWSABLE
import android.content.Intent.CATEGORY_DEFAULT
import android.content.IntentFilter
import android.content.pm.PackageInfo
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.provider.Settings
import androidx.core.net.toUri
import androidx.fragment.app.testing.EmptyFragmentActivity
import androidx.preference.PreferenceManager
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.android.settings.core.BasePreferenceController.AVAILABLE
import com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE
import com.android.settings.testutils.inflateViewHolder
import com.android.settings.testutils.shadow.SettingsShadowResources
import com.android.settingslib.widget.ButtonPreference
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@Config(shadows = [SettingsShadowResources::class])
@RunWith(RobolectricTestRunner::class)
class DisabilitySupportButtonPreferenceControllerTest {
    @get:Rule val setFlagsRule = SetFlagsRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val shadowPackageManager = shadowOf(context.packageManager)
    private var activityScenario: ActivityScenario<EmptyFragmentActivity>? = null
    private val preferenceManager = PreferenceManager(context)
    private val preferenceScreen = preferenceManager.createPreferenceScreen(context)
    private lateinit var preference: ButtonPreference
    private lateinit var controller: DisabilitySupportButtonPreferenceController

    @Before
    fun setUp() {
        Settings.Global.putInt(context.contentResolver, Settings.Global.DEVICE_PROVISIONED, 1)
        SettingsShadowResources.overrideResource(
            R.string.help_url_accessibility_disability_support,
            URL,
        )
        setupIntentResolver()
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_DISABILITY_SUPPORT)
    fun getAvailabilityStatus_flagOn_returnAvailable() {
        setupController()

        assertThat(controller.availabilityStatus).isEqualTo(AVAILABLE)
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_DISABILITY_SUPPORT)
    fun getAvailabilityStatus_flagOff_returnUnavailable() {
        setupController()

        assertThat(controller.availabilityStatus).isEqualTo(CONDITIONALLY_UNAVAILABLE)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_DISABILITY_SUPPORT)
    fun getAvailabilityStatus_deviceProvisionedIsOff_returnUnavailable() {
        Settings.Global.putInt(context.contentResolver, Settings.Global.DEVICE_PROVISIONED, 0)
        setupController()

        assertThat(controller.availabilityStatus).isEqualTo(CONDITIONALLY_UNAVAILABLE)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_DISABILITY_SUPPORT)
    fun getAvailabilityStatus_helpIntentIsEmpty_returnUnavailable() {
        SettingsShadowResources.overrideResource(
            R.string.help_url_accessibility_disability_support,
            "",
        )
        setupController()

        assertThat(controller.availabilityStatus).isEqualTo(CONDITIONALLY_UNAVAILABLE)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_DISABILITY_SUPPORT)
    fun performClick_startsIntentWithCorrectUri() {
        setupController()

        preference.button.performClick()

        val startedIntent = Shadows.shadowOf(context as Application?).nextStartedActivity
        assertThat(startedIntent).isNotNull()
        assertThat(startedIntent.data).isEqualTo(URL.toUri())
    }

    private fun setupController() {
        val newContext: Context = createContext()
        controller = DisabilitySupportButtonPreferenceController(newContext, KEY)
        preference = ButtonPreference(newContext)
        preference.setKey(KEY)
        preferenceScreen.addPreference(preference)
        controller.displayPreference(preferenceScreen)
        preference.inflateViewHolder()
    }

    private fun createContext(): Context {
        var startedActivity: Context? = null
        val intent = Intent(context, EmptyFragmentActivity::class.java)
        activityScenario = ActivityScenario.launch(intent)
        activityScenario!!.onActivity { activity -> startedActivity = activity }
        return startedActivity!!
    }

    private fun setupIntentResolver() {
        shadowPackageManager.apply {
            val fakePackage = "foo.bar"
            val componentName = ComponentName(fakePackage, "activity")
            installPackage(PackageInfo().apply { packageName = fakePackage })
            addActivityIfNotPresent(componentName)
            addIntentFilterForActivity(
                componentName,
                IntentFilter(Intent.ACTION_VIEW).apply {
                    addCategory(CATEGORY_DEFAULT)
                    addCategory(CATEGORY_BROWSABLE)
                    addDataScheme("https")
                    addDataAuthority("support.example.com", null)
                },
            )
            addActivityIfNotPresent(ComponentName(context, EmptyFragmentActivity::class.java))
        }
    }

    companion object {
        private const val KEY = "test_key"
        private const val URL = "https://support.example.com/accessibility"
    }
}
