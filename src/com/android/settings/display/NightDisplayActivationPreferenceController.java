/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.display;

import android.content.Context;
import android.hardware.display.ColorDisplayManager;
import android.text.TextUtils;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

/**
 * Controller that updates the night display.
 */
public class NightDisplayActivationPreferenceController extends TogglePreferenceController {

    private final MetricsFeatureProvider mMetricsFeatureProvider;
    private ColorDisplayManager mColorDisplayManager;
    private NightDisplayTimeFormatter mTimeFormatter;

    public NightDisplayActivationPreferenceController(Context context, String key) {
        super(context, key);

        mColorDisplayManager = context.getSystemService(ColorDisplayManager.class);
        mTimeFormatter = new NightDisplayTimeFormatter(context);
        mMetricsFeatureProvider = FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
    }

    @Override
    public int getAvailabilityStatus() {
        return ColorDisplayManager.isNightDisplayAvailable(mContext) ? AVAILABLE_UNSEARCHABLE
                : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean isSliceable() {
        return TextUtils.equals(getPreferenceKey(), "night_display_activated");
    }

    @Override
    public boolean isPublicSlice() {
        return true;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_display;
    }

    /** FOR SLICES */

    @Override
    public boolean isChecked() {
        return mColorDisplayManager.isNightDisplayActivated();
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return mColorDisplayManager.setNightDisplayActivated(isChecked);
    }

    @Override
    public CharSequence getSummary() {
        return mTimeFormatter.getAutoModeSummary(mContext, mColorDisplayManager);
    }

}
