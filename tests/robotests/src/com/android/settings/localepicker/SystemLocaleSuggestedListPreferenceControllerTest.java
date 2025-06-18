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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.IActivityManager;
import android.content.Context;
import android.content.res.Configuration;
import android.os.LocaleList;
import android.os.Looper;
import android.telephony.TelephonyManager;
import android.util.ArrayMap;

import com.android.internal.app.LocaleStore;
import com.android.settings.testutils.shadow.ShadowActivityManager;
import com.android.settings.testutils.shadow.ShadowFragment;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowTelephonyManager;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        ShadowFragment.class,
        ShadowActivityManager.class,
})
public class SystemLocaleSuggestedListPreferenceControllerTest {
    private static final String KEY_CATEGORY_SYSTEM_SUGGESTED_LIST =
            "system_language_suggested_category";
    private static final String KEY_SUGGESTED = "system_locale_suggested_list";

    private Context mContext;
    private PreferenceCategory mPreferenceCategory;
    private PreferenceScreen mPreferenceScreen;
    private SystemLocaleSuggestedListPreferenceController mController;
    private List<LocaleStore.LocaleInfo> mLocaleList;
    private Map<String, Preference> mPreferences = new ArrayMap<>();
    @Mock
    private PreferenceManager mPreferenceManager;
    @Mock
    private IActivityManager mActivityService;
    @Mock
    private LocaleStore.LocaleInfo mSuggestedLocaleInfo_1;
    @Mock
    private LocaleStore.LocaleInfo mSuggestedLocaleInfo_2;
    @Mock
    private FragmentTransaction mFragmentTransaction;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mContext = spy(ApplicationProvider.getApplicationContext());
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }

        ShadowActivityManager.setService(mActivityService);
        final Configuration config = new Configuration();
        setUpLocaleConditions();
        config.setLocales(new LocaleList(mSuggestedLocaleInfo_1.getLocale(),
                mSuggestedLocaleInfo_2.getLocale()));
        when(mActivityService.getConfiguration()).thenReturn(config);
        ShadowTelephonyManager shadowTelephonyManager =
                Shadows.shadowOf(mContext.getSystemService(TelephonyManager.class));
        shadowTelephonyManager.setSimCountryIso("us");
        shadowTelephonyManager.setNetworkCountryIso("us");
        mPreferenceScreen = spy(new PreferenceScreen(mContext, null));
        mPreferenceCategory = spy(new PreferenceCategory(mContext, null));
        when(mPreferenceScreen.getPreferenceManager()).thenReturn(mPreferenceManager);
        when(mPreferenceCategory.getPreferenceManager()).thenReturn(mPreferenceManager);
        mPreferenceManager = new PreferenceManager(mContext);
        mPreferenceScreen = mPreferenceManager.createPreferenceScreen(mContext);
        mPreferenceCategory.setKey(KEY_CATEGORY_SYSTEM_SUGGESTED_LIST);
        mPreferenceScreen.addPreference(mPreferenceCategory);
        mController = new SystemLocaleSuggestedListPreferenceController(mContext, KEY_SUGGESTED);
    }

    private void setUpLocaleConditions() {
        mLocaleList = new ArrayList<>();
        when(mSuggestedLocaleInfo_1.getFullNameNative()).thenReturn("English");
        when(mSuggestedLocaleInfo_1.getLocale()).thenReturn(
                LocaleList.forLanguageTags("en-US").get(0));
        mLocaleList.add(mSuggestedLocaleInfo_1);
        when(mSuggestedLocaleInfo_2.getFullNameNative()).thenReturn("Español (Estados Unidos)");
        when(mSuggestedLocaleInfo_2.getLocale()).thenReturn(
                LocaleList.forLanguageTags("es-US").get(0));
        mLocaleList.add(mSuggestedLocaleInfo_2);
    }

    @Test
    public void displayPreference_hasSuggestedPreference_categoryIsVisible() {
        mController.displayPreference(mPreferenceScreen);
        mController.setupPreference(mLocaleList, mPreferences);

        assertTrue(mPreferenceCategory.isVisible());
        assertThat(mPreferenceCategory.getPreferenceCount()).isEqualTo(2);
    }

    @Test
    public void displayPreference_noSuggestedPreference_categoryIsGone() {
        mLocaleList.clear();
        mController.displayPreference(mPreferenceScreen);
        mController.setupPreference(mLocaleList, mPreferences);

        assertFalse(mPreferenceCategory.isVisible());
        assertThat(mPreferenceCategory.getPreferenceCount()).isEqualTo(0);
    }

    @Test
    public void switchFragment_shouldShowLocaleEditor() {
        when(mSuggestedLocaleInfo_1.isSuggested()).thenReturn(true);
        mController.shouldShowLocaleEditor(mSuggestedLocaleInfo_1);
        mController.switchFragment(mSuggestedLocaleInfo_1);

        verify(mFragmentTransaction, never()).add(any(LocaleListEditor.class),
                anyString());
    }

    @Test
    public void switchFragment_shouldShowRegionNumberingPicker() {
        Context activityContext = mock(Context.class);
        mController = new SystemLocaleSuggestedListPreferenceController(activityContext,
                KEY_SUGGESTED);
        when(mSuggestedLocaleInfo_1.isSuggested()).thenReturn(false);
        when(mSuggestedLocaleInfo_1.isSystemLocale()).thenReturn(false);
        when(mSuggestedLocaleInfo_1.getParent()).thenReturn(null);
        mController.shouldShowLocaleEditor(mSuggestedLocaleInfo_1);
        mController.switchFragment(mSuggestedLocaleInfo_1);

        verify(mFragmentTransaction, never()).add(any(RegionAndNumberingSystemPickerFragment.class),
                anyString());
    }
}
