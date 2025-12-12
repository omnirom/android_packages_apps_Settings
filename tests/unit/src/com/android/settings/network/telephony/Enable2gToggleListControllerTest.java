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

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;
import android.os.Looper;
import android.platform.test.flag.junit.SetFlagsRule;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;

import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.R;
import com.android.settingslib.RestrictedSwitchPreference;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public final class Enable2gToggleListControllerTest {
    private static final int SUB_ID = 2;
    private static final String PREFERENCE_KEY = "TEST_2G_PREFERENCE";
    private static final String ENABLE_2G = "2G network protection";
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Mock
    private SubscriptionManager mSubscriptionManager;

    private PreferenceCategory mPreferenceCategory;
    private PreferenceScreen mPreferenceScreen;
    private Enable2gToggleListController mController;
    private Context mContext;

    @Before
    public void setUp() {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }

        MockitoAnnotations.initMocks(this);

        mContext = spy(ApplicationProvider.getApplicationContext());

        Resources resources = spy(mContext.getResources());
        when(mContext.getResources()).thenReturn(resources);
        when(resources.getString(R.string.enable_2g_title)).thenReturn(ENABLE_2G);
        when(mContext.getString(R.string.enable_2g_title)).thenReturn(ENABLE_2G);

        when(mContext.getSystemService(SubscriptionManager.class)).thenReturn(mSubscriptionManager);

        mController = new Enable2gToggleListController(mContext, PREFERENCE_KEY);

        mPreferenceCategory = spy(new PreferenceCategory(mContext));
        mPreferenceCategory.setKey(PREFERENCE_KEY);
        RestrictedSwitchPreference preferenceCategory =
                spy(new com.android.settingslib.RestrictedSwitchPreference(mContext));
        preferenceCategory.setKey(ENABLE_2G + SUB_ID);
        mPreferenceScreen = new PreferenceManager(mContext).createPreferenceScreen(mContext);
        mPreferenceScreen.addPreference(mPreferenceCategory);
        mPreferenceScreen.addPreference(preferenceCategory);
    }

    @Test
    public void displayPreference_update_preferenceCategoryAsNull() {
        mController = new Enable2gToggleListController(mContext, "PREFERENCE_KEY");

        mController.displayPreference(mPreferenceScreen);

        verify(mSubscriptionManager, never()).getActiveSubscriptionInfoList();
    }

    @Test
    public void displayPreference_noActiveSim() {
        when(mSubscriptionManager.getActiveSubscriptionInfoList()).thenReturn(new ArrayList());

        mController.displayPreference(mPreferenceScreen);

        verify(mSubscriptionManager).getActiveSubscriptionInfoList();
        verify(mPreferenceCategory, never()).addPreference(any());
    }

    @Test
    public void displayPreference_withOneActiveSim() {
        String simName = "SIM1";
        List<SubscriptionInfo> subInfos = new ArrayList();
        subInfos.add(getSubscriptionInfo(SUB_ID, simName));
        when(mSubscriptionManager.getActiveSubscriptionInfoList()).thenReturn(subInfos);
        RestrictedSwitchPreference[] capturedObject = new RestrictedSwitchPreference[1];
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                capturedObject[0] = (RestrictedSwitchPreference) invocation.getArguments()[0];
                return null;
            }
        }).when(mPreferenceCategory).addPreference(any(RestrictedSwitchPreference.class));

        mController.displayPreference(mPreferenceScreen);
        assertThat(capturedObject[0].getKey()).isEqualTo(ENABLE_2G + SUB_ID);
        assertTrue(capturedObject[0].getTitle().toString().contentEquals(ENABLE_2G));
        verify(mPreferenceCategory).addPreference(any());
    }

    @Test
    public void displayPreference_withTwoActiveSim() {
        String simOneName = "SIM1";
        String simSecondName = "SIM2";
        int simSecondSubId = 2;
        List<SubscriptionInfo> subInfos = new ArrayList();
        subInfos.add(getSubscriptionInfo(SUB_ID, simOneName));
        subInfos.add(getSubscriptionInfo(simSecondSubId, simSecondName));
        when(mSubscriptionManager.getActiveSubscriptionInfoList()).thenReturn(subInfos);
        List<RestrictedSwitchPreference> capturedObject = new ArrayList<>();
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                capturedObject.add((RestrictedSwitchPreference) invocation.getArguments()[0]);
                return null;
            }
        }).when(mPreferenceCategory).addPreference(any(RestrictedSwitchPreference.class));

        mController.displayPreference(mPreferenceScreen);

        assertThat(capturedObject.get(0).getKey()).isEqualTo(ENABLE_2G + SUB_ID);
        assertTrue(capturedObject.get(0).getTitle().toString().contentEquals(ENABLE_2G));
        assertThat(capturedObject.get(1).getKey()).isEqualTo(ENABLE_2G + simSecondSubId);
        assertTrue(capturedObject.get(1).getTitle().toString().contentEquals(ENABLE_2G));
        verify(mPreferenceCategory, times(2)).addPreference(any());
    }

    private SubscriptionInfo getSubscriptionInfo(int subId, String simName) {
        return new SubscriptionInfo.Builder()
                .setId(subId)
                .setDisplayName(simName)
                .build();
    }
}
