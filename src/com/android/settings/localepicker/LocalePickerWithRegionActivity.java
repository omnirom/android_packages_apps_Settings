/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.localepicker;

import static android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT;

import static com.android.settings.regionalpreferences.RegionDialogFragment.ARG_CALLING_PAGE;
import static com.android.settings.regionalpreferences.RegionDialogFragment.CALLING_PAGE_LANGUAGE_CHOOSE_A_REGION;
import static com.android.settings.regionalpreferences.RegionDialogFragment.DIALOG_CHANGE_SYSTEM_LOCALE_REGION;
import static com.android.settings.regionalpreferences.RegionDialogFragment.DIALOG_CHANGE_PREFERRED_LOCALE_REGION;
import static com.android.settings.regionalpreferences.RegionDialogFragment.ARG_DIALOG_TYPE;
import static com.android.settings.regionalpreferences.RegionDialogFragment.ARG_TARGET_LOCALE;
import static com.android.settings.regionalpreferences.RegionDialogFragment.ARG_REPLACED_TARGET_LOCALE;

import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.os.LocaleList;
import android.provider.Settings;
import android.util.Log;
import android.view.MenuItem;
import android.window.OnBackInvokedCallback;

import androidx.core.view.ViewCompat;
import androidx.fragment.app.FragmentManager;

import com.android.internal.app.LocalePickerWithRegion;
import com.android.internal.app.LocaleStore;
import com.android.settings.R;
import com.android.settings.core.SettingsBaseActivity;
import com.android.settings.flags.Flags;
import com.android.settings.regionalpreferences.RegionDialogFragment;

import java.util.Locale;

/**
 * An activity to show the locale picker page.
 *
 * @deprecated use {@link com.android.settings.localepicker.SystemLocalePickerFragment} instead.
 */
