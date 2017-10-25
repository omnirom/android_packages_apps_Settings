/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.settings.fuelgauge;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.os.BatteryStats;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.SearchIndexableResource;
import android.support.annotation.VisibleForTesting;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.internal.os.BatterySipper;
import com.android.internal.os.BatterySipper.DrainType;
import com.android.internal.os.PowerProfile;
import com.android.settings.R;
import com.android.settings.Settings.HighPowerApplicationsActivity;
import com.android.settings.SettingsActivity;
import com.android.settings.Utils;
import com.android.settings.applications.LayoutPreference;
import com.android.settings.applications.ManageApplications;
import com.android.settings.core.PreferenceController;
import com.android.settings.core.instrumentation.MetricsFeatureProvider;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.display.AutoBrightnessPreferenceController;
import com.android.settings.display.BatteryPercentagePreferenceController;
import com.android.settings.display.BatteryImagePreferenceController;
import com.android.settings.display.TimeoutPreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.widget.FooterPreferenceMixin;
import com.android.settingslib.BatteryInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Displays a list of apps and subsystems that consume power, ordered by how much power was
 * consumed since the last time it was unplugged.
 */
public class PowerUsageSummary extends PowerUsageBase {

    static final String TAG = "PowerUsageSummary";

    private static final boolean DEBUG = false;
    private static final boolean USE_FAKE_DATA = false;
    private static final String KEY_APP_LIST = "app_list";
    private static final String KEY_BATTERY_HEADER = "battery_header";
    private static final String KEY_SHOW_ALL_APPS = "show_all_apps";
    private static final int MAX_ITEMS_TO_LIST = USE_FAKE_DATA ? 30 : 10;
    private static final int MIN_AVERAGE_POWER_THRESHOLD_MILLI_AMP = 10;

    private static final String KEY_SCREEN_USAGE = "screen_usage";
    private static final String KEY_TIME_SINCE_LAST_FULL_CHARGE = "last_full_charge";

    private static final String KEY_AUTO_BRIGHTNESS = "auto_brightness_battery";
    //private static final String KEY_SCREEN_TIMEOUT = "screen_timeout_battery";
    private static final String KEY_BATTERY_SAVER_SUMMARY = "battery_saver_summary";

    private static final int MENU_STATS_TYPE = Menu.FIRST;
    @VisibleForTesting
    static final int MENU_HIGH_POWER_APPS = Menu.FIRST + 3;
    @VisibleForTesting
    static final int MENU_ADDITIONAL_BATTERY_INFO = Menu.FIRST + 4;
    @VisibleForTesting
    static final int MENU_TOGGLE_APPS = Menu.FIRST + 5;
    private static final int MENU_HELP = Menu.FIRST + 6;

    private final FooterPreferenceMixin mFooterPreferenceMixin =
            new FooterPreferenceMixin(this, getLifecycle());

    @VisibleForTesting
    boolean mShowAllApps = false;
    @VisibleForTesting
    PowerGaugePreference mScreenUsagePref;
    @VisibleForTesting
    PowerGaugePreference mLastFullChargePref;
    @VisibleForTesting
    PowerUsageFeatureProvider mPowerFeatureProvider;
    @VisibleForTesting
    BatteryUtils mBatteryUtils;

    private BatteryHeaderPreferenceController mBatteryHeaderPreferenceController;
    private LayoutPreference mBatteryLayoutPref;
    private PreferenceGroup mAppListGroup;
    private int mStatsType = BatteryStats.STATS_SINCE_CHARGED;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setAnimationAllowed(true);

        mBatteryLayoutPref = (LayoutPreference) findPreference(KEY_BATTERY_HEADER);
        mAppListGroup = (PreferenceGroup) findPreference(KEY_APP_LIST);
        mScreenUsagePref = (PowerGaugePreference) findPreference(KEY_SCREEN_USAGE);
        mLastFullChargePref = (PowerGaugePreference) findPreference(
                KEY_TIME_SINCE_LAST_FULL_CHARGE);
        mFooterPreferenceMixin.createFooterPreference().setTitle(R.string.battery_footer_summary);

