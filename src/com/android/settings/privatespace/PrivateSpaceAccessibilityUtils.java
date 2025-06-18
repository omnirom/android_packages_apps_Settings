/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.privatespace;

import static com.android.settingslib.widget.preference.illustration.R.string.settingslib_action_label_pause;
import static com.android.settingslib.widget.preference.illustration.R.string.settingslib_action_label_resume;
import static com.android.settingslib.widget.preference.illustration.R.string.settingslib_illustration_content_description;

import android.content.Context;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

import com.airbnb.lottie.LottieAnimationView;

public class PrivateSpaceAccessibilityUtils {

    static void updateAccessibilityActionForAnimation(Context context,
            LottieAnimationView animationView, boolean isAnimationPlaying) {
        animationView.setContentDescription(
                context.getString(settingslib_illustration_content_description));
        ViewCompat.setAccessibilityDelegate(animationView, new AccessibilityDelegateCompat() {
            @Override
            public void onInitializeAccessibilityNodeInfo(
                    View host, AccessibilityNodeInfoCompat info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                // Clearing the class name to ensure the animation is not called out as "button"
                // inside the TalkBack flows
                info.setClassName("");
                info.removeAction(
                        AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_CLICK);

                final AccessibilityNodeInfoCompat.AccessibilityActionCompat clickAction =
                        new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                                AccessibilityNodeInfo.ACTION_CLICK,
                                getActionLabelForAnimation(context, isAnimationPlaying));
                info.addAction(clickAction);
            }
        });
    }

    private static String getActionLabelForAnimation(Context context, boolean isAnimationPlaying) {
        if (isAnimationPlaying) {
            return context.getString(settingslib_action_label_pause);
        } else {
            return context.getString(settingslib_action_label_resume);
        }
    }
}
