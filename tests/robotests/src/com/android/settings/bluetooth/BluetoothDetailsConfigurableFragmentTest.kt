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

import android.app.Application
import android.app.settings.SettingsEnums
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.testing.EmptyFragmentActivity
import androidx.preference.Preference
import androidx.preference.PreferenceGroup
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.android.settings.SettingsActivity
import com.android.settings.SubSettings
import com.android.settings.bluetooth.ui.model.FragmentTypeModel
import com.android.settings.connecteddevice.ConnectedDeviceDashboardFragment
import com.android.settings.flags.Flags
import com.android.settings.testutils.FakeFeatureFactory
import com.android.settings.testutils.shadow.ShadowBluetoothUtils
import com.android.settingslib.bluetooth.CachedBluetoothDevice
import com.android.settingslib.bluetooth.LocalBluetoothManager
import com.android.settingslib.bluetooth.devicesettings.DeviceSettingId
import com.android.settingslib.bluetooth.devicesettings.data.repository.DeviceSettingRepository
import com.android.settingslib.bluetooth.devicesettings.shared.model.DeviceSettingActionModel
import com.android.settingslib.bluetooth.devicesettings.shared.model.DeviceSettingConfigItemModel
import com.android.settingslib.bluetooth.devicesettings.shared.model.DeviceSettingConfigModel
import com.android.settingslib.bluetooth.devicesettings.shared.model.DeviceSettingIcon
import com.android.settingslib.bluetooth.devicesettings.shared.model.DeviceSettingModel
import com.android.settingslib.bluetooth.devicesettings.shared.model.DeviceSettingStateModel
import com.android.settingslib.bluetooth.devicesettings.shared.model.ToggleModel
import com.android.settingslib.widget.BannerMessagePreference
import com.android.settingslib.widget.CardPreference
import com.android.settingslib.widget.SegmentedButtonPreference
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Answers
import org.mockito.Mock
import org.mockito.Mockito.any
import org.mockito.Mockito.spy
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShadowBluetoothUtils::class])
@OptIn(ExperimentalCoroutinesApi::class)
class BluetoothDetailsConfigurableFragmentTest {
    @get:Rule val mockitoRule: MockitoRule = MockitoJUnit.rule()

    @get:Rule val setFlagsRule: SetFlagsRule = SetFlagsRule()

    private lateinit var activity: FragmentActivity
    private lateinit var fragment: TestConfigurableFragment
    private lateinit var context: Application
    private lateinit var featureFactory: FakeFeatureFactory

    @Mock private lateinit var repository: DeviceSettingRepository

    @Mock private lateinit var bluetoothDevice: BluetoothDevice

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private lateinit var localBtManager: LocalBluetoothManager

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private lateinit var cachedDevice: CachedBluetoothDevice

    private val testScope = TestScope()

    @Before
    fun setUp() {
        context = spy(ApplicationProvider.getApplicationContext<Application>())
        featureFactory = FakeFeatureFactory.setupForTest()
        ShadowBluetoothUtils.sLocalBluetoothManager = localBtManager
        whenever(localBtManager.bluetoothAdapter.getRemoteDevice(DEVICE_ADDRESS))
            .thenReturn(bluetoothDevice)
        whenever(localBtManager.bluetoothAdapter.getRemoteDevice(INVALID_DEVICE_ADDRESS))
            .thenReturn(null)
        whenever(localBtManager.bluetoothAdapter.getRemoteDevice(DEVICE_ADDRESS))
            .thenReturn(bluetoothDevice)
        whenever(bluetoothDevice.bondState).thenReturn(BluetoothDevice.BOND_BONDED)
        whenever(localBtManager.cachedDeviceManager.findDevice(bluetoothDevice))
            .thenReturn(cachedDevice)
        whenever(featureFactory.bluetoothFeatureProvider.getDeviceSettingRepository(any(), any()))
            .thenReturn(repository)
    }

