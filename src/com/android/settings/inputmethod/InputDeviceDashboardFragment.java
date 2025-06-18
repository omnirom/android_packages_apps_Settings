/*
 * Copyright 2025 The Android Open Source Project
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

package com.android.settings.inputmethod;

import android.hardware.input.InputManager;
import android.os.Bundle;

import androidx.annotation.NonNull;

import com.android.internal.util.Preconditions;
import com.android.settings.dashboard.DashboardFragment;

public abstract class InputDeviceDashboardFragment extends DashboardFragment
        implements InputManager.InputDeviceListener {
    private InputManager mInputManager;

    @Override
    public void onCreate(@NonNull Bundle icicle) {
        super.onCreate(icicle);
        mInputManager = Preconditions.checkNotNull(getActivity()
                .getSystemService(InputManager.class));
    }

    @Override
    public void onResume() {
        super.onResume();
        finishEarlyIfNeeded();
        mInputManager.registerInputDeviceListener(this /* listener */, null /* handler */);
    }

    @Override
    public void onPause() {
        super.onPause();
        mInputManager.unregisterInputDeviceListener(this /* listener */);
    }

    @Override
    public void onInputDeviceAdded(int deviceId) {
        finishEarlyIfNeeded();
    }

    @Override
    public void onInputDeviceRemoved(int deviceId) {
        finishEarlyIfNeeded();
    }

    @Override
    public void onInputDeviceChanged(int deviceId) {
        finishEarlyIfNeeded();
    }

    private void finishEarlyIfNeeded() {
        if (getActivity() == null) {
            return;
        }
        if (needToFinishEarly()) {
            getActivity().finish();
        }
    }

    /**
     * Returns whether the fragment should still be displayed given the input devices that are
     * currently connected.
     */
    protected abstract boolean needToFinishEarly();

    protected static boolean isTouchpadDetached() {
        return !InputPeripheralsSettingsUtils.isTouchpad();
    }

    protected static boolean isMouseDetached() {
        return !InputPeripheralsSettingsUtils.isMouse();
    }

    protected static boolean isHardKeyboardDetached() {
        return !InputPeripheralsSettingsUtils.isHardKeyboard();
    }
}
