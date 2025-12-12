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

package com.android.settings.display

import android.content.ContextWrapper
import android.hardware.display.ColorDisplayManager
import android.provider.Settings.Secure
import com.android.settings.Settings.ColorModeActivity
import com.android.settings.flags.Flags
import com.android.settings.testutils2.SettingsCatalystTestCase
import com.android.settingslib.datastore.SettingsSecureStore
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements

class ColorModeScreenTest : SettingsCatalystTestCase() {

    override val flagName: String
        get() = Flags.FLAG_DEEPLINK_DISPLAY_AND_TOUCH_25Q4

    override val preferenceScreenCreator = ColorModeScreen()
    private val mockColorDisplayManager = mock<ColorDisplayManager>()

    private val context =
        object : ContextWrapper(appContext) {
            override fun getSystemService(name: String): Any? = mockColorDisplayManager
        }

    @Before
    fun setUp() {
        SettingsSecureStore.get(context).setInt(Secure.ACCESSIBILITY_DISPLAY_INVERSION_ENABLED, 0)
        SettingsSecureStore.get(context).setInt(Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED, 0)
    }

    @Test
    fun key_isEqualToStatic() {
        assertThat(preferenceScreenCreator.key).isEqualTo(ColorModeScreen.KEY)
    }

    @Test
    fun getLaunchIntent_correctActivity() {
        val underTest = preferenceScreenCreator.getLaunchIntent(appContext, null)

        assertThat(underTest.component?.className)
            .isEqualTo(ColorModeActivity::class.java.getName())
    }

    @Test
    fun isAvailable_deviceNotColorManaged_returnsFalse() {
        mockColorDisplayManager.stub { on { isDeviceColorManaged } doReturn false }

        assertThat(preferenceScreenCreator.isAvailable(context)).isEqualTo(false)
    }

    @Test
    fun isAvailable_accessibilityInversionEnabled_returnsFalse() {
        mockColorDisplayManager.stub { on { isDeviceColorManaged } doReturn true }
        SettingsSecureStore.get(context).setInt(Secure.ACCESSIBILITY_DISPLAY_INVERSION_ENABLED, 1)

        assertThat(preferenceScreenCreator.isAvailable(context)).isEqualTo(false)
    }

    @Test
    fun isAvailable_accessibilityDaltonizerEnabled_returnsFalse() {
        mockColorDisplayManager.stub { on { isDeviceColorManaged } doReturn true }
        SettingsSecureStore.get(context).setInt(Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED, 1)

        assertThat(preferenceScreenCreator.isAvailable(context)).isEqualTo(false)
    }

    @Test
    fun isAvailable_deviceColorManagedAccessibilityFeaturesDisabled_returnsTrue() {
        mockColorDisplayManager.stub { on { isDeviceColorManaged } doReturn true }
        assertThat(preferenceScreenCreator.isAvailable(context)).isEqualTo(true)
    }

    @Test
    @Config(shadows = [ShadowColorDisplayManager::class])
    override fun migration() {
        super.migration()
    }
}

@Implements(ColorDisplayManager::class)
class ShadowColorDisplayManager {
    @Implementation fun isDeviceColorManaged(): Boolean = true
}
