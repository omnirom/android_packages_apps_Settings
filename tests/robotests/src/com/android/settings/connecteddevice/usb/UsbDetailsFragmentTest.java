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

package com.android.settings.connecteddevice.usb;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.lifecycle.Lifecycle;

import com.android.settingslib.core.AbstractPreferenceController;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class UsbDetailsFragmentTest {

    @Test
    public void isAuthenticated_whenSet_returnsTrue() {
        try (FragmentScenario<FakeUsbDetailsFragment> scenario =
                FragmentScenario.launchInContainer(FakeUsbDetailsFragment.class)) {
            scenario.onFragment(
                    fragment -> {
                        fragment.setUserAuthenticated(true);

                        assertThat(fragment.isUserAuthenticated()).isTrue();
                    });
        }
    }

    @Test
    public void isAuthenticated_whenStoppedAndStarted_isFalse() {
        try (FragmentScenario<FakeUsbDetailsFragment> scenario =
                FragmentScenario.launchInContainer(FakeUsbDetailsFragment.class)) {
            scenario.moveToState(Lifecycle.State.RESUMED);
            scenario.onFragment(
                    fragment -> {
                        fragment.setUserAuthenticated(true);
                    });

            // Simulate stopping the fragment (moves through onStop).
            scenario.moveToState(Lifecycle.State.CREATED);

            // Simulate starting the fragment again (moves through onStart).
            scenario.moveToState(Lifecycle.State.RESUMED);

            scenario.onFragment(
                    fragment -> {
                        assertThat(fragment.isUserAuthenticated()).isFalse();
                    });
        }
    }

    @Test
    public void isAuthenticated_whenRecreated_staysTrue() {
        try (FragmentScenario<FakeUsbDetailsFragment> scenario =
                FragmentScenario.launchInContainer(FakeUsbDetailsFragment.class)) {
            scenario.onFragment(
                    fragment -> {
                        fragment.setUserAuthenticated(true);
                    });

            scenario.recreate();

            scenario.onFragment(
                    fragment -> {
                        assertThat(fragment.isUserAuthenticated()).isTrue();
                    });
        }
    }

    /**
     * Fake wrapper over the fragment to avoid instantiating dependencies that can't be used in
     * tests.
     */
    public static class FakeUsbDetailsFragment extends UsbDetailsFragment {
        @Override
        protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
            return new ArrayList<>();
        }
    }
}
