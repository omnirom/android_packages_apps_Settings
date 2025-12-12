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

import android.content.Context
import android.content.res.Resources
import android.hardware.devicestate.DeviceStateManager
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.android.settings.testutils.DeviceStateAutoRotateSettingTestUtils.setAutoRotateEnabled
import com.android.settings.testutils.DeviceStateAutoRotateSettingTestUtils.setDeviceStateAutoRotateConfig
import com.android.settings.testutils.DeviceStateAutoRotateSettingTestUtils.setDeviceTypeFoldable
import com.android.settingslib.devicestate.DeviceStateAutoRotateSettingManagerImpl
import com.android.settingslib.devicestate.DeviceStateRotationLockSettingsManager
import com.android.window.flags.Flags
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when` as whenever
import org.mockito.junit.MockitoJUnit

@SmallTest
@RunWith(AndroidJUnit4::class)
class DeviceStateAutoRotateSettingManagerProviderTest {

    @get:Rule val setFlagsRule: SetFlagsRule = SetFlagsRule()
    @get:Rule val rule = MockitoJUnit.rule()

    @Mock private lateinit var mockContext: Context
    @Mock private lateinit var mockDeviceStateManager: DeviceStateManager
    @Mock private lateinit var mockResources: Resources

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setup() {
        whenever(mockContext.contentResolver).thenReturn(context.contentResolver)
        whenever(mockContext.getSystemService(DeviceStateManager::class.java))
            .thenReturn(mockDeviceStateManager)
        whenever(mockContext.resources).thenReturn(mockResources)
        setAutoRotateEnabled(/* isEnabled= */ true, mockResources)
        setDeviceStateAutoRotateConfig(/* isNonEmpty= */ true, mockResources)
        setDeviceTypeFoldable(/* isFoldable= */ true, mockDeviceStateManager)
    }

    @After
    fun tearDown() {
        DeviceStateAutoRotateSettingManagerProvider.resetInstance()
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_DEVICE_STATE_AUTO_ROTATE_SETTING_REFACTOR)
    fun getSingletonInstance_refactorFlagEnabled_returnsRefactoredManager() {
        val manager = DeviceStateAutoRotateSettingManagerProvider.getSingletonInstance(mockContext)

        assertThat(manager).isInstanceOf(DeviceStateAutoRotateSettingManagerImpl::class.java)
    }

    @Test
    fun getSingletonInstance_noFoldedDeviceStates_returnsNull() {
        setDeviceTypeFoldable(/* isFoldable= */ false, mockDeviceStateManager)

        val manager = DeviceStateAutoRotateSettingManagerProvider.getSingletonInstance(mockContext)

        assertThat(manager).isNull()
    }

    @Test
    fun getSingletonInstance_autoRotateDisabled_returnsNull() {
        setAutoRotateEnabled(/* isEnabled= */ false, mockResources)

        val manager = DeviceStateAutoRotateSettingManagerProvider.getSingletonInstance(mockContext)

        assertThat(manager).isNull()
    }

    @Test
    fun getSingletonInstance_dSAutoRotateConfigMissing_returnsNull() {
        setDeviceStateAutoRotateConfig(/* isNonEmpty= */ false, mockResources)

        val manager = DeviceStateAutoRotateSettingManagerProvider.getSingletonInstance(mockContext)

        assertThat(manager).isNull()
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_DEVICE_STATE_AUTO_ROTATE_SETTING_REFACTOR)
    fun getSingletonInstance_refactorFlagDisabled_returnsLegacyManager() {
        val manager = DeviceStateAutoRotateSettingManagerProvider.getSingletonInstance(mockContext)

        assertThat(manager).isInstanceOf(DeviceStateRotationLockSettingsManager::class.java)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_DEVICE_STATE_AUTO_ROTATE_SETTING_REFACTOR)
    fun getSingletonInstance_resetInstance_returnsNewInstance() {
        val manager1 = DeviceStateAutoRotateSettingManagerProvider.getSingletonInstance(mockContext)
        DeviceStateAutoRotateSettingManagerProvider.resetInstance()
        val manager2 = DeviceStateAutoRotateSettingManagerProvider.getSingletonInstance(mockContext)

        assertNotSame(manager1, manager2)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_DEVICE_STATE_AUTO_ROTATE_SETTING_REFACTOR)
    fun getSingletonInstance_getInstanceTwice_returnsSameInstance() {
        val manager1 = DeviceStateAutoRotateSettingManagerProvider.getSingletonInstance(mockContext)
        val manager2 = DeviceStateAutoRotateSettingManagerProvider.getSingletonInstance(mockContext)

        assertSame(manager1, manager2)
    }
}
