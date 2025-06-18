/*
 * Copyright (C) 2024 The Android Open Source Project
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
package com.android.settings.connecteddevice.display;

import static com.android.settings.connecteddevice.display.ExternalDisplaySettingsConfiguration.VIRTUAL_DISPLAY_PACKAGE_NAME_SYSTEM_PROPERTY;
import static com.android.settings.flags.Flags.FLAG_DISPLAY_SIZE_CONNECTED_DISPLAY_SETTING;
import static com.android.settings.flags.Flags.FLAG_DISPLAY_TOPOLOGY_PANE_IN_DISPLAY_LIST;
import static com.android.settings.flags.Flags.FLAG_RESOLUTION_AND_ENABLE_CONNECTED_DISPLAY_SETTING;
import static com.android.settings.flags.Flags.FLAG_ROTATION_CONNECTED_DISPLAY_SETTING;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.view.Display.Mode;

import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.connecteddevice.display.ExternalDisplaySettingsConfiguration.DisplayListener;
import com.android.settings.flags.FakeFeatureFlagsImpl;

import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayDeque;
import java.util.List;

public class ExternalDisplayTestBase {
    static final int EXTERNAL_DISPLAY_ID = 1;
    static final int OVERLAY_DISPLAY_ID = 2;

    @Mock
    ExternalDisplaySettingsConfiguration.Injector mMockedInjector;
    @Mock
    Resources mResources;
    FakeFeatureFlagsImpl mFlags = new FakeFeatureFlagsImpl();
    Context mContext;
    DisplayListener mListener;
    TestHandler mHandler;
    PreferenceManager mPreferenceManager;
    PreferenceScreen mPreferenceScreen;
    List<DisplayDevice> mDisplays;

    static class TestHandler extends Handler {
        private final ArrayDeque<Message> mPending = new ArrayDeque<>();
        private final Handler mSubhandler;

        TestHandler(Handler subhandler) {
            mSubhandler = subhandler;
        }

        ArrayDeque<Message> getPendingMessages() {
            return mPending;
        }

        /**
         * Schedules to send the message upon next invocation of {@link #flush()}. This ignores the
         * time argument since our code doesn't meaningfully use it, but this is the most convenient
         * way to intercept both Message and Callback objects synchronously.
         */
        @Override
        public boolean sendMessageAtTime(Message msg, long uptimeMillis) {
            mPending.add(msg);
            return true;
        }

        void flush() {
            for (var msg : mPending) {
                mSubhandler.sendMessage(msg);
            }
            mPending.clear();
            shadowOf(mSubhandler.getLooper()).idle();
        }
    }

    /**
     * Setup.
     */
    @Before
    public void setUp() throws RemoteException {
        MockitoAnnotations.initMocks(this);
        mContext = spy(ApplicationProvider.getApplicationContext());
        mResources = spy(mContext.getResources());
        doReturn(mResources).when(mContext).getResources();
        mPreferenceManager = new PreferenceManager(mContext);
        mPreferenceScreen = mPreferenceManager.createPreferenceScreen(mContext);
        mFlags.setFlag(FLAG_DISPLAY_TOPOLOGY_PANE_IN_DISPLAY_LIST, false);
        mFlags.setFlag(FLAG_ROTATION_CONNECTED_DISPLAY_SETTING, true);
        mFlags.setFlag(FLAG_RESOLUTION_AND_ENABLE_CONNECTED_DISPLAY_SETTING, true);
        mFlags.setFlag(FLAG_DISPLAY_SIZE_CONNECTED_DISPLAY_SETTING, true);
        mDisplays = List.of(
                createExternalDisplay(DisplayIsEnabled.YES),
                createOverlayDisplay(DisplayIsEnabled.YES));
        doReturn(mDisplays).when(mMockedInjector).getConnectedDisplays();
        for (var display : mDisplays) {
            doReturn(display).when(mMockedInjector).getDisplay(display.getId());
        }
        doReturn(mFlags).when(mMockedInjector).getFlags();
        mHandler = new TestHandler(mContext.getMainThreadHandler());
        doReturn(mHandler).when(mMockedInjector).getHandler();
        doReturn("").when(mMockedInjector).getSystemProperty(
                VIRTUAL_DISPLAY_PACKAGE_NAME_SYSTEM_PROPERTY);
        doReturn(true).when(mMockedInjector).isModeLimitForExternalDisplayEnabled();
        doAnswer((arg) -> {
            mListener = arg.getArgument(0);
            return null;
        }).when(mMockedInjector).registerDisplayListener(any());
        doReturn(0).when(mMockedInjector).getDisplayUserRotation(anyInt());
        doReturn(mContext).when(mMockedInjector).getContext();
    }

    DisplayDevice createExternalDisplay(DisplayIsEnabled isEnabled) {
        int displayId = EXTERNAL_DISPLAY_ID;
        var supportedModes = List.of(
                new Mode(0, 1920, 1080, 60, 60, new float[0], new int[0]),
                new Mode(1, 800, 600, 60, 60, new float[0], new int[0]),
                new Mode(2, 320, 240, 70, 70, new float[0], new int[0]),
                new Mode(3, 640, 480, 60, 60, new float[0], new int[0]),
                new Mode(4, 640, 480, 50, 60, new float[0], new int[0]),
                new Mode(5, 2048, 1024, 60, 60, new float[0], new int[0]),
                new Mode(6, 720, 480, 60, 60, new float[0], new int[0]));
        return new DisplayDevice(
                displayId, "HDMI", supportedModes.get(0), supportedModes, isEnabled);
    }

    DisplayDevice createOverlayDisplay(DisplayIsEnabled isEnabled) {
        int displayId = OVERLAY_DISPLAY_ID;
        var supportedModes = List.of(
                new Mode(0, 1240, 780, 60, 60, new float[0],
                    new int[0]));
        return new DisplayDevice(
                displayId, "Overlay #1", supportedModes.get(0), supportedModes, isEnabled);
    }
}
