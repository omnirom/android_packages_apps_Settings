/*
 * Copyright (C) 2013 SlimRoms Project
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

package com.android.settings;

import android.content.Context;
import android.preference.EditTextPreference;
import android.provider.Settings;
import android.util.AttributeSet;

public class CustomCarrier extends EditTextPreference {

    private static final String TAG = "CustomCarrier";

    private Context mContext;
    private String mCustomCarrierName;

    public CustomCarrier(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mContext = context;

        // determine the current value
        String id = getText();

        if (id != null && id.length() > 0) {
            mCustomCarrierName = id;
        } else {
            mCustomCarrierName = "";
        }

        getEditText().setHint(mCustomCarrierName);
    }

    public CustomCarrier(Context context, AttributeSet attrs) {
        this(context, attrs, com.android.internal.R.attr.editTextPreferenceStyle);
    }

    public CustomCarrier(Context context) {
        this(context, null);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            String customCarrierName = getEditText().getText().toString();
            setText(customCarrierName);
        }
    }

    @Override
    public void setText(String text) {
        Settings.System.putString(mContext.getContentResolver(),
                    Settings.System.NOTIFICATION_CUSTOM_CARRIER_LABEL, text);
    }

    @Override
    public String getText() {
        return Settings.System.getString(getContext().getContentResolver(),
                Settings.System.NOTIFICATION_CUSTOM_CARRIER_LABEL);
    }

    @Override
    public void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        // Do nothing
    }
}