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

import com.android.settings.R;
import com.android.settings.fuelgauge.PowerUsageDetail.DrainType;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.BatteryStats.Uid;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Contains information about package name, icon image, power usage about an
 * application or a system service.
 */
public class BatterySipper implements Comparable<BatterySipper> {
    final Context mContext;
    /* Cache cleared when PowerUsageSummary is destroyed */
    static final HashMap<String,UidToDetail> sUidCache = new HashMap<String,UidToDetail>();
    final ArrayList<BatterySipper> mRequestQueue;
    final Handler mHandler;
    String name;
    Drawable icon;
    int iconId; // For passing to the detail screen.
    Uid uidObj;
    double value;
    double[] values;
    DrainType drainType;
    long usageTime;
    long cpuTime;
    long gpsTime;
    long wifiRunningTime;
    long cpuFgTime;
    long wakeLockTime;
    long mobileRxBytes;
    long mobileTxBytes;
    long wifiRxBytes;
    long wifiTxBytes;
    double percent;
    double noCoveragePercent;
    String defaultPackageName;
    String[] mPackages;

    static class UidToDetail {
        String name;
        String packageName;
        Drawable icon;
    }

    BatterySipper(Context context, ArrayList<BatterySipper> requestQueue,
            Handler handler, String label, DrainType drainType,
            int iconId, Uid uid, double[] values) {
        mContext = context;
        mRequestQueue = requestQueue;
        mHandler = handler;
        this.values = values;
        name = label;
        this.drainType = drainType;
        if (iconId > 0) {
            icon = mContext.getResources().getDrawable(iconId);
        }
        if (values != null) value = values[0];
        if ((label == null || iconId == 0) && uid != null) {
            getQuickNameIconForUid(uid);
        }
        uidObj = uid;
    }

    double getSortValue() {
        return value;
    }

    double[] getValues() {
        return values;
    }

    public Drawable getIcon() {
        return icon;
    }

    /**
     * Gets the application name
     */
    public String getLabel() {
        return name;
    }

    @Override
    public int compareTo(BatterySipper other) {
        // Return the flipped value because we want the items in descending order
        return Double.compare(other.getSortValue(), getSortValue());
    }

    /**
     * Gets a list of packages associated with the current user
     */
    public String[] getPackages() {
        return mPackages;
    }

    public int getUid() {
        // Bail out if the current sipper is not an App sipper.
        if (uidObj == null) {
            return 0;
        }
        return uidObj.getUid();
    }

    void getQuickNameIconForUid(Uid uidObj) {
        final int uid = uidObj.getUid();
        final String uidString = Integer.toString(uid);
        if (sUidCache.containsKey(uidString)) {
            UidToDetail utd = sUidCache.get(uidString);
            if (utd != null) {
                defaultPackageName = utd.packageName;
                name = utd.name;
                icon = utd.icon;
                return;
            }
        }
        PackageManager pm = mContext.getPackageManager();
        String[] packages = pm.getPackagesForUid(uid);
        icon = pm.getDefaultActivityIcon();
        if (packages == null) {
            //name = Integer.toString(uid);
            if (uid == 0) {
                name = mContext.getResources().getString(R.string.process_kernel_label);
            } else if ("mediaserver".equals(name)) {
                name = mContext.getResources().getString(R.string.process_mediaserver_label);
            }
            iconId = R.drawable.ic_power_system;
            icon = mContext.getResources().getDrawable(iconId);
            return;
        } else {
            //name = packages[0];
        }
        if (mHandler != null) {
            synchronized (mRequestQueue) {
                mRequestQueue.add(this);
            }
        }
    }

    public static void clearUidCache() {
        sUidCache.clear();
    }

    /**
     * Loads the app label and icon image and stores into the cache.
     */
    public void loadNameAndIcon() {
        // Bail out if the current sipper is not an App sipper.
        if (uidObj == null) {
            return;
        }
        PackageManager pm = mContext.getPackageManager();
        final int uid = uidObj.getUid();
        final Drawable defaultActivityIcon = pm.getDefaultActivityIcon();
        mPackages = pm.getPackagesForUid(uid);
        if (mPackages == null) {
            name = Integer.toString(uid);
            return;
        }

        String[] packageLabels = new String[mPackages.length];
        System.arraycopy(mPackages, 0, packageLabels, 0, mPackages.length);

        int preferredIndex = -1;
        // Convert package names to user-facing labels where possible
        for (int i = 0; i < packageLabels.length; i++) {
            // Check if package matches preferred package
            if (packageLabels[i].equals(name)) preferredIndex = i;
            try {
                ApplicationInfo ai = pm.getApplicationInfo(packageLabels[i], 0);
                CharSequence label = ai.loadLabel(pm);
                if (label != null) {
                    packageLabels[i] = label.toString();
                }
                if (ai.icon != 0) {
                    defaultPackageName = mPackages[i];
                    icon = ai.loadIcon(pm);
                    break;
                }
            } catch (NameNotFoundException e) {
            }
        }
        if (icon == null) icon = defaultActivityIcon;

        if (packageLabels.length == 1) {
            name = packageLabels[0];
        } else {
            // Look for an official name for this UID.
            for (String pkgName : mPackages) {
                try {
                    final PackageInfo pi = pm.getPackageInfo(pkgName, 0);
                    if (pi.sharedUserLabel != 0) {
                        final CharSequence nm = pm.getText(pkgName,
                                pi.sharedUserLabel, pi.applicationInfo);
                        if (nm != null) {
                            name = nm.toString();
                            if (pi.applicationInfo.icon != 0) {
                                defaultPackageName = pkgName;
                                icon = pi.applicationInfo.loadIcon(pm);
                            }
                            break;
                        }
                    }
                } catch (PackageManager.NameNotFoundException e) {
                }
            }
        }
        final String uidString = Integer.toString(uidObj.getUid());
        UidToDetail utd = new UidToDetail();
        utd.name = name;
        utd.icon = icon;
        utd.packageName = defaultPackageName;
        sUidCache.put(uidString, utd);
        if (mHandler != null) {
            mHandler.sendMessage(
                    mHandler.obtainMessage(BatteryStatsHelper.MSG_UPDATE_NAME_ICON, this));
        }
    }
}
