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

import static android.net.ConnectivitySettingsManager.PRIVATE_DNS_MODE_OFF;
import static android.net.ConnectivitySettingsManager.PRIVATE_DNS_MODE_OPPORTUNISTIC;
import static android.net.ConnectivitySettingsManager.PRIVATE_DNS_MODE_PROVIDER_HOSTNAME;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.net.ConnectivitySettingsManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.appcompat.app.AlertDialog;

import com.android.settings.R;
import com.android.settingslib.CustomDialogPreferenceCompat.CustomPreferenceDialogFragment;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowOs;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowOs.class)
public class PrivateDnsModeDialogPreferenceTest {

    private static final String HOST_NAME = "dns.example.com";
    private static final String INVALID_HOST_NAME = "...,";

    private PrivateDnsModeDialogPreference mPreference;

    private Context mContext;
    private Button mSaveButton;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        ReflectionHelpers.setStaticField(android.system.OsConstants.class, "AF_INET", 2);
        ReflectionHelpers.setStaticField(android.system.OsConstants.class, "AF_INET6", 10);

        mContext = RuntimeEnvironment.application;
        mSaveButton = new Button(mContext);

        final CustomPreferenceDialogFragment fragment = mock(CustomPreferenceDialogFragment.class);
        final AlertDialog dialog = mock(AlertDialog.class);
        when(fragment.getDialog()).thenReturn(dialog);
        when(dialog.getButton(anyInt())).thenReturn(mSaveButton);

        mPreference = new PrivateDnsModeDialogPreference(mContext);
        ReflectionHelpers.setField(mPreference, "mFragment", fragment);

        final LayoutInflater inflater = LayoutInflater.from(mContext);
        final View view = inflater.inflate(R.layout.private_dns_mode_dialog,
                new LinearLayout(mContext), false);

        mPreference.onBindDialogView(view);
    }

    @Test
    public void onCheckedChanged_dnsModeOff_disableHostnameText() {
        mPreference.onCheckedChanged(null, R.id.private_dns_mode_off);

        assertThat(mPreference.mMode).isEqualTo(PRIVATE_DNS_MODE_OFF);
        assertThat(mPreference.mHostnameText.isEnabled()).isFalse();
    }

    @Test
    public void onCheckedChanged_dnsModeOpportunistic_disableHostnameText() {
        mPreference.onCheckedChanged(null, R.id.private_dns_mode_opportunistic);

        assertThat(mPreference.mMode).isEqualTo(PRIVATE_DNS_MODE_OPPORTUNISTIC);
        assertThat(mPreference.mHostnameText.isEnabled()).isFalse();
    }

    @Test
    public void onCheckedChanged_dnsModeProvider_enableHostnameText() {
        mPreference.onCheckedChanged(null, R.id.private_dns_mode_provider);

        assertThat(mPreference.mMode).isEqualTo(PRIVATE_DNS_MODE_PROVIDER_HOSTNAME);
        assertThat(mPreference.mHostnameText.isEnabled()).isTrue();
    }

    @Test
    public void onBindDialogView_containsCorrectData() {
        // Don't set settings to the default value ("opportunistic") as that
        // risks masking failure to read the mode from settings.
        ConnectivitySettingsManager.setPrivateDnsMode(mContext, PRIVATE_DNS_MODE_OFF);
        ConnectivitySettingsManager.setPrivateDnsHostname(mContext, HOST_NAME);

        final LayoutInflater inflater = LayoutInflater.from(mContext);
        final View view = inflater.inflate(R.layout.private_dns_mode_dialog,
                new LinearLayout(mContext), false);
        mPreference.onBindDialogView(view);

        assertThat(mPreference.mHostnameText.getText().toString()).isEqualTo(HOST_NAME);
        assertThat(mPreference.mRadioGroup.getCheckedRadioButtonId()).isEqualTo(
                R.id.private_dns_mode_off);
    }

    @Test
    public void doSaveButton_changeToOffMode_saveData() {
        // Set the default settings to OPPORTUNISTIC
        ConnectivitySettingsManager.setPrivateDnsMode(mContext, PRIVATE_DNS_MODE_OPPORTUNISTIC);

        mPreference.mMode = PRIVATE_DNS_MODE_OFF;
        mPreference.doSaveButton();

        // Change to OPPORTUNISTIC
        assertThat(ConnectivitySettingsManager.getPrivateDnsMode(mContext))
                .isEqualTo(PRIVATE_DNS_MODE_OFF);
    }

    @Test
    public void doSaveButton_changeToOpportunisticMode_saveData() {
        // Set the default settings to OFF
        ConnectivitySettingsManager.setPrivateDnsMode(mContext, PRIVATE_DNS_MODE_OFF);

        mPreference.mMode = PRIVATE_DNS_MODE_OPPORTUNISTIC;
        mPreference.doSaveButton();

        // Change to OPPORTUNISTIC
        assertThat(ConnectivitySettingsManager.getPrivateDnsMode(mContext))
                .isEqualTo(PRIVATE_DNS_MODE_OPPORTUNISTIC);
    }

    @Test
    public void doSaveButton_changeToProviderHostnameMode_saveData() {
        // Set the default settings to OFF
        ConnectivitySettingsManager.setPrivateDnsMode(mContext, PRIVATE_DNS_MODE_OFF);

        mPreference.onCheckedChanged(null, R.id.private_dns_mode_provider);
        mPreference.mHostnameText.setText(HOST_NAME);
        mPreference.doSaveButton();

        // Change to PROVIDER_HOSTNAME
        assertThat(ConnectivitySettingsManager.getPrivateDnsMode(mContext))
                .isEqualTo(PRIVATE_DNS_MODE_PROVIDER_HOSTNAME);
        assertThat(ConnectivitySettingsManager.getPrivateDnsHostname(mContext))
                .isEqualTo(HOST_NAME);
    }

    @Test
    public void doSaveButton_providerHostnameIsEmpty_setHostnameError() {
        ConnectivitySettingsManager.setPrivateDnsMode(mContext, PRIVATE_DNS_MODE_PROVIDER_HOSTNAME);
        ConnectivitySettingsManager.setPrivateDnsHostname(mContext, HOST_NAME);
        mPreference.onCheckedChanged(null, R.id.private_dns_mode_provider);

        mPreference.mHostnameText.setText("");
        mPreference.doSaveButton();

        assertThat(mPreference.mHostnameLayout.isErrorEnabled()).isTrue();
    }

    @Test
    public void doSaveButton_providerHostnameIsInvalid_setHostnameError() {
        ConnectivitySettingsManager.setPrivateDnsMode(mContext, PRIVATE_DNS_MODE_PROVIDER_HOSTNAME);
        ConnectivitySettingsManager.setPrivateDnsHostname(mContext, HOST_NAME);
        mPreference.onCheckedChanged(null, R.id.private_dns_mode_provider);

        mPreference.mHostnameText.setText(INVALID_HOST_NAME);
        mPreference.doSaveButton();

        assertThat(mPreference.mHostnameLayout.isErrorEnabled()).isTrue();
    }

    @Test
    public void doSaveButton_getDialogIsNull_noCrash() {
        mPreference = spy(new PrivateDnsModeDialogPreference(mContext));
        mPreference.mMode = PRIVATE_DNS_MODE_OFF;
        when(mPreference.getDialog()).thenReturn(null);

        mPreference.doSaveButton();
    }
}
