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

package com.android.settings.network.telephony.satellite;

import static android.telephony.CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_AUTOMATIC;
import static android.telephony.CarrierConfigManager.KEY_CARRIER_ROAMING_NTN_CONNECT_TYPE_INT;
import static android.telephony.CarrierConfigManager.KEY_SATELLITE_ENTITLEMENT_SUPPORTED_BOOL;
import static android.telephony.CarrierConfigManager.KEY_SATELLITE_INFORMATION_REDIRECT_URL_STRING;

import android.content.Context;
import android.content.Intent;
import android.os.PersistableBundle;
import android.telephony.TelephonyManager;
import android.text.Html;

import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.network.telephony.TelephonyBasePreferenceController;
import com.android.settingslib.HelpUtils;
import com.android.settingslib.widget.FooterPreference;

/** A controller for showing the dynamic disclaimer of Satellite service. */
public class SatelliteSettingFooterController extends TelephonyBasePreferenceController {
    private static final String TAG = "SatelliteSettingFooterController";
    @VisibleForTesting
    static final String KEY_FOOTER_PREFERENCE = "satellite_setting_extra_info_footer_pref";

    private PersistableBundle mConfigBundle = new PersistableBundle();
    private String mSimOperatorName;

    public SatelliteSettingFooterController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    void init(int subId, PersistableBundle configBundle) {
        mSubId = subId;
        mConfigBundle = configBundle;
        mSimOperatorName = mContext.getSystemService(TelephonyManager.class).getSimOperatorName(
                subId);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        updateFooterContent(screen);
    }

    @Override
    public int getAvailabilityStatus(int subId) {
        return AVAILABLE_UNSEARCHABLE;
    }

    private void updateFooterContent(PreferenceScreen screen) {
        // More about satellite messaging
        FooterPreference footerPreference = screen.findPreference(KEY_FOOTER_PREFERENCE);
        if (footerPreference == null) {
            return;
        }
        footerPreference.setSummary(
                Html.fromHtml(getFooterContent(), Html.FROM_HTML_SEPARATOR_LINE_BREAK_LIST_ITEM));
        final String link = readSatelliteMoreInfoString();
        if (link.isEmpty()) {
            return;
        }
        footerPreference.setLearnMoreAction(view -> {
            Intent helpIntent = HelpUtils.getHelpIntent(mContext, link, this.getClass().getName());
            if (helpIntent != null) {
                mContext.startActivityForResult(mContext.getPackageName(),
                        helpIntent, /*requestCode=*/ 0, null);
            }
        });
        footerPreference.setLearnMoreText(
                mContext.getString(R.string.more_about_satellite_connectivity));
    }

    private String getFooterContent() {
        boolean isEntitlementSupport = mConfigBundle.getBoolean(
                KEY_SATELLITE_ENTITLEMENT_SUPPORTED_BOOL);
        boolean isAUtoType = mConfigBundle.getInt(KEY_CARRIER_ROAMING_NTN_CONNECT_TYPE_INT)
                == CARRIER_ROAMING_NTN_CONNECT_AUTOMATIC;

        String result = "";
        result = mContext.getString(R.string.satellite_footer_content_section_0) + "\n\n";
        result += getHtmlStringCombination(R.string.satellite_footer_content_section_1);
        result += getHtmlStringCombination(R.string.satellite_footer_content_section_2);
        result += getHtmlStringCombination(R.string.satellite_footer_content_section_3);
        result += getHtmlStringCombination(R.string.satellite_footer_content_section_4);

        if (isEntitlementSupport) {
            if (isAUtoType) {
                result += getHtmlStringCombination(R.string.satellite_footer_content_section_5);
                result += getHtmlStringCombination(R.string.satellite_footer_content_section_7,
                        mSimOperatorName);
            }
        } else {
            if (isAUtoType) {
                result += getHtmlStringCombination(R.string.satellite_footer_content_section_6);
                result += getHtmlStringCombination(R.string.satellite_footer_content_section_5);
                return result;
            }
            result += getHtmlStringCombination(R.string.satellite_footer_content_section_7,
                    mSimOperatorName);
        }
        return result;
    }

    private String getHtmlStringCombination(int resId) {
        String prefix = "<li>&#160;";
        String subfix = "</li>";
        return prefix + mContext.getString(resId) + subfix;
    }

    private String getHtmlStringCombination(int resId, Object... value) {
        String prefix = "<li>&#160;";
        String subfix = "</li>";
        return prefix + mContext.getString(resId, value) + subfix;
    }

    private String readSatelliteMoreInfoString() {
        return mConfigBundle.getString(KEY_SATELLITE_INFORMATION_REDIRECT_URL_STRING);
    }
}
