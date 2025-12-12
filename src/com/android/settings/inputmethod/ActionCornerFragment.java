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

package com.android.settings.inputmethod;

import static com.android.settings.inputmethod.InputPeripheralsSettingsUtils.isMouse;
import static com.android.settings.inputmethod.InputPeripheralsSettingsUtils.isTouchpad;

import android.app.settings.SettingsEnums;
import android.content.Context;

import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

/** Input settings for action corners. */
@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class ActionCornerFragment extends InputDeviceDashboardFragment {

    private static final String TAG = "ActionCornerFragment";

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.ACTION_CORNERS;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.action_corner_customization;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.action_corner_customization) {
                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    return isTouchpad() || isMouse();
                }
            };

    @Override
    protected boolean needToFinishEarly() {
        return isMouseDetached() && isTouchpadDetached();
    }
}
