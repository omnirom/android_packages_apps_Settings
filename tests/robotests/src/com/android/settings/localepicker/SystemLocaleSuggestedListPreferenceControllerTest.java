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

import static com.android.settings.SettingsActivity.EXTRA_SHOW_FRAGMENT;
import static com.android.settings.flags.Flags.FLAG_REGIONAL_PREFERENCES_API_ENABLED;
import static com.android.settings.localepicker.LocalePickerBaseListPreferenceController.TAG_DIALOG_CHANGE_REGION_FOR_SYSTEM_LANGUAGE;
import static com.android.settings.localepicker.LocalePickerBaseListPreferenceController.TAG_DIALOG_CHANGE_REGION_PREFERRED_LANGUAGE;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.app.IActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.LocaleList;
import android.os.Looper;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.telephony.TelephonyManager;
import android.util.ArrayMap;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.internal.app.LocaleStore;
import com.android.settings.regionalpreferences.RegionDialogFragment;
import com.android.settings.testutils.shadow.ShadowActivityManager;
import com.android.settings.testutils.shadow.ShadowFragment;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLocaleList;
import org.robolectric.shadows.ShadowTelephonyManager;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        ShadowFragment.class,
        ShadowActivityManager.class,
})
public class SystemLocaleSuggestedListPreferenceControllerTest {
    private static final String KEY_CATEGORY_SYSTEM_SUGGESTED_LIST =
            "system_language_suggested_category";
    private static final String KEY_SUGGESTED = "system_locale_suggested_list";
    private static final String LOCALE_URDU_INDIA = "اردو (بھارت)";
    private static final int CURRENT = LocaleStore.LocaleInfo.SUGGESTION_TYPE_CURRENT;

    private Context mContext;
    private FragmentActivity mActivity;
    private PreferenceCategory mPreferenceCategory;
    private PreferenceScreen mPreferenceScreen;
    private SystemLocaleSuggestedListPreferenceController mController;
    private List<LocaleStore.LocaleInfo> mLocaleList;
    private List<LocaleStore.LocaleInfo> mSuggestedLocaleList;
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
    @Mock
    private FragmentManager mFragmentManager;

    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mContext = spy(Robolectric.setupActivity(Activity.class));
        mActivity = Robolectric.buildActivity(FragmentActivity.class).get();
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
        when(mFragmentManager.beginTransaction()).thenReturn(mFragmentTransaction);
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

    private void setupLocaleWithExtensionConditions() {
        mLocaleList = new ArrayList<>();
        when(mSuggestedLocaleInfo_1.getFullNameNative()).thenReturn(LOCALE_URDU_INDIA);
        when(mSuggestedLocaleInfo_1.getLocale()).thenReturn(
                LocaleList.forLanguageTags("ur-IN").get(0));
        mLocaleList.add(mSuggestedLocaleInfo_1);
        when(mSuggestedLocaleInfo_2.getFullNameNative()).thenReturn(LOCALE_URDU_INDIA);
        when(mSuggestedLocaleInfo_2.getLocale()).thenReturn(
                LocaleList.forLanguageTags("ur-IN-u-nu-latn").get(0));
        mLocaleList.add(mSuggestedLocaleInfo_2);
    }

