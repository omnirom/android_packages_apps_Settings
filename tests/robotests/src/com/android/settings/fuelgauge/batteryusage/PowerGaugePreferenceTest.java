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
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settingslib.widget.SettingsThemeHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {PowerGaugePreferenceTest.ShadowSettingsThemeHelper.class})
public class PowerGaugePreferenceTest {

    private static final String PERCENTAGE = "99%";
    private static final String SUBTITLE = "Summary";
    private static final String CONTENT_DESCRIPTION = "Content description";
    private static final float UNSELECTABLE_ALPHA = 0.65f;
    private static final float SELECTABLE_ALPHA = 1.0f;

    private Context mContext;
    private PowerGaugePreference mPowerGaugePreference;
    private View mRootView;
    private View mWidgetView;
    private PreferenceViewHolder mPreferenceViewHolder;

    @Mock
    Drawable mMockIcon;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ShadowSettingsThemeHelper.setExpressiveTheme(false);

        mContext = RuntimeEnvironment.application;
        mPowerGaugePreference = new PowerGaugePreference(mContext);
        setUpViewHolder();
    }

    @Test
    public void constructor_notExpressiveTheme_expectedLayoutResource() {
        ShadowSettingsThemeHelper.setExpressiveTheme(false);
        mPowerGaugePreference = new PowerGaugePreference(mContext);

        assertThat(mPowerGaugePreference.getLayoutResource()).isEqualTo(
                R.layout.warning_frame_preference);
    }

    @Test
    public void constructor_expressiveTheme_expectedLayoutResource() {
        ShadowSettingsThemeHelper.setExpressiveTheme(true);
        mPowerGaugePreference = new PowerGaugePreference(mContext);

        assertThat(mPowerGaugePreference.getLayoutResource()).isEqualTo(
                R.layout.expressive_warning_frame_preference);
    }

    @Test
    public void onBindViewHolder_showHint_hasHintChip() {
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
    public void onBindViewHolder_emptyHintText_withoutHintChip() {
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
    public void onBindViewHolder_noAppIconWithHintText_hasChipWithoutPaddingPlaceholder() {
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
    public void onBindViewHolder_bindContentDescription() {
        mPowerGaugePreference.setContentDescription(CONTENT_DESCRIPTION);
        mPowerGaugePreference.onBindViewHolder(mPreferenceViewHolder);

        assertThat(mPreferenceViewHolder.findViewById(android.R.id.title).getContentDescription())
                .isEqualTo(CONTENT_DESCRIPTION);
    }

    @Test
    public void onBindViewHolder_expressiveTheme_bindSelectablePercentage() {
        ShadowSettingsThemeHelper.setExpressiveTheme(true);
        mPowerGaugePreference.setPercentage(PERCENTAGE);
        mPowerGaugePreference.setSelectable(true);
        mPowerGaugePreference.onBindViewHolder(mPreferenceViewHolder);

        final TextView widgetSummary =
                (TextView) mPreferenceViewHolder.findViewById(R.id.widget_summary);
        assertThat(widgetSummary.getCurrentTextColor()).isEqualTo(
                Utils.getColorAttrDefaultColor(mContext, android.R.attr.textColorPrimary));
        assertThat(widgetSummary.getAlpha()).isEqualTo(SELECTABLE_ALPHA);
    }

    @Test
    public void onBindViewHolder_expressiveTheme_bindUnselectablePercentage() {
        ShadowSettingsThemeHelper.setExpressiveTheme(true);
        mPowerGaugePreference.setPercentage(PERCENTAGE);
        mPowerGaugePreference.setSelectable(false);
        mPowerGaugePreference.onBindViewHolder(mPreferenceViewHolder);

        final TextView widgetSummary =
                (TextView) mPreferenceViewHolder.findViewById(R.id.widget_summary);
        assertThat(widgetSummary.getCurrentTextColor()).isEqualTo(
                Utils.getColorAttrDefaultColor(mContext, android.R.attr.textColorPrimary));
        assertThat(widgetSummary.getAlpha()).isEqualTo(UNSELECTABLE_ALPHA);
    }

    @Test
    public void onBindViewHolder_bindPercentageContentDescription() {
        mPowerGaugePreference.setPercentageContentDescription(CONTENT_DESCRIPTION);
        mPowerGaugePreference.onBindViewHolder(mPreferenceViewHolder);

        assertThat(mPreferenceViewHolder.findViewById(R.id.widget_summary).getContentDescription())
                .isEqualTo(CONTENT_DESCRIPTION);
    }

    private void setUpViewHolder() {
        mRootView =
                View.inflate(mContext, mPowerGaugePreference.getLayoutResource(), null);
        final LinearLayout widgetFrame = mRootView.findViewById(android.R.id.widget_frame);
        assertThat(widgetFrame).isNotNull();
        mWidgetView =
                View.inflate(mContext, R.layout.preference_widget_summary, null);
        widgetFrame.addView(mWidgetView);
        mPreferenceViewHolder = PreferenceViewHolder.createInstanceForTests(mRootView);
    }

    @Implements(SettingsThemeHelper.class)
    public static class ShadowSettingsThemeHelper {
        private static boolean sIsExpressiveTheme;

        /** Shadow implementation of isExpressiveTheme */
        @Implementation
        public static boolean isExpressiveTheme(@NonNull Context context) {
            return sIsExpressiveTheme;
        }

        static void setExpressiveTheme(boolean isExpressiveTheme) {
            sIsExpressiveTheme = isExpressiveTheme;
        }
    }
}
