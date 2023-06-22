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

import static com.android.settings.accessibility.AccessibilityUtil.State.OFF;
import static com.android.settings.accessibility.AccessibilityUtil.State.ON;
import static com.android.settings.display.ToggleFontSizePreferenceFragment.fontSizeValueToIndex;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.provider.Settings;

import com.android.settingslib.R;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Data class for storing the configurations related to the font size.
 */
final class FontSizeData extends PreviewSizeData<Float> {
    private static final float FONT_SCALE_DEF_VALUE = 1.0f;

    FontSizeData(Context context) {
        super(context);

        final Resources resources = getContext().getResources();
        final ContentResolver resolver = getContext().getContentResolver();
        final List<String> strEntryValues =
                Arrays.asList(resources.getStringArray(R.array.entryvalues_font_size));
        setDefaultValue(FONT_SCALE_DEF_VALUE);
        final float currentScale =
                Settings.System.getFloat(resolver, Settings.System.FONT_SCALE, getDefaultValue());
        setInitialIndex(fontSizeValueToIndex(currentScale, strEntryValues.toArray(new String[0])));
        setValues(strEntryValues.stream().map(Float::valueOf).collect(Collectors.toList()));
    }

    @Override
    void commit(int currentProgress) {
        final ContentResolver resolver = getContext().getContentResolver();
        if (Settings.Secure.getInt(resolver,
                Settings.Secure.ACCESSIBILITY_FONT_SCALING_HAS_BEEN_CHANGED,
                /* def= */ OFF) != ON) {
            Settings.Secure.putInt(resolver,
                    Settings.Secure.ACCESSIBILITY_FONT_SCALING_HAS_BEEN_CHANGED, ON);
        }
        Settings.System.putFloat(resolver, Settings.System.FONT_SCALE,
                getValues().get(currentProgress));
    }
}
