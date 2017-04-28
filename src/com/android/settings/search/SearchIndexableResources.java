/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.settings.search;

import android.provider.SearchIndexableResource;
import com.android.settings.DateTimeSettings;
import com.android.settings.DevelopmentSettings;
import com.android.settings.DeviceInfoSettings;
import com.android.settings.DisplaySettings;
import com.android.settings.LegalSettings;
import com.android.settings.PrivacySettings;
import com.android.settings.R;
import com.android.settings.ScreenPinningSettings;
import com.android.settings.SecuritySettings;
import com.android.settings.WallpaperTypeSettings;
import com.android.settings.WirelessSettings;
import com.android.settings.accessibility.AccessibilitySettings;
import com.android.settings.accounts.AccountSettings;
import com.android.settings.applications.AdvancedAppSettings;
import com.android.settings.applications.SpecialAccessSettings;
import com.android.settings.bluetooth.BluetoothSettings;
import com.android.settings.datausage.DataUsageMeteredSettings;
import com.android.settings.datausage.DataUsageSummary;
import com.android.settings.deviceinfo.StorageSettings;
import com.android.settings.display.ScreenZoomSettings;
import com.android.settings.fuelgauge.BatterySaverSettings;
import com.android.settings.fuelgauge.PowerUsageSummary;
import com.android.settings.gestures.GestureSettings;
import com.android.settings.inputmethod.InputMethodAndLanguageSettings;
import com.android.settings.location.LocationSettings;
import com.android.settings.location.ScanningSettings;
import com.android.settings.notification.ConfigureNotificationSettings;
import com.android.settings.notification.OtherSoundSettings;
import com.android.settings.notification.SoundSettings;
import com.android.settings.notification.ZenModePrioritySettings;
import com.android.settings.notification.ZenModeSettings;
import com.android.settings.notification.ZenModeVisualInterruptionSettings;
import com.android.settings.print.PrintSettingsFragment;
import com.android.settings.sim.SimSettings;
import com.android.settings.users.UserSettings;
import com.android.settings.wifi.AdvancedWifiSettings;
import com.android.settings.wifi.SavedAccessPointsWifiSettings;
import com.android.settings.wifi.WifiSettings;

//Omni
import org.omnirom.omnigears.ButtonBrightnessSettings;
import org.omnirom.omnigears.ButtonSettings;
import org.omnirom.omnigears.DisplayRotation;
import org.omnirom.omnigears.batterylight.BatteryLightSettings;
import org.omnirom.omnigears.batterylight.NotificationLightSettings;
import org.omnirom.omnigears.interfacesettings.BarsSettings;
import org.omnirom.omnigears.interfacesettings.GlobalActionsSettings;
import org.omnirom.omnigears.interfacesettings.LockscreenSettings;
import org.omnirom.omnigears.interfacesettings.NetworkTraffic;
import org.omnirom.omnigears.interfacesettings.StatusbarBatterySettings;
import org.omnirom.omnigears.interfacesettings.StyleSettings;
import org.omnirom.omnigears.lightssettings.LightsSettings;
import org.omnirom.omnigears.moresettings.MoreSettings;

import java.util.Collection;
import java.util.HashMap;

public final class SearchIndexableResources {

    public static int NO_DATA_RES_ID = 0;

    private static HashMap<String, SearchIndexableResource> sResMap =
            new HashMap<String, SearchIndexableResource>();

    static {
        sResMap.put(WifiSettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(WifiSettings.class.getName()),
                        NO_DATA_RES_ID,
                        WifiSettings.class.getName(),
                        R.drawable.ic_settings_wireless));

