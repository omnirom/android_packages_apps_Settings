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

import static android.hardware.biometrics.SensorProperties.STRENGTH_STRONG;
import static android.hardware.face.FaceSensorProperties.TYPE_RGB;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.biometrics.ComponentInfoInternal;
import android.hardware.face.FaceManager;
import android.hardware.face.FaceSensorPropertiesInternal;
import android.hardware.face.IFaceAuthenticatorsRegisteredCallback;
import android.os.RemoteException;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.ArrayList;

@RunWith(AndroidJUnit4.class)
public class FaceFeatureProviderImplTest {

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Spy
    Context mContext = ApplicationProvider.getApplicationContext();
    @Mock
    private FaceManager mFaceManager;
    @Mock
    private PackageManager mPackageManager;

    private FaceFeatureProvider mProvider;
    private ArgumentCaptor<IFaceAuthenticatorsRegisteredCallback> mCallbackCaptor =
            ArgumentCaptor.forClass(IFaceAuthenticatorsRegisteredCallback.class);
    private FaceSensorPropertiesInternal mSensorProperty =
            new FaceSensorPropertiesInternal(
                    0 /* sensor id */,
                    STRENGTH_STRONG,
                    1 /* maxEnrollmentsPerUser */,
                    new ArrayList<ComponentInfoInternal>(),
                    TYPE_RGB,
                    true /* supportsFaceDetection */,
                    true /* supportsSelfIllumination */,
                    true /* resetLockoutRequiresChallenge */
            );

    @Before
    public void setUp() {
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mPackageManager.hasSystemFeature(PackageManager.FEATURE_FACE)).thenReturn(true);
        when(mContext.getSystemService(Context.FACE_SERVICE)).thenReturn(mFaceManager);

        mProvider = new FaceFeatureProviderImpl(mContext);
        verify(mFaceManager).addAuthenticatorsRegisteredCallback(mCallbackCaptor.capture());
    }

    @Test
    public void getMaxEnrollableCount_callbackCalled() throws RemoteException {
        final ArrayList<FaceSensorPropertiesInternal> list = new ArrayList<>();
        list.add(mSensorProperty);
        mCallbackCaptor.getValue().onAllAuthenticatorsRegistered(list);

        assertThat(mProvider.getMaxEnrollableCount(mContext)).isEqualTo(1);
    }

    @Test
    public void getMaxEnrollableCount_callbackNotCalled() {
        assertThat(mProvider.getMaxEnrollableCount(mContext)).isEqualTo(0);
    }

    @Test
    public void test_getParentalConsentPage() {
        assertThat(mProvider.getParentalConsentPage()).isEqualTo(FaceEnrollParentalConsent.class);
    }

    @Test
    public void test_getParentalConsentStringRes() {
        assertThat(mProvider.getParentalConsentStringRes())
                .isEqualTo(FaceEnrollParentalConsent.CONSENT_STRING_RESOURCES);
    }
}