    @Test
    public void displayPreference_hasSuggestedPreference_removeLocalesWithExtension() {
        setupLocaleWithExtensionConditions();
        mController.displayPreference(mPreferenceScreen);
        mController.setupPreference(mLocaleList, mPreferences);

        assertTrue(mPreferenceCategory.isVisible());
        assertThat(mPreferenceCategory.getPreferenceCount()).isEqualTo(1);
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
    @DisableFlags(FLAG_REGIONAL_PREFERENCES_API_ENABLED)
    public void switchFragment_shouldShowLocaleEditor() {
        when(mSuggestedLocaleInfo_1.isSuggested()).thenReturn(true);

        mController.setFragmentManager(mFragmentManager);
        mController.shouldShowLocaleEditor(mSuggestedLocaleInfo_1);
        mController.switchFragment(mSuggestedLocaleInfo_1);

        verify(mFragmentManager).popBackStack(
                anyString(), eq(FragmentManager.POP_BACK_STACK_INCLUSIVE));
    }

    @Test
    @DisableFlags(FLAG_REGIONAL_PREFERENCES_API_ENABLED)
    public void switchFragment_shouldShowRegionNumberingPicker() {
        Context activityContext = spy(Robolectric.setupActivity(Activity.class));
        mController = new SystemLocaleSuggestedListPreferenceController(activityContext,
                KEY_SUGGESTED);
        when(mSuggestedLocaleInfo_1.isSuggested()).thenReturn(false);
        when(mSuggestedLocaleInfo_1.isSystemLocale()).thenReturn(false);
        when(mSuggestedLocaleInfo_1.getParent()).thenReturn(null);

        mController.setFragmentManager(mFragmentManager);
        mController.shouldShowLocaleEditor(mSuggestedLocaleInfo_1);
        mController.switchFragment(mSuggestedLocaleInfo_1);

        Intent intent = Shadows.shadowOf(mActivity.getApplication()).getNextStartedActivity();
        assertThat(intent.getStringExtra(EXTRA_SHOW_FRAGMENT))
                .isEqualTo(RegionAndNumberingSystemPickerFragment.class.getName());
    }

    @Test
    @EnableFlags(FLAG_REGIONAL_PREFERENCES_API_ENABLED)
    public void disposedEvent_switchFragment_shouldShowLocaleEditor() {
        ShadowLocaleList.reset();
        Locale defaultLocale1 = new Locale("de", "DE");
        Locale defaultLocale2 = new Locale("fr", "FR");
        Locale defaultLocale3 = new Locale("ja", "JP");
        LocaleList.setDefault(new LocaleList(defaultLocale1, defaultLocale2, defaultLocale3));
        when(mSuggestedLocaleInfo_1.isSuggested()).thenReturn(true);

        mController.setFragmentManager(mFragmentManager);
        mController.shouldShowLocaleEditor(mSuggestedLocaleInfo_1);
        mController.switchFragment(mSuggestedLocaleInfo_1);

        verify(mFragmentManager).popBackStack(
                anyString(), eq(FragmentManager.POP_BACK_STACK_INCLUSIVE));
    }

    @Test
    @EnableFlags(FLAG_REGIONAL_PREFERENCES_API_ENABLED)
    public void changeSystemLanguageRegion_switchFragment_showDialogForTheEvent() {
        ShadowLocaleList.reset();
        Locale defaultLocale1 = new Locale("en", "IN"); //mSuggestedLocaleInfo_1 is en_US
        Locale defaultLocale2 = new Locale("fr", "FR");
        Locale defaultLocale3 = new Locale("ja", "JP");
        LocaleList.setDefault(new LocaleList(defaultLocale1, defaultLocale2, defaultLocale3));
        when(mSuggestedLocaleInfo_1.isSuggested()).thenReturn(true);

        mController.setFragmentManager(mFragmentManager);
        mController.shouldShowLocaleEditor(mSuggestedLocaleInfo_1);
        mController.switchFragment(mSuggestedLocaleInfo_1);

        verify(mFragmentTransaction).add(any(RegionDialogFragment.class),
                eq(TAG_DIALOG_CHANGE_REGION_FOR_SYSTEM_LANGUAGE));
    }

    @Test
    @EnableFlags(FLAG_REGIONAL_PREFERENCES_API_ENABLED)
    public void changePreferredLanguageRegion_switchFragment_showDialogForTheEvent() {
        ShadowLocaleList.reset();
        Locale defaultLocale1 = new Locale("fr", "FR");
        Locale defaultLocale2 = new Locale("en", "IN"); //mSuggestedLocaleInfo_1 is en_US
        Locale defaultLocale3 = new Locale("ja", "JP");
        LocaleList.setDefault(new LocaleList(defaultLocale1, defaultLocale2, defaultLocale3));
        when(mSuggestedLocaleInfo_1.isSuggested()).thenReturn(true);

        mController.setFragmentManager(mFragmentManager);
        mController.shouldShowLocaleEditor(mSuggestedLocaleInfo_1);
        mController.switchFragment(mSuggestedLocaleInfo_1);

        verify(mFragmentTransaction).add(any(RegionDialogFragment.class),
                eq(TAG_DIALOG_CHANGE_REGION_PREFERRED_LANGUAGE));
    }

    @Test
    public void setupPreference_checkLocaleByXml() throws Exception {
        mSuggestedLocaleList = new ArrayList<>();
        // Get each locale from resource: locale_config.xml
        final String[] localeNames = Resources.getSystem().getStringArray(
                Resources.getSystem().getIdentifier("supported_locales", "array", "android"));
        final Configuration config = new Configuration();
        Locale[] locales = new Locale[localeNames.length];
        for (int i = 0; i < localeNames.length; i++) {
            locales[i] = Locale.forLanguageTag(localeNames[i]);
            // Suggested list: "en-US"
            LocaleStore.LocaleInfo localeInfo = LocaleStore.fromLocale(locales[i]);
            if (localeInfo.getLocale().toLanguageTag().equals("en-US")) {
                localeInfo.mSuggestionFlags = CURRENT;
                mSuggestedLocaleList.add(localeInfo);
            }
        }
        // Set the list which get from resource(locale_config.xml) into configuration
        LocaleList localelist = new LocaleList(locales);
        config.setLocales(localelist);
        when(mActivityService.getConfiguration()).thenReturn(config);
        // Remove the "-" and duplicated items from array: supported list
        Set<String> localeItems = new HashSet<>();
        for (String localeName : localeNames) {
            localeItems.add(localeName.substring(0, localeName.indexOf('-')));
        }
        // Set the suggested list to the suggested preference controller
        ReflectionHelpers.setField(mController, "mLocaleOptions", mSuggestedLocaleList);
        ReflectionHelpers.setField(mController, "mPreferenceCategory", mPreferenceCategory);
        mController.setupPreference(mSuggestedLocaleList, mPreferences);

        assertThat(mPreferenceCategory.getPreferenceCount()).isEqualTo(1);
        assertThat(mPreferenceCategory.getPreference(0).getTitle()).isEqualTo(
                "English (United States)");

        // Check the language of the suggested locale should be "en"
        String suggestedLocaleName = mSuggestedLocaleList.get(0).toString();
        suggestedLocaleName = suggestedLocaleName.substring(0, suggestedLocaleName.indexOf('-'));

        assertThat(suggestedLocaleName).isEqualTo("en");

        // Check the size of list after removing the suggested locale
        int originalSize = localeItems.size();
        localeItems.removeIf(localeInfo -> localeInfo.equals("en"));

        assertThat(localeItems.size() == originalSize - 1).isTrue();
    }
}
