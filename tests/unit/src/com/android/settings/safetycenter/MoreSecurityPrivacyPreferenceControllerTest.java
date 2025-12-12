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

package com.android.settings.safetycenter;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


import android.content.Context;
import android.content.Intent;

import androidx.preference.Preference;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import android.app.settings.SettingsEnums;
import com.android.settings.safetycenter.MoreSecurityPrivacyFragment;
import com.android.settings.SettingsActivity;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settings.safetycenter.ui.MoreSecurityPrivacyPreferenceController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

@RunWith(AndroidJUnit4.class)
public class MoreSecurityPrivacyPreferenceControllerTest {

    private static final String PREFERENCE_KEY = "safety_center_more_security_privacy";

    private MoreSecurityPrivacyPreferenceController mController;
    private Preference mPreference;

    private Context mContext;

    @Before
    public void setUp() {

        mContext = spy(ApplicationProvider.getApplicationContext());

        mPreference = new Preference(mContext);
        mPreference.setKey(PREFERENCE_KEY);

        doNothing().when(mContext).startActivity(any(Intent.class));
        mController = new MoreSecurityPrivacyPreferenceController(mContext, PREFERENCE_KEY);
    }

    @Test
    public void handlePreferenceTreeClick_launchesMoreSecurityPrivacyFragment() {
        final ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);

        boolean preferenceHandled = mController.handlePreferenceTreeClick(mPreference);

        assertThat(preferenceHandled).isTrue();
        verify(mContext).startActivity(intentCaptor.capture());

        assertThat(intentCaptor.getValue().getAction())
                .isEqualTo(Intent.ACTION_MAIN);
        assertThat(intentCaptor.getValue().getExtra(
                MetricsFeatureProvider.EXTRA_SOURCE_METRICS_CATEGORY))
                .isEqualTo(SettingsEnums.SAFETY_CENTER);
        assertThat(intentCaptor.getValue().getExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT))
                .isEqualTo(MoreSecurityPrivacyFragment.class.getName());

    }

    @Test
    public void handlePreferenceTreeClick_forDifferentPreferenceKey_isNotHandled() {
        Preference preference = new Preference(mContext);
        preference.setKey("some_other_preference");

        boolean preferenceHandled = mController.handlePreferenceTreeClick(preference);

        assertThat(preferenceHandled).isFalse();
    }
}