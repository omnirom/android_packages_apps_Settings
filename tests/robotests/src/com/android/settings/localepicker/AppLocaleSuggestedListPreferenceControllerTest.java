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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.app.IActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.LocaleList;
import android.os.Looper;
import android.telephony.TelephonyManager;
import android.util.ArrayMap;

import com.android.internal.app.AppLocaleCollector;
import com.android.internal.app.LocaleStore;
import com.android.settings.R;
import com.android.settings.testutils.shadow.ShadowActivityManager;
import com.android.settings.testutils.shadow.ShadowFragment;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

import androidx.fragment.app.FragmentTransaction;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.InstrumentationRegistry;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowTelephonyManager;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        ShadowFragment.class,
        ShadowActivityManager.class,
})
public class AppLocaleSuggestedListPreferenceControllerTest {
    private static final String KEY_CATEGORY_APP_SUGGESTED_LIST =
            "app_language_suggested_category";
    private static final String KEY_SUGGESTED = "app_locale_suggested_list";
    private static final String TEST_PACKAGE_NAME = "com.android.settings";

    private Activity mActivity;
    private Context mContext;
    private PreferenceCategory mPreferenceCategory;
    private AppLocaleSuggestedListPreferenceController mController;
    private List<LocaleStore.LocaleInfo> mLocaleList;
    private Set<LocaleStore.LocaleInfo> mSuggestedLocale;
    private Map<String, Preference> mPreferences = new ArrayMap<>();
    private AppLocaleCollector mAppLocaleCollector;
    @Mock
    private PreferenceManager mPreferenceManager;
    @Mock
    private PreferenceScreen mPreferenceScreen;
    @Mock
    private IActivityManager mActivityService;
    @Mock
    private LocaleStore.LocaleInfo mSuggestedLocaleInfo_1;
    @Mock
    private LocaleStore.LocaleInfo mSuggestedLocaleInfo_2;
    @Mock
    private FragmentTransaction mFragmentTransaction;
    @Mock
    private SharedPreferences mSharedPreferences;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mActivity = Robolectric.setupActivity(Activity.class);
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
        mAppLocaleCollector = spy(
                new AppLocaleCollector(InstrumentationRegistry.getContext(), TEST_PACKAGE_NAME));
        mPreferenceCategory = spy(new PreferenceCategory(mContext, null));
        when(mPreferenceScreen.getPreferenceManager()).thenReturn(mPreferenceManager);
        when(mPreferenceCategory.getPreferenceManager()).thenReturn(mPreferenceManager);
        when(mPreferenceManager.getSharedPreferences()).thenReturn(mSharedPreferences);
        when(mPreferenceScreen.findPreference(KEY_CATEGORY_APP_SUGGESTED_LIST)).thenReturn(
                mPreferenceCategory);
        mPreferenceCategory.setKey(KEY_CATEGORY_APP_SUGGESTED_LIST);
        mPreferenceScreen.addPreference(mPreferenceCategory);
        mController = new AppLocaleSuggestedListPreferenceController(mContext, KEY_SUGGESTED,
                TEST_PACKAGE_NAME, false, null, mActivity, mAppLocaleCollector);
    }

    private void setUpLocaleConditions() {
        mLocaleList = new ArrayList<>();
        when(mSuggestedLocaleInfo_1.getFullNameNative()).thenReturn("English (United States)");
        when(mSuggestedLocaleInfo_1.getLocale()).thenReturn(
                LocaleList.forLanguageTags("en-US").get(0));
        when(mSuggestedLocaleInfo_1.getId()).thenReturn("en-US");
        mLocaleList.add(mSuggestedLocaleInfo_1);
        when(mSuggestedLocaleInfo_2.getFullNameNative()).thenReturn("Español (Estados Unidos)");
        when(mSuggestedLocaleInfo_2.getLocale()).thenReturn(
                LocaleList.forLanguageTags("es-US").get(0));
        when(mSuggestedLocaleInfo_2.getId()).thenReturn("es-US");
        mLocaleList.add(mSuggestedLocaleInfo_2);
    }

    @Test
    public void displayPreference_hasSuggestedPreference_categoryIsVisible() {
        mController.displayPreference(mPreferenceScreen);
        mSuggestedLocale = mAppLocaleCollector.getSupportedLocaleList(null, false, false);
        List<LocaleStore.LocaleInfo> localeList = new ArrayList<>();
        localeList.addAll(
                mSuggestedLocale.stream().filter(localeInfo -> localeInfo.isSuggested()).collect(
                        Collectors.toList()));

        assertTrue(mPreferenceCategory.isVisible());
        assertThat(mPreferenceCategory.getPreferenceCount()).isEqualTo(localeList.size());
    }

    @Test
    public void onPreferenceClick_shouldShowAppLanguagePage() {
        mContext = RuntimeEnvironment.application;
        mController = new AppLocaleSuggestedListPreferenceController(mContext, KEY_SUGGESTED,
                TEST_PACKAGE_NAME, false, null, mActivity, mAppLocaleCollector);
        mController.displayPreference(mPreferenceScreen);
        mController.setupSuggestedPreference(mLocaleList, mPreferences);
        SelectorWithWidgetPreference preference = mPreferenceCategory.findPreference(
                mSuggestedLocaleInfo_1.toString());
        preference.performClick();

        verify(mFragmentTransaction, never()).add(any(), anyString());
    }
}
