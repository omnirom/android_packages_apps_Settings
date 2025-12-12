/*
 * Copyright 2025 The Android Open Source Project
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

package com.android.settings.appfunctions

import android.content.Context
import com.android.settings.accessibility.AccessibilityScreen
import com.android.settings.accessibility.ColorAndMotionScreen
import com.android.settings.accessibility.VibrationIntensityScreen
import com.android.settings.accessibility.VibrationScreen
import com.android.settings.accessibility.colorcorrection.ui.ColorCorrectionScreen
import com.android.settings.accessibility.colorinversion.ui.ColorInversionScreen
import com.android.settings.accessibility.detail.a11yactivity.ui.A11yActivityScreen
import com.android.settings.accessibility.detail.a11yservice.ui.A11yServiceScreen
import com.android.settings.accessibility.extradim.ui.ExtraDimScreen
import com.android.settings.accessibility.flashnotifications.ui.FlashNotificationsScreen
import com.android.settings.accessibility.textreading.ui.TextReadingScreen
import com.android.settings.accounts.AccountScreen
import com.android.settings.applications.AppDashboardScreen
import com.android.settings.applications.specialaccess.AlarmsAndRemindersAppDetailScreen
import com.android.settings.applications.specialaccess.AlarmsAndRemindersAppListScreen
import com.android.settings.applications.specialaccess.AllFilesAccessAppDetailScreen
import com.android.settings.applications.specialaccess.AllFilesAccessAppListScreen
import com.android.settings.applications.specialaccess.DisplayOverOtherAppsAppDetailScreen
import com.android.settings.applications.specialaccess.DisplayOverOtherAppsAppListScreen
import com.android.settings.applications.specialaccess.FullScreenNotificationsAppDetailScreen
import com.android.settings.applications.specialaccess.FullScreenNotificationsAppListScreen
import com.android.settings.applications.specialaccess.InstallUnknownAppsAppDetailScreen
import com.android.settings.applications.specialaccess.InstallUnknownAppsAppListScreen
import com.android.settings.applications.specialaccess.InteractAcrossProfilesAppDetailScreen
import com.android.settings.applications.specialaccess.InteractAcrossProfilesAppListScreen
import com.android.settings.applications.specialaccess.ManageWriteSettingsAppDetailScreen
import com.android.settings.applications.specialaccess.ManageWriteSettingsAppListScreen
import com.android.settings.applications.specialaccess.SpecialAccessSettingsScreen
import com.android.settings.applications.specialaccess.WifiControlAppDetailScreen
import com.android.settings.applications.specialaccess.WifiControlAppListScreen
import com.android.settings.applications.specialaccess.WriteSystemPreferencesAppDetailScreen
import com.android.settings.applications.specialaccess.WriteSystemPreferencesAppListScreen
import com.android.settings.applications.specialaccess.notificationaccess.AppsNotificationAccessScreen
import com.android.settings.applications.specialaccess.pictureinpicture.PictureInPictureAppDetailScreen
import com.android.settings.applications.specialaccess.pictureinpicture.PictureInPictureAppListScreen
import com.android.settings.connecteddevice.AdvancedConnectedDeviceScreen
import com.android.settings.connecteddevice.BluetoothDashboardScreen
import com.android.settings.connecteddevice.ConnectedDeviceDashboardScreen
import com.android.settings.connecteddevice.NfcAndPaymentScreen
import com.android.settings.connecteddevice.PreviouslyConnectedDeviceScreen
import com.android.settings.datausage.DataSaverScreen
import com.android.settings.datausage.DataUsageAppDetailScreen
import com.android.settings.datausage.DataUsageListScreen
import com.android.settings.datetime.DateTimeSettingsScreen
import com.android.settings.deviceinfo.aboutphone.MyDeviceInfoScreen
import com.android.settings.deviceinfo.firmwareversion.FirmwareVersionScreen
import com.android.settings.deviceinfo.hardwareinfo.DeviceModelPreference
import com.android.settings.deviceinfo.hardwareinfo.HardwareInfoScreen
import com.android.settings.deviceinfo.hardwareinfo.HardwareVersionPreference
import com.android.settings.deviceinfo.legal.LegalSettingsScreen
import com.android.settings.deviceinfo.legal.ModuleLicensesScreen
import com.android.settings.deviceinfo.storage.StoragePreferenceScreen
import com.android.settings.display.AmbientDisplayAlwaysOnPreferenceScreen
import com.android.settings.display.AutoBrightnessScreen
import com.android.settings.display.ColorModeScreen
import com.android.settings.display.DisplayScreen
import com.android.settings.display.NightDisplayScreen
import com.android.settings.display.ScreenTimeoutScreen
import com.android.settings.display.darkmode.DarkModeScreen
import com.android.settings.dream.ScreensaverScreen
import com.android.settings.emergency.EmergencyDashboardScreen
import com.android.settings.fuelgauge.batterysaver.BatterySaverScreen
import com.android.settings.fuelgauge.batteryusage.PowerUsageAdvancedScreen
import com.android.settings.fuelgauge.batteryusage.PowerUsageSummaryScreen
import com.android.settings.gestures.ButtonNavigationSettingsScreen
import com.android.settings.gestures.DoubleTapPowerScreen
import com.android.settings.gestures.SystemNavigationGestureScreen
import com.android.settings.language.LanguageAndRegionScreen
import com.android.settings.location.LocationScreen
import com.android.settings.location.LocationServicesScreen
import com.android.settings.location.RecentLocationAccessScreen
import com.android.settings.network.AdaptiveConnectivityScreen
import com.android.settings.network.MobileNetworkListScreen
import com.android.settings.network.NetworkDashboardScreen
import com.android.settings.network.NetworkProviderScreen
import com.android.settings.network.apn.ApnSettingsScreen
import com.android.settings.network.telephony.MobileNetworkScreen
import com.android.settings.network.tether.TetherScreen
import com.android.settings.notification.BubbleNotificationScreen
import com.android.settings.notification.SoundScreen
import com.android.settings.notification.app.ConversationListScreen
import com.android.settings.notification.modes.ZenModesListScreen
import com.android.settings.security.LockScreenPreferenceScreen
import com.android.settings.sound.MediaControlsScreen
import com.android.settings.spa.app.catalyst.AllAppsScreen
import com.android.settings.spa.app.catalyst.AppInfoScreen
import com.android.settings.spa.app.catalyst.AppInfoStorageScreen
import com.android.settings.spa.app.catalyst.AppStorageAppListScreen
import com.android.settings.supervision.SupervisionDashboardScreen
import com.android.settings.supervision.SupervisionPinManagementScreen
import com.android.settings.supervision.SupervisionWebContentFiltersScreen
import com.android.settings.system.ResetDashboardScreen
import com.android.settings.system.SystemDashboardScreen
import com.android.settings.vpn2.VpnSettingsScreen
import com.android.settings.wfd.WifiDisplayScreen
import com.android.settings.wifi.ConfigureWifiScreen
import com.android.settings.wifi.WifiDataUsagePreference
import com.android.settings.wifi.calling.WifiCallingScreen
import com.android.settings.wifi.savedaccesspoints2.SavedAccessPointsWifiScreen
import com.android.settings.wifi.tether.WifiHotspotScreen
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.getPreferenceSummary

/**
 * Configuration of a single setting for the device state app functions. It controls how the setting
 * is presented in the device state results.
 *
 * @param enabled whether expose the device state to App Functions
 * @param settingKey the unique ID of the device state
 * @param settingScreenKey the ID of the screen that the device state is associated with
 * @param hintText additional context about the device state
 */
