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
import static com.android.settings.localepicker.LocaleDialogFragment.DIALOG_NOT_AVAILABLE_LOCALE_WITH_CANCEL;
import static com.android.settings.localepicker.LocaleDialogFragment.DIALOG_REMOVE_AND_CHANGE_SYSTEM_LOCALE;
import static com.android.settings.localepicker.LocaleDialogFragment.DIALOG_REMOVE_LOCALE;
import static com.android.settings.localepicker.LocaleUtils.getUserLocaleList;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.LocaleList;
import android.util.ArrayMap;
import android.util.Log;

import com.android.internal.app.LocalePicker;
import com.android.internal.app.LocaleStore;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.localepicker.NotificationController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.R;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.widget.OrderMenuPreference;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

public class UserPreferredLocalePreferenceController extends BasePreferenceController implements
        LifecycleObserver, DefaultLifecycleObserver {
    private static final String TAG = "UserPreferredLocalePreferenceController";
    private static final String KEY_PREFERENCE_CATEGORY_LANGUAGES =
            "languages_category";
    private static final String KEY_PREFERENCE_USER_PREFERRED_LOCALE_LIST =
            "user_preferred_locale_list";

    private PreferenceCategory mPreferenceCategory;
    private Map<String, OrderMenuPreference> mPreferences;
    private MetricsFeatureProvider mMetricsFeatureProvider;
    private List<LocaleStore.LocaleInfo> mUpdatedLocaleInfoList;
    private LocaleStore.LocaleInfo mSelectedLocaleInfo;
    private int mMenuItemId;
    private LanguageAndRegionViewModel mViewModel;

    @SuppressWarnings("NullAway")
    public UserPreferredLocalePreferenceController(@NonNull Context context,
            @NonNull String preferenceKey) {
        super(context, preferenceKey);
        mPreferences = new ArrayMap<>();
        mMetricsFeatureProvider = FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
    }

    @SuppressWarnings("NullAway")
    public UserPreferredLocalePreferenceController(@NonNull Context context,
            @NonNull String preferenceKey, @NonNull Lifecycle lifecycle,
            @NonNull LifecycleOwner lifecycleOwner) {
        super(context, preferenceKey);
        mPreferences = new ArrayMap<>();
        mMetricsFeatureProvider = FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
        mViewModel = new ViewModelProvider((ViewModelStoreOwner) lifecycleOwner).get(
                LanguageAndRegionViewModel.class);
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
        Log.d(TAG, "displayPreference");
        mPreferenceCategory = screen.findPreference(KEY_PREFERENCE_CATEGORY_LANGUAGES);
        updatePreferences();
    }

    void updatePreferences() {
        if (mPreferenceCategory == null) {
            Log.d(TAG, "updatePreferences, mPreferenceCategory is null");
            return;
        }

        final Map<String, OrderMenuPreference> existingPreferences = mPreferences;
        mPreferences = new ArrayMap<>();
        setupPreference(existingPreferences);

        for (OrderMenuPreference pref : existingPreferences.values()) {
            mPreferenceCategory.removePreference(pref);
        }
    }

    @VisibleForTesting
    void setupPreference(Map<String, OrderMenuPreference> existingPreferences) {
        Log.d(TAG, "setupPreference");
        List<LocaleStore.LocaleInfo> localeInfoList = getUserLocaleList();
        int listSize = localeInfoList.size();
        for (int i = 0; i < listSize; i++) {
            LocaleStore.LocaleInfo localeInfo = localeInfoList.get(i);
            OrderMenuPreference pref = existingPreferences.remove(localeInfo.getId());
            if (pref == null) {
                pref = new OrderMenuPreference(mContext);
                mPreferenceCategory.addPreference(pref);
            }
            String localeName = localeInfo.getFullNameNative();
            pref.setTitle(localeName);
            String summary = null;
            if (!localeInfo.isTranslated()) {
                summary = mContext.getString(R.string.locale_not_translated);
            } else if (localeInfo.getLocale().equals(Locale.getDefault())) {
                summary = mContext.getString(R.string.desc_current_default_language);
            }
            pref.setSummary(summary);
            pref.setKey(localeInfo.toString());
            if (listSize == 1) {
                pref.setMenuButtonVisible(false);
            } else {
                int menuId = R.menu.preferred_locale_menu;
                if (i == 0) {
                    menuId = R.menu.preferred_locale_menu_down;
                } else if (i == 1) {
                    menuId = listSize == 2
                            ? R.menu.preferred_locale_menu_up
                            : R.menu.preferred_locale_menu_up_down;
                } else if (i == (listSize - 1)) {
                    menuId = R.menu.preferred_locale_menu_top;
                }
                pref.setMenuButtonContentDescription(
                        mContext.getString(R.string.action_label_menu_option,
                                localeInfo.getFullNameNative()));
                pref.setMenuResId(menuId);
                pref.setMenuItemClickListener((item, preference) -> {
                    int menuItemId = item.getItemId();
                    mMenuItemId = menuItemId;
                    if (menuItemId == R.id.move_top || menuItemId == R.id.move_up
                            || menuItemId == R.id.move_down) {
                        LocaleStore.LocaleInfo saved = localeInfo;
                        mSelectedLocaleInfo = saved;
                        int position = localeInfoList.indexOf(localeInfo);
                        localeInfoList.remove(position);
                        int toPosition = 0; // menuItemId is R.id.move_top
                        if (menuItemId == R.id.move_up) {
                            toPosition = position - 1;
                        } else if (menuItemId == R.id.move_down) {
                            toPosition = position + 1;
                        }
                        localeInfoList.add(toPosition, saved);
                        mUpdatedLocaleInfoList = localeInfoList;
                        showConfirmDialog(localeInfoList);
                        mMetricsFeatureProvider.action(mContext,
                                SettingsEnums.ACTION_REORDER_LANGUAGE);
                    } else {
                        NotificationController controller = NotificationController.getInstance(
                                mContext);
                        controller.removeNotificationInfo(
                                localeInfo.getLocale().toLanguageTag());
                        mSelectedLocaleInfo = localeInfo;
                        int position = localeInfoList.indexOf(localeInfo);
                        localeInfoList.remove(position);
                        mUpdatedLocaleInfoList = localeInfoList;
                        if (localeInfo.isTranslated() && (position == 0 || Locale.getDefault()
                            .equals(localeInfo.getLocale()))) {
                            LocaleStore.LocaleInfo defaultAfterChange =
                                    localeInfoList.stream().filter(
                                            first -> first.isTranslated()).findFirst().orElse(
                                            localeInfoList.get(0));
                            mViewModel.setDefaultAfterChange(defaultAfterChange);
                            mViewModel.setSelectedLocaleInfo(mSelectedLocaleInfo);
                            mViewModel.setDialogType(DIALOG_REMOVE_AND_CHANGE_SYSTEM_LOCALE);
                        } else {
                            mViewModel.setDefaultAfterChange(localeInfo);
                            mViewModel.setDialogType(DIALOG_REMOVE_LOCALE);
                        }
                    }
                    updatePreferences();
                    return true;
                });
            }
            pref.setNumber(i + 1);
            pref.setShowIconsInPopupMenu(true);
            mPreferences.put(localeInfo.getId(), pref);
        }
    }

    protected void showConfirmDialog(List<LocaleStore.LocaleInfo> localeInfoList) {
        // localeInfoList is the list after user reorders
        LocaleStore.LocaleInfo firstLocaleInfo = localeInfoList.get(0);
        // LocalePicker.getLocales() is the list before user reorders
        Locale defaultBeforeChange = LocalePicker.getLocales().get(0);
        LocaleStore.LocaleInfo defaultAfterChange = localeInfoList.stream()
            .filter(i -> i.isTranslated()).findFirst().orElse(firstLocaleInfo);
        boolean isSelectedLocaleInfoTranslated = mSelectedLocaleInfo.isTranslated();
        boolean isSameDefaultInList = Locale.getDefault().equals(defaultAfterChange.getLocale());
        if (localeInfoList.size() == 2) {
            boolean allTranslated = localeInfoList.stream()
                .allMatch(LocaleStore.LocaleInfo::isTranslated);
            if (allTranslated) {
                // All languages in the list are translated.
                setViewModelData(defaultAfterChange, mSelectedLocaleInfo, mMenuItemId,
                        DIALOG_CONFIRM_SYSTEM_DEFAULT);
            } else {
                // One is translated, the other is non-translated.
                if (mMenuItemId == R.id.move_up) {
                    if (!isSelectedLocaleInfoTranslated) {
                        setViewModelData(mSelectedLocaleInfo, mSelectedLocaleInfo, mMenuItemId,
                                DIALOG_NOT_AVAILABLE_LOCALE_WITH_CANCEL);
                    } else {
                        doTheUpdate();
                    }
                } else {
                    if (isSameDefaultInList) {
                        doTheUpdate();
                    } else {
                        setViewModelData(defaultAfterChange, mSelectedLocaleInfo, mMenuItemId,
                                DIALOG_NOT_AVAILABLE_LOCALE_WITH_CANCEL);
                    }
                }
            }
        } else {
            boolean allNotTranslated = localeInfoList.stream()
                .noneMatch(LocaleStore.LocaleInfo::isTranslated);
            boolean isSystemDefaultChanged =
                defaultBeforeChange != null && !defaultBeforeChange.equals(
                    defaultAfterChange.getLocale());
            if (allNotTranslated) {
                if (isSystemDefaultChanged) {
                    setViewModelData(defaultAfterChange, mSelectedLocaleInfo, mMenuItemId,
                            DIALOG_NOT_AVAILABLE_LOCALE_WITH_CANCEL);
                } else {
                    doTheUpdate();
                }
            } else {
                if (isSameDefaultInList && (
                    isSelectedLocaleInfoTranslated ||
                        (defaultBeforeChange.equals(mSelectedLocaleInfo.getLocale())
                            && firstLocaleInfo.getLocale().equals(defaultAfterChange.getLocale()))
                        || firstLocaleInfo.getLocale().equals(defaultBeforeChange))) {
                    // Case 1: unavailable + supported 1 + supported 2,	Move supported 1 to top
                    // Case 2: supported + unavailable 1 + unavailable 2, Move supported to last
                    // Case 3: unavailable 1 + supported + unavailable 2, Move supported to top
                    // Case 4: unavailable 1 + supported + unavailable 2, Move unavailable 1 to last
                    // Case 5: unavailable 1 + unavailable 2 + supported, Move supported to top
                    doTheUpdate();
                } else if (isSelectedLocaleInfoTranslated && isSystemDefaultChanged) {
                    // System language is changed
                    setViewModelData(defaultAfterChange, mSelectedLocaleInfo, mMenuItemId,
                            DIALOG_CONFIRM_SYSTEM_DEFAULT);
                } else {
                    setViewModelData(
                            !isSystemDefaultChanged ? mSelectedLocaleInfo : firstLocaleInfo,
                            mSelectedLocaleInfo, mMenuItemId,
                            DIALOG_NOT_AVAILABLE_LOCALE_WITH_CANCEL);
                }
            }
        }
    }

    private void setViewModelData(LocaleStore.LocaleInfo defaultAfterChange,
            LocaleStore.LocaleInfo selectedLocaleInfo, int menuItemId, Integer type) {
        mViewModel.setDefaultAfterChange(defaultAfterChange);
        mViewModel.setSelectedLocaleInfo(selectedLocaleInfo);
        mViewModel.setMenuItemId(menuItemId);
        mViewModel.setDialogType(type);
    }

    public void doTheUpdate() {
        LocaleList localeList = new LocaleList(mUpdatedLocaleInfoList.stream()
                .map(LocaleStore.LocaleInfo::getLocale)
                .toArray(Locale[]::new));
        // Update the Settings application to make things feel more responsive.
        LocaleList.setDefault(localeList);
        // Update the system.
        LocalePicker.updateLocales(localeList);
        updatePreferences();
    }

    protected List<LocaleStore.LocaleInfo> setUpdatedLocaleList(
            LocaleStore.LocaleInfo selectedLocaleInfo, int menuId) {
        mUpdatedLocaleInfoList = getUserLocaleList();
        if (menuId == R.id.move_top || menuId == R.id.move_up || menuId == R.id.move_down) {
            LocaleStore.LocaleInfo saved = selectedLocaleInfo;
            mSelectedLocaleInfo = saved;
            for (int i = 0; i < mUpdatedLocaleInfoList.size(); i++) {
                if (mUpdatedLocaleInfoList.get(i).toString().equals(
                        selectedLocaleInfo.toString())) {
                    mUpdatedLocaleInfoList.remove(i);
                    int toPosition = 0; // menuId is R.id.move_top
                    if (menuId == R.id.move_up) {
                        toPosition = i - 1;
                    } else if (menuId == R.id.move_down) {
                        toPosition = i + 1;
                    }
                    mUpdatedLocaleInfoList.add(toPosition, saved);
                    break;
                }
            }
        } else {
            for (int i = 0; i < mUpdatedLocaleInfoList.size(); i++) {
                if (mUpdatedLocaleInfoList.get(i).toString().equals(
                        selectedLocaleInfo.toString())) {
                    mUpdatedLocaleInfoList.remove(i);
                    break;
                }
            }
        }
        return mUpdatedLocaleInfoList;
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public @NonNull String getPreferenceKey() {
        return KEY_PREFERENCE_USER_PREFERRED_LOCALE_LIST;
    }
}
