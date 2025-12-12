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

import static com.android.settings.testutils.AccessibilityTestUtils.inflateShortcutPreferenceView;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.widget.CompoundButton;

import androidx.preference.PreferenceViewHolder;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/**
 * Tests for {@link ShortcutPreference}
 */
@RunWith(RobolectricTestRunner.class)
public class ShortcutPreferenceTest {

    private static final String TOGGLE_CLICKED = "toggle_clicked";
    private static final String SETTINGS_CLICKED = "settings_clicked";

    private ShortcutPreference mShortcutPreference;
    private PreferenceViewHolder mViewHolder;
    private String mResult;

    private final ShortcutPreference.OnClickCallback mListener =
            new ShortcutPreference.OnClickCallback() {
                @Override
                public void onToggleClicked(ShortcutPreference preference) {
                    mResult = TOGGLE_CLICKED;
                }

                @Override
                public void onSettingsClicked(ShortcutPreference preference) {
                    mResult = SETTINGS_CLICKED;
                }
            };

    @Before
    public void setUp() {
        final Context context = ApplicationProvider.getApplicationContext();
        mShortcutPreference = new ShortcutPreference(context, null);
        mViewHolder = inflateShortcutPreferenceView(context, mShortcutPreference);
    }

    @Test
    public void clickToggle_toggleClicked() {
        mShortcutPreference.onBindViewHolder(mViewHolder);
        mShortcutPreference.setOnClickCallback(mListener);

        CompoundButton switchWidget = mViewHolder.itemView.findViewById(
                mShortcutPreference.getSwitchResId());
        assert switchWidget != null;
        switchWidget.performClick();

        assertThat(mResult).isEqualTo(TOGGLE_CLICKED);
        assertThat(mShortcutPreference.isChecked()).isTrue();
    }

    @Test
    public void clickItem_settingsClicked() {
        mShortcutPreference.onBindViewHolder(mViewHolder);
        mShortcutPreference.setOnClickCallback(mListener);

        mViewHolder.itemView.performClick();

        assertThat(mResult).isEqualTo(SETTINGS_CLICKED);
    }

    @Test
    public void clickPreference_settingsClicked() {
        mShortcutPreference.onBindViewHolder(mViewHolder);
        mShortcutPreference.setOnClickCallback(mListener);

        mShortcutPreference.performClick();

        assertThat(mResult).isEqualTo(SETTINGS_CLICKED);
    }

    @Test
    public void setCheckedTrue_getToggleIsTrue() {
        mShortcutPreference.setChecked(true);

        assertThat(mShortcutPreference.isChecked()).isEqualTo(true);
    }
}
