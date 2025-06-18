/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.activityembedding

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.SettingsActivity.EXTRA_IS_DEEPLINK_HOME_STARTED_FROM_SEARCH
import com.android.settings.activityembedding.EmbeddedDeepLinkUtils.getTrampolineIntent
import com.android.settings.activityembedding.EmbeddedDeepLinkUtils.getTrampolineIntentForSearchResult
import com.android.settings.flags.Flags
import com.android.settings.homepage.DeepLinkHomepageActivityInternal
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EmbeddedDeepLinkUtilsTest {
    @get:Rule
    val setFlagsRule = SetFlagsRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun getTrampolineIntent_intentSelector_shouldNotChangeIntentAction() {
        val targetIntent = Intent().setClassName(
            "android",
            "com.android.internal.app.PlatLogoActivity"
        )
        val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS).apply {
            setComponent(resolveActivity(context.packageManager))
            setSelector(
                Intent().setData(
                    Uri.fromParts(
                        targetIntent.toUri(Intent.URI_INTENT_SCHEME),
                        /* ssp= */ "",
                        /* fragment= */ null,
                    )
                )
            )
        }

        val resultIntent = getTrampolineIntent(intent, "menu_key")

        val intentUriString =
            resultIntent.getStringExtra(Settings.EXTRA_SETTINGS_EMBEDDED_DEEP_LINK_INTENT_URI)
        val parsedIntent = Intent.parseUri(intentUriString, Intent.URI_INTENT_SCHEME)
        assertThat(parsedIntent.action).isEqualTo(intent.action)
    }

    @Test
    fun getTrampolineIntent_shouldNotHaveNewTaskFlag() {
        val intent = Intent("com.android.settings.SEARCH_RESULT_TRAMPOLINE")

        val resultIntent = getTrampolineIntent(intent, "menu_key")

        val hasNewTaskFlag = (resultIntent.flags and Intent.FLAG_ACTIVITY_NEW_TASK) != 0
        assertThat(hasNewTaskFlag).isFalse()
    }

    @Test
    fun getTrampolineIntentForSearchResult_shouldHaveDeepLinkHomepageClass() {
        val intent = Intent("com.android.settings.SEARCH_RESULT_TRAMPOLINE")

        val resultIntent = getTrampolineIntentForSearchResult(context, intent, "menu_key")

        val className = resultIntent.getComponent()!!.className
        assertThat(className).isEqualTo(DeepLinkHomepageActivityInternal::class.java.name)
    }

    @Test
    @DisableFlags(Flags.FLAG_SETTINGS_SEARCH_RESULT_DEEP_LINK_IN_SAME_TASK)
    fun getTrampolineIntentForSearchResult_shouldHaveNewTaskFlag() {
        val intent = Intent("com.android.settings.SEARCH_RESULT_TRAMPOLINE")

        val resultIntent = getTrampolineIntentForSearchResult(context, intent, "menu_key")

        val hasNewTaskFlag = (resultIntent.flags and Intent.FLAG_ACTIVITY_NEW_TASK) != 0
        assertThat(hasNewTaskFlag).isTrue()
    }

    @Test
    @EnableFlags(Flags.FLAG_SETTINGS_SEARCH_RESULT_DEEP_LINK_IN_SAME_TASK)
    fun getTrampolineIntentForSearchResult_shouldNotHaveNewTaskFlag() {
        val intent = Intent("com.android.settings.SEARCH_RESULT_TRAMPOLINE")

        val resultIntent = getTrampolineIntentForSearchResult(context, intent, "menu_key")

        val hasNewTaskFlag = (resultIntent.flags and Intent.FLAG_ACTIVITY_NEW_TASK) != 0
        assertThat(hasNewTaskFlag).isFalse()
    }

    @Test
    @DisableFlags(Flags.FLAG_SETTINGS_SEARCH_RESULT_DEEP_LINK_IN_SAME_TASK)
    fun getTrampolineIntentForSearchResult_shouldNotHaveExtraStartedFromSearch() {
        val intent = Intent("com.android.settings.SEARCH_RESULT_TRAMPOLINE")

        val resultIntent = getTrampolineIntentForSearchResult(context, intent, "menu_key")

        assertThat(resultIntent.hasExtra(EXTRA_IS_DEEPLINK_HOME_STARTED_FROM_SEARCH)).isFalse()
        assertThat(
            resultIntent.getBooleanExtra(EXTRA_IS_DEEPLINK_HOME_STARTED_FROM_SEARCH, false)
        ).isFalse()
    }

    @Test
    @EnableFlags(Flags.FLAG_SETTINGS_SEARCH_RESULT_DEEP_LINK_IN_SAME_TASK)
    fun getTrampolineIntentForSearchResult_shouldHaveExtraStartedFromSearch() {
        val intent = Intent("com.android.settings.SEARCH_RESULT_TRAMPOLINE")

        val resultIntent = getTrampolineIntentForSearchResult(context, intent, "menu_key")

        assertThat(resultIntent.hasExtra(EXTRA_IS_DEEPLINK_HOME_STARTED_FROM_SEARCH)).isTrue()
        assertThat(
            resultIntent.getBooleanExtra(EXTRA_IS_DEEPLINK_HOME_STARTED_FROM_SEARCH, false)
        ).isTrue()
    }
}