        mBatteryUtils = BatteryUtils.getInstance(getContext());

        restoreSavedInstance(icicle);
        initFeatureProvider();
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.FUELGAUGE_POWER_USAGE_SUMMARY;
    }

    @Override
    public void onPause() {
        BatteryEntry.stopRequestQueue();
        mHandler.removeMessages(BatteryEntry.MSG_UPDATE_NAME_ICON);
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (getActivity().isChangingConfigurations()) {
            BatteryEntry.clearUidCache();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_SHOW_ALL_APPS, mShowAllApps);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (KEY_BATTERY_HEADER.equals(preference.getKey())) {
            performBatteryHeaderClick();
            return true;
        } else if (!(preference instanceof PowerGaugePreference)) {
            return super.onPreferenceTreeClick(preference);
        }
        PowerGaugePreference pgp = (PowerGaugePreference) preference;
        BatteryEntry entry = pgp.getInfo();
        AdvancedPowerUsageDetail.startBatteryDetailPage((SettingsActivity) getActivity(),
                this, mStatsHelper, mStatsType, entry, pgp.getPercent());
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.power_usage_summary;
    }

    @Override
    protected List<PreferenceController> getPreferenceControllers(Context context) {
        final List<PreferenceController> controllers = new ArrayList<>();
        mBatteryHeaderPreferenceController = new BatteryHeaderPreferenceController(context);
        controllers.add(mBatteryHeaderPreferenceController);
        controllers.add(new AutoBrightnessPreferenceController(context, KEY_AUTO_BRIGHTNESS));
        //controllers.add(new TimeoutPreferenceController(context, KEY_SCREEN_TIMEOUT));
        controllers.add(new BatterySaverController(context, getLifecycle()));
        controllers.add(new BatteryPercentagePreferenceController(context));
        controllers.add(new BatteryImagePreferenceController(context));
        return controllers;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (DEBUG) {
            menu.add(Menu.NONE, MENU_STATS_TYPE, Menu.NONE, R.string.menu_stats_total)
                    .setIcon(com.android.internal.R.drawable.ic_menu_info_details)
                    .setAlphabeticShortcut('t');
        }

        menu.add(Menu.NONE, MENU_HIGH_POWER_APPS, Menu.NONE, R.string.high_power_apps);

        if (mPowerFeatureProvider.isAdditionalBatteryInfoEnabled()) {
            menu.add(Menu.NONE, MENU_ADDITIONAL_BATTERY_INFO,
                    Menu.NONE, R.string.additional_battery_info);
        }
        if (mPowerFeatureProvider.isPowerAccountingToggleEnabled()) {
            menu.add(Menu.NONE, MENU_TOGGLE_APPS, Menu.NONE,
                    mShowAllApps ? R.string.hide_extra_apps : R.string.show_all_apps);
        }

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_url_battery;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final SettingsActivity sa = (SettingsActivity) getActivity();
        final Context context = getContext();
        final MetricsFeatureProvider metricsFeatureProvider =
                FeatureFactory.getFactory(context).getMetricsFeatureProvider();

        switch (item.getItemId()) {
            case MENU_STATS_TYPE:
                if (mStatsType == BatteryStats.STATS_SINCE_CHARGED) {
                    mStatsType = BatteryStats.STATS_SINCE_UNPLUGGED;
                } else {
                    mStatsType = BatteryStats.STATS_SINCE_CHARGED;
                }
                refreshUi();
                return true;
            case MENU_HIGH_POWER_APPS:
                Bundle args = new Bundle();
                args.putString(ManageApplications.EXTRA_CLASSNAME,
                        HighPowerApplicationsActivity.class.getName());
                sa.startPreferencePanel(this, ManageApplications.class.getName(), args,
                        R.string.high_power_apps, null, null, 0);
                metricsFeatureProvider.action(context,
                        MetricsEvent.ACTION_SETTINGS_MENU_BATTERY_OPTIMIZATION);
                return true;
            case MENU_ADDITIONAL_BATTERY_INFO:
                startActivity(FeatureFactory.getFactory(getContext())
                        .getPowerUsageFeatureProvider(getContext())
                        .getAdditionalBatteryInfoIntent());
                metricsFeatureProvider.action(context,
                        MetricsEvent.ACTION_SETTINGS_MENU_BATTERY_USAGE_ALERTS);
                return true;
            case MENU_TOGGLE_APPS:
                mShowAllApps = !mShowAllApps;
                item.setTitle(mShowAllApps ? R.string.hide_extra_apps : R.string.show_all_apps);
                metricsFeatureProvider.action(context,
                        MetricsEvent.ACTION_SETTINGS_MENU_BATTERY_APPS_TOGGLE, mShowAllApps);
                restartBatteryStatsLoader();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @VisibleForTesting
    void restoreSavedInstance(Bundle savedInstance) {
        if (savedInstance != null) {
            mShowAllApps = savedInstance.getBoolean(KEY_SHOW_ALL_APPS, false);
        }
    }

    private void addNotAvailableMessage() {
        final String NOT_AVAILABLE = "not_available";
        Preference notAvailable = getCachedPreference(NOT_AVAILABLE);
        if (notAvailable == null) {
            notAvailable = new Preference(getPrefContext());
            notAvailable.setKey(NOT_AVAILABLE);
            notAvailable.setTitle(R.string.power_usage_not_available);
            mAppListGroup.addPreference(notAvailable);
        }
    }

    private void performBatteryHeaderClick() {
        final Context context = getContext();
        final PowerUsageFeatureProvider featureProvider = FeatureFactory.getFactory(context)
                .getPowerUsageFeatureProvider(context);

        if (featureProvider.isAdvancedUiEnabled()) {
            Utils.startWithFragment(getContext(), PowerUsageAdvanced.class.getName(), null,
                    null, 0, R.string.advanced_battery_title, null, getMetricsCategory());
        } else {
            mStatsHelper.storeStatsHistoryInFile(BatteryHistoryDetail.BATTERY_HISTORY_FILE);
            Bundle args = new Bundle(2);
            args.putString(BatteryHistoryDetail.EXTRA_STATS,
                    BatteryHistoryDetail.BATTERY_HISTORY_FILE);
            args.putParcelable(BatteryHistoryDetail.EXTRA_BROADCAST,
                    mStatsHelper.getBatteryBroadcast());
            Utils.startWithFragment(getContext(), BatteryHistoryDetail.class.getName(), args,
                    null, 0, R.string.history_details_title, null, getMetricsCategory());
        }
    }

    private static boolean isSharedGid(int uid) {
        return UserHandle.getAppIdFromSharedAppGid(uid) > 0;
    }

    private static boolean isSystemUid(int uid) {
        return uid >= Process.SYSTEM_UID && uid < Process.FIRST_APPLICATION_UID;
    }

    /**
     * We want to coalesce some UIDs. For example, dex2oat runs under a shared gid that
     * exists for all users of the same app. We detect this case and merge the power use
     * for dex2oat to the device OWNER's use of the app.
     *
     * @return A sorted list of apps using power.
     */
    private List<BatterySipper> getCoalescedUsageList(final List<BatterySipper> sippers) {
        final SparseArray<BatterySipper> uidList = new SparseArray<>();

        final ArrayList<BatterySipper> results = new ArrayList<>();
        final int numSippers = sippers.size();
        for (int i = 0; i < numSippers; i++) {
            BatterySipper sipper = sippers.get(i);
            if (sipper.getUid() > 0) {
                int realUid = sipper.getUid();

                // Check if this UID is a shared GID. If so, we combine it with the OWNER's
                // actual app UID.
                if (isSharedGid(sipper.getUid())) {
                    realUid = UserHandle.getUid(UserHandle.USER_SYSTEM,
                            UserHandle.getAppIdFromSharedAppGid(sipper.getUid()));
                }

                // Check if this UID is a system UID (mediaserver, logd, nfc, drm, etc).
                if (isSystemUid(realUid)
                        && !"mediaserver".equals(sipper.packageWithHighestDrain)) {
                    // Use the system UID for all UIDs running in their own sandbox that
                    // are not apps. We exclude mediaserver because we already are expected to
                    // report that as a separate item.
                    realUid = Process.SYSTEM_UID;
                }

                if (realUid != sipper.getUid()) {
                    // Replace the BatterySipper with a new one with the real UID set.
                    BatterySipper newSipper = new BatterySipper(sipper.drainType,
                            new FakeUid(realUid), 0.0);
                    newSipper.add(sipper);
                    newSipper.packageWithHighestDrain = sipper.packageWithHighestDrain;
                    newSipper.mPackages = sipper.mPackages;
                    sipper = newSipper;
                }

                int index = uidList.indexOfKey(realUid);
                if (index < 0) {
                    // New entry.
                    uidList.put(realUid, sipper);
                } else {
                    // Combine BatterySippers if we already have one with this UID.
                    final BatterySipper existingSipper = uidList.valueAt(index);
                    existingSipper.add(sipper);
                    if (existingSipper.packageWithHighestDrain == null
                            && sipper.packageWithHighestDrain != null) {
                        existingSipper.packageWithHighestDrain = sipper.packageWithHighestDrain;
                    }

                    final int existingPackageLen = existingSipper.mPackages != null ?
                            existingSipper.mPackages.length : 0;
                    final int newPackageLen = sipper.mPackages != null ?
                            sipper.mPackages.length : 0;
                    if (newPackageLen > 0) {
                        String[] newPackages = new String[existingPackageLen + newPackageLen];
                        if (existingPackageLen > 0) {
                            System.arraycopy(existingSipper.mPackages, 0, newPackages, 0,
                                    existingPackageLen);
                        }
                        System.arraycopy(sipper.mPackages, 0, newPackages, existingPackageLen,
                                newPackageLen);
                        existingSipper.mPackages = newPackages;
                    }
                }
            } else {
                results.add(sipper);
            }
        }

        final int numUidSippers = uidList.size();
        for (int i = 0; i < numUidSippers; i++) {
            results.add(uidList.valueAt(i));
        }

        // The sort order must have changed, so re-sort based on total power use.
        mBatteryUtils.sortUsageList(results);
        return results;
    }

    protected void refreshUi() {
        final Context context = getContext();
        if (context == null) {
            return;
        }

        cacheRemoveAllPrefs(mAppListGroup);
        mAppListGroup.setOrderingAsAdded(false);
        boolean addedSome = false;

        final PowerProfile powerProfile = mStatsHelper.getPowerProfile();
        final BatteryStats stats = mStatsHelper.getStats();
        final double averagePower = powerProfile.getAveragePower(PowerProfile.POWER_SCREEN_FULL);

        final long elapsedRealtimeUs = SystemClock.elapsedRealtime() * 1000;
        Intent batteryBroadcast = context.registerReceiver(null,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        BatteryInfo batteryInfo = BatteryInfo.getBatteryInfo(context, batteryBroadcast,
                mStatsHelper.getStats(), elapsedRealtimeUs, false);
        mBatteryHeaderPreferenceController.updateHeaderPreference(batteryInfo);

        final TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.colorControlNormal, value, true);
        final int colorControl = context.getColor(value.resourceId);
        final int dischargeAmount = USE_FAKE_DATA ? 5000
                : stats != null ? stats.getDischargeAmount(mStatsType) : 0;

        final long lastFullChargeTime = mBatteryUtils.calculateLastFullChargeTime(mStatsHelper,
                System.currentTimeMillis());
        updateScreenPreference();
        updateLastFullChargePreference(lastFullChargeTime);

        final CharSequence timeSequence = Utils.formatElapsedTime(context, lastFullChargeTime,
                false);
        final int resId = mShowAllApps ? R.string.power_usage_list_summary_device
                : R.string.power_usage_list_summary;
        mAppListGroup.setTitle(TextUtils.expandTemplate(getText(resId), timeSequence));

        if (averagePower >= MIN_AVERAGE_POWER_THRESHOLD_MILLI_AMP || USE_FAKE_DATA) {
            final List<BatterySipper> usageList = getCoalescedUsageList(
                    USE_FAKE_DATA ? getFakeStats() : mStatsHelper.getUsageList());
            double hiddenPowerMah = mShowAllApps ? 0 :
                    mBatteryUtils.removeHiddenBatterySippers(usageList);
            mBatteryUtils.sortUsageList(usageList);

            final int numSippers = usageList.size();
            for (int i = 0; i < numSippers; i++) {
                final BatterySipper sipper = usageList.get(i);
                double totalPower = USE_FAKE_DATA ? 4000 : mStatsHelper.getTotalPower();

                final double percentOfTotal = mBatteryUtils.calculateBatteryPercent(
                        sipper.totalPowerMah, totalPower, hiddenPowerMah, dischargeAmount);

                if (((int) (percentOfTotal + .5)) < 1) {
                    continue;
                }
                if (sipper.drainType == BatterySipper.DrainType.OVERCOUNTED) {
                    // Don't show over-counted unless it is at least 2/3 the size of
                    // the largest real entry, and its percent of total is more significant
                    if (sipper.totalPowerMah < ((mStatsHelper.getMaxRealPower() * 2) / 3)) {
                        continue;
                    }
                    if (percentOfTotal < 10) {
                        continue;
                    }
                    if ("user".equals(Build.TYPE)) {
                        continue;
                    }
                }
                if (sipper.drainType == BatterySipper.DrainType.UNACCOUNTED) {
                    // Don't show over-counted unless it is at least 1/2 the size of
                    // the largest real entry, and its percent of total is more significant
                    if (sipper.totalPowerMah < (mStatsHelper.getMaxRealPower() / 2)) {
                        continue;
                    }
                    if (percentOfTotal < 5) {
                        continue;
                    }
                    if ("user".equals(Build.TYPE)) {
                        continue;
                    }
                }
                final UserHandle userHandle = new UserHandle(UserHandle.getUserId(sipper.getUid()));
                final BatteryEntry entry = new BatteryEntry(getActivity(), mHandler, mUm, sipper);
                final Drawable badgedIcon = mUm.getBadgedIconForUser(entry.getIcon(),
                        userHandle);
                final CharSequence contentDescription = mUm.getBadgedLabelForUser(entry.getLabel(),
                        userHandle);

                final String key = extractKeyFromSipper(sipper);
                PowerGaugePreference pref = (PowerGaugePreference) getCachedPreference(key);
                if (pref == null) {
                    pref = new PowerGaugePreference(getPrefContext(), badgedIcon,
                            contentDescription, entry);
                    pref.setKey(key);
                }

                final double percentOfMax = (sipper.totalPowerMah * 100)
                        / mStatsHelper.getMaxPower();
                sipper.percent = percentOfTotal;
                pref.setTitle(entry.getLabel());
                pref.setOrder(i + 1);
                pref.setPercent(percentOfTotal);
                if (sipper.usageTimeMs == 0 && sipper.drainType == DrainType.APP) {
                    sipper.usageTimeMs = mBatteryUtils.getProcessTimeMs(
                            BatteryUtils.StatusType.FOREGROUND, sipper.uidObj, mStatsType);
                }
                setUsageSummary(pref, sipper);
                if ((sipper.drainType != DrainType.APP
                        || sipper.uidObj.getUid() == Process.ROOT_UID)
                        && sipper.drainType != DrainType.USER) {
                    pref.setTint(colorControl);
                }
                addedSome = true;
                mAppListGroup.addPreference(pref);
                if (mAppListGroup.getPreferenceCount() - getCachedCount()
                        > (MAX_ITEMS_TO_LIST + 1)) {
                    break;
                }
            }
        }
        if (!addedSome) {
            addNotAvailableMessage();
        }
        removeCachedPrefs(mAppListGroup);

        BatteryEntry.startRequestQueue();
    }

    @VisibleForTesting
    BatterySipper findBatterySipperByType(List<BatterySipper> usageList, DrainType type) {
        for (int i = 0, size = usageList.size(); i < size; i++) {
            final BatterySipper sipper = usageList.get(i);
            if (sipper.drainType == type) {
                return sipper;
            }
        }
        return null;
    }

    @VisibleForTesting
    void updateScreenPreference() {
        final BatterySipper sipper = findBatterySipperByType(
                mStatsHelper.getUsageList(), DrainType.SCREEN);
        final long usageTimeMs = sipper != null ? sipper.usageTimeMs : 0;

        mScreenUsagePref.setSubtitle(Utils.formatElapsedTime(getContext(), usageTimeMs, false));
    }

    @VisibleForTesting
    void updateLastFullChargePreference(long timeMs) {
        final CharSequence timeSequence = Utils.formatElapsedTime(getContext(), timeMs, false);
        mLastFullChargePref.setSubtitle(
                TextUtils.expandTemplate(getText(R.string.power_last_full_charge_summary),
                        timeSequence));
    }

    @VisibleForTesting
    long calculateRunningTimeBasedOnStatsType() {
        final long elapsedRealtimeUs = SystemClock.elapsedRealtime() * 1000;
        // Return the battery time (millisecond) on status mStatsType
        return mStatsHelper.getStats().computeBatteryRealtime(elapsedRealtimeUs,
                mStatsType /* STATS_SINCE_CHARGED */) / 1000;
    }

    @VisibleForTesting
    double calculatePercentage(double powerUsage, double dischargeAmount) {
        final double totalPower = mStatsHelper.getTotalPower();
        return totalPower == 0 ? 0 :
                ((powerUsage / totalPower) * dischargeAmount);
    }

    @VisibleForTesting
    void setUsageSummary(Preference preference, BatterySipper sipper) {
        // Only show summary when usage time is longer than one minute
        final long usageTimeMs = sipper.usageTimeMs;
        if (usageTimeMs >= DateUtils.MINUTE_IN_MILLIS) {
            final CharSequence timeSequence = Utils.formatElapsedTime(getContext(), usageTimeMs,
                    false);
            preference.setSummary(mBatteryUtils.shouldHideSipper(sipper) ? timeSequence :
                    TextUtils.expandTemplate(getText(R.string.battery_screen_usage), timeSequence));
        }
    }

    @VisibleForTesting
    String extractKeyFromSipper(BatterySipper sipper) {
        if (sipper.uidObj != null) {
            return Integer.toString(sipper.getUid());
        } else if (sipper.drainType != DrainType.APP) {
            return sipper.drainType.toString();
        } else if (sipper.getPackages() != null) {
            return TextUtils.concat(sipper.getPackages()).toString();
        } else {
            Log.w(TAG, "Inappropriate BatterySipper without uid and package names: " + sipper);
            return "-1";
        }
    }

    @VisibleForTesting
    void setBatteryLayoutPreference(LayoutPreference layoutPreference) {
        mBatteryLayoutPref = layoutPreference;
    }

    @VisibleForTesting
    void initFeatureProvider() {
        final Context context = getContext();
        mPowerFeatureProvider = FeatureFactory.getFactory(context)
                .getPowerUsageFeatureProvider(context);
    }

    private static List<BatterySipper> getFakeStats() {
        ArrayList<BatterySipper> stats = new ArrayList<>();
        float use = 5;
        for (DrainType type : DrainType.values()) {
            if (type == DrainType.APP) {
                continue;
            }
            stats.add(new BatterySipper(type, null, use));
            use += 5;
        }
        for (int i = 0; i < 100; i++) {
            stats.add(new BatterySipper(DrainType.APP,
                    new FakeUid(Process.FIRST_APPLICATION_UID + i), use));
        }
        stats.add(new BatterySipper(DrainType.APP,
                new FakeUid(0), use));

        // Simulate dex2oat process.
        BatterySipper sipper = new BatterySipper(DrainType.APP,
                new FakeUid(UserHandle.getSharedAppGid(Process.FIRST_APPLICATION_UID)), 10.0f);
        sipper.packageWithHighestDrain = "dex2oat";
        stats.add(sipper);

        sipper = new BatterySipper(DrainType.APP,
                new FakeUid(UserHandle.getSharedAppGid(Process.FIRST_APPLICATION_UID + 1)), 10.0f);
        sipper.packageWithHighestDrain = "dex2oat";
        stats.add(sipper);

        sipper = new BatterySipper(DrainType.APP,
                new FakeUid(UserHandle.getSharedAppGid(Process.LOG_UID)), 9.0f);
        stats.add(sipper);

        return stats;
    }

    Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case BatteryEntry.MSG_UPDATE_NAME_ICON:
                    BatteryEntry entry = (BatteryEntry) msg.obj;
                    PowerGaugePreference pgp =
                            (PowerGaugePreference) findPreference(
                                    Integer.toString(entry.sipper.uidObj.getUid()));
                    if (pgp != null) {
                        final int userId = UserHandle.getUserId(entry.sipper.getUid());
                        final UserHandle userHandle = new UserHandle(userId);
                        pgp.setIcon(mUm.getBadgedIconForUser(entry.getIcon(), userHandle));
                        pgp.setTitle(entry.name);
                        if (entry.sipper.drainType == DrainType.APP) {
                            pgp.setContentDescription(entry.name);
                        }
                    }
                    break;
                case BatteryEntry.MSG_REPORT_FULLY_DRAWN:
                    Activity activity = getActivity();
                    if (activity != null) {
                        activity.reportFullyDrawn();
                    }
                    break;
            }
            super.handleMessage(msg);
        }
    };

    private static class SummaryProvider implements SummaryLoader.SummaryProvider {
        private final Context mContext;
        private final SummaryLoader mLoader;
        private final BatteryBroadcastReceiver mBatteryBroadcastReceiver;

        private SummaryProvider(Context context, SummaryLoader loader) {
            mContext = context;
            mLoader = loader;
            mBatteryBroadcastReceiver = new BatteryBroadcastReceiver(mContext);
            mBatteryBroadcastReceiver.setBatteryChangedListener(() -> {
                BatteryInfo.getBatteryInfo(mContext, new BatteryInfo.Callback() {
                    @Override
                    public void onBatteryInfoLoaded(BatteryInfo info) {
                        mLoader.setSummary(SummaryProvider.this, info.chargeLabelString);
                    }
                });
            });
        }

        @Override
        public void setListening(boolean listening) {
            if (listening) {
                mBatteryBroadcastReceiver.register();
            } else {
                mBatteryBroadcastReceiver.unRegister();
            }
        }
    }

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(
                        Context context, boolean enabled) {
                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.power_usage_summary;
                    return Arrays.asList(sir);
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    List<String> niks = new ArrayList<>();
                    // Duplicates in display
                    niks.add(KEY_AUTO_BRIGHTNESS);
                    //niks.add(KEY_SCREEN_TIMEOUT);
                    niks.add(KEY_BATTERY_SAVER_SUMMARY);
                    return niks;
                }
            };

    public static final SummaryLoader.SummaryProviderFactory SUMMARY_PROVIDER_FACTORY
            = new SummaryLoader.SummaryProviderFactory() {
        @Override
        public SummaryLoader.SummaryProvider createSummaryProvider(Activity activity,
                SummaryLoader summaryLoader) {
            return new SummaryProvider(activity, summaryLoader);
        }
    };
}
