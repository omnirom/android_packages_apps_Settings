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
package com.android.settings.applications

import android.content.ContextWrapper
import android.content.res.Resources
import android.permission.PermissionControllerManager
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import androidx.annotation.NonNull
import com.android.settings.R
import com.android.settings.Settings
import com.android.settings.flags.Flags
import com.android.settings.testutils2.SettingsCatalystTestCase
import com.android.settingslib.widget.theme.flags.Flags.FLAG_IS_EXPRESSIVE_DESIGN_ENABLED
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executor
import java.util.function.IntConsumer
import org.junit.Test
import org.mockito.kotlin.mock
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowPermissionControllerManager

// To avoid NPE in HibernatedAppsPreferenceController.updatePreference
@Config(shadows = [ShadowPermController::class])
class AppDashboardScreenTest : SettingsCatalystTestCase() {

    private val mockResources = mock<Resources>()

    private val context =
        object : ContextWrapper(appContext) {
            override fun getResources(): Resources = mockResources
        }

    override val preferenceScreenCreator = AppDashboardScreen()

    override val flagName: String
        get() = Flags.FLAG_DEEPLINK_APPS_25Q4

    @Test
    fun key_isEqualToStatic() {
        assertThat(preferenceScreenCreator.key).isEqualTo(AppDashboardScreen.KEY)
    }

    @Test
    fun getLaunchIntent_correctActivity() {
        val underTest = preferenceScreenCreator.getLaunchIntent(appContext, null)

        assertThat(underTest?.component?.className)
            .isEqualTo(Settings.AppDashboardActivity::class.java.getName())
    }

    @Test
    @EnableFlags(FLAG_IS_EXPRESSIVE_DESIGN_ENABLED)
    fun getIcon_isExpressiveTheme() {
        assertThat(preferenceScreenCreator.getIcon(context)).isEqualTo(R.drawable.ic_homepage_apps)
    }

    @Test
    @DisableFlags(FLAG_IS_EXPRESSIVE_DESIGN_ENABLED)
    fun getIcon_notExpressiveTheme() {
        assertThat(preferenceScreenCreator.getIcon(context)).isEqualTo(R.drawable.ic_apps_filled)
    }
}

@Implements(PermissionControllerManager::class)
class ShadowPermController : ShadowPermissionControllerManager() {
    @Implementation
    fun getUnusedAppCount(@NonNull executor: Executor, @NonNull callback: IntConsumer) {
        callback.accept(0)
    }
}
