/*
 * Copyright (C) 2020 The Android Open Source Project
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
package com.android.settings.fuelgauge.batterytip.tips;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.text.SpannableString;
import android.util.Log;

import androidx.preference.Preference;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.widget.BannerMessagePreference;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLog;

@RunWith(RobolectricTestRunner.class)
public class BatteryDefenderTipTest {

    private Context mContext;
    private FakeFeatureFactory mFeatureFactory;
    private BatteryDefenderTip mBatteryDefenderTip;
    private MetricsFeatureProvider mMetricsFeatureProvider;
    private BannerMessagePreference mCardPreference;

    @Rule public MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Mock private BatteryTip mBatteryTip;
    @Mock private Preference mPreference;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mMetricsFeatureProvider = mFeatureFactory.metricsFeatureProvider;
        mBatteryDefenderTip =
                new BatteryDefenderTip(BatteryTip.StateType.NEW, /* isPluggedIn= */ false);
        mCardPreference = spy(new BannerMessagePreference(mContext));

        when(mPreference.getContext()).thenReturn(mContext);
    }

    @Test
    public void getTitle_showTitle() {
        assertThat(mBatteryDefenderTip.getTitle(mContext))
                .isEqualTo(mContext.getString(R.string.battery_tip_limited_temporarily_title));
    }

    @Test
    public void getSummary_showSummary() {
        assertThat(mBatteryDefenderTip.getSummary(mContext))
                .isEqualTo(mContext.getString(R.string.battery_tip_limited_temporarily_summary));
    }

    @Test
    public void getIcon_showIcon() {
        assertThat(mBatteryDefenderTip.getIconId())
                .isEqualTo(R.drawable.ic_battery_defender_tip_shield);
    }

    @Test
    public void log_logMetric() {
        mBatteryDefenderTip.updateState(mBatteryTip);
        mBatteryDefenderTip.log(mContext, mMetricsFeatureProvider);

        verify(mMetricsFeatureProvider)
                .action(mContext, SettingsEnums.ACTION_BATTERY_DEFENDER_TIP, mBatteryTip.mState);
    }

    @Test
    public void updatePreference_castFail_logErrorMessage() {
        mBatteryDefenderTip.updatePreference(mPreference);

        assertThat(getLastErrorLog()).isEqualTo("cast Preference to TipCardPreference failed");
    }

    @Test
    public void updatePreference_shouldSetNegativeButtonText() {
        SpannableString expectedText =
                Utils.createAccessibleSequence(
                        mContext.getString(R.string.learn_more),
                        mContext.getString(R.string
                                .battery_tip_limited_temporarily_sec_button_content_description));
        ArgumentCaptor<CharSequence> captor = ArgumentCaptor.forClass(CharSequence.class);

        mBatteryDefenderTip.updatePreference(mCardPreference);

        verify(mCardPreference).setNegativeButtonText(captor.capture());
        assertThat(captor.getValue().toString()).isEqualTo(expectedText.toString());
    }

    @Test
    public void updatePreference_shouldSetPositiveButtonText() {
        String expected = mContext.getString(R.string.battery_tip_charge_to_full_button);

        mBatteryDefenderTip.updatePreference(mCardPreference);

        verify(mCardPreference).setPositiveButtonText(eq(expected));
    }

    @Test
    public void updatePreference_shouldSetNegativeButtonVisible() {
        mBatteryDefenderTip.updatePreference(mCardPreference);

        verify(mCardPreference).setNegativeButtonVisible(true);
    }

    @Test
    public void updatePreference_whenCharging_SetNegativeButtonVisibleToBeTrue() {
        mBatteryDefenderTip =
                new BatteryDefenderTip(BatteryTip.StateType.NEW, /* isPluggedIn= */ true);

        mBatteryDefenderTip.updatePreference(mCardPreference);

        verify(mCardPreference).setNegativeButtonVisible(true);
    }

    @Test
    public void updatePreference_whenNotCharging_SetPositiveButtonVisibleToBeFalse() {
        mBatteryDefenderTip.updatePreference(mCardPreference);

        // Once for init, once for update
        verify(mCardPreference, times(2)).setPositiveButtonVisible(false);
    }

    private String getLastErrorLog() {
        return ShadowLog.getLogsForTag(BatteryDefenderTip.class.getSimpleName()).stream()
                .filter(log -> log.type == Log.ERROR)
                .reduce((first, second) -> second)
                .orElse(createErrorLog("No Error Log"))
                .msg;
    }

    private ShadowLog.LogItem createErrorLog(String msg) {
        return new ShadowLog.LogItem(Log.ERROR, "tag", msg, null);
    }
}
