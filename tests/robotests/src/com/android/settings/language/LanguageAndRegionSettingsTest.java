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

import static com.android.settings.localepicker.LocaleDialogFragment.DIALOG_CONFIRM_SYSTEM_DEFAULT;
import static com.android.settings.localepicker.LocaleDialogFragment.DIALOG_NOT_AVAILABLE_LOCALE;
import static com.android.settings.localepicker.LocaleDialogFragment.DIALOG_REMOVE_LOCALE;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.LocaleList;

import android.app.IActivityManager;
import android.util.ArrayMap;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.test.core.app.ApplicationProvider;

import com.android.internal.app.LocaleStore;
import com.android.settings.R;
import com.android.settings.localepicker.LocaleDialogFragment;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowActivityManager;
import com.android.settings.testutils.shadow.ShadowAlertDialogCompat;
import com.android.settings.testutils.shadow.ShadowFragment;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.widget.OrderMenuPreference;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        ShadowAlertDialogCompat.class,
        ShadowActivityManager.class,
        ShadowFragment.class,
})
public class LanguageAndRegionSettingsTest {
    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    private static final String ARG_DIALOG_TYPE = "arg_dialog_type";
    private static final String
            ARG_SHOW_DIALOG_FOR_NOT_TRANSLATED = "arg_show_dialog_for_not_translated";
    private static final String TAG_DIALOG_NOT_AVAILABLE = "dialog_not_available_locale";

    private FragmentActivity mActivity;
    private LanguageAndRegionSettings mFragment;
    private Context mContext;
    private Intent mIntent = new Intent();
    private List<LocaleStore.LocaleInfo> mLocaleInfoList;
    private LocaleList mLocaleList;

    @Mock
    private UserPreferredLocalePreferenceController mController;
    @Mock
    private FragmentManager mFragmentManager;
    @Mock
    private FragmentTransaction mFragmentTransaction;
    @Mock
    private IActivityManager mActivityService;
    @Mock
    private MetricsFeatureProvider mMetricsFeatureProvider;

    @Before
    public void setUp() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        mContext = spy(context);
        mFragment = spy(new LanguageAndRegionSettings());
        when(mFragment.getContext()).thenReturn(mContext);
        mActivity = spy(Robolectric.buildActivity(FragmentActivity.class).get());
        when(mFragment.getActivity()).thenReturn(mActivity);
        ReflectionHelpers.setField(mFragment, "mRestrictionsManager",
                context.getSystemService(Context.RESTRICTIONS_SERVICE));
        ReflectionHelpers.setField(mFragment, "mUserManager",
                context.getSystemService(Context.USER_SERVICE));
        ReflectionHelpers.setField(mFragment, "mFragmentManager", mFragmentManager);
        ReflectionHelpers.setField(mFragment, "mMetricsFeatureProvider", mMetricsFeatureProvider);
        ReflectionHelpers.setField(mFragment, "mUserPreferredLocalePreferenceController",
                mController);
        doReturn(mFragmentManager).when(mFragment).getChildFragmentManager();
        when(mFragmentManager.beginTransaction()).thenReturn(mFragmentTransaction);
        ShadowActivityManager.setService(mActivityService);
        final Configuration config = new Configuration();
        setUpLocaleConditions();
        config.setLocales(mLocaleList);
        when(mActivityService.getConfiguration()).thenReturn(config);
        FakeFeatureFactory.setupForTest();
    }

    @After
    public void tearDown() throws Exception {
        ShadowAlertDialogCompat.reset();
    }

    private void setUpLocaleConditions() {
        mLocaleInfoList = new ArrayList<>();
        mLocaleInfoList.add(LocaleStore.getLocaleInfo(Locale.forLanguageTag("en-US")));
        mLocaleInfoList.add(LocaleStore.getLocaleInfo(Locale.forLanguageTag("ak-GH")));
        mLocaleInfoList.add(LocaleStore.getLocaleInfo(Locale.forLanguageTag("es-US")));
        final Locale[] newList = new Locale[mLocaleInfoList.size()];
        for (int i = 0; i < mLocaleInfoList.size(); i++) {
            final LocaleStore.LocaleInfo li = mLocaleInfoList.get(i);
            newList[i] = li.getLocale();
        }
        mLocaleList = new LocaleList(newList);
    }

    @Test
    public void onActivityResult_ResultCodeIsOk_showNotAvailableDialog() {
        Bundle bundle = new Bundle();
        bundle.putInt(ARG_DIALOG_TYPE, DIALOG_CONFIRM_SYSTEM_DEFAULT);
        bundle.putInt(LocaleDialogFragment.ARG_MENU_ITEM_ID, R.id.move_down);
        bundle.putSerializable(LocaleDialogFragment.ARG_SELECTED_LOCALE,
                LocaleStore.getLocaleInfo(Locale.forLanguageTag("en-US")));
        mIntent.putExtras(bundle);
        mIntent.putExtra(ARG_SHOW_DIALOG_FOR_NOT_TRANSLATED, true);
        setUpLocaleConditions();
        mFragment.onActivityResult(DIALOG_CONFIRM_SYSTEM_DEFAULT, Activity.RESULT_OK, mIntent);

        verify(mFragmentTransaction).add(any(LocaleDialogFragment.class),
                eq(TAG_DIALOG_NOT_AVAILABLE));
    }

    @Test
    public void onActivityResult_ResultCodeIsOk_removeDialog_updatePreference() {
        Bundle bundle = new Bundle();
        bundle.putInt(ARG_DIALOG_TYPE, DIALOG_REMOVE_LOCALE);
        bundle.putInt(LocaleDialogFragment.ARG_MENU_ITEM_ID, R.id.move_down);
        bundle.putSerializable(LocaleDialogFragment.ARG_SELECTED_LOCALE,
                LocaleStore.getLocaleInfo(Locale.forLanguageTag("en-US")));
        mIntent.putExtras(bundle);
        mIntent.putExtra(ARG_SHOW_DIALOG_FOR_NOT_TRANSLATED, false);
        setUpLocaleConditions();
        mFragment.onActivityResult(DIALOG_REMOVE_LOCALE, Activity.RESULT_OK, mIntent);

        verify(mController).doTheUpdate();
        verify(mController).updatePreferences();
    }

    @Test
    public void onActivityResult_ResultCodeIsOk_notAvailableDialog_updatePreference() {
        Bundle bundle = new Bundle();
        bundle.putInt(ARG_DIALOG_TYPE, DIALOG_NOT_AVAILABLE_LOCALE);
        bundle.putInt(LocaleDialogFragment.ARG_MENU_ITEM_ID, R.id.move_down);
        bundle.putSerializable(LocaleDialogFragment.ARG_SELECTED_LOCALE,
                LocaleStore.getLocaleInfo(Locale.forLanguageTag("ak-GH")));
        mIntent.putExtras(bundle);
        mIntent.putExtra(ARG_SHOW_DIALOG_FOR_NOT_TRANSLATED, false);
        setUpLocaleConditions();
        mFragment.onActivityResult(DIALOG_NOT_AVAILABLE_LOCALE, Activity.RESULT_OK, mIntent);

        verify(mController).doTheUpdate();
        verify(mController).updatePreferences();
    }
}
