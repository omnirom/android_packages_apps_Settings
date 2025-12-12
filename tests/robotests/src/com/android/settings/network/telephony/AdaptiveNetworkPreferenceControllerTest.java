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

package com.android.settings.network.telephony;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.PendingIntent;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.safetycenter.SafetyCenterManager;
import android.safetycenter.SafetySourceData;
import android.safetycenter.SafetySourceIssue;
import android.telephony.TelephonyManager;

import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.R;
import com.android.settingslib.widget.BannerMessagePreference;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class AdaptiveNetworkPreferenceControllerTest {
    private static final String CELLULAR_SECURITY_SAFETY_SOURCE_ID =
            "AndroidCellularNetworkSecurity";
    private static final String IDENTIFIER_DISCLOSURE_ISSUE_ID = "identifier_disclosure";
    private static final String ACTION_LEARN_MORE_ID = "learn_more";


    @Mock
    private TelephonyManager mTelephonyManager;
    @Mock
    private SafetyCenterManager mSafetyCenterManager;
    @Mock
    private PreferenceScreen mPreferenceScreen;
    private Context mContext;
    private AdaptiveNetworkPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        // Tests must be skipped if these conditions aren't met as they cannot be mocked
        Assume.assumeTrue(Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU);

        mContext = spy(ApplicationProvider.getApplicationContext());
        when(mContext.getSystemService(SafetyCenterManager.class)).thenReturn(mSafetyCenterManager);
        when(mContext.getSystemService(TelephonyManager.class)).thenReturn(mTelephonyManager);
        mController = new AdaptiveNetworkPreferenceController(mContext);
    }

    @Test
    public void addToScreen_telephonyManagerNotInit() {
        when(mContext.getSystemService(TelephonyManager.class)).thenReturn(null);

        mController.addToScreen(mPreferenceScreen);

        verify(mPreferenceScreen, never()).addPreference(any(BannerMessagePreference.class));
    }

    @Test
    public void addToScreen_safetyCenterManagerNotInit() {
        when(mTelephonyManager.isNullCipherNotificationsEnabled()).thenReturn(true);
        when(mTelephonyManager.isCellularIdentifierDisclosureNotificationsEnabled()).thenReturn(
                true);
        when(mContext.getSystemService(SafetyCenterManager.class)).thenReturn(null);

        mController.addToScreen(mPreferenceScreen);

        verify(mPreferenceScreen, never()).addPreference(any(BannerMessagePreference.class));
    }

    @Test
    public void addToScreen_SafetySourceData_issuesIsEmpty() {
        when(mSafetyCenterManager.isSafetyCenterEnabled()).thenReturn(true);
        when(mTelephonyManager.isNullCipherNotificationsEnabled()).thenReturn(true);
        when(mTelephonyManager.isCellularIdentifierDisclosureNotificationsEnabled()).thenReturn(
                true);
        SafetySourceData data = mock(SafetySourceData.class);
        when(data.getIssues()).thenReturn(new ArrayList<SafetySourceIssue>());
        when(mSafetyCenterManager.getSafetySourceData(
                CELLULAR_SECURITY_SAFETY_SOURCE_ID)).thenReturn(data);

        mController.addToScreen(mPreferenceScreen);

        verify(mPreferenceScreen, never()).addPreference(any(BannerMessagePreference.class));
    }

    @Test
    public void addToScreen_bannerMessagePreference() {
        String learMoreTitle = "Learn More";
        String IssueTitle = "Title";
        String IssueSummary = "Summary";
        when(mSafetyCenterManager.isSafetyCenterEnabled()).thenReturn(true);
        when(mTelephonyManager.isNullCipherNotificationsEnabled()).thenReturn(true);
        when(mTelephonyManager.isCellularIdentifierDisclosureNotificationsEnabled()).thenReturn(
                true);
        SafetySourceData data = mock(SafetySourceData.class);

        PendingIntent pendIntent = mock(PendingIntent.class);
        List<SafetySourceIssue> issues = new ArrayList<>();
        SafetySourceIssue.Builder builder = new SafetySourceIssue.Builder(
                "1",
                IssueTitle,
                IssueSummary,
                SafetySourceData.SEVERITY_LEVEL_INFORMATION,
                IDENTIFIER_DISCLOSURE_ISSUE_ID);
        builder.addAction(new SafetySourceIssue.Action.Builder(
                ACTION_LEARN_MORE_ID,
                learMoreTitle,
                pendIntent).build());
        issues.add(builder.build());
        when(data.getIssues()).thenReturn(issues);
        when(mSafetyCenterManager.getSafetySourceData(
                CELLULAR_SECURITY_SAFETY_SOURCE_ID)).thenReturn(data);
        Drawable drawable = mock(Drawable.class);
        when(mContext.getDrawable(R.drawable.ic_info_selector)).thenReturn(drawable);
        BannerMessagePreference[] capturedObject = new BannerMessagePreference[1];
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                capturedObject[0] = (BannerMessagePreference) invocation.getArguments()[0];
                return null;
            }
        }).when(mPreferenceScreen).addPreference(any(BannerMessagePreference.class));

        mController.addToScreen(mPreferenceScreen);

        assertThat(capturedObject[0].getOrder()).isEqualTo(1);
        assertTrue(capturedObject[0].getTitle().toString().contentEquals(IssueTitle));
        assertTrue(capturedObject[0].getSummary().toString().contentEquals(IssueSummary));
        assertThat(capturedObject[0].getIcon()).isEqualTo(drawable);
        verify(mPreferenceScreen).addPreference(any(BannerMessagePreference.class));
    }

    @Test
    public void isSafetyCenterSupported_safetyCenterManagerNotInit() {
        when(mContext.getSystemService(SafetyCenterManager.class)).thenReturn(null);

        boolean result = mController.isSafetyCenterSupported();

        assertFalse(result);
    }

    @Test
    public void isSafetyCenterSupported_disable() {
        when(mSafetyCenterManager.isSafetyCenterEnabled()).thenReturn(false);

        boolean result = mController.isSafetyCenterSupported();

        assertFalse(result);
    }

    @Test
    public void isSafetyCenterSupported_enable() {
        when(mSafetyCenterManager.isSafetyCenterEnabled()).thenReturn(true);

        boolean result = mController.isSafetyCenterSupported();

        assertTrue(result);
    }

    @Test
    public void areNotificationsEnabled_telephonyManagerNotInit() {
        when(mContext.getSystemService(TelephonyManager.class)).thenReturn(null);

        boolean result = mController.areNotificationsEnabled();

        assertFalse(result);
    }

    @Test
    public void areNotificationsEnabled_telephonyManagerNullCipherDisable() {
        when(mTelephonyManager.isNullCipherNotificationsEnabled()).thenReturn(false);

        boolean result = mController.areNotificationsEnabled();

        assertFalse(result);
    }

    @Test
    public void areNotificationsEnabled_telephonyManagerIdenfierDiscosureDisable() {
        when(mTelephonyManager.isCellularIdentifierDisclosureNotificationsEnabled()).thenReturn(
                false);

        boolean result = mController.areNotificationsEnabled();

        assertFalse(result);
    }

    @Test
    public void areNotificationsEnabled_enable() {
        when(mTelephonyManager.isNullCipherNotificationsEnabled()).thenReturn(
                true);
        when(mTelephonyManager.isCellularIdentifierDisclosureNotificationsEnabled()).thenReturn(
                true);

        boolean result = mController.areNotificationsEnabled();

        assertTrue(result);
    }

    @Test
    public void areNotificationsEnabled_NullCipherNotificationsUnsupportedException() {
        when(mTelephonyManager.isNullCipherNotificationsEnabled()).thenThrow(
                new UnsupportedOperationException("Test Exception"));

        boolean result = mController.areNotificationsEnabled();

        assertFalse(result);
    }

    @Test
    public void areNotificationsEnabled_NullCipherNotificationsIllegalStateException() {
        when(mTelephonyManager.isNullCipherNotificationsEnabled()).thenThrow(
                new IllegalStateException("Test Exception"));

        boolean result = mController.areNotificationsEnabled();

        assertFalse(result);
    }

    @Test
    public void areNotificationsEnabled_CellularDisclosureUnsupportedException() {
        when(mTelephonyManager.isNullCipherNotificationsEnabled()).thenReturn(true);
        when(mTelephonyManager.isCellularIdentifierDisclosureNotificationsEnabled()).thenThrow(
                new UnsupportedOperationException("Test Exception"));

        boolean result = mController.areNotificationsEnabled();

        assertFalse(result);
    }

    @Test
    public void areNotificationsEnabled_CellularDisclosureIllegalStateException() {
        when(mTelephonyManager.isNullCipherNotificationsEnabled()).thenReturn(true);
        when(mTelephonyManager.isCellularIdentifierDisclosureNotificationsEnabled()).thenThrow(
                new IllegalStateException("Test Exception"));

        boolean result = mController.areNotificationsEnabled();

        assertFalse(result);
    }
}
