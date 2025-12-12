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

package com.android.settings.safetycenter

import android.app.Application
import android.content.Context
import android.os.Build
import android.safetycenter.SafetyCenterData
import android.safetycenter.SafetyCenterEntryOrGroup
import android.safetycenter.SafetyCenterIssue
import android.safetycenter.SafetyCenterManager
import android.safetycenter.SafetyCenterStaticEntryGroup
import android.safetycenter.SafetyCenterStatus
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.swipeUp
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.safetycenter.ui.SafetyCenterFragment
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowContextImpl
import org.robolectric.shadows.ShadowLooper

@RunWith(AndroidJUnit4::class)
@Config(minSdk = Build.VERSION_CODES.BAKLAVA)
class SafetyCenterFragmentTest {

    private lateinit var mApplication: Application
    private val mockSafetyCenterManager = mock<SafetyCenterManager>()

    @Before
    fun setUp() {
        mApplication = ApplicationProvider.getApplicationContext()

        val shadowContextImpl = Shadow.extract<ShadowContextImpl>(mApplication.baseContext)
        shadowContextImpl.setSystemService(Context.SAFETY_CENTER_SERVICE, mockSafetyCenterManager)

        val dummyStatus =
            SafetyCenterStatus.Builder("Test Title", "Test Summary")
                .setSeverityLevel(SafetyCenterStatus.OVERALL_SEVERITY_LEVEL_OK)
                .build()

        val dummySafetyCenterData =
            SafetyCenterData(
                dummyStatus,
                emptyList<SafetyCenterIssue>(),
                emptyList<SafetyCenterEntryOrGroup>(),
                emptyList<SafetyCenterStaticEntryGroup>(),
            )

        whenever(mockSafetyCenterManager.safetyCenterData) doReturn dummySafetyCenterData
    }

    private fun launchScenario(): FragmentScenario<SafetyCenterFragment> {
        return launchFragmentInContainer<SafetyCenterFragment>()
    }

    @Test
    fun shouldShowAllPreferences() {
        val scenario = launchScenario()

        scenario.onFragment { fragment ->
            ShadowLooper.idleMainLooper()

            onView(withText(mApplication.getString(R.string.security_header)))
                .check(matches(isDisplayed()))

            onView(withText(mApplication.getString(R.string.device_unlock_subpage_title)))
                .check(matches(isDisplayed()))

            onView(withText(mApplication.getString(R.string.privacy_dashboard_title)))
                .check(matches(isDisplayed()))

            onView(withText(mApplication.getString(R.string.privacy_sources_title)))
                .check(matches(isDisplayed()))

            onView(withText(mApplication.getString(R.string.permissions_usage_title)))
                .perform(scrollTo())
                .check(matches(isDisplayed()))

            onView(isRoot()).perform(swipeUp())
            onView(withText(mApplication.getString(R.string.more_security_privacy_category_title)))
                .perform(scrollTo())
                .check(matches(isDisplayed()))

            onView(withText(mApplication.getString(R.string.more_security_privacy_settings)))
                .check(matches(isDisplayed()))
        }
    }
}
