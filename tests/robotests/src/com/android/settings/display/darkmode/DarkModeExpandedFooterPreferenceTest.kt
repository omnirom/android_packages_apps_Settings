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

package com.android.settings.display.darkmode

import android.app.Activity
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
import android.view.View
import android.view.accessibility.Flags
import android.widget.TextView
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.android.settings.testutils.inflateViewHolder
import com.android.settings.testutils.shadow.SettingsShadowResources
import com.android.settings.widget.FooterPreferenceMetadata
import com.android.settingslib.preference.createAndBindWidget
import com.android.settingslib.widget.FooterPreference
import com.android.settingslib.widget.preference.footer.R as SettingsLibR
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

@Config(shadows = [SettingsShadowResources::class])
@RunWith(RobolectricTestRunner::class)
class DarkModeExpandedFooterPreferenceTest {
    @get:Rule val setFlagsRule = SetFlagsRule()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val shadowPackageManager = shadowOf(context.packageManager)
    private val preference = DarkModeExpandedFooterPreference()

    @Test
    fun key() {
        assertThat(preference.key).isEqualTo("dark_theme_expanded_footer")
    }

    @Test
    fun isIndexable() {
        assertThat(preference.indexable).isFalse()
    }

    @EnableFlags(Flags.FLAG_FORCE_INVERT_COLOR)
    @Test
    fun isAvailable_isTrue() {
        assertThat(preference.isAvailable(context)).isTrue()
    }

    @DisableFlags(Flags.FLAG_FORCE_INVERT_COLOR)
    @Test
    fun isAvailable_isFalse() {
        assertThat(preference.isAvailable(context)).isFalse()
    }

    @Test
    fun bindWidgetAndClickLearnMore_launchLearMoreActivity() {
        setupIntentResolver()
        launchActivityAndRunTest { context ->
            Settings.Global.putInt(context.contentResolver, Settings.Global.DEVICE_PROVISIONED, 1)
            val helpLink = "https://www.google.com"
            val metadata =
                object : FooterPreferenceMetadata {
                    override val key: String
                        get() = DarkModeCustomModesFooterPreference.KEY
                }
            SettingsShadowResources.overrideResource(R.string.help_url_dark_theme_link, helpLink)

            val widget = preference.createAndBindWidget<FooterPreference>(context)
            preference.bind(widget, metadata)
            val prefViewHolder = widget.inflateViewHolder()
            val learnMoreView =
                prefViewHolder.itemView.findViewById<TextView>(
                    SettingsLibR.id.settingslib_learn_more
                )

            assertThat(learnMoreView).isNotNull()
            assertThat(learnMoreView?.visibility).isEqualTo(View.VISIBLE)
            assertThat(learnMoreView?.text.toString())
                .isEqualTo(
                    context.getString(
                        R.string.accessibility_dark_theme_footer_learn_more_helper_link
                    )
                )

            ReflectionHelpers.getField<View.OnClickListener>(widget, "mLearnMoreListener")
                .onClick(learnMoreView)

            val intentForResult = shadowOf(context).nextStartedActivityForResult
            assertThat(intentForResult).isNotNull()
            assertThat(intentForResult.intent.data).isEqualTo(helpLink.toUri())
        }
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
                    addDataAuthority("www.google.com", null)
                },
            )
            addActivityIfNotPresent(ComponentName(context, FragmentActivity::class.java))
        }
    }

    private fun launchActivityAndRunTest(test: (activity: Activity) -> Unit) {
        ActivityScenario.launch(FragmentActivity::class.java).use {
            it.onActivity { activity -> test(activity) }
        }
    }
}