data class DeviceStateItemConfig(
    val enabled: Boolean = true,
    val settingKey: String,
    val settingScreenKey: String,
    // TODO hint text should come from a "description" field, which currently only exists on Screens
    val hintText: (Context, PreferenceMetadata) -> String? = { _, _ -> null },
)

/**
 * Configuration of a screen converting to device states.
 *
 * @param enabled whether expose device states on this screen to App Functions.
 * @param screenKey the unique ID for the screen.
 * @param appFunctionTypes the app function associated with this screen. The default is
 *   UNCATEGORIZED.
 */
data class PerScreenCatalystConfig(
    val enabled: Boolean,
    val screenKey: String,
    // TODO(b/405344827): map categories to PreferenceMetadata#tags
    val appFunctionTypes: Set<DeviceStateAppFunctionType> =
        setOf(DeviceStateAppFunctionType.GET_UNCATEGORIZED),
    val additionalDescription: String? = null,
)

/**
 * Configuration of the device state app functions.
 *
 * @param screenConfigs a list of catalyst screen configurations
 * @param deviceStateItems a list of device state items
 */
data class CatalystConfig(
    val deviceStateItems: List<DeviceStateItemConfig>,
    val screenConfigs: List<PerScreenCatalystConfig>,
)

fun getSettingsCatalystConfig() =
    CatalystConfig(
        screenConfigs = getCatalystScreenConfigs(),
        deviceStateItems = getDeviceStateItemList(),
    )

