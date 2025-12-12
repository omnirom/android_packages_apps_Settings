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

package com.android.settings.biometrics.face;

import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.hardware.biometrics.ComponentInfoInternal;
import android.hardware.biometrics.SensorProperties;
import android.hardware.face.FaceManager;
import android.hardware.face.FaceSensorProperties;
import android.hardware.face.FaceSensorPropertiesInternal;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.ArrayList;

@RunWith(AndroidJUnit4.class)
public class FaceSettingsAppsPreferenceControllerTest {
    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Spy
    private Context mContext = ApplicationProvider.getApplicationContext();
    @Mock
    private FaceManager mFaceManager;

    private FaceSettingsAppsPreferenceController mController;

    private FaceSensorPropertiesInternal mConvenienceSensorProperty =
            new FaceSensorPropertiesInternal(
                    0 /* sensorId */,
                    SensorProperties.STRENGTH_CONVENIENCE,
                    1 /* maxEnrollmentsPerUser */,
                    new ArrayList<ComponentInfoInternal>(),
                    FaceSensorProperties.TYPE_UNKNOWN,
                    true /* supportsFaceDetection */,
                    true /* supportsSelfIllumination */,
                    true /* resetLockoutRequiresChallenge */);
    private FakeFeatureFactory mFeatureFactory;

    @Before
    public void setUp() {
        when(mContext.getSystemService(Context.FACE_SERVICE)).thenReturn(mFaceManager);
        final ArrayList<FaceSensorPropertiesInternal> list = new ArrayList<>();
        list.add(mConvenienceSensorProperty);
        when(mFaceManager.getSensorPropertiesInternal()).thenReturn(list);
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mController = new FaceSettingsAppsPreferenceController(
                mContext, "biometric_settings_face_app");
    }

    @Test
    public void isSliceable_returnFalse() {
        assertThat(mController.isSliceable()).isFalse();
    }

    @Test
    public void setChecked_checked_updateMetrics() {
        mController.setChecked(true);
        verify(mFeatureFactory.metricsFeatureProvider).action(any(),
                eq(SettingsEnums.ACTION_FACE_ENABLED_FOR_APP_SETTINGS), eq(true));
    }

    @Test
    public void setChecked_unchecked_updateMetrics() {
        mController.setChecked(false);
        verify(mFeatureFactory.metricsFeatureProvider).action(any(),
                eq(SettingsEnums.ACTION_FACE_ENABLED_FOR_APP_SETTINGS), eq(false));
    }

    @Test
    public void getAvailabilityStatus_strengthConvenience_returnUnsupported() {
        int status = mController.getAvailabilityStatus();
        assertThat(status).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }
}