    @Test
    fun noAddress_launchConnectedDevicesScreen() =
        buildFragment(address = null) {
            assertThat(fragment.activity?.isFinishing).isTrue()
            val startedIntent: Intent = shadowOf(fragment.activity).nextStartedActivity
            val shadowIntent = shadowOf(startedIntent)
            assertThat(shadowIntent.intentClass).isEqualTo(SubSettings::class.java)
            assertThat(startedIntent.getStringExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT))
                .isEqualTo(ConnectedDeviceDashboardFragment::class.java.name)
        }

    @Test
    fun deviceNotFound_launchConnectedDevicesScreen() =
        buildFragment(address = INVALID_DEVICE_ADDRESS) {
            assertThat(fragment.activity?.isFinishing).isTrue()
            val startedIntent: Intent = shadowOf(fragment.activity).nextStartedActivity
            val shadowIntent = shadowOf(startedIntent)
            assertThat(shadowIntent.intentClass).isEqualTo(SubSettings::class.java)
            assertThat(startedIntent.getStringExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT))
                .isEqualTo(ConnectedDeviceDashboardFragment::class.java.name)
        }

    @Test
    fun setPreferenceDisplayOrder_null_unchanged() = buildFragment {
        fragment.requestUpdateLayout(null)

        assertThat(getDisplayedKeys()).containsExactly("key1", "key2")
    }

    @Test
    fun setPreferenceDisplayOrder_hideItem() = buildFragment {
        fragment.requestUpdateLayout(mutableListOf("key2"))

        assertThat(getDisplayedKeys()).containsExactly("key2")
    }

    @Test
    fun setPreferenceDisplayOrder_hideAndReShownItem() = buildFragment {
        fragment.requestUpdateLayout(mutableListOf("key2"))
        fragment.requestUpdateLayout(mutableListOf("key2", "key1"))

        assertThat(getDisplayedKeys()).containsExactly("key2", "key1")
    }

    @Test
    fun setPreferenceDisplayOrderToMainFragment_configIsNull_unchanged() = buildFragment {
        fragment.requestUpdateLayout(FragmentTypeModel.DeviceDetailsMainFragment)

        assertThat(getDisplayedKeys()).containsExactly("key1", "key2")
    }

    @Test
    fun setPreferenceDisplayOrderToMainFragment_itemNotInConfig_hideItem() = buildFragment {
        testScope.runTest {
            whenever(repository.getDeviceSettingsConfig(cachedDevice))
                .thenReturn(
                    DeviceSettingConfigModel(
                        listOf(
                            DeviceSettingConfigItemModel.BuiltinItem.CommonBuiltinItem(
                                1,
                                false,
                                "key2",
                            )
                        ),
                        listOf(),
                        null,
                    )
                )

            fragment.requestUpdateLayout(FragmentTypeModel.DeviceDetailsMainFragment)
            runCurrent()

            assertThat(getDisplayedKeys()).containsExactly("key2")
        }
    }

    @Test
    fun setPreferenceDisplayOrderToMainFragment_newItems_showNewItem() = buildFragment {
        testScope.runTest {
            whenever(repository.getDeviceSettingsConfig(cachedDevice))
                .thenReturn(
                    DeviceSettingConfigModel(
                        listOf(
                            DeviceSettingConfigItemModel.AppProvidedItem(
                                DeviceSettingId.DEVICE_SETTING_ID_ANC,
                                highlighted = false,
                            ),
                            DeviceSettingConfigItemModel.BuiltinItem.CommonBuiltinItem(
                                1,
                                false,
                                "key2",
                            ),
                            DeviceSettingConfigItemModel.AppProvidedItem(456, highlighted = false),
                        ),
                        listOf(),
                        null,
                    )
                )
            val intent = Intent("test_intent")
            whenever(
                    repository.getDeviceSetting(cachedDevice, DeviceSettingId.DEVICE_SETTING_ID_ANC)
                )
                .thenReturn(
                    flowOf(
                        DeviceSettingModel.MultiTogglePreference(
                            cachedDevice,
                            DeviceSettingId.DEVICE_SETTING_ID_ANC,
                            "title",
                            toggles =
                                listOf(
                                    ToggleModel(
                                        "label",
                                        DeviceSettingIcon.BitmapIcon(
                                            Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
                                        ),
                                    )
                                ),
                            isActive = true,
                            state = DeviceSettingStateModel.MultiTogglePreferenceState(0),
                            isAllowedChangingState = true,
                            updateState = {},
                        )
                    )
                )
            whenever(repository.getDeviceSetting(cachedDevice, 456))
                .thenReturn(
                    flowOf(
                        DeviceSettingModel.ActionSwitchPreference(
                            cachedDevice = cachedDevice,
                            id = 456,
                            title = "title",
                            summary = "summary",
                            icon = null,
                            action = DeviceSettingActionModel.IntentAction(intent),
                        )
                    )
                )

            fragment.requestUpdateLayout(FragmentTypeModel.DeviceDetailsMainFragment)
            runCurrent()

            assertThat(getDisplayedKeys())
                .containsExactly(
                    "DEVICE_SETTING_${DeviceSettingId.DEVICE_SETTING_ID_ANC}",
                    "key2",
                    "DEVICE_SETTING_456",
                )
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_BLUETOOTH_SETTINGS_EXPRESSIVE_DESIGN)
    fun setPreferenceDisplayOrderToMainFragment_highlight_useCardPreference() = buildFragment {
        testScope.runTest {
            whenever(repository.getDeviceSettingsConfig(cachedDevice))
                .thenReturn(
                    DeviceSettingConfigModel(
                        listOf(
                            DeviceSettingConfigItemModel.AppProvidedItem(456, highlighted = true)
                        ),
                        listOf(),
                        null,
                    )
                )
            val intent = Intent("test_intent")
            whenever(repository.getDeviceSetting(cachedDevice, 456))
                .thenReturn(
                    flowOf(
                        DeviceSettingModel.ActionSwitchPreference(
                            cachedDevice = cachedDevice,
                            id = 456,
                            title = "title",
                            summary = "summary",
                            icon = null,
                            action = DeviceSettingActionModel.IntentAction(intent),
                        )
                    )
                )

            fragment.requestUpdateLayout(FragmentTypeModel.DeviceDetailsMainFragment)
            runCurrent()

            assertThat(getDisplayedKeys()).containsExactly("DEVICE_SETTING_456")
            assertThat(getDisplayedPreferences()[0]).isInstanceOf(CardPreference::class.java)
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_BLUETOOTH_SETTINGS_EXPRESSIVE_DESIGN)
    fun setPreferenceDisplayOrderToMainFragment_multiToggle_useSegmentedButtonPreference() =
        buildFragment {
            testScope.runTest {
                whenever(repository.getDeviceSettingsConfig(cachedDevice))
                    .thenReturn(
                        DeviceSettingConfigModel(
                            listOf(
                                DeviceSettingConfigItemModel.AppProvidedItem(
                                    456,
                                    highlighted = false,
                                )
                            ),
                            listOf(),
                            null,
                        )
                    )
                val intent = Intent("test_intent")
                whenever(repository.getDeviceSetting(cachedDevice, 456))
                    .thenReturn(
                        flowOf(
                            DeviceSettingModel.MultiTogglePreference(
                                cachedDevice = cachedDevice,
                                id = 456,
                                title = "title",
                                toggles =
                                    listOf(
                                        ToggleModel(
                                            "label",
                                            DeviceSettingIcon.BitmapIcon(
                                                Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
                                            ),
                                        )
                                    ),
                                isActive = true,
                                state = DeviceSettingStateModel.MultiTogglePreferenceState(0),
                                isAllowedChangingState = true,
                                updateState = {},
                            )
                        )
                    )

                fragment.requestUpdateLayout(FragmentTypeModel.DeviceDetailsMainFragment)
                runCurrent()

                assertThat(getDisplayedKeys()).containsExactly("DEVICE_SETTING_456")
                assertThat((getDisplayedPreferences()[0] as PreferenceGroup).getPreference(0))
                    .isInstanceOf(SegmentedButtonPreference::class.java)
            }
        }

    @Test
    fun setPreferenceDisplayOrderToMainFragment_clickPlainPreference_startActivity() =
        buildFragment {
            testScope.runTest {
                whenever(repository.getDeviceSettingsConfig(cachedDevice))
                    .thenReturn(
                        DeviceSettingConfigModel(
                            listOf(
                                DeviceSettingConfigItemModel.AppProvidedItem(
                                    456,
                                    highlighted = false,
                                )
                            ),
                            listOf(),
                            null,
                        )
                    )
                val intent = Intent("test_intent")
                whenever(repository.getDeviceSetting(cachedDevice, 456))
                    .thenReturn(
                        flowOf(
                            DeviceSettingModel.ActionSwitchPreference(
                                cachedDevice = cachedDevice,
                                id = 456,
                                title = "title",
                                summary = "summary",
                                icon = null,
                                action = DeviceSettingActionModel.IntentAction(intent),
                            )
                        )
                    )

                fragment.requestUpdateLayout(FragmentTypeModel.DeviceDetailsMainFragment)
                runCurrent()
                getDisplayedPreferences()[0].performClick()

                assertThat(getDisplayedKeys()).containsExactly("DEVICE_SETTING_456")
                assertThat(Shadows.shadowOf(context).nextStartedActivity).isEqualTo(intent)
                verify(featureFactory.metricsFeatureProvider)
                    .action(
                        SettingsEnums.PAGE_UNKNOWN,
                        SettingsEnums.ACTION_BLUETOOTH_DEVICE_DETAILS_ITEM_CLICKED,
                        0,
                        "DEVICE_SETTING_456",
                        2,
                    )
            }
        }

    @Test
    fun setPreferenceDisplayOrderToMainFragment_clickSwitchPreference_updateState() =
        buildFragment {
            testScope.runTest {
                whenever(repository.getDeviceSettingsConfig(cachedDevice))
                    .thenReturn(
                        DeviceSettingConfigModel(
                            listOf(
                                DeviceSettingConfigItemModel.AppProvidedItem(
                                    456,
                                    highlighted = false,
                                )
                            ),
                            listOf(),
                            null,
                        )
                    )
                val intent = Intent("test_intent")
                var updatedState: DeviceSettingStateModel.ActionSwitchPreferenceState? = null
                whenever(repository.getDeviceSetting(cachedDevice, 456))
                    .thenReturn(
                        flowOf(
                            DeviceSettingModel.ActionSwitchPreference(
                                cachedDevice = cachedDevice,
                                id = 456,
                                title = "title",
                                summary = "summary",
                                icon = null,
                                action = null,
                                switchState =
                                    DeviceSettingStateModel.ActionSwitchPreferenceState(true),
                                isAllowedChangingState = true,
                                updateState = { updatedState = it },
                            )
                        )
                    )

                fragment.requestUpdateLayout(FragmentTypeModel.DeviceDetailsMainFragment)
                runCurrent()
                getDisplayedPreferences()[0].performClick()

                assertThat(getDisplayedKeys()).containsExactly("DEVICE_SETTING_456")
                assertThat(updatedState)
                    .isEqualTo(DeviceSettingStateModel.ActionSwitchPreferenceState(false))
                verify(featureFactory.metricsFeatureProvider)
                    .action(
                        SettingsEnums.PAGE_UNKNOWN,
                        SettingsEnums.ACTION_BLUETOOTH_DEVICE_DETAILS_ITEM_CLICKED,
                        0,
                        "DEVICE_SETTING_456",
                        0,
                    )
            }
        }

    @Test
    fun setPreferenceDisplayOrderToMainFragment_banner_useBannerPreference() = buildFragment {
        testScope.runTest {
            whenever(repository.getDeviceSettingsConfig(cachedDevice))
                .thenReturn(
                    DeviceSettingConfigModel(
                        listOf(
                            DeviceSettingConfigItemModel.AppProvidedItem(456, highlighted = false)
                        ),
                        listOf(),
                        null,
                    )
                )
            val intent = Intent("test_intent")
            whenever(repository.getDeviceSetting(cachedDevice, 456))
                .thenReturn(
                    flowOf(
                        DeviceSettingModel.BannerPreference(
                            cachedDevice = cachedDevice,
                            id = 456,
                            title = "title",
                            message = "message",
                            icon = null,
                            positiveButton = null,
                            negativeButton = null,
                        )
                    )
                )

            fragment.requestUpdateLayout(FragmentTypeModel.DeviceDetailsMainFragment)
            runCurrent()

            assertThat(getDisplayedKeys()).containsExactly("DEVICE_SETTING_456")
            assertThat(getDisplayedPreferences()[0])
                .isInstanceOf(BannerMessagePreference::class.java)
        }
    }

    private fun buildFragment(address: String? = DEVICE_ADDRESS, r: (() -> Unit)) {
        ActivityScenario.launch(EmptyFragmentActivity::class.java).use { activityScenario ->
            activityScenario.onActivity { activity: EmptyFragmentActivity ->
                this@BluetoothDetailsConfigurableFragmentTest.activity = activity
                fragment = TestConfigurableFragment()
                fragment.arguments =
                    Bundle().apply {
                        putString(BluetoothDetailsConfigurableFragment.KEY_DEVICE_ADDRESS, address)
                    }
                activity.supportFragmentManager.beginTransaction().add(fragment, null).commitNow()
                r.invoke()
            }
        }
    }

    private fun getDisplayedPreferences(
        container: PreferenceGroup = fragment.preferenceScreen
    ): List<Preference> {
        val prefs: MutableList<Preference> = mutableListOf()
        for (i in 0..<container.preferenceCount) {
            if (container.getPreference(i).isVisible) {
                val pref = container.getPreference(i)
                if (pref.key.startsWith("CATEGORY_STARTS_WITH_")) {
                    prefs.addAll(getDisplayedPreferences(pref as PreferenceGroup))
                } else {
                    prefs.add(pref)
                }
            }
        }
        return prefs
    }

    private fun getDisplayedKeys(): List<String> {
        return getDisplayedPreferences().map { it.key }
    }

    class TestConfigurableFragment : BluetoothDetailsConfigurableFragment() {
        protected override fun getPreferenceScreenResId(): Int {
            return R.xml.bluetooth_fake_settings
        }

        override fun getLogTag(): String {
            return "TAG"
        }

        override fun getMetricsCategory(): Int {
            return 0
        }
    }

    private companion object {
        const val DEVICE_ADDRESS = "12:34:56:78:12:34"
        const val INVALID_DEVICE_ADDRESS = "12345"
    }
}
