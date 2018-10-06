/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.settings.location;

import android.app.Activity;
import android.content.Context;
import android.content.ContentResolver;
import android.location.SettingInjectorService;
import android.os.Bundle;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;
import android.support.v14.preference.SwitchPreference;
import android.util.Log;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.widget.SwitchBar;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.location.RecentLocationApps;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

/**
 * System location settings (Settings &gt; Location). The screen has three parts:
 * <ul>
 *     <li>Platform location controls</li>
 *     <ul>
 *         <li>In switch bar: location master switch. Used to toggle location on and off.
 *         </li>
 *     </ul>
 *     <li>Recent location requests: automatically populated by {@link RecentLocationApps}</li>
 *     <li>Location services: multi-app settings provided from outside the Android framework. Each
 *     is injected by a system-partition app via the {@link SettingInjectorService} API.</li>
 * </ul>
 * <p>
 * Note that as of KitKat, the {@link SettingInjectorService} is the preferred method for OEMs to
 * add their own settings to this page, rather than directly modifying the framework code. Among
 * other things, this simplifies integration with future changes to the default (AOSP)
 * implementation.
 */
public class LocationSettings extends DashboardFragment {

    private static final String TAG = "LocationSettings";

    private LocationSwitchBarController mSwitchBarController;

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.LOCATION;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final SettingsActivity activity = (SettingsActivity) getActivity();
        final SwitchBar switchBar = activity.getSwitchBar();
        switchBar.setSwitchBarText(R.string.location_settings_master_switch_title,
                R.string.location_settings_master_switch_title);
        mSwitchBarController = new LocationSwitchBarController(activity, switchBar, getLifecycle());
        switchBar.show();
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.location_settings;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, this, getLifecycle());
    }

    static void addPreferencesSorted(List<Preference> prefs, PreferenceGroup container) {
        // If there's some items to display, sort the items and add them to the container.
        Collections.sort(prefs, new Comparator<Preference>() {
            @Override
            public int compare(Preference lhs, Preference rhs) {
                return lhs.getTitle().toString().compareTo(rhs.getTitle().toString());
            }
        });
        for (Preference entry : prefs) {
            container.addPreference(entry);
        }
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_location_access;
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(
            Context context, LocationSettings fragment, Lifecycle lifecycle) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        controllers.add(new AppLocationPermissionPreferenceController(context));
        controllers.add(new LocationForWorkPreferenceController(context, lifecycle));
        controllers.add(
                new RecentLocationRequestPreferenceController(context, fragment, lifecycle));
        controllers.add(new LocationScanningPreferenceController(context));
        controllers.add(
                new LocationServicePreferenceController(context, fragment, lifecycle));
        controllers.add(new LocationFooterPreferenceController(context, lifecycle));
        controllers.add(new AgpsPreferenceController(context));
        return controllers;
    }

    private static class SummaryProvider implements SummaryLoader.SummaryProvider {

        private final Context mContext;
        private final SummaryLoader mSummaryLoader;

        public SummaryProvider(Context context, SummaryLoader summaryLoader) {
            mContext = context;
            mSummaryLoader = summaryLoader;
        }

        @Override
        public void setListening(boolean listening) {
            if (listening) {
                mSummaryLoader.setSummary(
                    this, LocationPreferenceController.getLocationSummary(mContext));
            }
        }
    }

    public static final SummaryLoader.SummaryProviderFactory SUMMARY_PROVIDER_FACTORY
            = new SummaryLoader.SummaryProviderFactory() {
        @Override
        public SummaryLoader.SummaryProvider createSummaryProvider(Activity activity,
                                                                   SummaryLoader summaryLoader) {
            return new SummaryProvider(activity, summaryLoader);
        }
    };

    /**
     * For Search.
     */
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(
                        Context context, boolean enabled) {
                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.location_settings;
                    return Arrays.asList(sir);
                }

                @Override
                public List<AbstractPreferenceController> createPreferenceControllers(Context
                        context) {
                    return buildPreferenceControllers(context, null /* fragment */,
                            null /* lifecycle */);
                }
            };

    //PreferenceController of AGPS
    public static class AgpsPreferenceController extends BasePreferenceController {
        // CMCC assisted gps SUPL(Secure User Plane Location) server address
        private static final String ASSISTED_GPS_SUPL_HOST = "assisted_gps_supl_host";
        // CMCC agps SUPL port address
        private static final String ASSISTED_GPS_SUPL_PORT = "assisted_gps_supl_port";

        private static final String KEY_ASSISTED_GPS = "assisted_gps";
        private static final String PROPERTIES_FILE = "/etc/gps.conf";

        private SwitchPreference mAgpsPreference;

        public AgpsPreferenceController(Context context) {
            super(context, KEY_ASSISTED_GPS);
        }

        @Override
        public String getPreferenceKey() {
            return KEY_ASSISTED_GPS;
        }

        @AvailabilityStatus
        public int getAvailabilityStatus() {
            return mContext.getResources().getBoolean(R.bool.config_agps_enabled)
                    ? AVAILABLE
                    : UNSUPPORTED_ON_DEVICE;
        }

        @Override
        public void displayPreference(PreferenceScreen screen) {
            super.displayPreference(screen);
            mAgpsPreference =
                    (SwitchPreference) screen.findPreference(KEY_ASSISTED_GPS);
        }

        @Override
        public void updateState(Preference preference) {
            if (mAgpsPreference != null) {
                mAgpsPreference.setChecked(Settings.Global.getInt(
                        mContext.getContentResolver(), Settings.Global.ASSISTED_GPS_ENABLED, 0) == 1);
            }
        }

        @Override
        public boolean handlePreferenceTreeClick(Preference preference) {
            if (KEY_ASSISTED_GPS.equals(preference.getKey())) {
                final ContentResolver cr = mContext.getContentResolver();
                final boolean switchState = mAgpsPreference.isChecked();
                if (switchState) {
                    if (Settings.Global.getString(cr, ASSISTED_GPS_SUPL_HOST) == null
                            || Settings.Global
                            .getString(cr, ASSISTED_GPS_SUPL_PORT) == null) {
                        FileInputStream stream = null;
                        try {
                            Properties properties = new Properties();
                            File file = new File(PROPERTIES_FILE);
                            stream = new FileInputStream(file);
                            properties.load(stream);
                            Settings.Global.putString(cr, ASSISTED_GPS_SUPL_HOST,
                                    properties.getProperty("SUPL_HOST", null));
                            Settings.Global.putString(cr, ASSISTED_GPS_SUPL_PORT,
                                    properties.getProperty("SUPL_PORT", null));
                        } catch (IOException e) {
                            Log.e(TAG, "Could not open GPS configuration file "
                                            + PROPERTIES_FILE + ", e=" + e);
                        } finally {
                            if (stream != null) {
                                try {
                                    stream.close();
                                } catch (Exception e) {
                                }
                            }
                        }
                    }
                }
                Settings.Global.putInt(cr, Settings.Global.ASSISTED_GPS_ENABLED,
                        switchState ? 1 : 0);
                return true;
            }
            return false;
        }
    }
}