@Deprecated
public class LocalePickerWithRegionActivity extends SettingsBaseActivity
        implements LocalePickerWithRegion.LocaleSelectedListener, MenuItem.OnActionExpandListener {
    private static final String TAG = LocalePickerWithRegionActivity.class.getSimpleName();
    private static final String PARENT_FRAGMENT_NAME = "localeListEditor";
    private static final String CHILD_FRAGMENT_NAME = "LocalePickerWithRegion";
    private static final String TAG_DIALOG_CHANGE_REGION = "dialog_change_region";
    private static final int DISPOSE = -1;
    private static final int SHOW_DIALOG_FOR_SYSTEM_LANGUAGE = 0;
    private static final int SHOW_DIALOG_FOR_PREFERRED_LANGUAGE = 1;

    private LocalePickerWithRegion mSelector;

    private final OnBackInvokedCallback mOnBackInvokedCallback = () -> {
        handleBackPressed();
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle(R.string.add_a_language);
        LocaleList explicitLocales = null;
        if (isDeviceDemoMode()) {
            Bundle bundle = getIntent().getExtras();
            explicitLocales = bundle == null
                    ? null
                    : bundle.getParcelable(Settings.EXTRA_EXPLICIT_LOCALES, LocaleList.class);
            Log.i(TAG, "Has explicit locales : " + explicitLocales);
        }
        getOnBackInvokedDispatcher()
                .registerOnBackInvokedCallback(PRIORITY_DEFAULT, mOnBackInvokedCallback);
        mSelector = LocalePickerWithRegion.createLanguagePicker(
                this,
                LocalePickerWithRegionActivity.this,
                false /* translate only */,
                explicitLocales,
                null /* appPackageName */,
                this);

        if (getFragmentManager().findFragmentByTag(CHILD_FRAGMENT_NAME) == null) {
            getFragmentManager()
                    .beginTransaction()
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .replace(R.id.content_frame, mSelector, CHILD_FRAGMENT_NAME)
                    .addToBackStack(PARENT_FRAGMENT_NAME)
                    .commit();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(mOnBackInvokedCallback);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            handleBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onLocaleSelected(LocaleStore.LocaleInfo locale) {
        if (Flags.regionalPreferencesApiEnabled()) {
            int index = indexOfSameLanguageAndScript(locale.getLocale());
            switch(getDialogEvent(index)) {
                case SHOW_DIALOG_FOR_SYSTEM_LANGUAGE:
                    showDialogForSystemLanguage(locale, getSupportFragmentManager());
                    break;
                case SHOW_DIALOG_FOR_PREFERRED_LANGUAGE:
                    Locale replacedLocale = LocaleList.getDefault().get(index);
                    showDialogForPreferredLanguage(
                            locale, replacedLocale, getSupportFragmentManager());
                    break;
                default:
                    dispose(locale);
            }
        } else {
            dispose(locale);
        }
    }

    private static void showDialogForSystemLanguage(
            LocaleStore.LocaleInfo locale, FragmentManager fragmentManager) {
        Bundle args = new Bundle();
        args.putInt(ARG_DIALOG_TYPE, DIALOG_CHANGE_SYSTEM_LOCALE_REGION);
        args.putInt(ARG_CALLING_PAGE, CALLING_PAGE_LANGUAGE_CHOOSE_A_REGION);
        args.putSerializable(ARG_TARGET_LOCALE, locale);
        RegionDialogFragment regionDialogFragment = RegionDialogFragment.newInstance();
        regionDialogFragment.setArguments(args);
        regionDialogFragment.show(fragmentManager, TAG_DIALOG_CHANGE_REGION);
    }

    private static void showDialogForPreferredLanguage(
            LocaleStore.LocaleInfo locale, Locale replacedLocale, FragmentManager fragmentManager) {
        Bundle args = new Bundle();
        args.putInt(ARG_DIALOG_TYPE, DIALOG_CHANGE_PREFERRED_LOCALE_REGION);
        args.putInt(ARG_CALLING_PAGE, CALLING_PAGE_LANGUAGE_CHOOSE_A_REGION);
        args.putSerializable(ARG_TARGET_LOCALE, locale);
        args.putSerializable(ARG_REPLACED_TARGET_LOCALE, replacedLocale);
        RegionDialogFragment regionDialogFragment = RegionDialogFragment.newInstance();
        regionDialogFragment.setArguments(args);
        regionDialogFragment.show(fragmentManager, TAG_DIALOG_CHANGE_REGION);
    }

    private static int getDialogEvent(int index) {
        if (index == -1) {
            return DISPOSE;
        }

        return index == 0
            ? SHOW_DIALOG_FOR_SYSTEM_LANGUAGE
            : SHOW_DIALOG_FOR_PREFERRED_LANGUAGE;
    }

    private static int indexOfSameLanguageAndScript(Locale source) {
        int index = -1;
        LocaleList localeList = LocaleList.getDefault();
        for (int i = 0; i < localeList.size(); i++) {
            Locale target = localeList.get(i);
            if (sameLanguageAndScript(source, target)) {
                index = i;
            }
        }
        return index;
    }

    private static boolean sameLanguageAndScript(Locale source, Locale target) {
        String sourceLanguage = source.getLanguage();
        String targetLanguage = target.getLanguage();
        String sourceLocaleScript = source.getScript();
        String targetLocaleScript = target.getScript();
        if (sourceLanguage.equals(targetLanguage)) {
            if (!sourceLocaleScript.isEmpty() && !targetLocaleScript.isEmpty()) {
                return sourceLocaleScript.equals(targetLocaleScript);
            }
            return true;
        }
        return false;
    }

    private void dispose(LocaleStore.LocaleInfo locale) {
        final Intent intent = new Intent();
        intent.putExtra(LocaleListEditor.INTENT_LOCALE_KEY, locale);
        setResult(RESULT_OK, intent);
        finish();
    }

    private void handleBackPressed() {
        if (getFragmentManager().getBackStackEntryCount() > 1) {
            super.onBackPressed();
        } else {
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    private boolean isDeviceDemoMode() {
        return Settings.Global.getInt(
                getContentResolver(), Settings.Global.DEVICE_DEMO_MODE, 0) == 1;
    }

    @Override
    public boolean onMenuItemActionExpand(MenuItem item) {
        // To prevent a large space on tool bar.
        mAppBarLayout.setExpanded(false /*expanded*/, false /*animate*/);
        // To prevent user can expand the collpasing tool bar view.
        ViewCompat.setNestedScrollingEnabled(mSelector.getListView(), false);
        return true;
    }

    @Override
    public boolean onMenuItemActionCollapse(MenuItem item) {
        mAppBarLayout.setExpanded(false /*expanded*/, false /*animate*/);
        ViewCompat.setNestedScrollingEnabled(mSelector.getListView(), true);
        return true;
    }
}

