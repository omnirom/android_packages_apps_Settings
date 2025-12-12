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
import android.content.Intent
import android.location.LocationManager
import android.view.View
import com.android.settings.R
import com.android.settings.Settings.LocationSettingsActivity
import com.android.settings.testutils.inflateViewHolder
import com.android.settingslib.preference.createAndBindWidget
import com.android.settingslib.widget.BannerMessagePreference
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameters
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestParameterInjector

@RunWith(RobolectricTestParameterInjector::class)
class TwilightLocationPreferenceTest {
    private val mockLocationManager = mock<LocationManager>()
    private val context =
        spy(Robolectric.buildActivity(Activity::class.java).create().get()) {
            on { getSystemService(LocationManager::class.java) } doReturn mockLocationManager
        }
    private val preference = TwilightLocationPreference()

    @Test
    fun key() {
        assertThat(preference.key).isEqualTo(TwilightLocationPreference.KEY)
    }

    @Test
    fun getTitle() {
        assertThat(preference.title).isEqualTo(R.string.twilight_mode_location_off_dialog_message)
    }

    @Test
    @TestParameters(value = ["{locationEnabled: false}", "{locationEnabled: true}"])
    fun isAvailable(locationEnabled: Boolean) {
        mockLocationManager.stub { on { isLocationEnabled } doReturn locationEnabled }

        assertThat(preference.isAvailable(context)).isEqualTo(!locationEnabled)
    }

    @Test
    fun isIndexable() {
        assertThat(preference.indexable).isFalse()
    }

    @Test
    fun performClick() {
        preference.createAndBindWidget<BannerMessagePreference>(context).also {
            val holder = it.inflateViewHolder()
            val positiveButton = holder.itemView.findViewById<View?>(R.id.banner_positive_btn)
            assertThat(positiveButton).isNotNull()

            positiveButton?.performClick()

            val intentCaptor = ArgumentCaptor.forClass(Intent::class.java)
            verify(context).startActivity(intentCaptor.capture())
            val intent = intentCaptor.value
            assertThat(intent.component?.className)
                .isEqualTo(LocationSettingsActivity::class.java.name)
        }
    }
}
