/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.accessibility;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.view.View;

import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceViewHolder;
import androidx.test.core.app.ApplicationProvider;

import com.android.settingslib.widget.ButtonPreference;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** Tests for {@link ViewAllBluetoothDevicesPreferenceController}. */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        com.android.settings.testutils.shadow.ShadowFragment.class,
})
public class ViewAllBluetoothDevicesPreferenceControllerTest {

    @Rule
    public MockitoRule mocks = MockitoJUnit.rule();
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private final String TEST_KEY = "test_key";

    @Spy
    private HearingDevicePairingFragment mFragment = new HearingDevicePairingFragment();
    private FragmentActivity mActivity;
    @Mock
    private PreferenceScreen mScreen;
    private ViewAllBluetoothDevicesPreferenceController mController;
    private ButtonPreference mPreference = new ButtonPreference(mContext);


    @Before
    public void setUp() {
        mActivity = Robolectric.setupActivity(FragmentActivity.class);
        when(mFragment.getContext()).thenReturn(mContext);
        when(mFragment.getActivity()).thenReturn(mActivity);
        View rootView = View.inflate(mContext, mPreference.getLayoutResource(), null);
        mPreference.setKey(TEST_KEY);
        mPreference.onBindViewHolder(PreferenceViewHolder.createInstanceForTests(rootView));
        when(mScreen.findPreference(TEST_KEY)).thenReturn(mPreference);

        mController = spy(new ViewAllBluetoothDevicesPreferenceController(mActivity, TEST_KEY));
        mController.init(mFragment);
        mController.displayPreference(mScreen);
    }

    @Test
    public void buttonPreferenceOnClick_launchConnectedDevicePage() {
        doNothing().when(mController).launchConnectedDevicePage();

        mPreference.getButton().callOnClick();

        verify(mController).launchConnectedDevicePage();
    }
}
