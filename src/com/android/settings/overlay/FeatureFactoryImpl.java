/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.overlay;

import android.app.AppGlobals;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.VpnManager;
import android.os.UserManager;

import androidx.annotation.Keep;

import com.android.settings.accessibility.AccessibilityMetricsFeatureProvider;
import com.android.settings.accessibility.AccessibilityMetricsFeatureProviderImpl;
import com.android.settings.accessibility.AccessibilitySearchFeatureProvider;
import com.android.settings.accessibility.AccessibilitySearchFeatureProviderImpl;
import com.android.settings.accounts.AccountFeatureProvider;
import com.android.settings.accounts.AccountFeatureProviderImpl;
import com.android.settings.applications.ApplicationFeatureProvider;
import com.android.settings.applications.ApplicationFeatureProviderImpl;
import com.android.settings.aware.AwareFeatureProvider;
import com.android.settings.aware.AwareFeatureProviderImpl;
import com.android.settings.biometrics.face.FaceFeatureProvider;
import com.android.settings.biometrics.face.FaceFeatureProviderImpl;
import com.android.settings.bluetooth.BluetoothFeatureProvider;
import com.android.settings.bluetooth.BluetoothFeatureProviderImpl;
import com.android.settings.connecteddevice.dock.DockUpdaterFeatureProviderImpl;
import com.android.settings.core.instrumentation.SettingsMetricsFeatureProvider;
import com.android.settings.dashboard.DashboardFeatureProvider;
import com.android.settings.dashboard.DashboardFeatureProviderImpl;
import com.android.settings.dashboard.suggestions.SuggestionFeatureProvider;
import com.android.settings.dashboard.suggestions.SuggestionFeatureProviderImpl;
import com.android.settings.enterprise.EnterprisePrivacyFeatureProvider;
import com.android.settings.enterprise.EnterprisePrivacyFeatureProviderImpl;
import com.android.settings.fuelgauge.BatterySettingsFeatureProvider;
import com.android.settings.fuelgauge.BatterySettingsFeatureProviderImpl;
import com.android.settings.fuelgauge.BatteryStatusFeatureProvider;
import com.android.settings.fuelgauge.BatteryStatusFeatureProviderImpl;
import com.android.settings.fuelgauge.PowerUsageFeatureProvider;
import com.android.settings.fuelgauge.PowerUsageFeatureProviderImpl;
import com.android.settings.gestures.AssistGestureFeatureProvider;
import com.android.settings.gestures.AssistGestureFeatureProviderImpl;
import com.android.settings.homepage.contextualcards.ContextualCardFeatureProvider;
import com.android.settings.homepage.contextualcards.ContextualCardFeatureProviderImpl;
import com.android.settings.localepicker.LocaleFeatureProvider;
import com.android.settings.localepicker.LocaleFeatureProviderImpl;
import com.android.settings.panel.PanelFeatureProvider;
import com.android.settings.panel.PanelFeatureProviderImpl;
import com.android.settings.search.SearchFeatureProvider;
import com.android.settings.search.SearchFeatureProviderImpl;
import com.android.settings.security.SecurityFeatureProvider;
import com.android.settings.security.SecurityFeatureProviderImpl;
import com.android.settings.security.SecuritySettingsFeatureProvider;
import com.android.settings.security.SecuritySettingsFeatureProviderImpl;
import com.android.settings.slices.SlicesFeatureProvider;
import com.android.settings.slices.SlicesFeatureProviderImpl;
import com.android.settings.users.UserFeatureProvider;
import com.android.settings.users.UserFeatureProviderImpl;
import com.android.settings.vpn2.AdvancedVpnFeatureProvider;
import com.android.settings.vpn2.AdvancedVpnFeatureProviderImpl;
import com.android.settings.wifi.WifiTrackerLibProvider;
import com.android.settings.wifi.WifiTrackerLibProviderImpl;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

/**
 * {@link FeatureFactory} implementation for AOSP Settings.
 */
@Keep
public class FeatureFactoryImpl extends FeatureFactory {

