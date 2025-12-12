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

package com.android.settings.display;

import static android.provider.Settings.Secure.HDR_BRIGHTNESS_ENABLED;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;

import android.app.settings.SettingsEnums;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Outline;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.provider.Settings;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.widget.LayoutPreference;

@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class HdrBrightnessSettings extends DashboardFragment {
    private static final String TAG = "HdrBrightnessSettings";
    private static final String PREVIEW_KEY = "preview";

    private ContentObserver mContentObserver;
    private Handler mHandler;
    private Context mContext;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getContext().getApplicationContext();
        mHandler = new Handler(Looper.getMainLooper());
        mContentObserver = new ContentObserver(mHandler) {
            @Override
            public void onChange(boolean selfChange, @Nullable Uri uri) {
                super.onChange(selfChange, uri);
                if (Settings.Secure.getUriFor(HDR_BRIGHTNESS_ENABLED).equals(uri)) {
                    updatePreferenceStates();
                }
            }
        };
        requireActivity().getWindow().setColorMode(ActivityInfo.COLOR_MODE_HDR10);

        ViewOutlineProvider vop = new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(/* left= */ 0, /* top= */ 0, view.getWidth(), view.getHeight(),
                        getResources().getDimensionPixelSize(
                                R.dimen.hdr_brightness_preview_corner_radius));
            }
        };
        LayoutPreference preview = findPreference(PREVIEW_KEY);

        ImageView standardImage = preview.findViewById(R.id.standard_image);
        Bitmap standardBitmap = BitmapFactory.decodeResource(getResources(),
                R.drawable.mountain_lake);
        standardBitmap = Bitmap.createBitmap(standardBitmap, 0, 0,
                standardBitmap.getHeight() * 3 / 4, standardBitmap.getHeight());
        standardBitmap.setGainmap(null);
        standardImage.setImageBitmap(standardBitmap);
        standardImage.setOutlineProvider(vop);
        standardImage.setClipToOutline(true);

        ImageView hdrImage = preview.findViewById(R.id.hdr_image);
        Bitmap hdrBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.mountain_lake);
        hdrBitmap = Bitmap.createBitmap(hdrBitmap, 0, 0, standardBitmap.getHeight() * 3 / 4,
                standardBitmap.getHeight());
        hdrImage.setImageBitmap(hdrBitmap);
        hdrImage.setOutlineProvider(vop);
        hdrImage.setClipToOutline(true);
    }

    @Override
    public void onStart() {
        super.onStart();
        mHandler.post(() -> {
            ContentResolver cr = mContext.getContentResolver();
            cr.registerContentObserver(Settings.Secure.getUriFor(HDR_BRIGHTNESS_ENABLED),
                    /* notifyForDescendants= */ false, mContentObserver, UserHandle.USER_CURRENT);
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        mHandler.post(() ->
                mContext.getContentResolver().unregisterContentObserver(mContentObserver));
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.hdr_brightness_detail;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.HDR_BRIGHTNESS_SETTINGS;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.hdr_brightness_detail) {
                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    return HdrBrightnessUtils.getAvailabilityStatus(context) == AVAILABLE;
                }
            };
}
