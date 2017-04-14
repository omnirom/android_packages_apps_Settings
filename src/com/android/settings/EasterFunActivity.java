/*
 * Copyright (C) 2017 The OmniROM Project
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

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;

public class EasterFunActivity extends Activity {
    private static final String TAG = "EasterFunActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            final Intent intent = new Intent();
            ComponentName cn = new ComponentName("ca.gsalisi.games.eggs", "ca.gsalisi.games.eggs.StartingActivity");
            intent.setComponent(cn);
            startActivity(intent);
        } catch (Resources.NotFoundException | ActivityNotFoundException e) {
            Log.w(TAG, "Failed to resolve help", e);
        }
        setResult(RESULT_CANCELED);
        finish();
    }
}
