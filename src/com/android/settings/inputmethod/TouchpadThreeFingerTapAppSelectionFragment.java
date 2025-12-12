/*
 * Copyright 2024 The Android Open Source Project
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

package com.android.settings.inputmethod;

import static com.android.settings.inputmethod.InputPeripheralsSettingsUtils.isTouchpad;
import static com.android.settings.inputmethod.TouchpadThreeFingerTapActionPreferenceController.SET_GESTURE;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;

import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

/** List all installed apps to be launched with three finger tap. */
@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class TouchpadThreeFingerTapAppSelectionFragment extends InputDeviceDashboardFragment {

    private static final String TAG = "TouchpadThreeFingerTapAppSelectionFragment";

    @Override
    public void onCreatePreferences(@NonNull Bundle savedInstanceState, @NonNull String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);
        Bundle args = getArguments();
        if (args != null) {
            Bundle extras = getPreferenceScreen().getExtras();
            extras.putBoolean(SET_GESTURE, args.getBoolean(SET_GESTURE, false));
        }
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.TOUCHPAD_THREE_FINGER_TAP;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.input_touchpad_three_finger_tap_app_selection;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.input_touchpad_three_finger_tap_app_selection) {
                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    return isTouchpad();
                }
            };

    @Override
    protected boolean needToFinishEarly() {
        return isTouchpadDetached();
    }
}
