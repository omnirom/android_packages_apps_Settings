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

package com.android.settings.applications.appcompat;

import static android.content.pm.PackageManager.USER_MIN_ASPECT_RATIO_3_2;
import static android.content.pm.PackageManager.USER_MIN_ASPECT_RATIO_FULLSCREEN;

import static com.android.settings.applications.appcompat.UserAspectRatioRestoreStorage.ASPECT_RATIO_STAGED_DATA_PREFS;
import static com.android.settings.applications.appcompat.UserAspectRatioRestoreStorage.KEY_STAGED_DATA_TIME;
import static com.android.window.flags.Flags.FLAG_RESTORE_USER_ASPECT_RATIO_SETTINGS_USING_SERVICE;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.platform.test.annotations.RequiresFlagsDisabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.testutils.FakeInstantSource;
import com.android.settings.testutils.FakeSharedPreferences;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import java.io.File;
import java.time.Duration;

/**
 * Unit tests for the {@link UserAspectRatioRestoreStorage}.
 *
 * To run this test: atest SettingsUnitTests:UserAspectRatioRestoreStorageTest
 */
@RunWith(AndroidJUnit4.class)
public class UserAspectRatioRestoreStorageTest {
    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    private static final String DEFAULT_PACKAGE_NAME = "com.android.testapp";
    private static final String OTHER_PACKAGE_NAME = "com.android.anotherapp";

    private static final int DEFAULT_USER_ID = 0;

    private UserAspectRatioRestoreStorage mRestoreStorage;

    private Context mContext;
    private FakeInstantSource mInstantSource;
    private FakeSharedPreferences mFakeSharedPreferences;

    @Before
    public void setUp() throws Exception {
        mContext = spy(ApplicationProvider.getApplicationContext());
        mInstantSource = new FakeInstantSource();
        setupMockSharedPreferences();
        mRestoreStorage = new UserAspectRatioRestoreStorage(mContext, DEFAULT_USER_ID,
                mInstantSource);
    }

    @Test
    @RequiresFlagsDisabled(FLAG_RESTORE_USER_ASPECT_RATIO_SETTINGS_USING_SERVICE)
    public void testCreateRestoreStorage_sharedPreferencesCreated() {
        verify(mContext).createDeviceProtectedStorageContext();
        ArgumentCaptor<File> fileCaptor = ArgumentCaptor.forClass(File.class);
        verify(mContext).getSharedPreferences(fileCaptor.capture(), eq(Context.MODE_PRIVATE));
        assertEquals(mContext.getPackageName() + "." + DEFAULT_USER_ID + "."
                + ASPECT_RATIO_STAGED_DATA_PREFS, fileCaptor.getValue().getName());
    }

    @Test
    @RequiresFlagsDisabled(FLAG_RESTORE_USER_ASPECT_RATIO_SETTINGS_USING_SERVICE)
    public void testRestoreCompleted_currentTimeStoredInSharedPreferences() {
        mRestoreStorage.restoreCompleted();

        assertEquals(mInstantSource.millis(), mFakeSharedPreferences.getLong(
                KEY_STAGED_DATA_TIME, -1));
    }

    @Test
    @RequiresFlagsDisabled(FLAG_RESTORE_USER_ASPECT_RATIO_SETTINGS_USING_SERVICE)
    public void testStorePackageAndUserAspectRatio() {
        mRestoreStorage.storePackageAndUserAspectRatio(DEFAULT_PACKAGE_NAME,
                USER_MIN_ASPECT_RATIO_FULLSCREEN);

        assertEquals(USER_MIN_ASPECT_RATIO_FULLSCREEN,
                mFakeSharedPreferences.getInt(DEFAULT_PACKAGE_NAME, -1));
    }

    @Test
    @RequiresFlagsDisabled(FLAG_RESTORE_USER_ASPECT_RATIO_SETTINGS_USING_SERVICE)
    public void testGetAndRemoveUserAspectRatioForPackage_dataLessThanAWeekOld_dataReturned() {
        mRestoreStorage.storePackageAndUserAspectRatio(DEFAULT_PACKAGE_NAME,
                USER_MIN_ASPECT_RATIO_FULLSCREEN);
        mRestoreStorage.restoreCompleted();

        mInstantSource.advanceByMillis(Duration.ofDays(6).toMillis());

        final int aspectRatio = mRestoreStorage
                .getAndRemoveUserAspectRatioForPackage(DEFAULT_PACKAGE_NAME);
        assertEquals(USER_MIN_ASPECT_RATIO_FULLSCREEN, aspectRatio);
    }

    @Test
    @RequiresFlagsDisabled(FLAG_RESTORE_USER_ASPECT_RATIO_SETTINGS_USING_SERVICE)
    public void testGetAndRemoveUserAspectRatioForPackage_weekOldDataCleanedUpAndNothingReturned() {
        mRestoreStorage.storePackageAndUserAspectRatio(DEFAULT_PACKAGE_NAME,
                USER_MIN_ASPECT_RATIO_FULLSCREEN);
        mRestoreStorage.storePackageAndUserAspectRatio(OTHER_PACKAGE_NAME,
                USER_MIN_ASPECT_RATIO_3_2);
        mRestoreStorage.restoreCompleted();

        mInstantSource.advanceByMillis(Duration.ofDays(8).toMillis());

        mRestoreStorage.getAndRemoveUserAspectRatioForPackage(DEFAULT_PACKAGE_NAME);

        assertFalse(mFakeSharedPreferences.contains(DEFAULT_PACKAGE_NAME));
        assertFalse(mFakeSharedPreferences.contains(OTHER_PACKAGE_NAME));
    }

    private void setupMockSharedPreferences() {
        mFakeSharedPreferences = new FakeSharedPreferences();
        doReturn(mContext).when(mContext).createDeviceProtectedStorageContext();
        doReturn(mFakeSharedPreferences).when(mContext).getSharedPreferences(any(File.class),
                eq(Context.MODE_PRIVATE));
    }
}
