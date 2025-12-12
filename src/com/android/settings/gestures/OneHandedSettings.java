/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.gestures;

import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.Context;
import android.os.UserHandle;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.internal.accessibility.AccessibilityShortcutController;
import com.android.settings.R;
import com.android.settings.accessibility.BaseSupportFragment;
import com.android.settings.accessibility.ToggleShortcutPreferenceController;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.widget.IllustrationPreference;
import com.android.settingslib.widget.MainSwitchPreference;

/**
 * Fragment for One-handed mode settings
 */
@SearchIndexable(forTarget = SearchIndexable.MOBILE)
public class OneHandedSettings extends BaseSupportFragment {

    private static final String TAG = "OneHandedSettings";
    private static final String ONE_HANDED_ILLUSTRATION_KEY = "one_handed_header";
    protected static final String ONE_HANDED_MAIN_SWITCH_KEY =
            "gesture_one_handed_mode_enabled_main_switch";
    @Nullable
    private OneHandedSettingsUtils mUtils;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
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
    }

    @Override
    protected void updatePreferenceStates() {
        OneHandedSettingsUtils.setUserId(UserHandle.myUserId());
        super.updatePreferenceStates();

        final IllustrationPreference illustrationPreference =
                getPreferenceScreen().findPreference(ONE_HANDED_ILLUSTRATION_KEY);
        final boolean isSwipeDownNotification =
                OneHandedSettingsUtils.isSwipeDownNotificationEnabled(getContext());
        illustrationPreference.setLottieAnimationResId(
                isSwipeDownNotification ? R.raw.lottie_swipe_for_notifications
                        : R.raw.lottie_one_hand_mode);

        final MainSwitchPreference mainSwitchPreference =
                getPreferenceScreen().findPreference(ONE_HANDED_MAIN_SWITCH_KEY);
        mainSwitchPreference.addOnSwitchChangeListener(CompoundButton::setChecked);
    }

    @Override
    public int getDialogMetricsCategory(int dialogId) {
        final int dialogMetrics = super.getDialogMetricsCategory(dialogId);
        return dialogMetrics == SettingsEnums.ACTION_UNKNOWN ? SettingsEnums.SETTINGS_ONE_HANDED
                : dialogMetrics;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_ONE_HANDED;
    }

    @Override
    public void onStart() {
        super.onStart();
        mUtils = new OneHandedSettingsUtils(this.getContext());
        mUtils.registerToggleAwareObserver(uri -> {
            Activity activity = getActivity();
            if (activity != null) {
                activity.runOnUiThread(() -> updatePreferenceStates());
            }
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mUtils != null) {
            mUtils.unregisterToggleAwareObserver();
        }
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.one_handed_settings;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.one_handed_settings) {
                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    return OneHandedSettingsUtils.isSupportOneHandedMode();
                }
            };

    @NonNull
    private CharSequence getFeatureName() {
        return getText(R.string.one_handed_title);
    }

    @NonNull
    private ComponentName getFeatureComponentName() {
        return AccessibilityShortcutController.ONE_HANDED_COMPONENT_NAME;
    }
}
