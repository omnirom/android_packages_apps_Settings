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
package com.android.settings.fuelgauge.batteryusage;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Space;

import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settingslib.widget.SettingsThemeHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class PowerGaugePreferenceTest {

    private static final String SUBTITLE = "Summary";
    private static final String CONTENT_DESCRIPTION = "Content description";

    private Context mContext;
    private PowerGaugePreference mPowerGaugePreference;
    private View mRootView;
    private View mWidgetView;
    private PreferenceViewHolder mPreferenceViewHolder;

    @Mock Drawable mMockIcon;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        mPowerGaugePreference = new PowerGaugePreference(mContext);
        mRootView =
                LayoutInflater.from(mContext)
                        .inflate(mPowerGaugePreference.getLayoutResource(), null);
        mWidgetView =
                LayoutInflater.from(mContext).inflate(R.layout.preference_widget_summary, null);
        final LinearLayout widgetFrame = mRootView.findViewById(android.R.id.widget_frame);
        assertThat(widgetFrame).isNotNull();
        widgetFrame.addView(mWidgetView);
        mPreferenceViewHolder = PreferenceViewHolder.createInstanceForTests(mRootView);

        assertThat(mPowerGaugePreference.getLayoutResource())
                .isEqualTo(
                        SettingsThemeHelper.isExpressiveTheme(mContext)
                                ? R.layout.expressive_warning_frame_preference
                                : R.layout.warning_frame_preference);
    }

    @Test
    public void testOnBindViewHolder_showHint_hasHintChip() {
        mPowerGaugePreference.setHint("Hint Text");
        mPowerGaugePreference.setIcon(mMockIcon);
        mPowerGaugePreference.onBindViewHolder(mPreferenceViewHolder);

        final LinearLayout warningChipFrame =
                (LinearLayout) mPreferenceViewHolder.findViewById(R.id.warning_chip_frame);
        final Space warningPaddingPlaceHolder =
                warningChipFrame.findViewById(R.id.warning_padding_placeholder);

        assertThat(warningChipFrame.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(warningPaddingPlaceHolder.getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void testOnBindViewHolder_emptyHintText_withoutHintChip() {
        mPowerGaugePreference.setHint("");
        mPowerGaugePreference.setIcon(mMockIcon);
        mPowerGaugePreference.onBindViewHolder(mPreferenceViewHolder);

        final LinearLayout warningChipFrame =
                (LinearLayout) mPreferenceViewHolder.findViewById(R.id.warning_chip_frame);
        final Space warningPaddingPlaceholder =
                warningChipFrame.findViewById(R.id.warning_padding_placeholder);

        assertThat(warningChipFrame.getVisibility()).isEqualTo(View.GONE);
        assertThat(warningPaddingPlaceholder.getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void testOnBindViewHolder_noAppIconWithHintText_hasChipWithoutPaddingPlaceholder() {
        mPowerGaugePreference.setHint("Anomaly Hint Text");
        mPowerGaugePreference.setIcon(null);
        mPowerGaugePreference.onBindViewHolder(mPreferenceViewHolder);

        final LinearLayout warningChipFrame =
                (LinearLayout) mPreferenceViewHolder.findViewById(R.id.warning_chip_frame);
        final Space warningPaddingPlaceHolder =
                warningChipFrame.findViewById(R.id.warning_padding_placeholder);

        assertThat(warningChipFrame.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(warningPaddingPlaceHolder.getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void testOnBindViewHolder_bindContentDescription() {
        mPowerGaugePreference.setContentDescription(CONTENT_DESCRIPTION);
        mPowerGaugePreference.onBindViewHolder(mPreferenceViewHolder);

        assertThat(mPreferenceViewHolder.findViewById(android.R.id.title).getContentDescription())
                .isEqualTo(CONTENT_DESCRIPTION);
    }

    @Test
    public void testOnBindViewHolder_bindPercentageContentDescription() {
        mPowerGaugePreference.setPercentageContentDescription(CONTENT_DESCRIPTION);
        mPowerGaugePreference.onBindViewHolder(mPreferenceViewHolder);

        assertThat(mPreferenceViewHolder.findViewById(R.id.widget_summary).getContentDescription())
                .isEqualTo(CONTENT_DESCRIPTION);
    }
}
