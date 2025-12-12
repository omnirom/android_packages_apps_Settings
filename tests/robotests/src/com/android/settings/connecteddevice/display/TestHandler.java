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
package com.android.settings.connecteddevice.display;

import static org.robolectric.Shadows.shadowOf;

import android.os.Handler;
import android.os.Message;

import java.util.ArrayDeque;

/**
 * A Handler for use in tests which prevents an actual delay from being used when a message is
 * queued, and allows all queued messages to be run by calling {@link #flush()}.
 */
class TestHandler extends Handler {
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
