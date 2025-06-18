/*
 * Copyright (C) 2015 The Android Open Source Project
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

import static com.android.internal.accessibility.AccessibilityShortcutController.AUTOCLICK_COMPONENT_NAME;

import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.internal.annotations.VisibleForTesting;
import com.android.server.accessibility.Flags;
import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

import java.util.List;

/**
 * Fragment for preference screen for settings related to Automatically click after mouse stops
 * feature.
 */
@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class ToggleAutoclickPreferenceFragment
        extends AccessibilityShortcutPreferenceFragment {

    private static final String TAG = "AutoclickPrefFragment";

    @VisibleForTesting
    static final String KEY_AUTOCLICK_SHORTCUT_PREFERENCE = "autoclick_shortcut_preference";

    /**
     * Autoclick settings do not need to set any restriction key for pin protected.
     */
    public ToggleAutoclickPreferenceFragment() {
        super(/* restrictionKey= */ null);
    }

    @Override
    protected CharSequence getLabelName() {
        return getContext().getString(R.string.accessibility_autoclick_shortcut_title);
    }

    @Override
    protected boolean showGeneralCategory() {
        return false;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.ACCESSIBILITY_TOGGLE_AUTOCLICK;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_autoclick;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.accessibility_autoclick_settings;
    }

    @Override
    protected ComponentName getComponentName() {
        return AUTOCLICK_COMPONENT_NAME;
    }

    @Override
    protected CharSequence getShortcutTitle() {
        return getString(R.string.accessibility_autoclick_shortcut_title);
    }

    @Override
    protected String getShortcutPreferenceKey() {
        return KEY_AUTOCLICK_SHORTCUT_PREFERENCE;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        if (!Flags.enableAutoclickIndicator()) {
            getPreferenceScreen().removePreference(mShortcutPreference);
        }
        return view;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.accessibility_autoclick_settings) {
                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    List<String> niks = super.getNonIndexableKeys(context);

                    if (!Flags.enableAutoclickIndicator()) {
                        niks.add(KEY_AUTOCLICK_SHORTCUT_PREFERENCE);
                    }
                    return niks;
                }
            };
}
