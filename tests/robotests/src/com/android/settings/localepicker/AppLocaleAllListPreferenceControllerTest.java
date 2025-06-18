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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
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
import android.util.Log;

import com.android.internal.app.AppLocaleCollector;
import com.android.internal.app.LocaleStore;
import com.android.settings.R;
import com.android.settings.backup.BackupSettingsFragment;
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
public class AppLocaleAllListPreferenceControllerTest {
    private static final String KEY_CATEGORY_APP_SUUPPORTED_LIST =
            "app_language_all_supported_category";
    private static final String KEY_ALL_LOCALE = "app_locale_list";
    private static final String TEST_PACKAGE_NAME = "com.android.settings";

    private Activity mActivity;
    private Context mContext;
    private PreferenceCategory mPreferenceCategory;
    private AppLocaleAllListPreferenceController mController;
    private Set<LocaleStore.LocaleInfo> mSupportedLocale;
    @Mock
    private PreferenceManager mPreferenceManager;
    @Mock
    private PreferenceScreen mPreferenceScreen;
    @Mock
    private IActivityManager mActivityService;
    @Mock
    private LocaleStore.LocaleInfo mSupportedLocaleInfo_1;
    @Mock
    private FragmentTransaction mFragmentTransaction;
    @Mock
    private SharedPreferences mSharedPreferences;
    private AppLocaleCollector mAppLocaleCollector;

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
        config.setLocales(new LocaleList(mSupportedLocaleInfo_1.getLocale()));
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
        when(mPreferenceScreen.findPreference(KEY_CATEGORY_APP_SUUPPORTED_LIST)).thenReturn(
                mPreferenceCategory);
        mPreferenceCategory.setKey(KEY_CATEGORY_APP_SUUPPORTED_LIST);
        mPreferenceScreen.addPreference(mPreferenceCategory);
        mController = new AppLocaleAllListPreferenceController(mContext, KEY_ALL_LOCALE,
                TEST_PACKAGE_NAME, false, null, mActivity, mAppLocaleCollector);
    }

    private void setUpLocaleConditions() {
        when(mSupportedLocaleInfo_1.getFullNameNative()).thenReturn("English (United States)");
        when(mSupportedLocaleInfo_1.getLocale()).thenReturn(
                LocaleList.forLanguageTags("en-US").get(0));
        when(mSupportedLocaleInfo_1.getId()).thenReturn("en-US");
    }

    @Test
    public void displayPreference_hasSupportedPreference_categoryIsVisible() {
        mController.displayPreference(mPreferenceScreen);
        mSupportedLocale = mAppLocaleCollector.getSupportedLocaleList(null, false, false);
        List<LocaleStore.LocaleInfo> localeList = new ArrayList<>();
        localeList.addAll(
                mSupportedLocale.stream().filter(localeInfo -> !localeInfo.isSuggested()).collect(
                        Collectors.toList()));

        assertTrue(mPreferenceCategory.isVisible());
        assertThat(mPreferenceCategory.getPreferenceCount()).isEqualTo(localeList.size());
    }

    @Test
    public void onPreferenceClick_shouldShowAppLanguagePage() {
        Context activityContext = mock(Context.class);
        mController = new AppLocaleAllListPreferenceController(activityContext, KEY_ALL_LOCALE,
                TEST_PACKAGE_NAME, false, null, mActivity, mAppLocaleCollector);
        when(mSupportedLocaleInfo_1.isSuggested()).thenReturn(false);
        when(mSupportedLocaleInfo_1.isSystemLocale()).thenReturn(true);
        when(mSupportedLocaleInfo_1.getParent()).thenReturn(null);
        boolean shouldShowAppLanguage = mController.shouldShowAppLanguage(mSupportedLocaleInfo_1);
        mController.switchFragment(activityContext, mSupportedLocaleInfo_1, shouldShowAppLanguage);

        verify(mFragmentTransaction, never()).add(any(), anyString());
    }

    @Test
    public void onPreferenceClick_shouldShowRegionalPage() {
        Context activityContext = mock(Context.class);
        mController = new AppLocaleAllListPreferenceController(activityContext, KEY_ALL_LOCALE,
                TEST_PACKAGE_NAME, false, null, mActivity, mAppLocaleCollector);
        when(mSupportedLocaleInfo_1.isSuggested()).thenReturn(false);
        when(mSupportedLocaleInfo_1.isSystemLocale()).thenReturn(false);
        when(mSupportedLocaleInfo_1.getParent()).thenReturn(null);
        boolean shouldShowAppLanguage = mController.shouldShowAppLanguage(mSupportedLocaleInfo_1);
        mController.switchFragment(activityContext, mSupportedLocaleInfo_1, shouldShowAppLanguage);

        verify(mFragmentTransaction, never()).add(any(RegionAndNumberingSystemPickerFragment.class),
                anyString());
    }
}