private fun getCatalystScreenConfigs() =
    listOf(
        PerScreenCatalystConfig(enabled = true, screenKey = DarkModeScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = ColorAndMotionScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = ColorInversionScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = ColorCorrectionScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = ExtraDimScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = AdaptiveConnectivityScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = AutoBrightnessScreen.KEY),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = BatterySaverScreen.KEY,
            appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_BATTERY),
            additionalDescription =
                ". The Battery Saver screen allows users to set the device to Standard or Extreme Battery Saver mode. Users can also configure schedules and reminders for Battery Saver.",
        ),
        PerScreenCatalystConfig(enabled = true, screenKey = BluetoothDashboardScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = LockScreenPreferenceScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = DisplayScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = FirmwareVersionScreen.KEY),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = NetworkProviderScreen.KEY,
            appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_MOBILE_DATA),
        ),
        PerScreenCatalystConfig(enabled = true, screenKey = LanguageAndRegionScreen.KEY),
        PerScreenCatalystConfig(enabled = false, screenKey = ModuleLicensesScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = LegalSettingsScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = LocationServicesScreen.KEY),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = LocationScreen.KEY,
            appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_UNCATEGORIZED),
        ),
        PerScreenCatalystConfig(enabled = true, screenKey = RecentLocationAccessScreen.KEY),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = MobileNetworkListScreen.KEY,
            appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_MOBILE_DATA),
        ),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = ApnSettingsScreen.KEY,
            appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_MOBILE_DATA),
        ),
        PerScreenCatalystConfig(enabled = true, screenKey = MyDeviceInfoScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = DataSaverScreen.KEY),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = NetworkDashboardScreen.KEY,
            appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_MOBILE_DATA),
        ),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = PowerUsageSummaryScreen.KEY,
            appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_BATTERY),
        ),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = PowerUsageAdvancedScreen.KEY,
            appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_BATTERY),
        ),
        PerScreenCatalystConfig(enabled = true, screenKey = ScreenTimeoutScreen.KEY),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = SoundScreen.KEY,
            appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_NOTIFICATIONS),
        ),
        PerScreenCatalystConfig(enabled = false, screenKey = SupervisionPinManagementScreen.KEY),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = TetherScreen.KEY,
            appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_MOBILE_DATA),
        ),
        PerScreenCatalystConfig(enabled = false, screenKey = SupervisionDashboardScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = VibrationIntensityScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = VibrationScreen.KEY),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = AppStorageAppListScreen.KEY,
            appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_STORAGE),
        ),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = AllAppsScreen.KEY,
            appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_APPS),
        ),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = StoragePreferenceScreen.KEY,
            appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_STORAGE),
        ),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = ScreensaverScreen.KEY,
            appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_UNCATEGORIZED),
        ),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = SystemNavigationGestureScreen.KEY,
            appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_UNCATEGORIZED),
        ),
        PerScreenCatalystConfig(enabled = true, screenKey = A11yActivityScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = A11yServiceScreen.KEY),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = AmbientDisplayAlwaysOnPreferenceScreen.KEY,
            additionalDescription =
                ". Always-on-display settings indicates whether the lock screen, including the time, is visible when the phone is off.",
        ),
        PerScreenCatalystConfig(enabled = true, screenKey = DataUsageAppDetailScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = InstallUnknownAppsAppListScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = ManageWriteSettingsAppListScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = ColorModeScreen.KEY),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = ConfigureWifiScreen.KEY,
            appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_MOBILE_DATA),
        ),
        PerScreenCatalystConfig(enabled = true, screenKey = AdvancedConnectedDeviceScreen.KEY),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = ConversationListScreen.KEY,
            appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_NOTIFICATIONS),
        ),
        PerScreenCatalystConfig(enabled = true, screenKey = DateTimeSettingsScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = HardwareInfoScreen.KEY),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = AppInfoStorageScreen.KEY,
            appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_STORAGE),
        ),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = FlashNotificationsScreen.KEY,
            appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_NOTIFICATIONS),
        ),
        PerScreenCatalystConfig(enabled = true, screenKey = DoubleTapPowerScreen.KEY),
        //        PerScreenCatalystConfig(enabled = true, screenKey = HearingDevicesScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = AppInfoScreen.KEY),
        //        PerScreenCatalystConfig(enabled = true, screenKey = MagnificationScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = MediaControlsScreen.KEY),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = MobileNetworkScreen.KEY,
            appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_MOBILE_DATA),
        ),
        PerScreenCatalystConfig(enabled = true, screenKey = NfcAndPaymentScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = NightDisplayScreen.KEY),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = BubbleNotificationScreen.KEY,
            appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_NOTIFICATIONS),
        ),
        PerScreenCatalystConfig(enabled = true, screenKey = ResetDashboardScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = DisplayOverOtherAppsAppListScreen.KEY),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = FullScreenNotificationsAppListScreen.KEY,
        ),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = InteractAcrossProfilesAppListScreen.KEY,
        ),
        PerScreenCatalystConfig(enabled = true, screenKey = PictureInPictureAppListScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = WifiControlAppListScreen.KEY),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = WriteSystemPreferencesAppListScreen.KEY,
        ),
        PerScreenCatalystConfig(enabled = true, screenKey = AllFilesAccessAppListScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = AlarmsAndRemindersAppListScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = SpecialAccessSettingsScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = SupervisionWebContentFiltersScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = TextReadingScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = AccessibilityScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = AccountScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = AppDashboardScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = ConnectedDeviceDashboardScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = EmergencyDashboardScreen.KEY),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = ZenModesListScreen.KEY,
            appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_NOTIFICATIONS),
        ),
        PerScreenCatalystConfig(enabled = true, screenKey = SystemDashboardScreen.KEY),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = VpnSettingsScreen.KEY,
            appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_MOBILE_DATA),
        ),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = WifiCallingScreen.KEY,
            appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_MOBILE_DATA),
        ),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = WifiHotspotScreen.KEY,
            appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_MOBILE_DATA),
        ),
        PerScreenCatalystConfig(enabled = true, screenKey = WifiDisplayScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = AppsNotificationAccessScreen.KEY),
        PerScreenCatalystConfig(enabled = true, screenKey = PreviouslyConnectedDeviceScreen.KEY),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = ButtonNavigationSettingsScreen.KEY,
            appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_UNCATEGORIZED),
        ),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = SavedAccessPointsWifiScreen.KEY,
            appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_MOBILE_DATA),
        ),
        PerScreenCatalystConfig(
            enabled = true,
            screenKey = DataUsageListScreen.KEY,
            appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_MOBILE_DATA),
            additionalDescription =
                ". This SIM data usage screen shows the amount of data each app has consumed from a specific SIM card.",
        ),
    )