        sResMap.put(AdvancedWifiSettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(AdvancedWifiSettings.class.getName()),
                        R.xml.wifi_advanced_settings,
                        AdvancedWifiSettings.class.getName(),
                        R.drawable.ic_settings_wireless));

        sResMap.put(SavedAccessPointsWifiSettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(SavedAccessPointsWifiSettings.class.getName()),
                        R.xml.wifi_display_saved_access_points,
                        SavedAccessPointsWifiSettings.class.getName(),
                        R.drawable.ic_settings_wireless));

        sResMap.put(BluetoothSettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(BluetoothSettings.class.getName()),
                        NO_DATA_RES_ID,
                        BluetoothSettings.class.getName(),
                        R.drawable.ic_settings_bluetooth));

        sResMap.put(SimSettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(SimSettings.class.getName()),
                        NO_DATA_RES_ID,
                        SimSettings.class.getName(),
                        R.drawable.ic_settings_sim));

        sResMap.put(DataUsageSummary.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(DataUsageSummary.class.getName()),
                        NO_DATA_RES_ID,
                        DataUsageSummary.class.getName(),
                        R.drawable.ic_settings_data_usage));

        sResMap.put(DataUsageMeteredSettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(DataUsageMeteredSettings.class.getName()),
                        NO_DATA_RES_ID,
                        DataUsageMeteredSettings.class.getName(),
                        R.drawable.ic_settings_data_usage));

        sResMap.put(WirelessSettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(WirelessSettings.class.getName()),
                        NO_DATA_RES_ID,
                        WirelessSettings.class.getName(),
                        R.drawable.ic_settings_more));

        sResMap.put(ScreenZoomSettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(ScreenZoomSettings.class.getName()),
                        NO_DATA_RES_ID,
                        ScreenZoomSettings.class.getName(),
                        R.drawable.ic_settings_display));

        sResMap.put(DisplaySettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(DisplaySettings.class.getName()),
                        NO_DATA_RES_ID,
                        DisplaySettings.class.getName(),
                        R.drawable.ic_settings_display));

        sResMap.put(WallpaperTypeSettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(WallpaperTypeSettings.class.getName()),
                        NO_DATA_RES_ID,
                        WallpaperTypeSettings.class.getName(),
                        R.drawable.ic_settings_display));

        sResMap.put(ConfigureNotificationSettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(ConfigureNotificationSettings.class.getName()),
                        R.xml.configure_notification_settings,
                        ConfigureNotificationSettings.class.getName(),
                        R.drawable.ic_settings_notifications));

        sResMap.put(SoundSettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(SoundSettings.class.getName()),
                        NO_DATA_RES_ID,
                        SoundSettings.class.getName(),
                        R.drawable.ic_settings_sound));

        sResMap.put(OtherSoundSettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(OtherSoundSettings.class.getName()),
                        NO_DATA_RES_ID,
                        OtherSoundSettings.class.getName(),
                        R.drawable.ic_settings_sound));

        sResMap.put(ZenModeSettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(ZenModeSettings.class.getName()),
                        R.xml.zen_mode_settings,
                        ZenModeSettings.class.getName(),
                        R.drawable.ic_settings_notifications));

        sResMap.put(ZenModePrioritySettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(ZenModePrioritySettings.class.getName()),
                        R.xml.zen_mode_priority_settings,
                        ZenModePrioritySettings.class.getName(),
                        R.drawable.ic_settings_notifications));

        sResMap.put(StorageSettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(StorageSettings.class.getName()),
                        NO_DATA_RES_ID,
                        StorageSettings.class.getName(),
                        R.drawable.ic_settings_storage));

        sResMap.put(PowerUsageSummary.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(PowerUsageSummary.class.getName()),
                        R.xml.power_usage_summary,
                        PowerUsageSummary.class.getName(),
                        R.drawable.ic_settings_battery));

        sResMap.put(BatterySaverSettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(BatterySaverSettings.class.getName()),
                        R.xml.battery_saver_settings,
                        BatterySaverSettings.class.getName(),
                        R.drawable.ic_settings_battery));

        sResMap.put(AdvancedAppSettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(AdvancedAppSettings.class.getName()),
                        NO_DATA_RES_ID,
                        AdvancedAppSettings.class.getName(),
                        R.drawable.ic_settings_applications));

        sResMap.put(SpecialAccessSettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(SpecialAccessSettings.class.getName()),
                        R.xml.special_access,
                        SpecialAccessSettings.class.getName(),
                        R.drawable.ic_settings_applications));

        sResMap.put(UserSettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(UserSettings.class.getName()),
                        NO_DATA_RES_ID,
                        UserSettings.class.getName(),
                        R.drawable.ic_settings_multiuser));

        sResMap.put(GestureSettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(GestureSettings.class.getName()),
                        NO_DATA_RES_ID,
                        GestureSettings.class.getName(),
                        R.drawable.ic_settings_gestures));

        sResMap.put(LocationSettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(LocationSettings.class.getName()),
                        R.xml.location_settings,
                        LocationSettings.class.getName(),
                        R.drawable.ic_settings_location));

        sResMap.put(ScanningSettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(ScanningSettings.class.getName()),
                        R.xml.location_scanning,
                        ScanningSettings.class.getName(),
                        R.drawable.ic_settings_location));

        sResMap.put(SecuritySettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(SecuritySettings.class.getName()),
                        NO_DATA_RES_ID,
                        SecuritySettings.class.getName(),
                        R.drawable.ic_settings_security));

        sResMap.put(ScreenPinningSettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(ScreenPinningSettings.class.getName()),
                        NO_DATA_RES_ID,
                        ScreenPinningSettings.class.getName(),
                        R.drawable.ic_settings_security));

        sResMap.put(AccountSettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(AccountSettings.class.getName()),
                        NO_DATA_RES_ID,
                        AccountSettings.class.getName(),
                        R.drawable.ic_settings_accounts));

        sResMap.put(InputMethodAndLanguageSettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(InputMethodAndLanguageSettings.class.getName()),
                        NO_DATA_RES_ID,
                        InputMethodAndLanguageSettings.class.getName(),
                        R.drawable.ic_settings_language));

        sResMap.put(PrivacySettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(PrivacySettings.class.getName()),
                        NO_DATA_RES_ID,
                        PrivacySettings.class.getName(),
                        R.drawable.ic_settings_backup));

        sResMap.put(DateTimeSettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(DateTimeSettings.class.getName()),
                        NO_DATA_RES_ID,
                        DateTimeSettings.class.getName(),
                        R.drawable.ic_settings_date_time));

        sResMap.put(AccessibilitySettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(AccessibilitySettings.class.getName()),
                        NO_DATA_RES_ID,
                        AccessibilitySettings.class.getName(),
                        R.drawable.ic_settings_accessibility));

        sResMap.put(PrintSettingsFragment.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(PrintSettingsFragment.class.getName()),
                        NO_DATA_RES_ID,
                        PrintSettingsFragment.class.getName(),
                        R.drawable.ic_settings_print));

        sResMap.put(DevelopmentSettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(DevelopmentSettings.class.getName()),
                        NO_DATA_RES_ID,
                        DevelopmentSettings.class.getName(),
                        R.drawable.ic_settings_development));

        sResMap.put(DeviceInfoSettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(DeviceInfoSettings.class.getName()),
                        NO_DATA_RES_ID,
                        DeviceInfoSettings.class.getName(),
                        R.drawable.ic_settings_about));

        sResMap.put(LegalSettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(LegalSettings.class.getName()),
                        NO_DATA_RES_ID,
                        LegalSettings.class.getName(),
                        R.drawable.ic_settings_about));

        sResMap.put(ZenModeVisualInterruptionSettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(
                                ZenModeVisualInterruptionSettings.class.getName()),
                        R.xml.zen_mode_visual_interruptions_settings,
                        ZenModeVisualInterruptionSettings.class.getName(),
                        R.drawable.ic_settings_notifications));

        //Omni
        sResMap.put(BarsSettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(
                                BarsSettings.class.getName()),
                        R.xml.bars_settings,
                        BarsSettings.class.getName(),
                        R.drawable.ic_bars_tile));

        sResMap.put(NetworksTraffic.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(
                                NetworksTraffic.class.getName()),
                        R.xml.network_traffic,
                        NetworksTraffic.class.getName(),
                        R.drawable.ic_bars_tile));


        sResMap.put(ButtonSettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(
                                ButtonSettings.class.getName()),
                        R.xml.button_settings,
                        ButtonSettings.class.getName(),
                        R.drawable.ic_settings_buttons));

        sResMap.put(ButtonBrightnessSettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(
                                ButtonBrightnessSettings.class.getName()),
                        R.xml.button_brightness_settings,
                        ButtonBrightnessSettings.class.getName(),
                        R.drawable.ic_settings_buttons));

        sResMap.put(GlobalActionsSettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(
                                GlobalActionsSettings.class.getName()),
                        R.xml.global_actions,
                        GlobalActionsSettings.class.getName(),
                        R.drawable.ic_settings_buttons));

        sResMap.put(LockscreenSettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(
                                LockscreenSettings.class.getName()),
                        R.xml.lockscreen_settings,
                        LockscreenSettings.class.getName(),
                        R.drawable.ic_lockscreen_tile));

        sResMap.put(LightsSettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(
                                LightsSettings.class.getName()),
                        R.xml.lights_settings,
                        LightsSettings.class.getName(),
                        R.drawable.ic_settings_lights));

        sResMap.put(BatteryLightSettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(
                                BatteryLightSettings.class.getName()),
                        R.xml.battery_light_settings,
                        BatteryLightSettings.class.getName(),
                        R.drawable.ic_settings_lights));

        sResMap.put(NotificationLightSettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(
                                NotificationLightSettings.class.getName()),
                        R.xml.notification_light_settings,
                        NotificationLightSettings.class.getName(),
                        R.drawable.ic_settings_lights));

        sResMap.put(StyleSettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(
                                StyleSettings.class.getName()),
                        R.xml.style_settings,
                        StyleSettings.class.getName(),
                        R.drawable.ic_settings_style));

        sResMap.put(StatusbarBatterySettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(
                                StatusbarBatterySettings.class.getName()),
                        R.xml.statusbar_battery_settings,
                        StatusbarBatterySettings.class.getName(),
                        R.drawable.ic_settings_style));

        sResMap.put(MoreSettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(
                                MoreSettings.class.getName()),
                        R.xml.more_settings,
                        MoreSettings.class.getName(),
                        R.drawable.ic_settings_more));

    }

    private SearchIndexableResources() {
    }

    public static int size() {
        return sResMap.size();
    }

    public static SearchIndexableResource getResourceByName(String className) {
        return sResMap.get(className);
    }

    public static Collection<SearchIndexableResource> values() {
        return sResMap.values();
    }
}
