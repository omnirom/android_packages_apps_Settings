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

import android.content.Context;
import android.content.res.TypedArray;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceViewHolder;

import com.android.settingslib.widget.SettingsPreferenceGroupAdapter;
import com.android.settingslib.widget.SettingsThemeHelper;

/**
 * PreferenceAdapterInSuw is a temporary fix on the padding issue introduced in the expressive style
 * in SettingsPreferenceGroupAdapter. This adapter should be removed once the padding issue is
 * resolved in the SettingsLib.
 *
 * TODO(b/403645956): Remove this adapter
 */
public class PreferenceAdapterInSuw extends SettingsPreferenceGroupAdapter {
    private final int mListPreferredItemPaddingStart;
    private final int mListPreferredItemPaddingEnd;
    private final int mContentPadding;

    public PreferenceAdapterInSuw(@NonNull PreferenceGroup preferenceGroup) {
        super(preferenceGroup);
        TypedArray resolvedAttributes = preferenceGroup.getContext().obtainStyledAttributes(
                new int[]{android.R.attr.listPreferredItemPaddingStart,
                        android.R.attr.listPreferredItemPaddingEnd});
        mListPreferredItemPaddingStart = resolvedAttributes.getDimensionPixelSize(0, 0);
        mListPreferredItemPaddingEnd = resolvedAttributes.getDimensionPixelSize(1, 0);
        resolvedAttributes.recycle();
        mContentPadding = preferenceGroup.getContext().getResources().getDimensionPixelSize(
                com.android.settingslib.widget.theme
                        .R.dimen.settingslib_expressive_space_small1);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        View view = holder.itemView;
        int paddingTop = view.getPaddingTop();
        int paddingBottom = view.getPaddingBottom();

        if (shouldApplyPadding(view.getContext(), position)) {
            view.setPaddingRelative(mListPreferredItemPaddingStart + mContentPadding, paddingTop,
                    mListPreferredItemPaddingEnd + mContentPadding, paddingBottom);
        } else {
            view.setPaddingRelative(mListPreferredItemPaddingStart, paddingTop,
                    mListPreferredItemPaddingEnd, paddingBottom);
        }
    }

    private boolean shouldApplyPadding(@NonNull Context context, int position) {
        if (SettingsThemeHelper.isExpressiveTheme(context)) {
            return getRoundCornerDrawableRes(position, /* isSelected= */ false) != 0;
        }
        return false;
    }
}
