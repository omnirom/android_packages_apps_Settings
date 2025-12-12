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
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settingslib.widget.SliderPreference;

public class TextCursorBlinkRateSliderPreference extends SliderPreference {
    private @Nullable Button mResetButton;
    private @Nullable View.OnClickListener mClickListener;

    public TextCursorBlinkRateSliderPreference(@NonNull Context context,
            @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setTickVisible(true);
        setSliderIncrement(1);
        setIconStart(R.drawable.ic_remove_24dp);
        setIconStartContentDescription(R.string.accessibility_text_cursor_blink_rate_slower_desc);
        setIconEnd(R.drawable.ic_add_24dp);
        setIconEndContentDescription(R.string.accessibility_text_cursor_blink_rate_faster_desc);
        setLayoutResource(R.layout.text_cursor_blink_rate_slider_preference);
    }

    public TextCursorBlinkRateSliderPreference(@NonNull Context context,
            @Nullable AttributeSet attrs) {
        this(context, attrs, 0 /* defStyleAttr */);
    }

    public TextCursorBlinkRateSliderPreference(@NonNull Context context) {
        this(context, null);
    }

    /**
     * Return Button
     */
    public @Nullable Button getButton() {
        return mResetButton;
    }

    /**
     * Set the click listener for the reset button.
     */
    public void setResetClickListener(@NonNull View.OnClickListener clickListener) {
        mClickListener = clickListener;
        if (mResetButton != null) {
            mResetButton.setOnClickListener(clickListener);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        mResetButton = (Button) holder.findViewById(R.id.reset_button);
        if (mClickListener != null) {
            setResetClickListener(mClickListener);
        }
    }
}
