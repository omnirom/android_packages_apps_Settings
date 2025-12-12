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

package com.android.settings.screensaver

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Resources
import android.provider.Settings.Secure.SCREENSAVER_COMPONENTS
import android.provider.Settings.Secure.SCREENSAVER_ENABLED
import com.android.internal.R.bool.config_dreamsDisabledByAmbientModeSuppressionConfig
import com.android.settings.dream.ScreensaverScreen
import com.android.settings.flags.Flags
import com.android.settings.testutils2.SettingsCatalystTestCase
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.dream.DreamBackend
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

class ScreensaverScreenTest : SettingsCatalystTestCase() {
    private val mockResources = mock<Resources>()
    private val settingsStore = mock<KeyValueStore>()
    private val context =
        object : ContextWrapper(appContext) {
            override fun getResources(): Resources = mockResources
        }

    private val dreamBackend: DreamBackend = mock(DreamBackend::class.java)
    private var dreamEnabled = false
    private var activeDreamName = DREAM_NAME

    override val preferenceScreenCreator =
        ScreensaverScreen(context).also {
            it.setDreamBackend(dreamBackend)
            it.setScreensaverStore(settingsStore)
        }

    override val flagName: String
        get() = Flags.FLAG_CATALYST_SCREENSAVER

    @Before
    fun setUp() {
        preferenceScreenCreator.setAmbientModeSuppressionProvider(
            object : ScreensaverScreen.AmbientModeSuppressionProvider {
                override fun isSuppressedByBedtime(context: Context) = false
            }
        )

        preferenceScreenCreator.setSummaryStringsProvider(
            object : ScreensaverScreen.SummaryStringsProvider {
                override fun dreamOff(context: Context) = SCREENSAVER_SUMMARY_OFF

                override fun dreamOn(context: Context, activeDreamName: CharSequence) =
                    getSummaryOnWithDreamName(activeDreamName)

                override fun dreamOffBedtime(context: Context) = SCREENSAVER_SUMMARY_OFF_BEDTIME
            }
        )
        settingsStore.stub {
            on { getString(SCREENSAVER_COMPONENTS) } doAnswer { activeDreamName }
            on { getBoolean(SCREENSAVER_ENABLED) } doAnswer { dreamEnabled }
        }
    }

    @Test
    fun getSummary_dreamsNotDisabledByAmbientModeSuppression_dreamsDisabled() {
        mockResources.stub {
            on { getBoolean(config_dreamsDisabledByAmbientModeSuppressionConfig) } doReturn false
        }

        dreamBackend.stub { on { isEnabled } doReturn false }

        assertThat(preferenceScreenCreator.getSummary(context)).isEqualTo(SCREENSAVER_SUMMARY_OFF)
    }

    @Test
    fun getSummary_dreamsNotDisabledByAmbientModeSuppression_dreamsEnabled() {
        mockResources.stub {
            on { getBoolean(config_dreamsDisabledByAmbientModeSuppressionConfig) } doReturn false
        }

        dreamBackend.stub {
            on { isEnabled } doReturn true
            on { activeDreamName } doReturn DREAM_NAME
        }

        assertThat(preferenceScreenCreator.getSummary(context))
            .isEqualTo(getSummaryOnWithDreamName(DREAM_NAME))
    }

    @Test
    fun getSummary_dreamsDisabledByAmbientModeSuppression() {
        mockResources.stub {
            on { getBoolean(config_dreamsDisabledByAmbientModeSuppressionConfig) } doReturn true
        }

        preferenceScreenCreator.setAmbientModeSuppressionProvider(
            object : ScreensaverScreen.AmbientModeSuppressionProvider {
                override fun isSuppressedByBedtime(context: Context) = true
            }
        )

        assertThat(preferenceScreenCreator.getSummary(context))
            .isEqualTo(SCREENSAVER_SUMMARY_OFF_BEDTIME)
    }

    @Test
    fun getSummary_onScreenSaverEnabledChanged() {
        mockResources.stub {
            on { getBoolean(config_dreamsDisabledByAmbientModeSuppressionConfig) } doReturn false
        }

        dreamBackend.stub {
            on { isEnabled } doAnswer { settingsStore.getBoolean(SCREENSAVER_ENABLED) }
            on { activeDreamName } doReturn DREAM_NAME
        }

        dreamEnabled = true
        assertThat(preferenceScreenCreator.getSummary(context))
            .isEqualTo(getSummaryOnWithDreamName(DREAM_NAME))

        dreamEnabled = false
        assertThat(preferenceScreenCreator.getSummary(context)).isEqualTo(SCREENSAVER_SUMMARY_OFF)
    }

    @Test
    fun getSummary_onActiveDreamChanged() {
        mockResources.stub {
            on { getBoolean(config_dreamsDisabledByAmbientModeSuppressionConfig) } doReturn false
        }

        dreamBackend.stub {
            on { isEnabled } doReturn true
            on { activeDreamName } doAnswer { settingsStore.getString(SCREENSAVER_COMPONENTS) }
        }

        activeDreamName = DREAM_NAME
        assertThat(preferenceScreenCreator.getSummary(context))
            .isEqualTo(getSummaryOnWithDreamName(DREAM_NAME))

        activeDreamName = DREAM_NAME_2
        assertThat(preferenceScreenCreator.getSummary(context))
            .isEqualTo(getSummaryOnWithDreamName(DREAM_NAME_2))
    }

    private fun getSummaryOnWithDreamName(dreamName: CharSequence) =
        SCREENSAVER_SUMMARY_ON + dreamName

    @Test
    override fun migration() {}

    private companion object {
        const val SCREENSAVER_SUMMARY_OFF = "screensaver_summary_dream_off"
        const val SCREENSAVER_SUMMARY_ON = "screensaver_summary_on"
        const val SCREENSAVER_SUMMARY_OFF_BEDTIME = "screensaver_summary_off_bedtime"
        const val DREAM_NAME = "dream"
        const val DREAM_NAME_2 = "second dream"
    }
}
