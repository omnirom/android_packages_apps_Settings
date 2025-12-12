/*
 * Copyright (C) 2019 The Android Open Source Project
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

import static com.android.internal.accessibility.AccessibilityShortcutController.MAGNIFICATION_COMPONENT_NAME;
import static com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.DEFAULT;

import android.content.Context;

import androidx.annotation.NonNull;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

public class MagnificationPreferenceController extends BasePreferenceController {

    public MagnificationPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public CharSequence getSummary() {
        return getServiceSummary(mContext);
    }

    @NonNull
    private CharSequence getServiceSummary(@NonNull Context context) {
        // Get the user shortcut type from settings provider.
        final int userShortcutType = AccessibilityUtil.getUserShortcutTypesFromSettings(
                context, MAGNIFICATION_COMPONENT_NAME);
        final CharSequence featureState =
                (userShortcutType != DEFAULT)
                        ? context.getText(R.string.accessibility_summary_shortcut_enabled)
                        : context.getText(R.string.generic_accessibility_feature_shortcut_off);
        final CharSequence featureSummary = context.getText(R.string.magnification_feature_summary);
        return context.getString(
                com.android.settingslib.R.string.preference_summary_default_combination,
                featureState, featureSummary);
    }
}
