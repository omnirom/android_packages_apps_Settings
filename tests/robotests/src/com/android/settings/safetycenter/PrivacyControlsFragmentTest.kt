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
import android.hardware.SensorPrivacyManager
import android.os.Build
import androidx.fragment.app.testing.FragmentScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.swipeUp
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.safetycenter.ui.PrivacyControlsFragment
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.anyInt
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(minSdk = Build.VERSION_CODES.BAKLAVA)
class PrivacyControlsFragmentTest {

    private lateinit var mApplication: Application
    @Mock private lateinit var mSensorPrivacyManager: SensorPrivacyManager

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        mApplication = ApplicationProvider.getApplicationContext()

        Shadows.shadowOf(mApplication)
            .setSystemService(Context.SENSOR_PRIVACY_SERVICE, mSensorPrivacyManager)

        whenever(mSensorPrivacyManager.supportsSensorToggle(anyInt())).thenReturn(true)
    }

    @Test
    fun shouldShowAllPreferences() {
        val scenario = FragmentScenario.launchInContainer(PrivacyControlsFragment::class.java)

        scenario.onFragment { fragment ->
            onView(withText(mApplication.getString(R.string.app_permissions)))
                .check(matches(isDisplayed()))

            onView(withText(mApplication.getString(R.string.privacy_controls_title)))
                .check(matches(isDisplayed()))

            onView(withText(mApplication.getString(R.string.camera_toggle_title)))
                .check(matches(isDisplayed()))

            onView(withText(mApplication.getString(R.string.mic_toggle_title)))
                .check(matches(isDisplayed()))

            onView(withText(mApplication.getString(R.string.show_clip_access_notification)))
                .check(matches(isDisplayed()))

            onView(isRoot()).perform(swipeUp())
            onView(withText(mApplication.getString(R.string.show_password)))
                .check(matches(isDisplayed()))

            onView(isRoot()).perform(swipeUp())
            onView(withText(mApplication.getString(R.string.location_settings)))
                .check(matches(isDisplayed()))
        }
    }
}
