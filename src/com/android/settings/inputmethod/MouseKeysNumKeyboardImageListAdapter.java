/*
 * Copyright 2025 The Android Open Source Project
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

package com.android.settings.inputmethod;

import android.content.Context;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MouseKeysNumKeyboardImageListAdapter extends
        RecyclerView.Adapter<MouseKeysNumKeyboardImageListAdapter.MouseKeyImageViewHolder> {
    private static final String LABEL_DELIMITER = ", ";
    private static final ImmutableList<Integer> DRAWABLE_LIST = ImmutableList.of(
            R.drawable.numpad_move, R.drawable.numpad_left_click,
            R.drawable.numpad_click_and_hold, R.drawable.numpad_release,
            R.drawable.numpad_scroll, R.drawable.numpad_right_click);
    private static final ImmutableList<Integer> DIRECTIONAL_CHAR_KEYCODE_LIST = ImmutableList.of(
            KeyEvent.KEYCODE_NUMPAD_7, KeyEvent.KEYCODE_NUMPAD_8, KeyEvent.KEYCODE_NUMPAD_9,
            KeyEvent.KEYCODE_NUMPAD_4, KeyEvent.KEYCODE_NUMPAD_6, KeyEvent.KEYCODE_NUMPAD_1,
            KeyEvent.KEYCODE_NUMPAD_2, KeyEvent.KEYCODE_NUMPAD_3
    );
    private static final int LEFT_CLICK_CHAR_KEYCODE =
            KeyEvent.KEYCODE_NUMPAD_5;
    private static final int PRESS_HOLD_CHAR_KEYCODE =
            KeyEvent.KEYCODE_NUMPAD_MULTIPLY;
    private static final int RELEASE_CHAR_KEYCODE =
            KeyEvent.KEYCODE_NUMPAD_SUBTRACT;
    private static final ImmutableList<Integer> TOGGLE_SCROLL_CHAR_KEYCODE_LIST = ImmutableList.of(
            KeyEvent.KEYCODE_NUMPAD_0, KeyEvent.KEYCODE_NUMPAD_8, KeyEvent.KEYCODE_NUMPAD_2,
            KeyEvent.KEYCODE_NUMPAD_4, KeyEvent.KEYCODE_NUMPAD_6
    );
    private static final int RIGHT_CLICK_CHAR_KEYCODE =
            KeyEvent.KEYCODE_NUMPAD_DOT;
    private final List<String> mComposedSummaryList = new ArrayList<>();

    public MouseKeysNumKeyboardImageListAdapter(@NonNull Context context,
            @Nullable InputDevice currentInputDevice) {
        composeSummaryForImages(context, currentInputDevice);
    }

    @NonNull
    @Override
    public MouseKeyImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.mouse_keys_image_item, parent, false);
        return new MouseKeyImageViewHolder(view, parent.getContext());
    }

    @Override
    public void onBindViewHolder(@NonNull MouseKeyImageViewHolder holder, int position) {
        if (mComposedSummaryList.isEmpty()) {
            return;
        }
        ((MouseKeyImageViewHolder) holder).bindView(DRAWABLE_LIST.get(position),
                mComposedSummaryList.get(position));
    }

    @Override
    public int getItemCount() {
        return DRAWABLE_LIST.size();
    }

    private void composeSummaryForImages(Context context,
            @Nullable InputDevice currentInputDevice) {
        if (currentInputDevice == null) {
            return;
        }
        mComposedSummaryList.clear();
        List<String> directionalLabelList = DIRECTIONAL_CHAR_KEYCODE_LIST.stream().map(
                (key) -> getDisplayLabel(currentInputDevice, key)).toList();
        mComposedSummaryList.add(context.getString(R.string.mouse_keys_directional_summary,
                String.join(LABEL_DELIMITER, directionalLabelList)));
        String leftClickLabel = getDisplayLabel(currentInputDevice, LEFT_CLICK_CHAR_KEYCODE);
        mComposedSummaryList.add(
                context.getString(R.string.mouse_keys_click_summary, leftClickLabel));
        String pressHoldLabel = getDisplayLabel(currentInputDevice, PRESS_HOLD_CHAR_KEYCODE);
        mComposedSummaryList.add(
                context.getString(R.string.mouse_keys_press_hold_summary, pressHoldLabel));
        String releaseLabel = getDisplayLabel(currentInputDevice, RELEASE_CHAR_KEYCODE);
        mComposedSummaryList.add(
                context.getString(R.string.mouse_keys_release_summary, releaseLabel));
        List<String> toggleScrollLabelList = TOGGLE_SCROLL_CHAR_KEYCODE_LIST.stream().map(
                (key) -> getDisplayLabel(currentInputDevice, key)).toList();
        mComposedSummaryList.add(context.getString(R.string.mouse_keys_toggle_scroll_summary,
                toggleScrollLabelList.getFirst(),
                String.join(LABEL_DELIMITER,
                        toggleScrollLabelList.subList(1, toggleScrollLabelList.size()))
        ));
        String rightClickLabel = getDisplayLabel(currentInputDevice, RIGHT_CLICK_CHAR_KEYCODE);
        mComposedSummaryList.add(
                context.getString(R.string.mouse_keys_release2_summary, rightClickLabel));
    }

    private String getDisplayLabel(InputDevice currentInputDevice, int keycode) {
        return String.valueOf(currentInputDevice.getKeyCharacterMap().getDisplayLabel(
                currentInputDevice.getKeyCodeForKeyLocation(keycode))).toLowerCase(
                Locale.getDefault());
    }

    public static class MouseKeyImageViewHolder extends RecyclerView.ViewHolder {
        private final TextView mTextView;
        private final ImageView mImageView;
        private final Context mContext;

        public MouseKeyImageViewHolder(View itemView, Context context) {
            super(itemView);
            mTextView = itemView.findViewById(R.id.layout_description);
            mImageView = itemView.findViewById(R.id.image);
            mTextView.setGravity(Gravity.START);
            mImageView.setScaleType(ImageView.ScaleType.FIT_START);
            mContext = context;
        }

        void bindView(int drawableRes, String summary) {
            mTextView.setText(summary);
            mImageView.setImageDrawable(mContext.getDrawable(drawableRes));
        }
    }
}
