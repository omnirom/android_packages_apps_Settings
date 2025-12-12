/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.accessibility;

import android.app.settings.SettingsEnums;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.settings.R;
import com.android.settings.accessibility.flashnotifications.ui.FlashNotificationsScreen;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

/**
 * Fragment for flash notifications.
 */
@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class FlashNotificationsPreferenceFragment extends BaseSupportFragment {

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.flash_notifications_settings;
    }

    @Override
    protected String getLogTag() {
        return "FlashNotificationsPreferenceFragment";
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.FLASH_NOTIFICATION_SETTINGS;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (!isCatalystEnabled()) {
            use(ScreenFlashNotificationPreferenceController.class).setParentFragment(this);
        }
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_flash_notifications;
    }

    @Override
    public @Nullable String getPreferenceScreenBindingKey(@NonNull Context context) {
        return FlashNotificationsScreen.KEY;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(Flags.catalystFlashNotifications()
                    && com.android.settings.flags.Flags.catalystSettingsSearch() ? 0
                    : R.xml.flash_notifications_settings);
}
