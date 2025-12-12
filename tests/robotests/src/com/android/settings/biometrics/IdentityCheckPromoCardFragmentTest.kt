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
import android.content.DialogInterface
import android.content.DialogInterface.OnDismissListener
import android.os.Bundle
import androidx.fragment.app.testing.launchFragment
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.safetycenter.IdentityCheckSafetySource
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule

@RunWith(AndroidJUnit4::class)
class IdentityCheckPromoCardFragmentTest {
    @get:Rule val mockitoRule: MockitoRule = MockitoJUnit.rule()

    @Mock lateinit var onDismissListener: OnDismissListener
    @Mock lateinit var dialogInterface: DialogInterface

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val watchBundle = Bundle()

    @Test
    fun launchFragment_onDismissListener() {
        launchFragment { IdentityCheckPromoCardFragment() }
            .onFragment { fragment ->
                fragment.setOnDismissListener(onDismissListener)
                fragment.onDismiss(dialogInterface)
                verify(onDismissListener).onDismiss(dialogInterface)
            }
    }

    @Test
    fun launchFragment_showDialog() {
        launchFragment { IdentityCheckPromoCardFragment() }
            .onFragment {
                onView(withId(R.id.bottom_sheet)).inRoot(isDialog()).check(matches(isDisplayed()))
            }
    }

    @Test
    fun launchFragment_watchPromoCard_checkContent() {
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
        watchBundle.putString(
            IdentityCheckPromoCardFragment.KEY_INTENT_ACTION,
            IdentityCheckSafetySource.ACTION_ISSUE_CARD_WATCH_SHOW_DETAILS,
        )
        launchFragmentInContainer(watchBundle) { IdentityCheckPromoCardFragment() }
            .onFragment {
                onView(withId(R.id.illustration)).check(matches(isDisplayed()))
                onView(withId(R.id.title)).check(matches(withText(expectedTitle)))
                onView(withId(R.id.summary)).check(matches(withText(expectedSummary)))
            }
    }

    @Test
    fun launchFragment_generalPromoCard_checkContent() {
        val expectedTitle = context.getString(R.string.identity_check_promo_card_title)
        val expectedSummary = context.getString(R.string.identity_check_promo_card_summary)
        watchBundle.putString(
            IdentityCheckPromoCardFragment.KEY_INTENT_ACTION,
            IdentityCheckSafetySource.ACTION_ISSUE_CARD_SHOW_DETAILS,
        )
        launchFragmentInContainer(watchBundle) { IdentityCheckPromoCardFragment() }
            .onFragment {
                onView(withId(R.id.illustration)).check(matches(isDisplayed()))
                onView(withId(R.id.title)).check(matches(withText(expectedTitle)))
                onView(withId(R.id.summary)).check(matches(withText(expectedSummary)))
            }
    }
}
