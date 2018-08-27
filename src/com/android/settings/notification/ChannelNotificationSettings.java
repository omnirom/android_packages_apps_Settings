/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.notification;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.applications.AppInfoBase;
import com.android.settingslib.core.AbstractPreferenceController;

import java.util.ArrayList;
import java.util.List;

public class ChannelNotificationSettings extends NotificationSettingsBase {
    private static final String TAG = "ChannelSettings";

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.NOTIFICATION_TOPIC_NOTIFICATION;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final PreferenceScreen screen = getPreferenceScreen();
        Bundle args = getArguments();
        // If linking to this screen from an external app, expand settings
        if (screen != null && args != null) {
            if (!args.getBoolean(ARG_FROM_SETTINGS, false)) {
                screen.setInitialExpandedChildrenCount(Integer.MAX_VALUE);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mUid < 0 || TextUtils.isEmpty(mPkg) || mPkgInfo == null || mChannel == null) {
            Log.w(TAG, "Missing package or uid or packageinfo or channel");
            finish();
            return;
        }

        for (NotificationPreferenceController controller : mControllers) {
            controller.onResume(mAppRow, mChannel, mChannelGroup, mSuspendedAppsAdmin);
            controller.displayPreference(getPreferenceScreen());
        }
        updatePreferenceStates();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        for (NotificationPreferenceController controller : mControllers) {
            if (controller instanceof PreferenceManager.OnActivityResultListener) {
                ((PreferenceManager.OnActivityResultListener) controller)
                        .onActivityResult(requestCode, resultCode, data);
            }
        }
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return  R.xml.channel_notification_settings;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mNm.forcePulseLedLight(-1, -1, -1);
    }

    @Override
    public void onStop() {
        super.onStop();
        mNm.forcePulseLedLight(-1, -1, -1);
    }

    @Override
    public void onPause() {
        super.onPause();
        mNm.forcePulseLedLight(-1, -1, -1);
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        mControllers = new ArrayList<>();
        mControllers.add(new HeaderPreferenceController(context, this));
        mControllers.add(new BlockPreferenceController(context, mImportanceListener, mBackend));
        mControllers.add(new ImportancePreferenceController(
                context, mImportanceListener, mBackend));
        mControllers.add(new AllowSoundPreferenceController(
                context, mImportanceListener, mBackend));
        mControllers.add(new SoundPreferenceController(context, this,
                mImportanceListener, mBackend));
        mControllers.add(new VibrationPreferenceController(context, mBackend));
        mControllers.add(new AppLinkPreferenceController(context));
        mControllers.add(new DescriptionPreferenceController(context));
        mControllers.add(new VisibilityPreferenceController(context, new LockPatternUtils(context),
                mBackend));
        mControllers.add(new LightsPreferenceController(context, mBackend));
        mControllers.add(new CustomLightsPreferenceController(context, mBackend));
        mControllers.add(new CustomLightOnTimePreferenceController(context, mBackend));
        mControllers.add(new CustomLightOffTimePreferenceController(context, mBackend));
        mControllers.add(new LightOnZenPreferenceController(context, mBackend));
        mControllers.add(new BadgePreferenceController(context, mBackend));
        mControllers.add(new DndPreferenceController(context, mBackend));
        mControllers.add(new NotificationsOffPreferenceController(context));
        return new ArrayList<>(mControllers);
    }
}