    private ApplicationFeatureProvider mApplicationFeatureProvider;
    private MetricsFeatureProvider mMetricsFeatureProvider;
    private DashboardFeatureProviderImpl mDashboardFeatureProvider;
    private DockUpdaterFeatureProvider mDockUpdaterFeatureProvider;
    private LocaleFeatureProvider mLocaleFeatureProvider;
    private EnterprisePrivacyFeatureProvider mEnterprisePrivacyFeatureProvider;
    private SearchFeatureProvider mSearchFeatureProvider;
    private SecurityFeatureProvider mSecurityFeatureProvider;
    private SuggestionFeatureProvider mSuggestionFeatureProvider;
    private PowerUsageFeatureProvider mPowerUsageFeatureProvider;
    private BatteryStatusFeatureProvider mBatteryStatusFeatureProvider;
    private BatterySettingsFeatureProvider mBatterySettingsFeatureProvider;
    private AssistGestureFeatureProvider mAssistGestureFeatureProvider;
    private UserFeatureProvider mUserFeatureProvider;
    private SlicesFeatureProvider mSlicesFeatureProvider;
    private AccountFeatureProvider mAccountFeatureProvider;
    private PanelFeatureProvider mPanelFeatureProvider;
    private ContextualCardFeatureProvider mContextualCardFeatureProvider;
    private BluetoothFeatureProvider mBluetoothFeatureProvider;
    private AwareFeatureProvider mAwareFeatureProvider;
    private FaceFeatureProvider mFaceFeatureProvider;
    private WifiTrackerLibProvider mWifiTrackerLibProvider;
    private SecuritySettingsFeatureProvider mSecuritySettingsFeatureProvider;
    private AccessibilitySearchFeatureProvider mAccessibilitySearchFeatureProvider;
    private AccessibilityMetricsFeatureProvider mAccessibilityMetricsFeatureProvider;
    private AdvancedVpnFeatureProvider mAdvancedVpnFeatureProvider;

    @Override
    public SupportFeatureProvider getSupportFeatureProvider(Context context) {
        return null;
    }

    @Override
    public MetricsFeatureProvider getMetricsFeatureProvider() {
        if (mMetricsFeatureProvider == null) {
            mMetricsFeatureProvider = new SettingsMetricsFeatureProvider();
        }
        return mMetricsFeatureProvider;
    }

    @Override
    public PowerUsageFeatureProvider getPowerUsageFeatureProvider(Context context) {
        if (mPowerUsageFeatureProvider == null) {
            mPowerUsageFeatureProvider = new PowerUsageFeatureProviderImpl(
                    context.getApplicationContext());
        }
        return mPowerUsageFeatureProvider;
    }

    @Override
    public BatteryStatusFeatureProvider getBatteryStatusFeatureProvider(Context context) {
        if (mBatteryStatusFeatureProvider == null) {
            mBatteryStatusFeatureProvider = new BatteryStatusFeatureProviderImpl(
                    context.getApplicationContext());
        }
        return mBatteryStatusFeatureProvider;
    }

    @Override
    public BatterySettingsFeatureProvider getBatterySettingsFeatureProvider(Context context) {
        if (mBatterySettingsFeatureProvider == null) {
            mBatterySettingsFeatureProvider = new BatterySettingsFeatureProviderImpl(context);
        }
        return mBatterySettingsFeatureProvider;
    }

    @Override
    public DashboardFeatureProvider getDashboardFeatureProvider(Context context) {
        if (mDashboardFeatureProvider == null) {
            mDashboardFeatureProvider = new DashboardFeatureProviderImpl(
                    context.getApplicationContext());
        }
        return mDashboardFeatureProvider;
    }

    @Override
    public DockUpdaterFeatureProvider getDockUpdaterFeatureProvider() {
        if (mDockUpdaterFeatureProvider == null) {
            mDockUpdaterFeatureProvider = new DockUpdaterFeatureProviderImpl();
        }
        return mDockUpdaterFeatureProvider;
    }

    @Override
    public ApplicationFeatureProvider getApplicationFeatureProvider(Context context) {
        if (mApplicationFeatureProvider == null) {
            final Context appContext = context.getApplicationContext();
            mApplicationFeatureProvider = new ApplicationFeatureProviderImpl(appContext,
                    appContext.getPackageManager(),
                    AppGlobals.getPackageManager(),
                    (DevicePolicyManager) appContext
                            .getSystemService(Context.DEVICE_POLICY_SERVICE));
        }
        return mApplicationFeatureProvider;
    }

    @Override
    public LocaleFeatureProvider getLocaleFeatureProvider() {
        if (mLocaleFeatureProvider == null) {
            mLocaleFeatureProvider = new LocaleFeatureProviderImpl();
        }
        return mLocaleFeatureProvider;
    }

    @Override
    public EnterprisePrivacyFeatureProvider getEnterprisePrivacyFeatureProvider(Context context) {
        if (mEnterprisePrivacyFeatureProvider == null) {
            final Context appContext = context.getApplicationContext();
            mEnterprisePrivacyFeatureProvider = new EnterprisePrivacyFeatureProviderImpl(appContext,
                    (DevicePolicyManager) appContext.getSystemService(
                            Context.DEVICE_POLICY_SERVICE),
                    appContext.getPackageManager(),
                    UserManager.get(appContext),
                    appContext.getSystemService(ConnectivityManager.class),
                    appContext.getSystemService(VpnManager.class),
                    appContext.getResources());
        }
        return mEnterprisePrivacyFeatureProvider;
    }

    @Override
    public SearchFeatureProvider getSearchFeatureProvider() {
        if (mSearchFeatureProvider == null) {
            mSearchFeatureProvider = new SearchFeatureProviderImpl();
        }
        return mSearchFeatureProvider;
    }

