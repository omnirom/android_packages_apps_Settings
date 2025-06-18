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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
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

import java.util.Locale;
import java.util.Set;

@RunWith(AndroidJUnit4.class)
public class SystemLocaleSuggestedListPreferenceControllerTest {
    private static final String KEY_CATEGORY_SYSTEM_SUGGESTED_LIST =
            "system_language_suggested_category";
    private static final String KEY_SUGGESTED = "system_locale_suggested_list";

    private Context mContext;
    private PreferenceManager mPreferenceManager;
    private PreferenceCategory mPreferenceCategory;
    private PreferenceScreen mPreferenceScreen;
    private Preference mSuggestedPreference;
    private SystemLocaleSuggestedListPreferenceController mController;
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
        mPreferenceCategory.setKey(KEY_CATEGORY_SYSTEM_SUGGESTED_LIST);
        mPreferenceScreen.addPreference(mPreferenceCategory);
        mController = new SystemLocaleSuggestedListPreferenceController(mContext, KEY_SUGGESTED);
        mController.displayPreference(mPreferenceScreen);
    }

    @Test
    public void displayPreference_suggestedLocaleShouldBeInSuggestedCategory() {
        int count = 0;
        for (LocaleStore.LocaleInfo localeInfo : mLocaleList) {
            if (localeInfo.isSuggested()) {
                count++;
            }
        }

        assertThat(mPreferenceCategory.getPreferenceCount()).isEqualTo(count);
    }

    @Test
    public void displayPreference_hasSuggestedPreference_categoryIsVisible() {
        int count = 0;
        for (LocaleStore.LocaleInfo localeInfo : mLocaleList) {
            if (localeInfo.isSuggested()) {
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
