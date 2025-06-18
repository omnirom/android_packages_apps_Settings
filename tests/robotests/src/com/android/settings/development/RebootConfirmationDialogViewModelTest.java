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

package com.android.settings.development;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class RebootConfirmationDialogViewModelTest {

    @Mock
    private RebootConfirmationDialogHost mHost;

    private RebootConfirmationDialogViewModel mViewModel;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mViewModel = new RebootConfirmationDialogViewModel();
    }

    @Test
    public void getHost_returnsSetHost() {
        mViewModel.setHost(mHost);
        assertThat(mViewModel.getHost()).isEqualTo(mHost);
    }

    @Test
    public void getMessageId_returnsSetMessageId() {
        mViewModel.setMessageId(123);
        assertThat(mViewModel.getMessageId()).isEqualTo(123);
    }

    @Test
    public void getCancelButtonId_returnsSetCancelButtonId() {
        mViewModel.setCancelButtonId(456);
        assertThat(mViewModel.getCancelButtonId()).isEqualTo(456);
    }
}
