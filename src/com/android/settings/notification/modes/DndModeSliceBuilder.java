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

package com.android.settings.notification.modes;

import static android.app.slice.Slice.EXTRA_TOGGLE_STATE;

import android.annotation.ColorInt;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.UserHandle;
import android.os.UserManager;

import androidx.annotation.NonNull;
import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.Slice;
import androidx.slice.builders.ListBuilder;
import androidx.slice.builders.ListBuilder.RowBuilder;
import androidx.slice.builders.SliceAction;

import com.android.settings.R;
import com.android.settings.SubSettings;
import com.android.settings.Utils;
import com.android.settings.slices.CustomSliceRegistry;
import com.android.settings.slices.SliceBroadcastReceiver;
import com.android.settings.slices.SliceBuilderUtils;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.notification.modes.ZenMode;
import com.android.settingslib.notification.modes.ZenModesBackend;

public class DndModeSliceBuilder {

    private static final String TAG = "DndModeSliceBuilder";

    private static final String DND_MODE_SLICE_KEY = "zen_mode_toggle";

    /**
     * Action notifying a change on the Dnd Mode Slice.
     */
    public static final String ACTION_DND_MODE_SLICE_CHANGED =
            "com.android.settings.notification.DND_MODE_CHANGED";

    public static final IntentFilter INTENT_FILTER = new IntentFilter();

    static {
        INTENT_FILTER.addAction(NotificationManager.ACTION_ZEN_CONFIGURATION_CHANGED_INTERNAL);
    }

    private DndModeSliceBuilder() {
    }

    /**
     * Return a DND Mode Slice bound to {@link CustomSliceRegistry#ZEN_MODE_SLICE_URI}.
     * <p>
     * Note that you should register a listener for {@link #INTENT_FILTER} to get changes for
     * ZenMode.
     */
    @NonNull
    public static Slice getSlice(@NonNull Context context) {
        final boolean isDndModeEnabled = isDndActive(context);
        final CharSequence title = context.getText(
                com.android.settingslib.R.string.zen_mode_do_not_disturb_name);
        @ColorInt final int color = Utils.getColorAccentDefaultColor(context);
        final PendingIntent toggleAction = getBroadcastIntent(context);
        final PendingIntent primaryAction = getPrimaryAction(context);
        final SliceAction primarySliceAction = SliceAction.createDeeplink(primaryAction,
                (IconCompat) null /* icon */, ListBuilder.ICON_IMAGE, title);
        final SliceAction toggleSliceAction = SliceAction.createToggle(toggleAction,
                null /* actionTitle */,
                isDndModeEnabled);
        final RowBuilder rowBuilder = new RowBuilder()
                .setTitle(title)
                .setPrimaryAction(primarySliceAction);
        if (!isManagedByAdmin(context)) {
            rowBuilder.addEndItem(toggleSliceAction);
        }

        if (!Utils.isSettingsIntelligence(context)) {
            rowBuilder.setSubtitle(context.getText(R.string.zen_mode_slice_subtitle));
        }

        return new ListBuilder(context, CustomSliceRegistry.ZEN_MODE_SLICE_URI,
                ListBuilder.INFINITY)
                .setAccentColor(color)
                .addRow(rowBuilder)
                .build();
    }

    /**
     * Update the current ZenMode status to the boolean value keyed by
     * {@link android.app.slice.Slice#EXTRA_TOGGLE_STATE} on {@code intent}.
     */
    public static void handleUriChange(@NonNull Context context, @NonNull Intent intent) {
        final boolean dndOn = intent.getBooleanExtra(EXTRA_TOGGLE_STATE, false);
        setDndActive(context, dndOn);
    }

    @NonNull
    public static Intent getIntent(@NonNull Context context) {
        final Uri contentUri = new Uri.Builder().appendPath(DND_MODE_SLICE_KEY).build();
        final String screenTitle = context.getText(R.string.zen_mode_settings_title).toString();
        return SliceBuilderUtils.buildSearchResultPageIntent(context,
                        ZenModeFragment.class.getName(), DND_MODE_SLICE_KEY,
                        ZenSubSettingLauncher.getModeArguments(ZenMode.MANUAL_DND_MODE_ID),
                        screenTitle, SettingsEnums.NOTIFICATION_ZEN_MODE,
                        R.string.menu_key_priority_modes)
                .setClassName(context.getPackageName(), SubSettings.class.getName())
                .setData(contentUri);
    }

    private static boolean isDndActive(Context context) {
        ZenMode dnd = ZenModesBackend.getInstance(context).getMode(ZenMode.MANUAL_DND_MODE_ID);
        return dnd != null && dnd.isActive();
    }

    private static void setDndActive(Context context, boolean active) {
        ZenModesBackend backend = ZenModesBackend.getInstance(context);
        ZenMode dnd = backend.getMode(ZenMode.MANUAL_DND_MODE_ID);
        if (dnd != null) {
            if (active) {
                backend.activateMode(dnd, /* forDuration= */ null);
            } else {
                backend.deactivateMode(dnd);
            }
        }
    }

    private static PendingIntent getPrimaryAction(Context context) {
        final Intent intent = getIntent(context);
        return PendingIntent.getActivity(context, 0 /* requestCode */, intent,
                PendingIntent.FLAG_IMMUTABLE);
    }

    private static PendingIntent getBroadcastIntent(Context context) {
        final Intent intent = new Intent(ACTION_DND_MODE_SLICE_CHANGED)
                .setClass(context, SliceBroadcastReceiver.class);
        return PendingIntent.getBroadcast(context, 0 /* requestCode */, intent,
                PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_MUTABLE);
    }

    private static boolean isManagedByAdmin(Context context) {
        EnforcedAdmin enforcedAdmin = RestrictedLockUtilsInternal.checkIfRestrictionEnforced(
                context, UserManager.DISALLOW_ADJUST_VOLUME, UserHandle.myUserId());
        return enforcedAdmin != null;
    }
}
