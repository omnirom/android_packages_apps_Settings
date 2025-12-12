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

package com.android.settings.accessibility;

import static android.os.UserManager.DISALLOW_CONFIG_BLUETOOTH;

import static com.android.internal.accessibility.AccessibilityShortcutController.ACCESSIBILITY_HEARING_AIDS_COMPONENT_NAME;

import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.accessibility.hearingdevices.ui.HearingDevicesScreen;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

/** Accessibility settings for hearing aids. */
@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class AccessibilityHearingAidsFragment extends BaseRestrictedSupportFragment {
    private static final String TAG = "AccessibilityHearingAidsFragment";

    public AccessibilityHearingAidsFragment() {
        super(DISALLOW_CONFIG_BLUETOOTH);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (!isCatalystEnabled()) {
            use(AvailableHearingDevicePreferenceController.class).init(this);
            use(SavedHearingDevicePreferenceController.class).init(this);
            use(HearingAidCompatibilityPreferenceController.class).init(this);
            ToggleShortcutPreferenceController shortcutPreferenceController =
                    use(ToggleShortcutPreferenceController.class);
            if (shortcutPreferenceController != null) {
                shortcutPreferenceController.initialize(
                        getFeatureComponentName(),
                        getChildFragmentManager(),
                        getFeatureName(),
                        getMetricsCategory()
                );
            }
            use(HearingDevicesFeedbackButtonPreferenceController.class).initialize(
                    new FeedbackManager(context, getMetricsCategory()));
        }
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.ACCESSIBILITY_HEARING_AID_SETTINGS;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.accessibility_hearing_aids;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @NonNull
    private ComponentName getFeatureComponentName() {
        return ACCESSIBILITY_HEARING_AIDS_COMPONENT_NAME;
    }

    @NonNull
    private CharSequence getFeatureName() {
        return getText(R.string.accessibility_hearingaid_title);
    }

    @VisibleForTesting
    static boolean isPageSearchEnabled(Context context) {
        final HearingAidHelper mHelper = new HearingAidHelper(context);
        return mHelper.isHearingAidSupported();
    }

    @Override
    public @Nullable String getPreferenceScreenBindingKey(@NonNull Context context) {
        return HearingDevicesScreen.KEY;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.accessibility_hearing_aids) {
                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    return AccessibilityHearingAidsFragment.isPageSearchEnabled(context);
                }
            };
}
