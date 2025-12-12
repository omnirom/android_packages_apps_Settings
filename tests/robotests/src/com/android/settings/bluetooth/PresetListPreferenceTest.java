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

package com.android.settings.bluetooth;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;

import android.content.Context;
import android.os.Parcelable;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;

/** Tests for {@link PresetListPreference}. */
@RunWith(RobolectricTestRunner.class)
public class PresetListPreferenceTest {

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    private static final CharSequence[] TEST_ENTRY_NAMES = {"Preset1", "Preset2", "Preset3"};
    private static final CharSequence[] TEST_ENTRY_VALUES = {"1", "2", "3"};
    private static final String TEST_INITIAL_VALUE = "1";
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private PresetListPreference mPreference;

    @Mock
    PresetListPreference.PresetArrayAdapter mAdapter;

    @Before
    public void setUp() {
        mPreference = new PresetListPreference(mContext);
        mPreference.setAdapter(mAdapter);
        mPreference.setEntries(TEST_ENTRY_NAMES);
        mPreference.setEntryValues(TEST_ENTRY_VALUES);
        mPreference.setValue(TEST_INITIAL_VALUE);
    }

    @Test
    public void setValue_refreshAdapter() {
        String testValue = "2";
        mPreference.setValue(testValue);

        verify(mAdapter).setSelectedIndex(Arrays.asList(TEST_ENTRY_VALUES).indexOf(testValue));
    }

    @Test
    public void setEntries_refreshAdapter() {
        CharSequence[] updatedEntries = {"Preset1", "Preset2"};

        mPreference.setEntries(updatedEntries);

        verify(mAdapter).updateList(Arrays.asList(updatedEntries));
    }

    @Test
    public void onSaveAndRestoreInstanceState_entriesAndValueAreRestored() {
        final Parcelable savedState = mPreference.onSaveInstanceState();
        final PresetListPreference newPreference = new PresetListPreference(mContext);
        newPreference.onRestoreInstanceState(savedState);

        assertThat(newPreference.getEntries()).isEqualTo(TEST_ENTRY_NAMES);
        assertThat(newPreference.getEntryValues()).isEqualTo(TEST_ENTRY_VALUES);
        assertThat(newPreference.getValue()).isEqualTo(TEST_INITIAL_VALUE);
    }
}
