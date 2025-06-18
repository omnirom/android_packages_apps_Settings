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

package com.android.settings.connecteddevice.audiosharing;

import static com.google.common.truth.Truth.assertThat;

import static junit.framework.Assert.assertEquals;

import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import com.android.settings.R;
import com.android.settings.connecteddevice.audiosharing.audiostreams.AudioStreamsQrCodeFragment;

import androidx.fragment.app.Fragment;
import androidx.test.core.app.ApplicationProvider;

@RunWith(RobolectricTestRunner.class)
public class AudioSharingFeatureProviderImplTest {

    private AudioSharingFeatureProvider mFeatureProvider;
    @Mock private Fragment mFragment;
    @Mock private View mockView;
    private Context mContext;
    @Mock private Drawable mDrawable;

    @Before
    public void setup() {
        mContext = ApplicationProvider.getApplicationContext();
        mFeatureProvider = new AudioSharingFeatureProviderImpl();
    }

    @Test
    public void setQrCode_correctDialogLayout() {
        mFragment = new AudioSharingDialogFragment();
        View view =
                LayoutInflater.from(mContext)
                        .inflate(R.layout.dialog_custom_body_audio_sharing, null);
        mFeatureProvider.setQrCode(mFragment, view, R.id.description_image, mDrawable, "");
        ImageView imageView = view.findViewById(R.id.description_image);

        assertThat(imageView.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(imageView.getDrawable()).isEqualTo(mDrawable);
    }

    @Test
    public void setQrCode_correctLayout() {
        mFragment = new AudioStreamsQrCodeFragment();
        View view =
                LayoutInflater.from(mContext)
                        .inflate(R.layout.bluetooth_audio_streams_qr_code, null);
        mFeatureProvider.setQrCode(mFragment, view, R.id.qrcode_view, mDrawable, "");
        ImageView imageView = view.findViewById(R.id.qrcode_view);

        assertThat(imageView.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(imageView.getDrawable()).isEqualTo(mDrawable);
    }

    @Test(expected = IllegalArgumentException.class)
    public void setQrCode_nonExistedViewId() {
        mFragment = new AudioStreamsQrCodeFragment();
        View view =
                LayoutInflater.from(mContext)
                        .inflate(R.layout.bluetooth_audio_streams_qr_code, null);
        mFeatureProvider.setQrCode(mFragment, view, R.id.description_image, mDrawable, "");
    }
}
