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

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.webview.WebViewUpdateServiceWrapper;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

public class WebViewDevUiPreferenceController extends DeveloperOptionsPreferenceController
        implements  PreferenceControllerMixin {

    private static final String TAG = "WebViewDevUiPrefCtrl";
    private static final String WEBVIEW_APP_KEY = "webview_launch_devtools";

    @NonNull
    private final WebViewUpdateServiceWrapper mWebViewUpdateServiceWrapper;

    @VisibleForTesting
    public WebViewDevUiPreferenceController(@NonNull Context context,
            @NonNull WebViewUpdateServiceWrapper webViewUpdateServiceWrapper) {
        super(context);
        mWebViewUpdateServiceWrapper = webViewUpdateServiceWrapper;
    }

    public WebViewDevUiPreferenceController(@NonNull Context context) {
        this(context, new WebViewUpdateServiceWrapper());
    }

    @Override
    @NonNull
    public String getPreferenceKey() {
        return WEBVIEW_APP_KEY;
    }

    @Override
    public boolean handlePreferenceTreeClick(@NonNull Preference preference) {
        if (!WEBVIEW_APP_KEY.equals(preference.getKey())) {
            return false;
        }
        launchWebViewDevUi();
        return true;
    }

    private void launchWebViewDevUi() {
        PackageInfo currentWebViewPackage =
                mWebViewUpdateServiceWrapper.getCurrentWebViewPackage();
        if (currentWebViewPackage == null) {
            Log.e(TAG, "Couldn't find current WebView package");
            Toast.makeText(
                    mContext,
                    mContext.getString(
                            com.android.settingslib.R.string.webview_launch_devtools_no_package),
                    Toast.LENGTH_LONG).show();
            return;
        }
        String currentWebViewPackageName = currentWebViewPackage.packageName;
        Intent intent = new Intent("com.android.webview.SHOW_DEV_UI");
        intent.setPackage(currentWebViewPackageName);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            mContext.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e(
                    TAG,
                    "Couldn't launch developer UI from current WebView package: "
                            + currentWebViewPackage);
            Toast.makeText(
                    mContext,
                    mContext.getString(
                            com.android.settingslib.R.string.webview_launch_devtools_no_activity),
                    Toast.LENGTH_LONG).show();
        }
    }
}
