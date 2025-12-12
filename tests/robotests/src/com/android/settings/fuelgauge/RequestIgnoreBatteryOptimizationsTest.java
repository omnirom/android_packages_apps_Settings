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

package com.android.settings.fuelgauge;

import static com.android.settings.fuelgauge.BatteryOptimizeUtils.MODE_RESTRICTED;
import static com.android.settings.fuelgauge.BatteryOptimizeUtils.MODE_UNRESTRICTED;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import android.Manifest;
import android.app.AppOpsManager;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;

import com.android.settings.fuelgauge.RequestIgnoreBatteryOptimizations.RequestStatus;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowUtils;
import com.android.settingslib.fuelgauge.PowerAllowlistBackend;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowUtils.class})
public class RequestIgnoreBatteryOptimizationsTest {
    private static final int UID = 12345;
    private static final String PACKAGE_NAME = "com.android.app";
    private static final String UNKNOWN_PACKAGE_NAME = "com.android.unknown";
    private static final String METRICS_KEY = "RequestIgnoreBatteryOptimizations";
    private static final String PACKAGE_LABEL = "app";
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    private Context mContext;
    private FakeFeatureFactory mFeatureFactory;
    private RequestIgnoreBatteryOptimizations mActivity;
    private BatteryOptimizeUtils mBatteryOptimizeUtils;
    private PowerAllowlistBackend mPowerAllowlistBackend;
    private ActivityController<RequestIgnoreBatteryOptimizations> mActivityController;

    @Mock private PowerManager mMockPowerManager;
    @Mock private PackageManager mMockPackageManager;
    @Mock private ApplicationInfo mMockApplicationInfo;
    @Mock private BatteryUtils mMockBatteryUtils;
    @Mock private DialogInterface mMockDialog;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mActivityController = Robolectric.buildActivity(RequestIgnoreBatteryOptimizations.class);
        mActivity = spy(mActivityController.get());
        mBatteryOptimizeUtils = spy(new BatteryOptimizeUtils(mContext, UID, PACKAGE_NAME));
        mPowerAllowlistBackend = spy(PowerAllowlistBackend.getInstance(mContext));
        mBatteryOptimizeUtils.mPowerAllowListBackend = mPowerAllowlistBackend;
        mBatteryOptimizeUtils.mBatteryUtils = mMockBatteryUtils;
        mActivity.mApplicationInfo = mMockApplicationInfo;
        mActivity.mMetricsFeatureProvider = mFeatureFactory.metricsFeatureProvider;
        mMockApplicationInfo.uid = UID;
        doNothing().when(mMockDialog).dismiss();
        RequestIgnoreBatteryOptimizations.sTestBatteryOptimizeUtils = mBatteryOptimizeUtils;

        when(mActivity.getApplicationContext()).thenReturn(mContext);
        doReturn(mMockPowerManager).when(mActivity).getSystemService(PowerManager.class);
        doReturn(mMockPackageManager).when(mActivity).getPackageManager();
        doReturn(new PackageInfo()).when(mMockPackageManager).getPackageInfo(anyString(), anyInt());
        doReturn(mMockApplicationInfo)
                .when(mMockPackageManager)
                .getApplicationInfo(PACKAGE_NAME, 0);
        doThrow(new PackageManager.NameNotFoundException(""))
                .when(mMockPackageManager)
                .getApplicationInfo(UNKNOWN_PACKAGE_NAME, 0);
        doReturn(PACKAGE_LABEL)
                .when(mMockApplicationInfo)
                .loadSafeLabel(
                        mMockPackageManager,
                        PackageItemInfo.DEFAULT_MAX_LABEL_SIZE_PX,
                        PackageItemInfo.SAFE_LABEL_FLAG_TRIM
                                | PackageItemInfo.SAFE_LABEL_FLAG_FIRST_LINE);

