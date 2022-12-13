/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.applications.specialaccess.notificationaccess;

import static android.service.notification.NotificationListenerService.FLAG_FILTER_TYPE_CONVERSATIONS;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.DISABLED_DEPENDENT_SETTING;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.service.notification.NotificationListenerFilter;

import androidx.preference.Preference;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.notification.NotificationBackend;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class BridgedAppsLinkPreferenceControllerTest {

    private Context mContext;
    private BridgedAppsLinkPreferenceController mController;
    @Mock
    NotificationBackend mNm;
    ComponentName mCn = new ComponentName("a", "b");

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = ApplicationProvider.getApplicationContext();

        mController = new BridgedAppsLinkPreferenceController(mContext, "key");
        mController.setCn(mCn);
        mController.setNm(mNm);
        mController.setUserId(0);
    }

    @Test
    public void testAvailable_notGranted() {
        when(mNm.isNotificationListenerAccessGranted(any())).thenReturn(false);
        mController.setTargetSdk(Build.VERSION_CODES.CUR_DEVELOPMENT + 1);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(DISABLED_DEPENDENT_SETTING);

        // disables field
        Preference p = new Preference(mContext);
        mController.updateState(p);
        assertThat(p.isEnabled()).isFalse();
    }

    @Test
    public void testAvailable_lowTargetSdk_noCustomizations() {
        when(mNm.isNotificationListenerAccessGranted(any())).thenReturn(true);
        mController.setTargetSdk(Build.VERSION_CODES.S);
        when(mNm.getListenerFilter(mCn, 0)).thenReturn(new NotificationListenerFilter());

        assertThat(mController.getAvailabilityStatus()).isEqualTo(DISABLED_DEPENDENT_SETTING);

        // disables field
        Preference p = new Preference(mContext);
        mController.updateState(p);
        assertThat(p.isEnabled()).isFalse();
    }

    @Test
    public void testAvailable_lowTargetSdk_customizations() {
        when(mNm.isNotificationListenerAccessGranted(any())).thenReturn(true);
        mController.setTargetSdk(Build.VERSION_CODES.S);
        NotificationListenerFilter nlf = new NotificationListenerFilter();
        nlf.setTypes(FLAG_FILTER_TYPE_CONVERSATIONS);
        when(mNm.getListenerFilter(mCn, 0)).thenReturn(nlf);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);

        // enables field
        Preference p = new Preference(mContext);
        mController.updateState(p);
        assertThat(p.isEnabled()).isTrue();
    }

    @Test
    public void testAvailable_highTargetSdk_noCustomizations() {
        when(mNm.isNotificationListenerAccessGranted(any())).thenReturn(true);
        mController.setTargetSdk(Build.VERSION_CODES.CUR_DEVELOPMENT + 1);
        when(mNm.getListenerFilter(mCn, 0)).thenReturn(new NotificationListenerFilter());

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);

        // enables field
        Preference p = new Preference(mContext);
        mController.updateState(p);
        assertThat(p.isEnabled()).isTrue();
    }
}
