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
package com.android.settings.network.telephony;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;
import android.os.Looper;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.R;
import com.android.settingslib.RestrictedSwitchPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public final class Enable2gPreferenceControllerTest {
    private static final int SUB_ID = 2;
    private static final String PREFERENCE_KEY = "TEST_2G_PREFERENCE";
    private static final String ENABLE_2G_SUMMARY = "Avoids 2G networks, which are less secure."
            + " This may limit connectivity in some places. 2G is always allowed for emergency"
            + " calls, but calls may not connect if you're roaming internationally.";
    @Mock
    private TelephonyManager mTelephonyManager;
    @Mock
    private TelephonyManager mInvalidTelephonyManager;
    @Mock
    private SubscriptionManager mSubscriptionManager;

    private RestrictedSwitchPreference mPreference;
    private PreferenceScreen mPreferenceScreen;
    private Enable2gPreferenceController mController;
    private Context mContext;

    @Before
    public void setUp() {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }

        MockitoAnnotations.initMocks(this);

        mContext = spy(ApplicationProvider.getApplicationContext());
        when(mContext.getSystemService(Context.TELEPHONY_SERVICE)).thenReturn(mTelephonyManager);
        when(mContext.getSystemService(TelephonyManager.class)).thenReturn(mTelephonyManager);
        Resources resources = spy(mContext.getResources());
        when(mContext.getResources()).thenReturn(resources);
        when(resources.getString(R.string.enable_2g_summary)).thenReturn(ENABLE_2G_SUMMARY);
        when(mContext.getString(R.string.enable_2g_summary)).thenReturn(ENABLE_2G_SUMMARY);

        when(mContext.getSystemService(SubscriptionManager.class)).thenReturn(mSubscriptionManager);

        doReturn(mTelephonyManager).when(mTelephonyManager).createForSubscriptionId(SUB_ID);
        doReturn(mInvalidTelephonyManager).when(mTelephonyManager).createForSubscriptionId(
                SubscriptionManager.INVALID_SUBSCRIPTION_ID);

        mController = new Enable2gPreferenceController(mContext, PREFERENCE_KEY);

        mPreference = spy(new RestrictedSwitchPreference(mContext));
        mPreference.setKey(PREFERENCE_KEY);
        mPreferenceScreen = new PreferenceManager(mContext).createPreferenceScreen(mContext);
        mPreferenceScreen.addPreference(mPreference);
        mController.init(SUB_ID);
    }

    @Test
    public void getAvailabilityStatus_invalidSubId_returnUnavailable() {
        mController.init(SubscriptionManager.INVALID_SUBSCRIPTION_ID);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_capabilityNotSupported_returnUnavailable() {
        doReturn(false).when(mTelephonyManager).isRadioInterfaceCapabilitySupported(
                mTelephonyManager.CAPABILITY_USES_ALLOWED_NETWORK_TYPES_BITMASK);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_returnAvailable() {
        doReturn(true).when(mTelephonyManager).isRadioInterfaceCapabilitySupported(
                mTelephonyManager.CAPABILITY_USES_ALLOWED_NETWORK_TYPES_BITMASK);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void setChecked_invalidSubIdAndIsCheckedTrue_returnFalse() {
        mController.init(SubscriptionManager.INVALID_SUBSCRIPTION_ID);
        assertThat(mController.setChecked(true)).isFalse();
    }

    @Test
    public void setChecked_invalidSubIdAndIsCheckedFalse_returnFalse() {
        mController.init(SubscriptionManager.INVALID_SUBSCRIPTION_ID);
        assertThat(mController.setChecked(false)).isFalse();
    }

    @Test
    public void setChecked_disabledByAdmin_returnFalse() {
        when2gIsDisabledByAdmin(true);

        assertThat(mController.setChecked(false)).isFalse();
        verify(mPreference).useAdminDisabledSummary(true);
        verify(mPreference).getRestrictedPreferenceHelper();
    }

    @Test
    public void setChecked_disable2G() {
        when2gIsEnabledForReasonEnable2g();

        // Disable 2G
        boolean changed = mController.setChecked(true);
        assertThat(changed).isEqualTo(true);

        verify(mTelephonyManager, times(1)).setAllowedNetworkTypesForReason(
                TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_ENABLE_2G,
                TelephonyManager.NETWORK_TYPE_BITMASK_LTE);
    }

    @Test
    public void disabledByAdmin_toggleUnchecked() {
        when2gIsEnabledForReasonEnable2g();
        when2gIsDisabledByAdmin(true);
        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void userRestrictionInactivated_userToggleMaintainsState() {
        // Initially, 2g is enabled
        when2gIsEnabledForReasonEnable2g();
        when2gIsDisabledByAdmin(false);
        assertThat(mController.isChecked()).isFalse();

        // When we disable the preference by an admin, the preference should be unchecked
        when2gIsDisabledByAdmin(true);
        assertThat(mController.isChecked()).isTrue();

        // If the preference is re-enabled by an admin, former state should hold
        when2gIsDisabledByAdmin(false);
        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void updateState_isDisabledByAdmin() {
        when2gIsDisabledByAdmin(true);

        mController.updateState((Preference) mPreference);

        assertThat(mPreference.getSummary()).isNull();
    }

    @Test
    public void updateState_preferenceIsNull() {
        when2gIsDisabledByAdmin(false);

        mController.updateState(null);

        assertThat(mPreference.getSummary()).isNull();
    }

    @Test
    public void updateState_notUsableSubscriptionId() {
        mController.init(-1);
        when2gIsDisabledByAdmin(false);

        mController.updateState((Preference) mPreference);

        assertThat(mPreference.getSummary()).isNull();
    }

    @Test
    public void updateState_withEnable2gSummary() {
        when2gIsDisabledByAdmin(false);

        mController.updateState((Preference) mPreference);

        assertThat(mPreference.getSummary().toString()).isEqualTo(
                mContext.getString(R.string.enable_2g_summary));
    }

    @Test
    public void updateState_simNameAsSummary() {
        when2gIsDisabledByAdmin(false);
        String simName = "SIM1";
        SubscriptionInfo subscriptionInfo = new SubscriptionInfo.Builder()
                .setId(SUB_ID)
                .setDisplayName(simName)
                .build();
        List<SubscriptionInfo> subInfos = new ArrayList();
        subInfos.add(subscriptionInfo);
        when(mSubscriptionManager.getAllSubscriptionInfoList()).thenReturn(subInfos);
        mController.init(SUB_ID, true);

        mController.updateState((Preference) mPreference);

        assertThat(mPreference.getSummary().toString()).isEqualTo(simName);
    }

    @Test
    public void updateState_simNameAsSummary_2gPreferenceDisableByAdmin() {
        when2gIsDisabledByAdmin(true);
        String simName = "SIM1";
        SubscriptionInfo subscriptionInfo = new SubscriptionInfo.Builder()
                .setId(SUB_ID)
                .setDisplayName(simName)
                .build();
        List<SubscriptionInfo> subInfos = new ArrayList();
        subInfos.add(subscriptionInfo);
        when(mSubscriptionManager.getAllSubscriptionInfoList()).thenReturn(subInfos);
        mController.init(SUB_ID, true);

        mController.updateState((Preference) mPreference);

        assertThat(mPreference.getSummary().toString()).isEqualTo(simName);
    }

    @Test
    public void updateState_preferenceEnable() {
        when2gIsDisabledByAdmin(false);
        mockAllowedNetworkTypes(TelephonyManager.NETWORK_MODE_LTE_GSM_WCDMA);

        mController.updateState((Preference) mPreference);

        assertTrue(mPreference.isEnabled());
    }

    @Test
    public void updateState_isAirplaneModeOn_preferenceDisable() {
        when2gIsDisabledByAdmin(false);
        mockAllowedNetworkTypes(TelephonyManager.NETWORK_MODE_LTE_GSM_WCDMA);
        mController.notifyAirplaneModeChanged(true);

        mController.updateState((Preference) mPreference);

        assertFalse(mPreference.isEnabled());
    }

    @Test
    public void updateState_preferenceDisable() {
        when2gIsDisabledByAdmin(false);
        mockAllowedNetworkTypes(TelephonyManager.NETWORK_MODE_GSM_ONLY);

        mController.updateState((Preference) mPreference);

        assertFalse(mPreference.isEnabled());
    }

    @Test
    public void updateState_isDisabledByAdmin_networkTypeLte_preferenceDisable() {
        when2gIsDisabledByAdmin(true);
        mPreference.setEnabled(false);
        mockAllowedNetworkTypes(TelephonyManager.NETWORK_MODE_LTE_GSM_WCDMA);

        mController.updateState((Preference) mPreference);

        assertFalse(mPreference.isEnabled());
    }

    private void mockAllowedNetworkTypes(long allowedNetworkType) {
        doReturn(allowedNetworkType).when(mTelephonyManager).getAllowedNetworkTypesForReason(
                TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_USER);
    }

    private void when2gIsEnabledForReasonEnable2g() {
        when(mTelephonyManager.getAllowedNetworkTypesForReason(
                TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_ENABLE_2G)).thenReturn(
                (long) (TelephonyManager.NETWORK_TYPE_BITMASK_GSM
                        | TelephonyManager.NETWORK_TYPE_BITMASK_LTE));
    }

    private void when2gIsDisabledByAdmin(boolean is2gDisabledByAdmin) {
        // Our controller depends on state being initialized when the associated preference is
        // displayed because the admin disablement functionality flows from the association of a
        // Preference with the PreferenceScreen
        when(mPreference.isDisabledByAdmin()).thenReturn(is2gDisabledByAdmin);
        mController.displayPreference(mPreferenceScreen);
    }
}