    @Override
    public SurveyFeatureProvider getSurveyFeatureProvider(Context context) {
        return null;
    }

    @Override
    public SecurityFeatureProvider getSecurityFeatureProvider() {
        if (mSecurityFeatureProvider == null) {
            mSecurityFeatureProvider = new SecurityFeatureProviderImpl();
        }
        return mSecurityFeatureProvider;
    }

    @Override
    public SuggestionFeatureProvider getSuggestionFeatureProvider(Context context) {
        if (mSuggestionFeatureProvider == null) {
            mSuggestionFeatureProvider = new SuggestionFeatureProviderImpl(
                    context.getApplicationContext());
        }
        return mSuggestionFeatureProvider;
    }

    @Override
    public UserFeatureProvider getUserFeatureProvider(Context context) {
        if (mUserFeatureProvider == null) {
            mUserFeatureProvider = new UserFeatureProviderImpl(context.getApplicationContext());
        }
        return mUserFeatureProvider;
    }

    @Override
    public AssistGestureFeatureProvider getAssistGestureFeatureProvider() {
        if (mAssistGestureFeatureProvider == null) {
            mAssistGestureFeatureProvider = new AssistGestureFeatureProviderImpl();
        }
        return mAssistGestureFeatureProvider;
    }

    @Override
    public SlicesFeatureProvider getSlicesFeatureProvider() {
        if (mSlicesFeatureProvider == null) {
            mSlicesFeatureProvider = new SlicesFeatureProviderImpl();
        }
        return mSlicesFeatureProvider;
    }

    @Override
    public AccountFeatureProvider getAccountFeatureProvider() {
        if (mAccountFeatureProvider == null) {
            mAccountFeatureProvider = new AccountFeatureProviderImpl();
        }
        return mAccountFeatureProvider;
    }

    @Override
    public PanelFeatureProvider getPanelFeatureProvider() {
        if (mPanelFeatureProvider == null) {
            mPanelFeatureProvider = new PanelFeatureProviderImpl();
        }
        return mPanelFeatureProvider;
    }

    @Override
    public ContextualCardFeatureProvider getContextualCardFeatureProvider(Context context) {
        if (mContextualCardFeatureProvider == null) {
            mContextualCardFeatureProvider = new ContextualCardFeatureProviderImpl(
                    context.getApplicationContext());
        }
        return mContextualCardFeatureProvider;
    }

    @Override
    public BluetoothFeatureProvider getBluetoothFeatureProvider() {
        if (mBluetoothFeatureProvider == null) {
            mBluetoothFeatureProvider = new BluetoothFeatureProviderImpl(getAppContext());
        }
        return mBluetoothFeatureProvider;
    }

    @Override
    public AwareFeatureProvider getAwareFeatureProvider() {
        if (mAwareFeatureProvider == null) {
            mAwareFeatureProvider = new AwareFeatureProviderImpl();
        }
        return mAwareFeatureProvider;
    }

    @Override
    public FaceFeatureProvider getFaceFeatureProvider() {
        if (mFaceFeatureProvider == null) {
            mFaceFeatureProvider = new FaceFeatureProviderImpl();
        }
        return mFaceFeatureProvider;
    }

    @Override
    public WifiTrackerLibProvider getWifiTrackerLibProvider() {
        if (mWifiTrackerLibProvider == null) {
            mWifiTrackerLibProvider = new WifiTrackerLibProviderImpl();
        }
        return mWifiTrackerLibProvider;
    }

    @Override
    public SecuritySettingsFeatureProvider getSecuritySettingsFeatureProvider() {
        if (mSecuritySettingsFeatureProvider == null) {
            mSecuritySettingsFeatureProvider = new SecuritySettingsFeatureProviderImpl();
        }
        return mSecuritySettingsFeatureProvider;
    }

    @Override
    public AccessibilitySearchFeatureProvider getAccessibilitySearchFeatureProvider() {
        if (mAccessibilitySearchFeatureProvider == null) {
            mAccessibilitySearchFeatureProvider = new AccessibilitySearchFeatureProviderImpl();
        }
        return mAccessibilitySearchFeatureProvider;
    }

    @Override
    public AccessibilityMetricsFeatureProvider getAccessibilityMetricsFeatureProvider() {
        if (mAccessibilityMetricsFeatureProvider == null) {
            mAccessibilityMetricsFeatureProvider = new AccessibilityMetricsFeatureProviderImpl();
        }
        return mAccessibilityMetricsFeatureProvider;
    }

    @Override
    public AdvancedVpnFeatureProvider getAdvancedVpnFeatureProvider() {
        if (mAdvancedVpnFeatureProvider == null) {
            mAdvancedVpnFeatureProvider = new AdvancedVpnFeatureProviderImpl();
        }
        return mAdvancedVpnFeatureProvider;
    }
}
