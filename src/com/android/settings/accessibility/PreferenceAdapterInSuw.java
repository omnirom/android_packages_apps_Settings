/*
 * Copyright (C) 2025 The Android Open Source Project
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

import android.view.View;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceViewHolder;

import com.android.settingslib.widget.SettingsPreferenceGroupAdapter;

import com.google.android.setupdesign.util.ItemStyler;

public class PreferenceAdapterInSuw extends SettingsPreferenceGroupAdapter {

    public PreferenceAdapterInSuw(@NonNull PreferenceGroup preferenceGroup) {
        super(preferenceGroup);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        View view = holder.itemView;
        int paddingStart = view.getPaddingStart();
        int paddingTop = view.getPaddingTop();
        int paddingEnd = view.getPaddingEnd();
        int paddingBottom = view.getPaddingBottom();
        ItemStyler.applyPartnerCustomizationItemViewLayoutStyle(view);
        view.setPaddingRelative(view.getPaddingStart() + paddingStart,
                paddingTop, view.getPaddingEnd() + paddingEnd,
                paddingBottom);
    }
}
