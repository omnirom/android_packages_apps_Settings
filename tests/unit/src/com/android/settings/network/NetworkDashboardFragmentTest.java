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
package com.android.settings.network;

import static android.platform.test.flag.junit.SetFlagsRule.DefaultInitValueType.DEVICE_DEFAULT;

import static com.android.settings.flags.Flags.FLAG_CATALYST;
import static com.android.settings.network.AirplaneModePreferenceController.REQUEST_CODE_EXIT_ECM;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.app.Instrumentation;
import android.content.Context;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.SearchIndexableResource;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.settingslib.drawer.CategoryKey;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class NetworkDashboardFragmentTest {

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule(DEVICE_DEFAULT);
    @Spy
    private Context mContext = ApplicationProvider.getApplicationContext();

    private NetworkDashboardFragment mFragment;

    @Before
    public void setUp() {
        final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();

        instrumentation.runOnMainSync(() -> {
            mFragment = new NetworkDashboardFragment();
        });
    }

    @Test
    public void getCategoryKey_isNetwork() {
        assertThat(mFragment.getCategoryKey()).isEqualTo(CategoryKey.CATEGORY_NETWORK);
    }

    @Test
    public void getXmlResourcesToIndex_shouldIncludeFragmentXml() {
        final List<SearchIndexableResource> indexRes =
                NetworkDashboardFragment.SEARCH_INDEX_DATA_PROVIDER.getXmlResourcesToIndex(
                        mContext,
                        true /* enabled */);

        assertThat(indexRes).hasSize(1);
        assertThat(indexRes.get(0).xmlResId).isEqualTo(mFragment.getPreferenceScreenResId());
    }

    @Test
    public void onActivityResult_catalystIsEnabled_doNotCrash() {
        mSetFlagsRule.enableFlags(FLAG_CATALYST);
        NetworkDashboardFragment spyFragment = spy(mFragment);
        when(spyFragment.getContext()).thenReturn(mContext);

        spyFragment.onActivityResult(REQUEST_CODE_EXIT_ECM, 0, null);
    }
}
