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

package com.android.settings.language;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.app.IActivityManager;
import android.content.Context;
import android.content.res.Configuration;
import android.os.LocaleList;
import android.os.Looper;
import android.telephony.TelephonyManager;
import android.util.ArrayMap;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.internal.app.LocalePicker;
import com.android.internal.app.LocaleStore;
import com.android.settings.testutils.shadow.ShadowActivityManager;
import com.android.settings.testutils.shadow.ShadowFragment;
import com.android.settings.R;
import com.android.settingslib.widget.OrderMenuPreference;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowTelephonyManager;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowFragment.class, ShadowActivityManager.class,})
public class UserPreferredLocalePreferenceControllerTest {
    private static final String KEY_PREFERENCE_CATEGORY_LANGUAGES =
            "languages_category";
    private static final String KEY_PREFERENCE_USER_PREFERRED_LOCALE_LIST =
            "user_preferred_locale_list";

    private Context mContext;
    private PreferenceManager mPreferenceManager;
    private PreferenceCategory mPreferenceCategory;
    private PreferenceScreen mPreferenceScreen;
    private UserPreferredLocalePreferenceController mController;
    private List<LocaleStore.LocaleInfo> mLocaleInfoList;
    private Map<String, OrderMenuPreference> mPreferences = new ArrayMap<>();
    private LocaleList mCacheLocaleList;
    private Locale mCacheLocale;
    private LocaleList mLocaleList;

    @Mock
    private IActivityManager mActivityService;
    @Mock
    private LocaleStore.LocaleInfo mLocaleInfo_1;
    @Mock
    private LocaleStore.LocaleInfo mLocaleInfo_2;
    @Mock
    private LocaleStore.LocaleInfo mUnavailableLocaleInfo;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mContext = spy(ApplicationProvider.getApplicationContext());
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }

        LocaleStore.fillCache(mContext);
        mCacheLocale = Locale.getDefault(Locale.Category.FORMAT);
        mCacheLocaleList = LocaleList.getDefault();
        ShadowActivityManager.setService(mActivityService);
        final Configuration config = new Configuration();
        setUpLocaleConditions();
        config.setLocales(mLocaleList);
        when(mActivityService.getConfiguration()).thenReturn(config);
        ShadowTelephonyManager shadowTelephonyManager =
                Shadows.shadowOf(mContext.getSystemService(TelephonyManager.class));
        shadowTelephonyManager.setSimCountryIso("us");
        shadowTelephonyManager.setNetworkCountryIso("us");
        mPreferenceScreen = spy(new PreferenceScreen(mContext, null));
        mPreferenceCategory = spy(new PreferenceCategory(mContext, null));
        when(mPreferenceScreen.getPreferenceManager()).thenReturn(mock(PreferenceManager.class));
        when(mPreferenceCategory.getPreferenceManager()).thenReturn(mock(PreferenceManager.class));
        mPreferenceManager = new PreferenceManager(mContext);
        mPreferenceScreen = mPreferenceManager.createPreferenceScreen(mContext);
        mPreferenceCategory.setKey(KEY_PREFERENCE_CATEGORY_LANGUAGES);
        mPreferenceScreen.addPreference(mPreferenceCategory);

        mController = new UserPreferredLocalePreferenceController(mContext,
                KEY_PREFERENCE_USER_PREFERRED_LOCALE_LIST);
    }

    @After
    public void tearDown() throws Exception {
        Locale.setDefault(mCacheLocale);
        LocalePicker.updateLocales(mCacheLocaleList);
    }

    private void setUpLocaleConditions() {
        mLocaleInfoList = new ArrayList<>();
        when(mLocaleInfo_1.getFullNameNative()).thenReturn("English (United States)");
        when(mLocaleInfo_1.isTranslated()).thenReturn(true);
        when(mLocaleInfo_1.getLocale()).thenReturn(
                LocaleList.forLanguageTags("en-US").get(0));
        mLocaleInfoList.add(mLocaleInfo_1);
        when(mLocaleInfo_2.getFullNameNative()).thenReturn("Español (Estados Unidos)");
        when(mLocaleInfo_2.isTranslated()).thenReturn(true);
        when(mLocaleInfo_2.getLocale()).thenReturn(
                LocaleList.forLanguageTags("es-US").get(0));
        mLocaleInfoList.add(mLocaleInfo_2);
        when(mUnavailableLocaleInfo.getFullNameNative()).thenReturn("Akan (Gaana)");
        when(mUnavailableLocaleInfo.getLocale()).thenReturn(
                LocaleList.forLanguageTags("ak-GH").get(0));
        when(mUnavailableLocaleInfo.isTranslated()).thenReturn(false);
        mLocaleInfoList.add(mUnavailableLocaleInfo);

        final Locale[] newList = new Locale[mLocaleInfoList.size()];

        for (int i = 0; i < mLocaleInfoList.size(); i++) {
            final LocaleStore.LocaleInfo li = mLocaleInfoList.get(i);
            newList[i] = li.getLocale();
        }
        mLocaleList = new LocaleList(newList);
    }

    @Test
    public void setupPreference_localeCount() {
        ReflectionHelpers.setField(mController, "mPreferenceCategory", mPreferenceCategory);
        mController.setupPreference(mPreferences);

        assertThat(mPreferenceCategory.getPreferenceCount()).isEqualTo(mLocaleInfoList.size());
    }

    @Test
    public void setupPreference_setTitle_LocaleName() {
        ReflectionHelpers.setField(mController, "mPreferenceCategory", mPreferenceCategory);
        mController.setupPreference(mPreferences);

        assertThat(mPreferenceCategory.getPreferenceCount()).isEqualTo(mLocaleInfoList.size());

        OrderMenuPreference firstPreference =
                (OrderMenuPreference) mPreferenceCategory.getPreference(0);
        OrderMenuPreference secondPreference =
                (OrderMenuPreference) mPreferenceCategory.getPreference(1);
        OrderMenuPreference lastPreference =
                (OrderMenuPreference) mPreferenceCategory.getPreference(2);

        assertThat(firstPreference.getTitle().toString()).isEqualTo(
                mLocaleInfoList.get(0).getFullNameNative());
        assertThat(secondPreference.getTitle().toString()).isEqualTo(
                mLocaleInfoList.get(1).getFullNameNative());
        assertThat(lastPreference.getTitle().toString()).isEqualTo(
                mLocaleInfoList.get(2).getFullNameNative());
    }

    @Test
    public void setupPreference_setSummary_systemLocale() {
        ReflectionHelpers.setField(mController, "mPreferenceCategory", mPreferenceCategory);
        mController.setupPreference(mPreferences);
        OrderMenuPreference preference =
                (OrderMenuPreference) mPreferenceCategory.getPreference(0);

        assertThat(preference.getSummary().toString()).isEqualTo(
                mContext.getString(R.string.desc_current_default_language));
    }

    @Test
    public void setupPreference_setSummary_unavailableLocale() {
        ReflectionHelpers.setField(mController, "mPreferenceCategory", mPreferenceCategory);
        mController.setupPreference(mPreferences);

        assertThat(mPreferenceCategory.getPreferenceCount()).isEqualTo(mLocaleInfoList.size());

        OrderMenuPreference unavailablePreference =
                (OrderMenuPreference) mPreferenceCategory.getPreference(2);

        assertThat(unavailablePreference.getSummary().toString()).isEqualTo(
                        mContext.getString(R.string.locale_not_translated));
    }
}
