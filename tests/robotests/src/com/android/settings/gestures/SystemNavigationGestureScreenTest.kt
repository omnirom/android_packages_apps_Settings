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
package com.android.settings.gestures

import android.content.ComponentName
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.pm.ServiceInfo
import android.content.res.Resources
import android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_2BUTTON
import android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_3BUTTON
import android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_GESTURAL
import com.android.internal.R
import com.android.settings.Settings
import com.android.settings.flags.Flags
import com.android.settings.testutils.SystemProperty
import com.android.settings.testutils2.SettingsCatalystTestCase
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.robolectric.Shadows

class SystemNavigationGestureScreenTest : SettingsCatalystTestCase() {

    override val preferenceScreenCreator = SystemNavigationGestureScreen()
    override val flagName: String
        get() = Flags.FLAG_DEEPLINK_SYSTEM_25Q4

    private val mockResources = mock<Resources>()
    private val mockPackageManager = mock<PackageManager>()

    private val context =
        object : ContextWrapper(appContext) {
            override fun getResources(): Resources = mockResources

            override fun getPackageManager(): PackageManager = mockPackageManager
        }

    @Test
    fun isAvailable_configSwipeUpGestureSettingUnavailable_shouldReturnFalse() {
        mockResources.stub {
            on { getBoolean(R.bool.config_swipe_up_gesture_setting_available) } doReturn false
        }

        assertThat(preferenceScreenCreator.isAvailable(context)).isFalse()
    }

    @Test
    fun isAvailable_recentsComponentNotDefined_shouldReturnFalse() {

        mockResources.stub {
            on { getBoolean(R.bool.config_swipe_up_gesture_setting_available) } doReturn true
            on { getString(R.string.config_recentsComponentName) } doReturn ""
        }

        assertThat(preferenceScreenCreator.isAvailable(context)).isFalse()
    }

    @Test
    fun isAvailable_serviceDoesNotExist_shouldReturnFalse() {

        mockResources.stub {
            on { getBoolean(R.bool.config_swipe_up_gesture_setting_available) } doReturn true
            on { getString(R.string.config_recentsComponentName) } doReturn
                TEST_RECENTS_COMPONENT_NAME
        }

        assertThat(preferenceScreenCreator.isAvailable(context)).isFalse()
    }

    @Test
    fun isAvailable_serviceExists_shouldReturnTrue() {

        mockResources.stub {
            on { getBoolean(R.bool.config_swipe_up_gesture_setting_available) } doReturn true
            on { getString(R.string.config_recentsComponentName) } doReturn
                TEST_RECENTS_COMPONENT_NAME
        }

        val recentsComponentName =
            ComponentName.unflattenFromString(
                context.getString(R.string.config_recentsComponentName)
            )
        val resolveInfo = ResolveInfo()
        resolveInfo.serviceInfo = ServiceInfo()
        resolveInfo.resolvePackageName = recentsComponentName?.packageName
        resolveInfo.serviceInfo.packageName = resolveInfo.resolvePackageName
        resolveInfo.serviceInfo.name = recentsComponentName?.className
        resolveInfo.serviceInfo.applicationInfo = ApplicationInfo()
        resolveInfo.serviceInfo.applicationInfo.flags = ApplicationInfo.FLAG_SYSTEM

        mockPackageManager.stub {
            on { resolveService(any(), eq(PackageManager.MATCH_SYSTEM_ONLY)) } doReturn resolveInfo
        }

        assertThat(preferenceScreenCreator.isAvailable(context)).isTrue()
    }

    @Test
    fun getSummary_navBarModeGestural_returnsCorrectSummary() {

        mockResources.stub {
            on { getInteger(R.integer.config_navBarInteractionMode) } doReturn NAV_BAR_MODE_GESTURAL
        }

        assertThat(preferenceScreenCreator.getSummary(context))
            .isEqualTo(context.getText(com.android.settings.R.string.edge_to_edge_navigation_title))
    }

    @Test
    fun getSummary_navBarMode3Button_returnsCorrectSummary() {

        mockResources.stub {
            on { getInteger(R.integer.config_navBarInteractionMode) } doReturn NAV_BAR_MODE_3BUTTON
        }

        assertThat(preferenceScreenCreator.getSummary(context))
            .isEqualTo(context.getText(com.android.settings.R.string.legacy_navigation_title))
    }

    @Test
    fun getSummary_navBarMode2Button_returnsCorrectSummary() {

        mockResources.stub {
            on { getInteger(R.integer.config_navBarInteractionMode) } doReturn NAV_BAR_MODE_2BUTTON
        }

        assertThat(preferenceScreenCreator.getSummary(context))
            .isEqualTo(context.getText(com.android.settings.R.string.swipe_up_to_switch_apps_title))
    }

    @Test
    fun getLaunchIntent_returnsCorrectActivity() {
        val launchIntent = preferenceScreenCreator.getLaunchIntent(context, null)

        assertThat(launchIntent.component?.className)
            .isEqualTo(Settings.NavigationModeSettingsActivity::class.java.name)
    }

    @Test
    fun key_isEqualToStatic() {
        assertThat(preferenceScreenCreator.key).isEqualTo(SystemNavigationGestureScreen.KEY)
    }

    @Test
    override fun migration() {
        // avoid UnsupportedOperationException when getDisplay from context
        SystemProperty("robolectric.createActivityContexts", "true").use {

            // Make sure resolve info for quickstep intent is added to the package manager
            val recentsComponentName =
                ComponentName.unflattenFromString(
                    appContext.getString(R.string.config_recentsComponentName)
                )

            val quickStepIntent =
                Intent(ACTION_QUICKSTEP).apply { setPackage(recentsComponentName?.packageName) }

            val resolveInfo =
                ResolveInfo().apply {
                    resolvePackageName = recentsComponentName?.packageName
                    serviceInfo =
                        ServiceInfo().apply {
                            packageName = resolvePackageName
                            name = recentsComponentName?.className
                            applicationInfo =
                                ApplicationInfo().apply { flags = ApplicationInfo.FLAG_SYSTEM }
                        }
                }

            val shadowPackageManager = Shadows.shadowOf(appContext.packageManager)
            shadowPackageManager.addResolveInfoForIntent(quickStepIntent, resolveInfo)

            super.migration()
        }
    }

    companion object {
        const val TEST_RECENTS_COMPONENT_NAME = "test.component.name/.testActivity"
        const val ACTION_QUICKSTEP = "android.intent.action.QUICKSTEP_SERVICE"
    }
}
