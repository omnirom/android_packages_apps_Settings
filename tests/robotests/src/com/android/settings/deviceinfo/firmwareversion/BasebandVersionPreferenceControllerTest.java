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

package com.android.settings.deviceinfo.firmwareversion;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;
import android.sysprop.TelephonyProperties;
import android.telephony.TelephonyManager;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Arrays;

// LINT.IfChange
@RunWith(RobolectricTestRunner.class)
public class BasebandVersionPreferenceControllerTest {
    @Mock
    private Context mContext;
    private BasebandVersionPreferenceController mController;
    @Mock
    private TelephonyManager mTelephonyManager;
    @Mock
    private Resources mResources;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getSystemService(TelephonyManager.class)).thenReturn(mTelephonyManager);
        when(mContext.getSystemService(Context.TELEPHONY_SERVICE)).thenReturn(mTelephonyManager);
        when(mContext.getResources()).thenReturn(mResources);

        // By default, available
        when(mResources.getBoolean(R.bool.config_show_sim_info)).thenReturn(true);
        when(mTelephonyManager.isDataCapable()).thenReturn(true);
        when(mTelephonyManager.isDeviceVoiceCapable()).thenReturn(true);

        mController = new BasebandVersionPreferenceController(mContext, "key");
    }

    @Test
    public void getAvailability_default_available() {
        final String text = "test";
        TelephonyProperties.baseband_version(Arrays.asList(new String[]{text}));
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailability_notShowSimInfo_unavailable() {
        when(mResources.getBoolean(R.bool.config_show_sim_info)).thenReturn(false);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailability_voiceCapable_notDataCapable_available() {
        when(mTelephonyManager.isDataCapable()).thenReturn(false);
        when(mTelephonyManager.isDeviceVoiceCapable()).thenReturn(true);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailability_notVoiceCapable_dataCapable_available() {
        when(mTelephonyManager.isDataCapable()).thenReturn(true);
        when(mTelephonyManager.isDeviceVoiceCapable()).thenReturn(false);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailability_notVoiceCapable_notDataCapable_unavailable() {
        when(mTelephonyManager.isDataCapable()).thenReturn(false);
        when(mTelephonyManager.isDeviceVoiceCapable()).thenReturn(false);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }
}
// LINT.ThenChange(BasebandVersionPreferenceTest.kt)
