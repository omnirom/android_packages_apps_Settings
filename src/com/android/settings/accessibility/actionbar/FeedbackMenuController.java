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

package com.android.settings.accessibility.actionbar;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.annotation.NonNull;

import com.android.settings.R;
import com.android.settings.accessibility.FeedbackManager;
import com.android.settings.core.InstrumentedPreferenceFragment;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnCreateOptionsMenu;
import com.android.settingslib.core.lifecycle.events.OnOptionsItemSelected;

/**
 * A controller that adds feedback menu to any Settings page.
 */
public class FeedbackMenuController implements LifecycleObserver, OnCreateOptionsMenu,
        OnOptionsItemSelected {

    /**
     * The menu item ID for the feedback menu option.
     */
    public static final int MENU_FEEDBACK = Menu.FIRST + 10;

    /**
     * The menu item ID for the feedback menu option.
     */
    private final FeedbackManager mFeedbackManager;

    /**
     * Initializes the FeedbackMenuController for an InstrumentedPreferenceFragment with a provided
     * pade ID.
     *
     * @param host The InstrumentedPreferenceFragment to which the menu controller will be added.
     * @param pageId The page ID used for feedback tracking.
     */
    public static void init(@NonNull InstrumentedPreferenceFragment host, int pageId) {
        host.getSettingsLifecycle().addObserver(
                new FeedbackMenuController(
                        new FeedbackManager(host.getActivity(), pageId)));
    }

    /**
     * Initializes the FeedbackMenuController for an InstrumentedPreferenceFragment with a provided
     * FeedbackManager.
     *
     * @param host The InstrumentedPreferenceFragment to which the menu controller will be added.
     * @param feedbackManager The FeedbackManager to use for handling feedback actions.
     */
    public static void init(@NonNull InstrumentedPreferenceFragment host,
            @NonNull FeedbackManager feedbackManager) {
        host.getSettingsLifecycle().addObserver(
                new FeedbackMenuController(feedbackManager));
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        if (!mFeedbackManager.isAvailable()) {
            return;
        }
        menu.add(Menu.NONE, MENU_FEEDBACK, Menu.NONE, R.string.accessibility_send_feedback_title);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem menuItem) {
        if (menuItem.getItemId() == MENU_FEEDBACK) {
            mFeedbackManager.sendFeedback();
            return true;
        }
        return false;
    }

    private FeedbackMenuController(@NonNull FeedbackManager feedbackManager) {
        mFeedbackManager = feedbackManager;
    }
}
