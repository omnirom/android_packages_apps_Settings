/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.bluetooth.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.android.settings.R
import com.android.settings.bluetooth.ui.model.DeviceSettingPreferenceModel
import com.android.settings.bluetooth.ui.model.FragmentTypeModel
import com.android.settings.overlay.FeatureFactory.Companion.featureFactory
import com.android.settingslib.bluetooth.CachedBluetoothDevice
import com.android.settingslib.bluetooth.devicesettings.DeviceSettingId
import com.android.settingslib.bluetooth.devicesettings.shared.model.DeviceSettingConfigItemModel
import com.android.settingslib.bluetooth.devicesettings.shared.model.DeviceSettingIcon
import com.android.settingslib.bluetooth.devicesettings.shared.model.DeviceSettingModel
import com.android.settingslib.bluetooth.devicesettings.shared.model.DeviceSettingStateModel
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class BluetoothDeviceDetailsViewModel(
    private val application: Application,
    private val cachedDevice: CachedBluetoothDevice,
    backgroundCoroutineContext: CoroutineContext,
) : AndroidViewModel(application) {

    private val deviceSettingRepository =
        featureFactory.bluetoothFeatureProvider.getDeviceSettingRepository(
            application,
            viewModelScope,
        )

    private val items =
        viewModelScope.async(start = CoroutineStart.LAZY) {
            deviceSettingRepository.getDeviceSettingsConfig(cachedDevice)
        }

    suspend fun getItems(fragment: FragmentTypeModel): List<DeviceSettingConfigItemModel>? =
        when (fragment) {
            is FragmentTypeModel.DeviceDetailsMainFragment -> items.await()?.mainItems
            is FragmentTypeModel.DeviceDetailsMoreSettingsFragment ->
                items.await()?.moreSettingsItems
        }

    suspend fun getHelpItem(fragment: FragmentTypeModel): DeviceSettingConfigItemModel? =
        when (fragment) {
            is FragmentTypeModel.DeviceDetailsMainFragment -> null
            is FragmentTypeModel.DeviceDetailsMoreSettingsFragment ->
                items.await()?.moreSettingsHelpItem
        }

    fun getDeviceSetting(
        cachedDevice: CachedBluetoothDevice,
        @DeviceSettingId settingId: Int,
    ): Flow<DeviceSettingPreferenceModel?> {
        if (settingId == DeviceSettingId.DEVICE_SETTING_ID_MORE_SETTINGS) {
            return flowOf(DeviceSettingPreferenceModel.MoreSettingsPreference(settingId))
        }
        return deviceSettingRepository.getDeviceSetting(cachedDevice, settingId).map {
            it?.toPreferenceModel()
        }
    }

    private fun DeviceSettingModel.toPreferenceModel(): DeviceSettingPreferenceModel? {
        return when (this) {
            is DeviceSettingModel.ActionSwitchPreference -> {
                if (switchState != null) {
                    DeviceSettingPreferenceModel.SwitchPreference(
                        id = id,
                        title = title,
                        summary = summary,
                        icon = icon,
                        checked = switchState?.checked ?: false,
                        onCheckedChange = { newState ->
                            updateState?.invoke(
                                DeviceSettingStateModel.ActionSwitchPreferenceState(newState)
                            )
                        },
                        disabled = !isAllowedChangingState,
                        action = action,
                    )
                } else {
                    DeviceSettingPreferenceModel.PlainPreference(
                        id = id,
                        title = title,
                        summary = summary,
                        icon = icon,
                        action = action,
                    )
                }
            }
            is DeviceSettingModel.FooterPreference ->
                DeviceSettingPreferenceModel.FooterPreference(id = id, footerText = footerText)
            is DeviceSettingModel.HelpPreference ->
                DeviceSettingPreferenceModel.HelpPreference(
                    id = id,
                    icon = DeviceSettingIcon.ResourceIcon(R.drawable.ic_help),
                    intent = intent,
                )
            is DeviceSettingModel.MultiTogglePreference ->
                DeviceSettingPreferenceModel.MultiTogglePreference(
                    id = id,
                    title = title,
                    toggles = toggles,
                    isActive = isActive,
                    selectedIndex = state.selectedIndex,
                    isAllowedChangingState = isAllowedChangingState,
                    onSelectedChange = { newState ->
                        updateState(DeviceSettingStateModel.MultiTogglePreferenceState(newState))
                    },
                )
            is DeviceSettingModel.BannerPreference ->
                DeviceSettingPreferenceModel.BannerPreference(
                    id = id,
                    title = title,
                    message = message,
                    icon = icon,
                    positiveButton = positiveButton,
                    negativeButton = negativeButton,
                )
            is DeviceSettingModel.Unknown -> null
        }
    }

    class Factory(
        private val application: Application,
        private val cachedDevice: CachedBluetoothDevice,
        private val backgroundCoroutineContext: CoroutineContext,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return BluetoothDeviceDetailsViewModel(
                application,
                cachedDevice,
                backgroundCoroutineContext,
            )
                as T
        }
    }

    companion object {
        private const val TAG = "BluetoothDeviceDetailsViewModel"
    }
}
