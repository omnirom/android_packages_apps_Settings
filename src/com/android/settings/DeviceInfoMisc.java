/*
 * Copyright (C) 2014 AnimeROM
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

package com.android.settings.shockgensmod;

import com.android.settings.R;

import android.app.Activity;
import android.widget.TextView;
import android.os.Bundle;
import android.view.View;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemProperties;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DeviceInfoMisc extends Activity {

    private TextView mDeviceCpus;
    private TextView mMaxCpus;
    private TextView mDeviceMemorys;
    private TextView mFirmwares;
    private TextView mModels;
    private TextView mKernels;
    public static final String FREQ_MAX_FILES = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.device_info_misc);

        mDeviceCpus = (TextView) findViewById(R.id.device_cpus);
        mDeviceCpus.setText(getCPUInfo());
        mDeviceMemorys = (TextView) findViewById(R.id.device_memorys);
        mDeviceMemorys.setText(getMemTotal().toString()+" MB");
        mFirmwares = (TextView) findViewById(R.id.firmware_versions);
        mFirmwares.setText("v01");  // Set AnimeROM version
        mModels = (TextView) findViewById(R.id.model_numbers);
        mModels.setText("AnimeROM");
        mKernels = (TextView) findViewById(R.id.kernel_versions);
        mKernels.setText(getFormattedKernelVersion());
        mMaxCpus = (TextView) findViewById(R.id.max_cpus);
        mMaxCpus.setText(toMHz(readOneLine(FREQ_MAX_FILES)));
    }

    @Override
    public void onResume() {
        super.onResume();
        mMaxCpus.setText(toMHz(readOneLine(FREQ_MAX_FILES)));
    }

    private Long getMemTotal() {
      Long total = null;
      BufferedReader reader = null;

      try {
         // Grab a reader to /proc/meminfo
         reader = new BufferedReader(new InputStreamReader(new FileInputStream("/proc/meminfo")), 1000);

         // Grab the first line which contains mem total
         String line = reader.readLine();

         // Split line on the colon, we need info to the right of the colon
         String[] info = line.split(":");

         // We have to remove the kb on the end
         String[] memTotal = info[1].trim().split(" ");

         // Convert kb into mb
         total = Long.parseLong(memTotal[0]);
         total = total / 1024;
      }
      catch(Exception e) {
         e.printStackTrace();
         // We don't want to return null so default to 0
         total = Long.parseLong("0");
      }
      finally {
         // Make sure the reader is closed no matter what
         try { reader.close(); }
         catch(Exception e) {}
         reader = null;
      }

      return total;
    }

   private String getCPUInfo() {
      String[] info = null;
      BufferedReader reader = null;

      try {
         // Grab a reader to /proc/cpuinfo
         reader = new BufferedReader(new InputStreamReader(new FileInputStream("/proc/cpuinfo")), 1000);

         // Grab a single line from cpuinfo
         String line = reader.readLine();

         // Split on the colon, we need info to the right of colon
         info = line.split(":");
      }
      catch(IOException io) {
         io.printStackTrace();
         info = new String[1];
         info[1] = "error";
      }
      finally {
         // Make sure the reader is closed no matter what
         try { reader.close(); }
         catch(Exception e) {}
         reader = null;
      }

      return info[1];
    }

    private String getFormattedKernelVersion() {
        String procVersionStr;

        try {
            BufferedReader reader = new BufferedReader(new FileReader("/proc/version"), 256);
            try {
                procVersionStr = reader.readLine();
            } finally {
                reader.close();
            }

            final String PROC_VERSION_REGEX =
                "\\w+\\s+" + /* ignore: Linux */
                "\\w+\\s+" + /* ignore: version */
                "([^\\s]+)\\s+" + /* group 1: 2.6.22-omap1 */
                "\\(([^\\s@]+(?:@[^\\s.]+)?)[^)]*\\)\\s+" + /* group 2: (xxxxxx@xxxxx.constant) */
                "\\((?:[^(]*\\([^)]*\\))?[^)]*\\)\\s+" + /* ignore: (gcc ..) */
                "([^\\s]+)\\s+" + /* group 3: #26 */
                "(?:PREEMPT\\s+)?" + /* ignore: PREEMPT (optional) */
                "(.+)"; /* group 4: date */

            Pattern p = Pattern.compile(PROC_VERSION_REGEX);
            Matcher m = p.matcher(procVersionStr);

            if (!m.matches()) {
                return "Unavailable";
            } else if (m.groupCount() < 4) {
                return "Unavailable";
            } else {
                return (new StringBuilder(m.group(1)).append("\n").append(
                        m.group(2)).append(" ").append(m.group(3)).append("\n")
                        .append(m.group(4))).toString();
            }
        } catch (IOException e) {
            return "Unavailable";
        }
    }

    private static boolean fileExists(String filename) {
        return new File(filename).exists();
    }

    private static String readOneLine(String fname) {
        BufferedReader br;
        String line = null;

        try {
            br = new BufferedReader(new FileReader(fname), 512);
            try {
                line = br.readLine();
            } finally {
                br.close();
            }
        } catch (Exception e) {
        }
        return line;
    }

    private String toMHz(String mhzString) {
        if (mhzString == null)
            return "-";
        return new StringBuilder().append(Integer.valueOf(mhzString) / 1000).append(" MHz").toString();
    }
}
