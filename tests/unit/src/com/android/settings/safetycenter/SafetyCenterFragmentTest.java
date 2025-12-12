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
import static org.mockito.Mockito.when;

import android.content.Context;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.TestUtils;
import com.android.settings.flags.Flags;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.safetycenter.ui.SafetyCenterFragment;

import org.junit.Rule;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class SafetyCenterFragmentTest {

    private Context mContext;

    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = ApplicationProvider.getApplicationContext();
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SAFETY_CENTER_NEW_UI)
    public void isPageSearchEnabled_whenFlagIsEnabled() throws Exception{
        BaseSearchIndexProvider indexProvider = SafetyCenterFragment.SEARCH_INDEX_DATA_PROVIDER;

        List<String> allXmlKeys = TestUtils.getAllXmlKeys(mContext, indexProvider);
        List<String> nonIndexableKeys = indexProvider.getNonIndexableKeys(mContext);
        allXmlKeys.removeAll(nonIndexableKeys);

        assertThat(allXmlKeys).isNotEmpty();
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_SAFETY_CENTER_NEW_UI)
    public void isPageSearchEnabled_whenFlagIsDisabled() throws Exception {
        BaseSearchIndexProvider indexProvider = SafetyCenterFragment.SEARCH_INDEX_DATA_PROVIDER;

        List<String> allXmlKeys = TestUtils.getAllXmlKeys(mContext, indexProvider);
        List<String> nonIndexableKeys = indexProvider.getNonIndexableKeys(mContext);
        allXmlKeys.removeAll(nonIndexableKeys);

        assertThat(allXmlKeys).isEmpty();
    }
}
