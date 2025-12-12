/*
 * Copyright (C) 2022 The Android Open Source Project
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
package com.android.settings.accessibility

import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.AccessibilityShortcutInfo
import android.content.ComponentName
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import android.content.pm.ServiceInfo
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.security.Flags
import androidx.test.core.app.ApplicationProvider
import com.android.settings.testutils.shadow.ShadowRestrictedLockUtilsInternal
import com.android.settingslib.RestrictedPreference
import com.google.common.truth.Truth
import java.io.IOException
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Spy
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.xmlpull.v1.XmlPullParserException

/** Test for [InstalledA11yFeaturesPreferenceHelper]. */
@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShadowRestrictedLockUtilsInternal::class])
class InstalledA11yFeaturesPreferenceHelperTest {
    @get:Rule val mocks: MockitoRule = MockitoJUnit.rule()
    @get:Rule val setFlagsRule: SetFlagsRule = SetFlagsRule()
    private val context: Context = ApplicationProvider.getApplicationContext()
    @Spy
    private val serviceInfo: AccessibilityServiceInfo? =
        getMockAccessibilityServiceInfo(PACKAGE_NAME, CLASS_NAME)
    @Mock private lateinit var shortcutInfo: AccessibilityShortcutInfo
    private val helper = InstalledA11yFeaturesPreferenceHelper(context)

    @Test
    fun createAccessibilityServicePreferenceList_hasOneInfo_containsSameKey() {
        val key: String = COMPONENT_NAME.flattenToString()
        val infoList: List<AccessibilityServiceInfo> = listOf(serviceInfo!!)

        val preferenceList: List<RestrictedPreference> =
            helper.createAccessibilityServicePreferenceList(infoList)
        val preference = preferenceList[0]

        Truth.assertThat(preference.key).isEqualTo(key)
    }

    @Test
    @EnableFlags(
        value =
            [
                Flags.FLAG_EXTEND_ECM_TO_ALL_SETTINGS,
                android.permission.flags.Flags.FLAG_ENHANCED_CONFIRMATION_MODE_APIS_ENABLED,
            ]
    )
    fun createAccessibilityServicePreferenceList_ecmRestricted_prefIsEcmRestricted() {
        ShadowRestrictedLockUtilsInternal.setEcmRestrictedPkgs(
            serviceInfo!!.resolveInfo.serviceInfo.packageName
        )
        val infoList: List<AccessibilityServiceInfo> = listOf(serviceInfo)

        val preferenceList: List<RestrictedPreference> =
            helper.createAccessibilityServicePreferenceList(infoList)
        val preference = preferenceList[0]

        Truth.assertThat(preference.isDisabledByEcm).isTrue()
    }

    @Test
    @EnableFlags(
        value =
            [
                Flags.FLAG_EXTEND_ECM_TO_ALL_SETTINGS,
                android.permission.flags.Flags.FLAG_ENHANCED_CONFIRMATION_MODE_APIS_ENABLED,
            ]
    )
    fun createAccessibilityServicePreferenceList_ecmNotRestricted_prefIsNotEcmRestricted() {
        ShadowRestrictedLockUtilsInternal.setEcmRestrictedPkgs()
        val infoList: List<AccessibilityServiceInfo> = listOf(serviceInfo!!)

        val preferenceList: List<RestrictedPreference> =
            helper.createAccessibilityServicePreferenceList(infoList)
        val preference = preferenceList[0]

        Truth.assertThat(preference.isDisabledByEcm).isFalse()
    }

    @Test
    fun createAccessibilityActivityPreferenceList_hasOneInfo_containsSameKey() {
        val key: String = COMPONENT_NAME.flattenToString()
        setMockAccessibilityShortcutInfo(shortcutInfo)
        val infoList: List<AccessibilityShortcutInfo> = listOf(shortcutInfo)

        val preferenceList: List<AccessibilityActivityPreference> =
            helper.createAccessibilityActivityPreferenceList(infoList)
        val preference = preferenceList[0]

        Truth.assertThat(preference.key).isEqualTo(key)
    }

    private fun getMockAccessibilityServiceInfo(
        packageName: String,
        className: String,
    ): AccessibilityServiceInfo? {
        val applicationInfo = ApplicationInfo()
        val serviceInfo = ServiceInfo()
        applicationInfo.packageName = packageName
        serviceInfo.packageName = packageName
        serviceInfo.name = className
        serviceInfo.applicationInfo = applicationInfo

        val resolveInfo = ResolveInfo()
        resolveInfo.serviceInfo = serviceInfo
        try {
            val info = AccessibilityServiceInfo(resolveInfo, context)
            info.setComponentName(ComponentName(packageName, className))
            return info
        } catch (e: XmlPullParserException) {
            // Do nothing
        } catch (e: IOException) {}
        return null
    }

    private fun setMockAccessibilityShortcutInfo(mockInfo: AccessibilityShortcutInfo) {
        val activityInfo = mock(ActivityInfo::class.java)
        activityInfo.applicationInfo = ApplicationInfo()
        whenever(mockInfo.getActivityInfo()).thenReturn(activityInfo)
        whenever(activityInfo.loadLabel(anyOrNull())).thenReturn(DEFAULT_LABEL)
        whenever(mockInfo.loadSummary(anyOrNull())).thenReturn(DEFAULT_SUMMARY)
        whenever(mockInfo.loadDescription(anyOrNull())).thenReturn(DEFAULT_DESCRIPTION)
        whenever(mockInfo.getComponentName()).thenReturn(COMPONENT_NAME)
    }

    companion object {
        private const val PACKAGE_NAME = "com.android.test"
        private const val CLASS_NAME: String = "$PACKAGE_NAME.test_a11y_service"
        private val COMPONENT_NAME: ComponentName = ComponentName(PACKAGE_NAME, CLASS_NAME)
        private const val DEFAULT_SUMMARY = "default summary"
        private const val DEFAULT_DESCRIPTION = "default description"
        private const val DEFAULT_LABEL = "default label"
    }
}
