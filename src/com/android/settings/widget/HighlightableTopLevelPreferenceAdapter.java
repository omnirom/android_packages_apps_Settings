/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Rect;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceViewHolder;
import androidx.recyclerview.widget.RecyclerView;
import androidx.window.embedding.ActivityEmbeddingController;

import com.android.settings.core.RoundCornerPreferenceAdapter;
import com.android.settings.homepage.SettingsHomepageActivity;
import com.android.settingslib.widget.SettingsThemeHelper;
import com.android.settingslib.widget.theme.R;

/**
 *  Adapter for highlighting top level preferences
 */
public class HighlightableTopLevelPreferenceAdapter extends RoundCornerPreferenceAdapter implements
        SettingsHomepageActivity.HomepageLoadedListener {

    private static final String TAG = "HighlightableTopLevelAdapter";

    static final long DELAY_HIGHLIGHT_DURATION_MILLIS = 100L;

    private final SettingsHomepageActivity mHomepageActivity;
    private final RecyclerView mRecyclerView;
    private String mHighlightKey;
    private int mHighlightPosition = RecyclerView.NO_POSITION;
    private int mScrollPosition = RecyclerView.NO_POSITION;
    private boolean mHighlightNeeded;
    private boolean mScrolled;
    private SparseArray<PreferenceViewHolder> mViewHolders;

    public HighlightableTopLevelPreferenceAdapter(SettingsHomepageActivity homepageActivity,
            PreferenceGroup preferenceGroup, RecyclerView recyclerView, String key,
            boolean scrollNeeded) {
        super(preferenceGroup);
        mRecyclerView = recyclerView;
        mHighlightKey = key;
        mScrolled = !scrollNeeded;
        mViewHolders = new SparseArray<>();
        mHomepageActivity = homepageActivity;
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        mViewHolders.put(position, holder);
        updateBackground(holder, position);
    }

    @VisibleForTesting
    void updateBackground(PreferenceViewHolder holder, int position) {
        if (!isHighlightNeeded()) {
            removeHighlightBackground(holder, position);
            return;
        }

        if (position == mHighlightPosition
                && mHighlightKey != null
                && TextUtils.equals(mHighlightKey, getItem(position).getKey())) {
            // This position should be highlighted.
            addHighlightBackground(holder, position);
        } else {
            removeHighlightBackground(holder, position);
        }
    }

    /**
     * A function can highlight a specific setting in recycler view.
     */
    public void requestHighlight() {
        if (mRecyclerView == null) {
            return;
        }

        final int previousPosition = mHighlightPosition;
        if (TextUtils.isEmpty(mHighlightKey)) {
            // De-highlight previous preference.
            mHighlightPosition = RecyclerView.NO_POSITION;
            mScrolled = true;
            if (previousPosition >= 0) {
                notifyItemChanged(previousPosition);
            }
            return;
        }

        final int position = getPreferenceAdapterPosition(mHighlightKey);
        if (position < 0) {
            return;
        }

        // Scroll before highlight if needed.
        final boolean highlightNeeded = isHighlightNeeded();
        if (highlightNeeded) {
            mScrollPosition = position;
            scroll();
        }

        // Turn on/off highlight when screen split mode is changed.
        if (highlightNeeded != mHighlightNeeded) {
            Log.d(TAG, "Highlight needed change: " + highlightNeeded);
            mHighlightNeeded = highlightNeeded;
            mHighlightPosition = position;
            notifyItemChanged(position);
            if (!highlightNeeded) {
                // De-highlight to prevent a flicker
                removeHighlightAt(previousPosition);
            }
            return;
        }

        if (position == mHighlightPosition) {
            return;
        }

        mHighlightPosition = position;
        Log.d(TAG, "Request highlight position " + position);
        Log.d(TAG, "Is highlight needed: " + highlightNeeded);
        if (!highlightNeeded) {
            return;
        }

        // Highlight preference.
        notifyItemChanged(position);

        // De-highlight previous preference.
        if (previousPosition >= 0) {
            notifyItemChanged(previousPosition);
        }
    }

    /**
     * A function that highlights a setting by specifying a preference key. Usually used whenever a
     * preference is clicked.
     */
    public void highlightPreference(String key, boolean scrollNeeded) {
        mHighlightKey = key;
        mScrolled = !scrollNeeded;
        requestHighlight();
    }

    @Override
    public void onHomepageLoaded() {
        scroll();
    }

    private void scroll() {
        if (mScrolled || mScrollPosition < 0) {
            return;
        }

        if (mHomepageActivity.addHomepageLoadedListener(this)) {
            return;
        }

        // Only when the recyclerView is loaded, it can be scrolled
        final View view = mRecyclerView.getChildAt(mScrollPosition);
        if (view == null) {
            mRecyclerView.postDelayed(() -> scroll(), DELAY_HIGHLIGHT_DURATION_MILLIS);
            return;
        }

        mScrolled = true;
        Log.d(TAG, "Scroll to position " + mScrollPosition);
        // Scroll to the top to reset the position.
        mRecyclerView.nestedScrollBy(0, -mRecyclerView.getHeight());

        // get the visible area of the recycler view
        Rect rvRect = new Rect();
        mRecyclerView.getGlobalVisibleRect(rvRect);
        if (view.getBottom() <= rvRect.height()) {
            // the request position already fully visible in the visible area
            return;
        }

        final int scrollY = view.getTop();
        if (scrollY > 0) {
            mRecyclerView.nestedScrollBy(0, scrollY);
        }
    }

    private void removeHighlightAt(int position) {
        if (position >= 0) {
            // De-highlight the existing preference view holder at an early stage
            final PreferenceViewHolder holder = mViewHolders.get(position);
            if (holder != null) {
                removeHighlightBackground(holder, position);
            }
            notifyItemChanged(position);
        }
    }

    private void addHighlightBackground(PreferenceViewHolder holder, int position) {
        final View v = holder.itemView;
        @DrawableRes int bgRes = getRoundCornerDrawableRes(position, true /*isSelected*/);
        v.setBackgroundResource(bgRes);
        Context context = v.getContext();
        if (SettingsThemeHelper.isExpressiveTheme(context)) {
            // homepage preference title should change text appearance when it's selected
            TextView title = v.findViewById(android.R.id.title);
            if (title != null) {
                ColorStateList currentColor = title.getTextColors();
                title.setTextAppearance(context,
                        R.style.TextAppearance_SettingsLib_TitleMedium_Emphasized);
                title.setTextColor(currentColor);
            }
        }
    }

    private void removeHighlightBackground(PreferenceViewHolder holder, int position) {
        final View v = holder.itemView;
        @DrawableRes int bgRes = getRoundCornerDrawableRes(position, false /*isSelected*/);
        v.setBackgroundResource(bgRes);
        Context context = v.getContext();
        if (SettingsThemeHelper.isExpressiveTheme(context)) {
            // recover homepage preference title when it's unselected
            TextView title = v.findViewById(android.R.id.title);
            if (title != null) {
                ColorStateList currentColor = title.getTextColors();
                title.setTextAppearance(context, R.style.TextAppearance_SettingsLib_TitleMedium);
                title.setTextColor(currentColor);
            }
        }
    }

    private boolean isHighlightNeeded() {
        return ActivityEmbeddingController.getInstance(mHomepageActivity)
                .isActivityEmbedded(mHomepageActivity);
    }
}
