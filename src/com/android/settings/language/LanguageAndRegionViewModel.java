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

package com.android.settings.language;

import android.app.Application;

import com.android.internal.app.LocaleStore;

import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

/**
 * A view model to save data from being destroyed and recreated on rotation.
 */
public class LanguageAndRegionViewModel extends AndroidViewModel {
    protected MutableLiveData<Integer> mDialogType = new MutableLiveData<>();
    protected LocaleStore.LocaleInfo mDefaultAfterChange;
    protected LocaleStore.LocaleInfo mSelectedLocaleInfo;
    protected int mMenuItemId;

    public LanguageAndRegionViewModel(@NonNull Application application) {
        super(application);
        mDefaultAfterChange = LocaleStore.getLocaleInfo(Locale.getDefault());
        mSelectedLocaleInfo = LocaleStore.getLocaleInfo(Locale.getDefault());
    }

    public LiveData<Integer> getDialogType() {
        return mDialogType;
    }

    public void setDialogType(Integer type) {
        mDialogType.setValue(type);
    }

    public void clearType() {
        mDialogType.setValue(-1);
    }

    public void setDefaultAfterChange(LocaleStore.LocaleInfo defaultAfterChange) {
        mDefaultAfterChange = defaultAfterChange;
    }

    public LocaleStore.LocaleInfo getDefaultAfterChange() {
        return mDefaultAfterChange;
    }

    public void setSelectedLocaleInfo(LocaleStore.LocaleInfo selectedLocaleInfo) {
        mSelectedLocaleInfo = selectedLocaleInfo;
    }

    public LocaleStore.LocaleInfo getSelectedLocaleInfo() {
        return mSelectedLocaleInfo;
    }

    public void setMenuItemId(int menuItemId) {
        mMenuItemId = menuItemId;
    }

    public int getMenuItemId() {
        return mMenuItemId;
    }
}
