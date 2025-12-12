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

package com.android.settings.language

import android.app.IActivityManager
import android.content.res.Configuration
import android.os.LocaleList
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import com.android.settings.Settings
import com.android.settings.flags.Flags
import com.android.settings.testutils.shadow.ShadowActivityManager
import com.android.settings.testutils2.SettingsCatalystTestCase
import com.google.common.truth.Truth
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.robolectric.annotation.Config

class LanguageAndRegionScreenTest : SettingsCatalystTestCase() {
    override val preferenceScreenCreator = LanguageAndRegionScreen()

    override val flagName: String
        get() = Flags.FLAG_DEEPLINK_SYSTEM_25Q4

    @Test
    fun getLaunchIntent_correctActivity() {
        val underTest = preferenceScreenCreator.getLaunchIntent(appContext, null)

        Truth.assertThat(underTest.component?.className)
            .isEqualTo(Settings.LanguageAndRegionSettingsActivity::class.java.getName())
    }

    @Test
    @EnableFlags(Flags.FLAG_REGIONAL_PREFERENCES_API_ENABLED)
    fun isFlagEnabled_regionalFlagEnabled_returnFalse() {
        Truth.assertThat(preferenceScreenCreator.isFlagEnabled(appContext)).isFalse()
    }

    @Test
    @DisableFlags(Flags.FLAG_REGIONAL_PREFERENCES_API_ENABLED)
    fun isFlagEnabled_regionalFlagDisabled_returnTrue() {
        Truth.assertThat(preferenceScreenCreator.isFlagEnabled(appContext)).isTrue()
    }

    @Test
    @DisableFlags(Flags.FLAG_REGIONAL_PREFERENCES_API_ENABLED)
    @Config(shadows = [ShadowActivityManager::class])
    override fun migration() {
        val config = Configuration()
        config.setLocales(LocaleList.forLanguageTags("en-US,ak-GH,es-US"))
        val mockActivityManager: IActivityManager =
            mock<IActivityManager> { on { configuration } doReturn config }
        ShadowActivityManager.setService(mockActivityManager)

        super.migration()
    }
}