        doReturn(PackageManager.PERMISSION_GRANTED)
                .when(mMockPackageManager)
                .checkPermission(
                        eq(Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS), anyString());
    }

    @After
    public void tearDown() {
        ShadowUtils.reset();
        PowerAllowlistBackend.resetInstance();
    }

    @Test
    public void onCreate_withIntent_shouldNotFinish() {
        mActivity.setIntent(createIntent(PACKAGE_NAME));

        mActivity.onCreate(new Bundle());

        verify(mActivity, never()).finish();
        assertDialogMetrics(SettingsEnums.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZE_SHOW, UID);
    }

    @Test
    public void onCreate_withNoDataIntent_shouldFinish() {
        mActivity.setIntent(new Intent());

        mActivity.onCreate(new Bundle());

        verify(mActivity).finish();
        assertDialogMetrics(
                SettingsEnums.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZE_FAIL_SHOW,
                RequestStatus.PACKAGE_NOT_EXIST.value);
    }

    @Test
    public void onCreate_withEmptyPackageName_shouldFinish() {
        mActivity.setIntent(createIntent(""));

        mActivity.onCreate(new Bundle());

        verify(mActivity).finish();
        assertDialogMetrics(
                SettingsEnums.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZE_FAIL_SHOW,
                RequestStatus.PACKAGE_NOT_EXIST.value);
    }

    @Test
    public void onCreate_withPkgAlreadyIgnoreOptimization_shouldFinish() {
        mActivity.setIntent(createIntent(PACKAGE_NAME));
        doReturn(true).when(mMockPowerManager).isIgnoringBatteryOptimizations(PACKAGE_NAME);

        mActivity.onCreate(new Bundle());

        verify(mActivity).finish();
        assertDialogMetrics(
                SettingsEnums.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZE_FAIL_SHOW,
                RequestStatus.ALREADY_PROMPTED.value);
    }

    @Test
    public void onCreate_withPkgWithoutPermission_shouldFinish() {
        mActivity.setIntent(createIntent(PACKAGE_NAME));
        doReturn(PackageManager.PERMISSION_DENIED)
                .when(mMockPackageManager)
                .checkPermission(
                        Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, PACKAGE_NAME);

        mActivity.onCreate(new Bundle());

        verify(mActivity).finish();
        assertDialogMetrics(
                SettingsEnums.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZE_FAIL_SHOW,
                RequestStatus.NO_PERMISSION.value);
    }

    @Test
    public void onCreate_withPkgNameNotFound_shouldFinish() {
        mActivity.setIntent(createIntent(UNKNOWN_PACKAGE_NAME));

        mActivity.onCreate(new Bundle());

        verify(mActivity).finish();
        assertDialogMetrics(
                SettingsEnums.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZE_FAIL_SHOW,
                RequestStatus.PACKAGE_NOT_EXIST.value);
    }

    @Test
    public void onClick_clickNegativeButton_doNothing() {
        mActivity.onClick(mMockDialog, DialogInterface.BUTTON_NEGATIVE);

        verifyNoInteractions(mBatteryOptimizeUtils);
        assertDialogMetrics(
                SettingsEnums.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZE_DENY, UID);
        verify(mMockDialog).dismiss();
    }

    @Test
    public void onClick_clickPositiveButtonWithUnrestrictedMode_addAllowlist() {
        when(mBatteryOptimizeUtils.getAppOptimizationMode()).thenReturn(MODE_UNRESTRICTED);

        mActivity.onClick(mMockDialog, DialogInterface.BUTTON_POSITIVE);

        verify(mBatteryOptimizeUtils)
                .setAppUsageState(
                        MODE_UNRESTRICTED,
                        BatteryOptimizeHistoricalLogEntry.Action.APPLY,
                        /* forceMode= */ true);
        verify(mPowerAllowlistBackend).addApp(PACKAGE_NAME, UID);
        verify(mMockBatteryUtils).setForceAppStandby(UID, PACKAGE_NAME, AppOpsManager.MODE_ALLOWED);
        assertDialogMetrics(
                SettingsEnums.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZE_ALLOW, UID);
        verify(mMockDialog).dismiss();
    }

    @Test
    public void onClick_clickPositiveButtonWithRestrictedMode_addAllowlistAndSetStandby() {
        when(mBatteryOptimizeUtils.getAppOptimizationMode()).thenReturn(MODE_RESTRICTED);
        doNothing().when(mMockBatteryUtils).setForceAppStandby(anyInt(), anyString(), anyInt());

        mActivity.onClick(mMockDialog, DialogInterface.BUTTON_POSITIVE);

        verify(mBatteryOptimizeUtils)
                .setAppUsageState(
                        MODE_UNRESTRICTED,
                        BatteryOptimizeHistoricalLogEntry.Action.APPLY,
                        /* forceMode= */ true);
        verify(mPowerAllowlistBackend).addApp(PACKAGE_NAME, UID);
        verify(mMockBatteryUtils).setForceAppStandby(UID, PACKAGE_NAME, AppOpsManager.MODE_ALLOWED);
        assertDialogMetrics(
                SettingsEnums.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZE_ALLOW, UID);
        verify(mMockDialog).dismiss();
    }

    private Intent createIntent(String packageName) {
        final Intent intent = new Intent();
        intent.setData(new Uri.Builder().scheme("package").opaquePart(packageName).build());
        return intent;
    }

    private void assertDialogMetrics(final int action, final int value) {
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(
                        SettingsEnums.DIALOG_REQUEST_IGNORE_BATTERY_OPTIMIZE,
                        action,
                        SettingsEnums.DIALOG_REQUEST_IGNORE_BATTERY_OPTIMIZE,
                        METRICS_KEY,
                        value);
    }
}