private fun getDeviceStateItemList() =
    listOf(
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "daltonizer_preference",
            settingScreenKey = ColorAndMotionScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "toggle_inversion_preference",
            settingScreenKey = ColorAndMotionScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "dark_ui_mode",
            settingScreenKey = ColorAndMotionScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "animator_duration_scale",
            settingScreenKey = ColorAndMotionScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "adaptive_connectivity_enabled",
            settingScreenKey = AdaptiveConnectivityScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "battery_saver",
            settingScreenKey = BatterySaverScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "basic_battery_saver",
            settingScreenKey = BatterySaverScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "extreme_battery_saver",
            settingScreenKey = BatterySaverScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "adaptive_battery_top_intro",
            settingScreenKey = BatterySaverScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "adaptive_battery_management_enabled",
            settingScreenKey = BatterySaverScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "use_bluetooth",
            settingScreenKey = BluetoothDashboardScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "bluetooth_screen_footer",
            settingScreenKey = BluetoothDashboardScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "ambient_display_always_on",
            settingScreenKey = LockScreenPreferenceScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "brightness",
            settingScreenKey = DisplayScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "auto_brightness_entry",
            settingScreenKey = DisplayScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "lockscreen_from_display_settings",
            settingScreenKey = DisplayScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "dark_ui_mode",
            settingScreenKey = DisplayScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "peak_refresh_rate",
            settingScreenKey = DisplayScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "os_firmware_version",
            settingScreenKey = FirmwareVersionScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "security_key",
            settingScreenKey = FirmwareVersionScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "module_version",
            settingScreenKey = FirmwareVersionScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "base_band",
            settingScreenKey = FirmwareVersionScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "kernel_version",
            settingScreenKey = FirmwareVersionScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "os_build_number",
            settingScreenKey = FirmwareVersionScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "main_toggle_wifi",
            settingScreenKey = NetworkProviderScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "copyright",
            settingScreenKey = LegalSettingsScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "license",
            settingScreenKey = LegalSettingsScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "terms",
            settingScreenKey = LegalSettingsScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "module_license",
            settingScreenKey = LegalSettingsScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "webview_license",
            settingScreenKey = LegalSettingsScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "legal_source_code",
            settingScreenKey = LegalSettingsScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "wallpaper_attributions",
            settingScreenKey = LegalSettingsScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "mobile_data",
            settingScreenKey = MobileNetworkListScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "use_data_saver",
            settingScreenKey = DataSaverScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "mobile_network_list",
            settingScreenKey = NetworkDashboardScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "airplane_mode_on",
            settingScreenKey = NetworkDashboardScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "restrict_background_parent_entry",
            settingScreenKey = NetworkDashboardScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "battery_header",
            settingScreenKey = PowerUsageSummaryScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "status_bar_show_battery_percent",
            settingScreenKey = PowerUsageSummaryScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "adaptive_sleep",
            settingScreenKey = ScreenTimeoutScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "media_volume",
            settingScreenKey = SoundScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "call_volume",
            settingScreenKey = SoundScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "separate_ring_volume",
            settingScreenKey = SoundScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "dtmf_tone",
            settingScreenKey = SoundScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "supervision_pin_recovery",
            settingScreenKey = SupervisionPinManagementScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "supervision_change_pin",
            settingScreenKey = SupervisionPinManagementScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "wifi_tether",
            settingScreenKey = TetherScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "device_supervision_switch",
            settingScreenKey = SupervisionDashboardScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "supervision_pin_management",
            settingScreenKey = SupervisionDashboardScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "vibrate_on",
            settingScreenKey = VibrationIntensityScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = "vibrate_on",
            settingScreenKey = VibrationScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = DeviceModelPreference.KEY,
            settingScreenKey = HardwareInfoScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = HardwareVersionPreference.KEY,
            settingScreenKey = HardwareInfoScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = AppInfoStorageScreen.KEY,
            settingScreenKey = AppStorageAppListScreen.KEY,
            hintText = { context, metadata ->
                metadata.extras(context)?.getString(AppInfoStorageScreen.KEY_EXTRA_PACKAGE_NAME)
            },
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = StoragePreferenceScreen.KEY_SUMMARY_USED,
            settingScreenKey = StoragePreferenceScreen.KEY,
            hintText = { _, _ -> "Total device storage currently used" },
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = StoragePreferenceScreen.KEY_SUMMARY_TOTAL,
            settingScreenKey = StoragePreferenceScreen.KEY,
            hintText = { _, _ -> "Total device storage" },
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = StoragePreferenceScreen.KEY_FREE_UP_SPACE,
            settingScreenKey = StoragePreferenceScreen.KEY,
            hintText = { context, metadata -> metadata.getPreferenceSummary(context).toString() },
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = StoragePreferenceScreen.KEY_PREF_APPS,
            settingScreenKey = StoragePreferenceScreen.KEY,
            hintText = { _, _ -> "Total device storage used by apps" },
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = StoragePreferenceScreen.KEY_PREF_GAMES,
            settingScreenKey = StoragePreferenceScreen.KEY,
            hintText = { _, _ -> "Total device storage used by games" },
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = StoragePreferenceScreen.KEY_PREF_DOCUMENTS,
            settingScreenKey = StoragePreferenceScreen.KEY,
            hintText = { _, _ -> "Total device storage used by document files" },
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = StoragePreferenceScreen.KEY_PREF_VIDEOS,
            settingScreenKey = StoragePreferenceScreen.KEY,
            hintText = { _, _ -> "Total device storage used by video files" },
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = StoragePreferenceScreen.KEY_PREF_AUDIO,
            settingScreenKey = StoragePreferenceScreen.KEY,
            hintText = { _, _ -> "Total device storage used by audio files" },
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = StoragePreferenceScreen.KEY_PREF_IMAGES,
            settingScreenKey = StoragePreferenceScreen.KEY,
            hintText = { _, _ -> "Total device storage used by image files" },
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = StoragePreferenceScreen.KEY_PREF_TRASH,
            settingScreenKey = StoragePreferenceScreen.KEY,
            hintText = { _, _ -> "Total device storage used by files in trash" },
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = StoragePreferenceScreen.KEY_PREF_OTHER,
            settingScreenKey = StoragePreferenceScreen.KEY,
            hintText = { _, _ -> "Total device storage used by other files" },
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = StoragePreferenceScreen.KEY_PREF_SYSTEM,
            settingScreenKey = StoragePreferenceScreen.KEY,
            hintText = { _, _ -> "Total device storage used by the operating system" },
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = StoragePreferenceScreen.KEY_PREF_TEMP,
            settingScreenKey = StoragePreferenceScreen.KEY,
            hintText = { _, _ -> "Total device storage used by temporary system files" },
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = DisplayOverOtherAppsAppDetailScreen.KEY,
            settingScreenKey = DisplayOverOtherAppsAppListScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = FullScreenNotificationsAppDetailScreen.KEY,
            settingScreenKey = FullScreenNotificationsAppListScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = InteractAcrossProfilesAppDetailScreen.KEY,
            settingScreenKey = InteractAcrossProfilesAppListScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = PictureInPictureAppDetailScreen.KEY,
            settingScreenKey = PictureInPictureAppListScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = WifiControlAppDetailScreen.KEY,
            settingScreenKey = WifiControlAppListScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = WriteSystemPreferencesAppDetailScreen.KEY,
            settingScreenKey = WriteSystemPreferencesAppListScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = AllFilesAccessAppDetailScreen.KEY,
            settingScreenKey = AllFilesAccessAppListScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = AlarmsAndRemindersAppDetailScreen.KEY,
            settingScreenKey = AlarmsAndRemindersAppListScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = ManageWriteSettingsAppDetailScreen.KEY,
            settingScreenKey = ManageWriteSettingsAppListScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = InstallUnknownAppsAppDetailScreen.KEY,
            settingScreenKey = InstallUnknownAppsAppListScreen.KEY,
        ),
        DeviceStateItemConfig(
            enabled = true,
            settingKey = WifiDataUsagePreference.KEY,
            settingScreenKey = NetworkProviderScreen.KEY,
            hintText = { _, _ ->
                "This data usage shows the amount of data consumed by the device between specific dates that was not transmitted over a mobile carrier network."
            },
        ),
    )
