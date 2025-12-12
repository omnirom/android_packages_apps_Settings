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

package com.android.settings.development;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.view.View;
import android.widget.Button;

import androidx.preference.PreferenceViewHolder;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.accessibility.TextCursorBlinkRateSliderPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class TextCursorBlinkRateSliderPreferenceTest {
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private TextCursorBlinkRateSliderPreference mPreference;
    private PreferenceViewHolder mHolder;
    private final View.OnClickListener mClickListener = v -> mPreference.setValue(6);

    @Before
    public void setUp() {
        mPreference = new TextCursorBlinkRateSliderPreference(mContext);
        final View rootView =
                View.inflate(mContext, mPreference.getLayoutResource(), null /* parent */);
        mHolder = PreferenceViewHolder.createInstanceForTests(rootView);
    }

    @Test
    public void resetToDefault() {
        mPreference.setResetClickListener(mClickListener);
        mPreference.onBindViewHolder(mHolder);
        final Button button = mPreference.getButton();
        assertThat(mPreference.getValue()).isNotEqualTo(6);
        button.callOnClick();

        assertThat(mPreference.getValue()).isEqualTo(6);
    }
}
