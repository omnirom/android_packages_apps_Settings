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

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.Intent.CATEGORY_BROWSABLE
import android.content.Intent.CATEGORY_DEFAULT
import android.content.IntentFilter
import android.content.pm.PackageInfo
import android.provider.Settings
import android.view.View
import android.widget.TextView
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.android.settings.R.string.accessibility_screen_magnification_footer_learn_more_content_description as LearnMoreTextStringRes
import com.android.settings.accessibility.AccessibilityFooterPreference
import com.android.settings.testutils.inflateViewHolder
import com.android.settings.testutils.shadow.SettingsShadowResources
import com.android.settingslib.widget.preference.footer.R as SettingsLibR
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

/** Tests for [AccessibilityFooterPreferenceBinding]. */
@Config(shadows = [SettingsShadowResources::class])
@RunWith(RobolectricTestRunner::class)
class AccessibilityFooterPreferenceBindingTest {
    private val appContext = ApplicationProvider.getApplicationContext<Context>()
    private val shadowPackageManager = shadowOf(appContext.packageManager)
    private val binding: AccessibilityFooterPreferenceBinding =
        AccessibilityFooterPreferenceBindingImpl()

    @Test
    fun createWidget_returnsAccessibilityFooterPreference() {
        val widget = binding.createWidget(appContext)
        assertThat(widget).isInstanceOf(AccessibilityFooterPreference::class.java)
    }

    @Test
    fun bindWidgetAndClickLearnMore_launchLearMoreActivity() {
        setupIntentResolver()
        launchActivityAndRunTest { context ->
            Settings.Global.putInt(context.contentResolver, Settings.Global.DEVICE_PROVISIONED, 1)
            val helpLink = "https://www.google.com"
            val metadata =
                object : AccessibilityFooterPreferenceMetadataImpl() {
                    override val helpResource = R.string.help_url_magnification

                    override val learnMoreText = LearnMoreTextStringRes
                }
            SettingsShadowResources.overrideResource(metadata.helpResource, helpLink)

            val widget: AccessibilityFooterPreference = binding.createWidget(context)
            binding.bind(widget, metadata)
            val prefViewHolder = widget.inflateViewHolder()

            val learnMoreView =
                prefViewHolder.itemView.findViewById<TextView>(
                    SettingsLibR.id.settingslib_learn_more
                )
            assertThat(learnMoreView).isNotNull()
            assertThat(learnMoreView.visibility).isEqualTo(View.VISIBLE)
            assertThat(learnMoreView.text.toString())
                .isEqualTo(context.getString(metadata.learnMoreText))

            ReflectionHelpers.getField<View.OnClickListener>(widget, "mLearnMoreListener")
                .onClick(learnMoreView)

            val intentForResult = shadowOf(context).nextStartedActivityForResult
            assertThat(intentForResult).isNotNull()
            assertThat(intentForResult.intent.data).isEqualTo(helpLink.toUri())
        }
    }

    @Test
    fun bindWidget_noLearnMoreText_learnMoreTextViewNotVisible() {
        val metadata = AccessibilityFooterPreferenceMetadataImpl()
        val prefViewHolder =
            binding.createWidget(appContext).also { binding.bind(it, metadata) }.inflateViewHolder()

        val learnMoreView =
            prefViewHolder.itemView.findViewById<TextView>(SettingsLibR.id.settingslib_learn_more)
        assertThat(learnMoreView).isNotNull()
        assertThat(learnMoreView.visibility).isEqualTo(View.GONE)
    }

    @Test
    fun bindWidget_hasIntroTitle_verifyContentDescription() {
        val metadata =
            object : AccessibilityFooterPreferenceMetadataImpl() {
                override val introductionTitle: Int
                    get() = R.string.accessibility_screen_magnification_about_title

                override val title: Int
                    get() = R.string.accessibility_screen_magnification_title
            }
        val preferenceWidget = binding.createWidget(appContext).also { binding.bind(it, metadata) }
        assertThat(preferenceWidget.contentDescription)
            .isEqualTo(
                "${appContext.getString(metadata.introductionTitle)}\n\n${appContext.getString(metadata.title)}"
            )
    }

    @Test
    fun bindWidget_metadataImplementIntroTitleProvider_verifyContentDescription() {
        val fakeIntroTitle = "Fake intro title"
        val metadata =
            object :
                AccessibilityFooterPreferenceMetadataImpl(),
                AccessibilityFooterPreferenceIntroductionTitleProvider {
                override fun getIntroductionTitle(context: Context): CharSequence? = fakeIntroTitle

                override val title: Int
                    get() = R.string.accessibility_screen_magnification_title
            }
        val preferenceWidget = binding.createWidget(appContext).also { binding.bind(it, metadata) }
        assertThat(preferenceWidget.contentDescription)
            .isEqualTo("$fakeIntroTitle\n\n${appContext.getString(metadata.title)}")
    }

    @Test
    fun bindWidget_verifyPreferenceNotSelectable() {
        val widget =
            binding.createWidget(appContext).also {
                binding.bind(it, AccessibilityFooterPreferenceMetadataImpl())
            }

        assertThat(widget.isSelectable).isFalse()
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
            addActivityIfNotPresent(ComponentName(appContext, FragmentActivity::class.java))
        }
    }

    private fun launchActivityAndRunTest(test: (activity: Activity) -> Unit) {
        ActivityScenario.launch(FragmentActivity::class.java).use {
            it.onActivity { activity -> test(activity) }
        }
    }
}

private class AccessibilityFooterPreferenceBindingImpl : AccessibilityFooterPreferenceBinding

private open class AccessibilityFooterPreferenceMetadataImpl :
    AccessibilityFooterPreferenceMetadata {
    override val key: String
        get() = KEY

    companion object {
        const val KEY = "prefKey"
    }
}
