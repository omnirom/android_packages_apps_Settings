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

package com.android.settings.accessibility;

import static android.view.WindowManager.LayoutParams.SYSTEM_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.storage.StorageManager;
import android.text.BidiFormatter;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.android.settings.R;

import java.util.Locale;

/**
 * Utility class for creating the dialog that asks users for explicit permission for an
 * accessibility service to access user data before the service is enabled
 */
public class AccessibilityServiceWarning {
    /*private static final View.OnTouchListener filterTouchListener = (View v, MotionEvent event) -> {
        // Filter obscured touches by consuming them.
        if (((event.getFlags() & MotionEvent.FLAG_WINDOW_IS_OBSCURED) != 0)
                || ((event.getFlags() & MotionEvent.FLAG_WINDOW_IS_PARTIALLY_OBSCURED) != 0)) {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                Toast.makeText(v.getContext(), R.string.touch_filtered_warning,
                        Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        return false;
    };*/

    public static Dialog createCapabilitiesDialog(Activity parentActivity,
            AccessibilityServiceInfo info, View.OnClickListener listener) {
        final AlertDialog ad = new AlertDialog.Builder(parentActivity)
                .setView(createEnableDialogContentView(parentActivity, info, listener))
                .create();

        Window window = ad.getWindow();
        WindowManager.LayoutParams params = window.getAttributes();
        params.privateFlags |= SYSTEM_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS;
        window.setAttributes(params);
        ad.create();
        ad.setCanceledOnTouchOutside(true);

        return ad;
    }

    public static Dialog createDisableDialog(Activity parentActivity,
            AccessibilityServiceInfo info, View.OnClickListener listener) {
        final AlertDialog ad = new AlertDialog.Builder(parentActivity)
                .setView(createDisableDialogContentView(parentActivity, info, listener))
                .setCancelable(true)
                .create();

        return ad;
    }

    /**
     * Return whether the device is encrypted with legacy full disk encryption. Newer devices
     * should be using File Based Encryption.
     *
     * @return true if device is encrypted
     */
    private static boolean isFullDiskEncrypted() {
        return StorageManager.isNonDefaultBlockEncrypted();
    }

    /**
     * Get a content View for a dialog to confirm that they want to enable a service.
     *
     * @param context A valid context
     * @param info The info about a service
     * @return A content view suitable for viewing
     */
    private static View createEnableDialogContentView(Context context,
            AccessibilityServiceInfo info, View.OnClickListener listener) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);

        View content = inflater.inflate(R.layout.enable_accessibility_service_dialog_content,
                null);

        TextView encryptionWarningView = (TextView) content.findViewById(
                R.id.encryption_warning);
        if (isFullDiskEncrypted()) {
            String text = context.getString(R.string.enable_service_encryption_warning,
                    getServiceName(context, info));
            encryptionWarningView.setText(text);
            encryptionWarningView.setVisibility(View.VISIBLE);
        } else {
            encryptionWarningView.setVisibility(View.GONE);
        }

        final Drawable icon;
        if (info.getResolveInfo().getIconResource() == 0) {
            icon = ContextCompat.getDrawable(context, R.drawable.ic_accessibility_generic);
        } else {
            icon = info.getResolveInfo().loadIcon(context.getPackageManager());
        }

        ImageView permissionDialogIcon = content.findViewById(
                R.id.permissionDialog_icon);
        permissionDialogIcon.setImageDrawable(icon);

        TextView permissionDialogTitle = content.findViewById(R.id.permissionDialog_title);
        permissionDialogTitle.setText(context.getString(R.string.enable_service_title,
                getServiceName(context, info)));

        Button permissionAllowButton = content.findViewById(
                R.id.permission_enable_allow_button);
        Button permissionDenyButton = content.findViewById(
                R.id.permission_enable_deny_button);
        permissionAllowButton.setOnClickListener(listener);
        //permissionAllowButton.setOnTouchListener(filterTouchListener);
        permissionDenyButton.setOnClickListener(listener);

        return content;
    }

    private static View createDisableDialogContentView(Context context,
            AccessibilityServiceInfo info, View.OnClickListener listener) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);

        View content = inflater.inflate(R.layout.disable_accessibility_service_dialog_content,
                null);

        TextView permissionDialogTitle = content.findViewById(R.id.permissionDialog_disable_title);
        permissionDialogTitle.setText(context.getString(R.string.disable_service_title,
                getServiceName(context, info)));
        TextView permissionDialogMessage = content
                .findViewById(R.id.permissionDialog_disable_message);
        permissionDialogMessage.setText(context.getString(R.string.disable_service_message,
                context.getString(R.string.accessibility_dialog_button_stop),
                getServiceName(context, info)));

        Button permissionAllowButton = content.findViewById(
                R.id.permission_disable_stop_button);
        Button permissionDenyButton = content.findViewById(
                R.id.permission_disable_cancel_button);
        permissionAllowButton.setOnClickListener(listener);
        permissionDenyButton.setOnClickListener(listener);

        return content;
    }

    // Get the service name and bidi wrap it to protect from bidi side effects.
    private static CharSequence getServiceName(Context context, AccessibilityServiceInfo info) {
        final Locale locale = context.getResources().getConfiguration().getLocales().get(0);
        final CharSequence label =
                info.getResolveInfo().loadLabel(context.getPackageManager());
        return BidiFormatter.getInstance(locale).unicodeWrap(label);
    }
}
