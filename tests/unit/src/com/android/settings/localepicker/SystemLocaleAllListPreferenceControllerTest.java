/**
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.settings.localepicker;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.spy;

import android.content.Context;
import android.os.Looper;

import com.android.internal.app.LocaleStore;
import com.android.internal.app.SystemLocaleCollector;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;

import java.util.Set;

@RunWith(AndroidJUnit4.class)
public class SystemLocaleAllListPreferenceControllerTest {
    private static final String KEY_CATEGORY_SYSTEM_SUPPORTED_LIST =
            "system_language_all_supported_category";
    private static final String KEY_SUPPORTED = "system_locale_list";

    private Context mContext;
    private PreferenceManager mPreferenceManager;
    private PreferenceCategory mPreferenceCategory;
    private PreferenceScreen mPreferenceScreen;
    private SystemLocaleAllListPreferenceController mController;
    private Set<LocaleStore.LocaleInfo> mLocaleList;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mContext = spy(ApplicationProvider.getApplicationContext());

        if (Looper.myLooper() == null) {
            Looper.prepare();
        }

        SystemLocaleCollector systemLocaleCollector = new SystemLocaleCollector(mContext, null);
        mLocaleList = systemLocaleCollector.getSupportedLocaleList(null, false, false);
        mPreferenceManager = new PreferenceManager(mContext);
        mPreferenceScreen = mPreferenceManager.createPreferenceScreen(mContext);
        mPreferenceCategory = new PreferenceCategory(mContext);
        mPreferenceCategory.setKey(KEY_CATEGORY_SYSTEM_SUPPORTED_LIST);
        mPreferenceScreen.addPreference(mPreferenceCategory);
        mController = new SystemLocaleAllListPreferenceController(mContext, KEY_SUPPORTED);
        mController.displayPreference(mPreferenceScreen);
    }

    @Test
    public void displayPreference_supportedLocaleShouldBeInSupportedCategory() {
        int count = 0;
        for (LocaleStore.LocaleInfo localeInfo : mLocaleList) {
            if (!localeInfo.isSuggested()) {
                count++;
            }
        }

        assertThat(mPreferenceCategory.getPreferenceCount()).isEqualTo(count);
    }

    @Test
    public void displayPreference_hasSupportedPreference_categoryIsVisible() {
        int count = 0;
        for (LocaleStore.LocaleInfo localeInfo : mLocaleList) {
            if (!localeInfo.isSuggested()) {
                count++;
            }
        }

        if (count > 0) {
            assertTrue(mPreferenceCategory.isVisible());
        } else {
            assertFalse(mPreferenceCategory.isVisible());
        }
    }
}
