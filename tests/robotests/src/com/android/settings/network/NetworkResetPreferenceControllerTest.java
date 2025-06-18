/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.network;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;
import android.telephony.TelephonyManager;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
public class NetworkResetPreferenceControllerTest {

    @Mock
    private TelephonyManager mTelephonyManager;
    @Mock
    private NetworkResetRestrictionChecker mRestrictionChecker;
    private NetworkResetPreferenceController mController;
    private Context mContext;
    private Resources mResources;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);

        mResources = spy(mContext.getResources());
        when(mContext.getResources()).thenReturn(mResources);
        when(mContext.getSystemService(Context.TELEPHONY_SERVICE)).thenReturn(mTelephonyManager);

        mController = new NetworkResetPreferenceController(mContext);
        ReflectionHelpers.setField(mController, "mRestrictionChecker", mRestrictionChecker);

        // Availability defaults
        when(mTelephonyManager.isDataCapable()).thenReturn(true);
        when(mResources.getBoolean(R.bool.config_show_sim_info)).thenReturn(true);
        when(mRestrictionChecker.isRestrictionEnforcedByAdmin()).thenReturn(false);
    }

    @Test
    public void testIsAvailable_showSimInfo_notWifiOnly() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void testIsAvailable_hideSimInfo_notWifiOnly() {
        when(mResources.getBoolean(R.bool.config_show_sim_info)).thenReturn(false);
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void testIsAvailable_showSimInfo_wifiOnly() {
        when(mTelephonyManager.isDataCapable()).thenReturn(false);
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void testIsAvailable_userRestriction() {
        when(mRestrictionChecker.isRestrictionEnforcedByAdmin()).thenReturn(true);
        when(mRestrictionChecker.hasUserRestriction()).thenReturn(true);

        assertThat(mController.isAvailable()).isFalse();

        verify(mRestrictionChecker, never()).isRestrictionEnforcedByAdmin();
    }

    @Test
    public void testIsAvailable_noUserRestriction() {
        when(mRestrictionChecker.isRestrictionEnforcedByAdmin()).thenReturn(true);
        when(mRestrictionChecker.hasUserRestriction()).thenReturn(false);

        assertThat(mController.isAvailable()).isTrue();

        verify(mRestrictionChecker, never()).isRestrictionEnforcedByAdmin();
    }
}
