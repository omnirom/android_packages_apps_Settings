/*
 * Copyright (C) 2023 The Android Open Source Project
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.LocaleList;
import android.view.View;

import com.android.settings.testutils.BatteryTestUtils;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settingslib.widget.BannerMessagePreference;

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
import org.robolectric.annotation.RealObject;
import org.robolectric.shadow.api.Shadow;

import java.util.Locale;
import java.util.TimeZone;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = BatteryTipsControllerTest.ShadowBannerMessagePreference.class)
public final class BatteryTipsControllerTest {

    private Context mContext;
    private FakeFeatureFactory mFeatureFactory;
    private BatteryTipsController mBatteryTipsController;
    private BannerMessagePreference mCardPreference;

    @Mock Drawable mIconDrawable;
    @Mock View mView;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Locale.setDefault(new Locale("en_US"));
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        mContext = spy(RuntimeEnvironment.application);
        DatabaseUtils.removeDismissedPowerAnomalyKeys(mContext);
        final Resources resources = spy(mContext.getResources());
        resources.getConfiguration().setLocales(new LocaleList(new Locale("en_US")));
        doReturn(resources).when(mContext).getResources();
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mBatteryTipsController = spy(new BatteryTipsController(mContext));
        mCardPreference = spy(new BannerMessagePreference(mContext));
        mBatteryTipsController.mCardPreference = mCardPreference;
    }

    @Test
    public void handleBatteryTipsCardUpdated_null_hidePreference() {
        mBatteryTipsController.handleBatteryTipsCardUpdated(/* powerAnomalyEvents= */ null, false);

        assertThat(mCardPreference.isVisible()).isFalse();
    }

    @Test
    public void handleBatteryTipsCardUpdated_adaptiveBrightnessAnomaly_showAnomaly() {
        AnomalyEventWrapper anomalyEventWrapper =
                spy(
                        new AnomalyEventWrapper(
                                mContext,
                                BatteryTestUtils.createAdaptiveBrightnessAnomalyEvent(true)));
        when(anomalyEventWrapper.getIconDrawable()).thenReturn(mIconDrawable);
        when(mFeatureFactory.powerUsageFeatureProvider.isBatteryTipsEnabled()).thenReturn(true);

        mBatteryTipsController.handleBatteryTipsCardUpdated(anomalyEventWrapper, false);

        assertCardPreference(
                "Turn on adaptive brightness to extend battery life",
                "View Settings",
                "Got it",
                BannerMessagePreference.AttentionLevel.NORMAL);
        assertCardButtonActionAndMetrics(anomalyEventWrapper);
    }

    @Test
    public void handleBatteryTipsCardUpdated_screenTimeoutAnomaly_showAnomaly() {
        AnomalyEventWrapper anomalyEventWrapper =
                spy(
                        new AnomalyEventWrapper(
                                mContext, BatteryTestUtils.createScreenTimeoutAnomalyEvent(true)));
        when(anomalyEventWrapper.getIconDrawable()).thenReturn(mIconDrawable);
        when(mFeatureFactory.powerUsageFeatureProvider.isBatteryTipsEnabled()).thenReturn(true);

        mBatteryTipsController.handleBatteryTipsCardUpdated(anomalyEventWrapper, false);

        assertCardPreference(
                "Reduce screen timeout to extend battery life",
                "View Settings",
                "Got it",
                BannerMessagePreference.AttentionLevel.NORMAL);
        assertCardButtonActionAndMetrics(anomalyEventWrapper);
    }

    @Test
    public void handleBatteryTipsCardUpdated_screenTimeoutAnomalyHasTitle_showAnomaly() {
        PowerAnomalyEvent anomalyEvent = BatteryTestUtils.createScreenTimeoutAnomalyEvent(true);
        String testTitle = "TestTitle";
        anomalyEvent =
                anomalyEvent.toBuilder()
                        .setWarningBannerInfo(
                                anomalyEvent.getWarningBannerInfo().toBuilder()
                                        .setTitleString(testTitle)
                                        .build())
                        .build();
        AnomalyEventWrapper anomalyEventWrapper =
                spy(new AnomalyEventWrapper(mContext, anomalyEvent));
        when(anomalyEventWrapper.getIconDrawable()).thenReturn(mIconDrawable);
        when(mFeatureFactory.powerUsageFeatureProvider.isBatteryTipsEnabled()).thenReturn(true);

        mBatteryTipsController.handleBatteryTipsCardUpdated(anomalyEventWrapper, false);

        assertCardPreference(
                testTitle,
                "View Settings",
                "Got it",
                BannerMessagePreference.AttentionLevel.NORMAL);
        assertCardButtonActionAndMetrics(anomalyEventWrapper);
    }

    @Test
    public void handleBatteryTipsCardUpdated_appAnomaly_showAnomaly() {
        AnomalyEventWrapper anomalyEventWrapper =
                spy(new AnomalyEventWrapper(mContext, BatteryTestUtils.createAppAnomalyEvent()));
        when(anomalyEventWrapper.getIconDrawable()).thenReturn(mIconDrawable);
        when(mFeatureFactory.powerUsageFeatureProvider.isBatteryTipsEnabled()).thenReturn(true);

        anomalyEventWrapper.setRelatedBatteryDiffEntry(
                new BatteryDiffEntry(mContext, "", "Chrome", 0));
        mBatteryTipsController.setOnAnomalyConfirmListener(
                () -> mBatteryTipsController.acceptTipsCard());
        mBatteryTipsController.handleBatteryTipsCardUpdated(anomalyEventWrapper, true);

        assertCardPreference(
                "Chrome used more battery than usual",
                "Check",
                "Got it",
                BannerMessagePreference.AttentionLevel.MEDIUM);
        assertCardButtonActionAndMetrics(anomalyEventWrapper);
    }

    private void assertCardPreference(
            final String title,
            final String positiveBtnText,
            final String negativeBtnText,
            BannerMessagePreference.AttentionLevel attentionLevel) {
        verify(mCardPreference).setTitle(eq(title));
        verify(mCardPreference).setPositiveButtonText(eq(positiveBtnText));
        verify(mCardPreference).setNegativeButtonText(eq(negativeBtnText));
        verify(mCardPreference).setIcon(mIconDrawable);
        verify(mCardPreference).setAttentionLevel(attentionLevel);
        verify(mCardPreference).setPositiveButtonVisible(true);
        verify(mCardPreference).setNegativeButtonVisible(true);
        assertThat(mCardPreference.isVisible()).isTrue();
    }

    private void assertCardButtonActionAndMetrics(final AnomalyEventWrapper anomalyEventWrapper) {
        when(anomalyEventWrapper.updateSystemSettingsIfAvailable()).thenReturn(true);

        final int powerAnomalyKeyNumber = anomalyEventWrapper.getAnomalyKeyNumber();
        assertCardMetrics(SettingsEnums.ACTION_BATTERY_TIPS_CARD_SHOW, powerAnomalyKeyNumber);
        assertThat(mCardPreference.isVisible()).isTrue();
        final ShadowBannerMessagePreference shadowPreference = Shadow.extract(mCardPreference);

        // Check accept button action
        mCardPreference.setVisible(true);
        clearInvocations(mFeatureFactory.metricsFeatureProvider);
        shadowPreference.getPositiveButtonOnClickListener().onClick(mView);
        assertCardMetrics(SettingsEnums.ACTION_BATTERY_TIPS_CARD_ACCEPT, powerAnomalyKeyNumber);
        assertThat(mCardPreference.isVisible()).isFalse();
        final boolean isAppAnomalyCard = powerAnomalyKeyNumber > 1;
        verify(anomalyEventWrapper, isAppAnomalyCard ? never() : times(1))
                .updateSystemSettingsIfAvailable();

        // Check reject button action
        mCardPreference.setVisible(true);
        clearInvocations(mFeatureFactory.metricsFeatureProvider);
        shadowPreference.getNegativeButtonOnClickListener().onClick(mView);
        assertCardMetrics(SettingsEnums.ACTION_BATTERY_TIPS_CARD_DISMISS, powerAnomalyKeyNumber);
        assertThat(mCardPreference.isVisible()).isFalse();
    }

    private void assertCardMetrics(final int action, final int powerAnomalyKeyNumber) {
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(
                        SettingsEnums.FUELGAUGE_BATTERY_HISTORY_DETAIL,
                        action,
                        SettingsEnums.FUELGAUGE_BATTERY_HISTORY_DETAIL,
                        BatteryTipsController.ANOMALY_KEY,
                        powerAnomalyKeyNumber);
    }

    @Implements(BannerMessagePreference.class)
    public static class ShadowBannerMessagePreference {

        @RealObject protected BannerMessagePreference mRealBannerMessagePreference;

        private View.OnClickListener mPositiveButtonOnClickListener;
        private View.OnClickListener mNegativeButtonOnClickListener;

        /** Shadow implementation of setPositiveButtonOnClickListener */
        @Implementation
        public BannerMessagePreference setPositiveButtonOnClickListener(
                View.OnClickListener listener) {
            mPositiveButtonOnClickListener = listener;
            return mRealBannerMessagePreference;
        }

        /** Shadow implementation of setNegativeButtonOnClickListener */
        @Implementation
        public BannerMessagePreference setNegativeButtonOnClickListener(
                View.OnClickListener listener) {
            mNegativeButtonOnClickListener = listener;
            return mRealBannerMessagePreference;
        }

        View.OnClickListener getPositiveButtonOnClickListener() {
            return mPositiveButtonOnClickListener;
        }

        View.OnClickListener getNegativeButtonOnClickListener() {
            return mNegativeButtonOnClickListener;
        }
    }
}
