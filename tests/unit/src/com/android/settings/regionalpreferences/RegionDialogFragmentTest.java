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

package com.android.settings.regionalpreferences;

import static com.android.settings.regionalpreferences.RegionDialogFragment.ARG_CALLING_PAGE;
import static com.android.settings.regionalpreferences.RegionDialogFragment.ARG_DIALOG_TYPE;
import static com.android.settings.regionalpreferences.RegionDialogFragment.ARG_REPLACED_TARGET_LOCALE;
import static com.android.settings.regionalpreferences.RegionDialogFragment.ARG_TARGET_LOCALE;
import static com.android.settings.regionalpreferences.RegionDialogFragment.CALLING_PAGE_LANGUAGE_CHOOSE_A_REGION;
import static com.android.settings.regionalpreferences.RegionDialogFragment.DIALOG_CHANGE_PREFERRED_LOCALE_REGION;
import static com.android.settings.regionalpreferences.RegionDialogFragment.DIALOG_CHANGE_SYSTEM_LOCALE_REGION;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.os.Bundle;
import android.os.LocaleList;

import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;

import com.android.internal.app.LocalePicker;
import com.android.internal.app.LocaleStore;

import org.junit.Before;
import org.junit.Test;

import java.util.Locale;

@UiThreadTest
public class RegionDialogFragmentTest {

    private Context mContext;
    private RegionDialogFragment mRegionDialogFragment;
    private static final String DEFAULT_LANGUAGE_1_TAG = "en-US";
    private static final String DEFAULT_LANGUAGE_2_TAG = "ja-JP";
    private static final String DEFAULT_LANGUAGE_3_TAG = "fr-FR";
    private static final String SELECTED_SYSTEM_LANGUAGE_TAG = "en-AU";
    private static final String SELECTED_PREFERRED_LANGUAGE_TAG = "fr-DZ";

    @Before
    public void setUp() throws Exception {
        mContext = ApplicationProvider.getApplicationContext();
        mRegionDialogFragment = new RegionDialogFragment();
        Locale[] newLocales = new Locale[3];
        // LocaleList.getDefault().toLanguageTags is en-US,ja-JP,fr-FR
        newLocales[0] = Locale.forLanguageTag(DEFAULT_LANGUAGE_1_TAG);
        newLocales[1] = Locale.forLanguageTag(DEFAULT_LANGUAGE_2_TAG);
        newLocales[2] = Locale.forLanguageTag(DEFAULT_LANGUAGE_3_TAG);
        LocaleList localeList = new LocaleList(newLocales);
        LocaleList.setDefault(localeList);
        LocalePicker.updateLocales(localeList);
    }

    @Test
    public void updateRegion_changeSystemLocaleRegion_getExpectedDefaultLocaleList() {
        LocaleStore.LocaleInfo targetLocale =
                LocaleStore.getLocaleInfo(Locale.forLanguageTag(SELECTED_SYSTEM_LANGUAGE_TAG));
        Locale replacedLocale = null;
        setArgument(DIALOG_CHANGE_SYSTEM_LOCALE_REGION, targetLocale, replacedLocale);
        RegionDialogFragment.RegionDialogController controller =
                mRegionDialogFragment.getRegionDialogController(mContext, mRegionDialogFragment);
        Bundle arguments = mRegionDialogFragment.getArguments();
        LocaleStore.LocaleInfo localeInfo =
                (LocaleStore.LocaleInfo) arguments.getSerializable(ARG_TARGET_LOCALE);

        controller.updateRegion(localeInfo.getLocale().toLanguageTag());
        String resultForDefaultLocale = Locale.getDefault().toLanguageTag();
        // The new expected LocaleList: en-AU,ja-JP,fr-FR
        String resultForDefaultLocaleList = LocaleList.getDefault().get(0).toLanguageTag();

        assertEquals(SELECTED_SYSTEM_LANGUAGE_TAG, resultForDefaultLocale);
        assertEquals(SELECTED_SYSTEM_LANGUAGE_TAG, resultForDefaultLocaleList);
    }

    @Test
    public void updateRegion_changePreferredLocaleRegion_getExpectedDefaultLocaleList() {
        LocaleStore.LocaleInfo targetLocale =
                LocaleStore.getLocaleInfo(Locale.forLanguageTag(SELECTED_PREFERRED_LANGUAGE_TAG));
        Locale replacedLocale = Locale.forLanguageTag(DEFAULT_LANGUAGE_3_TAG);
        setArgument(DIALOG_CHANGE_PREFERRED_LOCALE_REGION, targetLocale, replacedLocale);
        RegionDialogFragment.RegionDialogController controller =
                mRegionDialogFragment.getRegionDialogController(mContext, mRegionDialogFragment);
        Bundle arguments = mRegionDialogFragment.getArguments();
        LocaleStore.LocaleInfo localeInfo =
                (LocaleStore.LocaleInfo) arguments.getSerializable(ARG_TARGET_LOCALE);

        controller.updateRegion(localeInfo.getLocale().toLanguageTag());
        String resultForDefaultLocale = Locale.getDefault().toLanguageTag();
        // The new expected list: en-US,ja-JP,fr-DZ
        String resultForDefaultLocaleList = LocaleList.getDefault().get(2).toLanguageTag();

        assertEquals(DEFAULT_LANGUAGE_1_TAG, resultForDefaultLocale);
        assertEquals(SELECTED_PREFERRED_LANGUAGE_TAG, resultForDefaultLocaleList);
    }

    private void setArgument(
            int dialogType, LocaleStore.LocaleInfo targetLocale, Locale replacedLocale) {
        Bundle arguments = new Bundle();
        arguments.putInt(ARG_DIALOG_TYPE, dialogType);
        arguments.putInt(ARG_CALLING_PAGE, CALLING_PAGE_LANGUAGE_CHOOSE_A_REGION);
        arguments.putSerializable(ARG_TARGET_LOCALE, targetLocale);
        arguments.putSerializable(ARG_REPLACED_TARGET_LOCALE, replacedLocale);
        mRegionDialogFragment.setArguments(arguments);
    }
}
