/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.biometrics

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.android.settings.R
import com.android.settings.safetycenter.IdentityCheckSafetySource
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule

@RunWith(AndroidJUnit4::class)
@SmallTest
class IdentityCheckPromoCardActivityTest {
    @get:Rule val mockitoRule: MockitoRule = MockitoJUnit.rule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun launchActivity_showsDialog() {
        ActivityScenario.launch<IdentityCheckPromoCardActivity>(getIntent()).use {
            onView(withId(R.id.bottom_sheet)).inRoot(isDialog()).check(matches(isDisplayed()))
        }
        assertThat(hasPromoCardBeenShown()).isTrue()
    }

    @Test
    fun launchActivity_showsFragment_showCardDetails() {
        val expectedTitle = context.getString(R.string.identity_check_promo_card_title)
        val expectedSummary = context.getString(R.string.identity_check_promo_card_summary)

        ActivityScenario.launch<IdentityCheckPromoCardActivity>(
                getIntent(IdentityCheckSafetySource.ACTION_ISSUE_CARD_SHOW_DETAILS)
            )
            .use {
                onView(withId(R.id.illustration)).inRoot(isDialog()).check(matches(isDisplayed()))
                onView(withId(R.id.title)).check(matches(withText(expectedTitle)))
                onView(withId(R.id.summary)).check(matches(withText(expectedSummary)))
            }
    }

    @Test
    fun launchActivity_showsFragment_showWatchCardDetails() {
        val watchTitleString = context.getString(R.string.identity_check_watch_promo_card_title)
        val watchSummaryString = context.getString(R.string.identity_check_promo_card_watch_summary)
        val shouldShowWatchStrings =
            context.resources.getBoolean(R.bool.config_show_identity_check_watch_promo)
        val expectedTitle =
            if (shouldShowWatchStrings) {
                watchTitleString
            } else {
                context.getString(R.string.identity_check_promo_card_title)
            }
        val expectedSummary =
            if (shouldShowWatchStrings) {
                watchSummaryString
            } else {
                context.getString(R.string.identity_check_promo_card_summary)
            }

        ActivityScenario.launch<IdentityCheckPromoCardActivity>(
                getIntent(IdentityCheckSafetySource.ACTION_ISSUE_CARD_WATCH_SHOW_DETAILS)
            )
            .use {
                onView(withId(R.id.illustration)).inRoot(isDialog()).check(matches(isDisplayed()))
                onView(withId(R.id.title)).check(matches(withText(expectedTitle)))
                onView(withId(R.id.summary)).check(matches(withText(expectedSummary)))
            }
    }

    private fun getIntent(action: String = "") =
        Intent(context, IdentityCheckPromoCardActivity::class.java).setAction(action)

    private fun hasPromoCardBeenShown(): Boolean {
        return Settings.Secure.getInt(
            context.contentResolver,
            Settings.Secure.IDENTITY_CHECK_PROMO_CARD_SHOWN,
            0,
        ) == 1
    }
}
