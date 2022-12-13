/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.settings.deviceinfo;

import android.os.Bundle;
import android.os.storage.VolumeInfo;
import android.view.View;
import android.widget.ImageView;

import com.android.settings.R;

public class StorageWizardReady extends StorageWizardBase {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (mDisk == null) {
            finish();
            return;
        }
        setContentView(R.layout.storage_wizard_generic);

        setHeaderText(R.string.storage_wizard_ready_title, getDiskShortDescription());

        final VolumeInfo privateVol = findFirstVolume(VolumeInfo.TYPE_PRIVATE);
        if (privateVol != null) {
            if (getIntent().getBooleanExtra(EXTRA_MIGRATE_SKIP, false)) {
                setBodyText(R.string.storage_wizard_ready_v2_internal_body,
                        getDiskDescription());
            } else {
                setBodyText(R.string.storage_wizard_ready_v2_internal_moved_body,
                        getDiskDescription(), getDiskShortDescription());
            }
        } else {
            setBodyText(R.string.storage_wizard_ready_v2_external_body,
                    getDiskDescription());
        }
        ImageView img = (ImageView) findViewById(R.id.storage_wizard_body_image);
        img.setImageResource(R.drawable.ic_storage_wizard_ready);
        setIcon(R.drawable.ic_test_tick);
        setNextButtonText(R.string.done);
        setBackButtonVisibility(View.INVISIBLE);
    }

    @Override
    public void onNavigateNext(View view) {
        finishAffinity();
    }
}
