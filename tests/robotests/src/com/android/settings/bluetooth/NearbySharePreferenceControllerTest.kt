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

package com.android.settings.bluetooth

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import com.android.settings.R
import com.android.settingslib.widget.LayoutPreference
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.eq
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NearbySharePreferenceControllerTest : BluetoothDetailsControllerTestBase() {
    @get:Rule val mockitoRule: MockitoRule = MockitoJUnit.rule()

    @Mock private lateinit var intent: Intent
    @Mock private lateinit var packageManager: PackageManager
    @Mock private lateinit var activityInfo: ActivityInfo

    private lateinit var context: Context
    private lateinit var controller: NearbySharePreferenceController

    override fun setUp() {
        super.setUp()
        context = spy(mContext)
        whenever(context.packageManager).thenReturn(packageManager)
        whenever(
                packageManager.getActivityInfo(
                    eq(ComponentName.unflattenFromString(COMPONENT_NAME)!!),
                    eq(PackageManager.GET_META_DATA),
                )
            )
            .thenReturn(activityInfo)

        controller = NearbySharePreferenceController(context, PREF_KEY)
    }

    @Test
    fun noIntent_notAvailable() {
        Settings.Secure.putString(
            context.contentResolver,
            Settings.Secure.NEARBY_SHARING_COMPONENT,
            COMPONENT_NAME,
        )
        whenever(activityInfo.loadLabel(any())).thenReturn("App")

        assertThat(controller.isAvailable).isFalse()
    }

    @Test
    fun noNearbyComponent_notAvailable() {
        controller.init(intent)

        assertThat(controller.isAvailable).isFalse()
    }

    @Test
    fun hasIntentAndNearbyComponent_available() {
        Settings.Secure.putString(
            context.contentResolver,
            Settings.Secure.NEARBY_SHARING_COMPONENT,
            COMPONENT_NAME,
        )
        whenever(activityInfo.loadLabel(any())).thenReturn("App")
        controller.init(intent)

        assertThat(controller.isAvailable).isTrue()
    }

    @Test
    fun clickPreference_startActivity() {
        Settings.Secure.putString(
            context.contentResolver,
            Settings.Secure.NEARBY_SHARING_COMPONENT,
            COMPONENT_NAME,
        )
        whenever(activityInfo.loadLabel(any())).thenReturn("App")
        controller.init(intent)
        doNothing().whenever(context).startActivity(any())
        val pref =
            LayoutPreference(
                context,
                LayoutInflater.from(context).inflate(R.layout.nearby_sharing_suggestion_card, null),
            )
        pref.key = PREF_KEY
        mScreen.addPreference(pref)
        controller.displayPreference(mScreen)

        pref.findViewById<View>(R.id.card_container).performClick()

        verify(context).startActivity(intent)
    }

    private companion object {
        const val COMPONENT_NAME = "com.example/.BComponent"
        const val PREF_KEY = "key"
    }
}
