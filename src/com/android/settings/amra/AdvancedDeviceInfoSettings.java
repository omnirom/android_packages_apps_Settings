/*
 * Copyright (C) 2008 The Android Open Source Project
 * Modifications (C) 2014 The Amra Project
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

package com.android.settings.amra;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.os.Bundle;
import android.os.SystemProperties;
import android.preference.PreferenceGroup;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.RestrictedSettingsFragment;
import com.android.settings.amra.utils.Helpers;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.opengl.GLES20.GL_EXTENSIONS;
import static android.opengl.GLES20.GL_RENDERER;
import static android.opengl.GLES20.GL_SHADING_LANGUAGE_VERSION;
import static android.opengl.GLES20.GL_VENDOR;
import static android.opengl.GLES20.GL_VERSION;
import static android.opengl.GLES20.glGetString;

public class AdvancedDeviceInfoSettings extends RestrictedSettingsFragment {

    private static final String LOG_TAG = "AdvancedDeviceInfoSettings";

    private static final String FILENAME_PROC_VERSION = "/proc/version";
    private static final String FILENAME_PROC_MEMINFO = "/proc/meminfo";
    private static final String FILENAME_PROC_CPUINFO = "/proc/cpuinfo";

    private static final String KEY_KERNEL_VERSION = "kernel_version";
    private static final String KEY_DEVICE_CPU = "device_cpu";
    private static final String KEY_DEVICE_CPU_FEATURES = "device_cpu_features";
    private static final String KEY_DEVICE_MEMORY = "device_memory";

    private static final String KEY_DEVICE_GPU_GROUP = "device_gpu";
    private static final String KEY_DEVICE_GPU_VENDOR = "device_gpu_vendor";
    private static final String KEY_DEVICE_GPU_RENDERER = "device_gpu_renderer";
    private static final String KEY_DEVICE_GPU_GL_VERSION = "device_gpu_gl_version";
    private static final String KEY_DEVICE_GPU_GL_EXTENSIONS = "device_gpu_gl_extensions";
    private static final String KEY_DEVICE_GPU_SHADER_VERSION = "device_gpu_shader_version";

    public AdvancedDeviceInfoSettings() {
        super(null /* Don't PIN protect the entire screen */);
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.device_advanced_info_settings);

        findPreference(KEY_KERNEL_VERSION).setSummary(getFormattedKernelVersion());

        final String cpuInfo = getCPUInfo();
        final String cpuFeatures = getCpuFeatures();
        final String memInfo = getMemInfo();

        if (cpuInfo != null) {
            setStringSummary(KEY_DEVICE_CPU, cpuInfo);
        } else {
            getPreferenceScreen().removePreference(findPreference(KEY_DEVICE_CPU));
        }

        if (cpuFeatures != null) {
            setStringSummary(KEY_DEVICE_CPU_FEATURES, cpuFeatures);
        } else {
            getPreferenceScreen().removePreference(findPreference(KEY_DEVICE_CPU_FEATURES));
        }

        if (memInfo != null) {
            setStringSummary(KEY_DEVICE_MEMORY, memInfo);
        } else {
            getPreferenceScreen().removePreference(findPreference(KEY_DEVICE_MEMORY));
        }

        final boolean supportsOpenGles20 = Helpers.supportsOpenGLES20(mContext);
        final PreferenceGroup gpuGroup = (PreferenceGroup) findPreference(KEY_DEVICE_GPU_GROUP);

        if (supportsOpenGles20) {
            final String gpuVendor = glGetString(GL_VENDOR);
            final String gpuName = glGetString(GL_RENDERER);
            final String gpuGlVersion = glGetString(GL_VERSION);
            final String gpuGlExtensions = glGetString(GL_EXTENSIONS);
            final String gpuGlShaderVersion = glGetString(GL_SHADING_LANGUAGE_VERSION);

            if (gpuVendor != null && !gpuVendor.isEmpty()) {
                setStringSummary(KEY_DEVICE_GPU_VENDOR, gpuVendor);
            } else {
                gpuGroup.removePreference(gpuGroup.findPreference(KEY_DEVICE_GPU_VENDOR));
            }

            if (gpuName != null && !gpuName.isEmpty()) {
                setStringSummary(KEY_DEVICE_GPU_RENDERER, gpuName);
            } else {
                gpuGroup.removePreference(gpuGroup.findPreference(KEY_DEVICE_GPU_RENDERER));
            }

            if (gpuGlVersion != null && !gpuGlVersion.isEmpty()) {
                setStringSummary(KEY_DEVICE_GPU_GL_VERSION, gpuGlVersion);
            } else {
                gpuGroup.removePreference(gpuGroup.findPreference(KEY_DEVICE_GPU_GL_VERSION));
            }

            if (gpuGlExtensions != null && !gpuGlExtensions.isEmpty()) {
                setStringSummary(KEY_DEVICE_GPU_GL_EXTENSIONS, gpuGlExtensions);
            } else {
                gpuGroup.removePreference(gpuGroup.findPreference(KEY_DEVICE_GPU_GL_EXTENSIONS));
            }

            if (gpuGlShaderVersion != null && !gpuGlShaderVersion.isEmpty()) {
                setStringSummary(KEY_DEVICE_GPU_SHADER_VERSION, gpuGlShaderVersion);
            } else {
                gpuGroup.removePreference(gpuGroup.findPreference(KEY_DEVICE_GPU_SHADER_VERSION));
            }
        } else {
            getPreferenceScreen().removePreference(gpuGroup);
        }

    }

    private void setStringSummary(String preference, String value) {
        try {
            findPreference(preference).setSummary(value);
        } catch (RuntimeException e) {
            findPreference(preference).setSummary(
                    getResources().getString(R.string.device_info_default));
        }
    }

    private void setValueSummary(String preference, String property) {
        try {
            findPreference(preference).setSummary(
                    SystemProperties.get(property,
                            getResources().getString(R.string.device_info_default)));
        } catch (RuntimeException e) {
            // No recovery
        }
    }

    /**
     * Reads a line from the specified file.
     *
     * @param filename the file to read from
     * @return the first line, if any.
     * @throws java.io.IOException if the file couldn't be read
     */
    private static String readLine(String filename) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filename), 256);
        try {
            return reader.readLine();
        } finally {
            reader.close();
        }
    }

    public static String getFormattedKernelVersion() {
        try {
            return formatKernelVersion(readLine(FILENAME_PROC_VERSION));

        } catch (IOException e) {
            Log.e(LOG_TAG,
                    "IO Exception when getting kernel version for Device Info screen",
                    e);

            return "Unavailable";
        }
    }

    public static String formatKernelVersion(String rawKernelVersion) {
        // Example (see tests for more):
        // Linux version 3.0.31-g6fb96c9 (android-build@xxx.xxx.xxx.xxx.com) \
        //     (gcc version 4.6.x-xxx 20120106 (prerelease) (GCC) ) #1 SMP PREEMPT \
        //     Thu Jun 28 11:02:39 PDT 2012

        final String PROC_VERSION_REGEX =
                "Linux version (\\S+) " + /* group 1: "3.0.31-g6fb96c9" */
                        "\\((\\S+?)\\) " +        /* group 2: "x@y.com" (kernel builder) */
                        "(?:\\(gcc.+? \\)) " +    /* ignore: GCC version information */
                        "(#\\d+) " +              /* group 3: "#1" */
                        "(?:.*?)?" +              /* ignore: optional SMP, PREEMPT, and any CONFIG_FLAGS */
                        "((Sun|Mon|Tue|Wed|Thu|Fri|Sat).+)"; /* group 4: "Thu Jun 28 11:02:39 PDT 2012" */

        Matcher m = Pattern.compile(PROC_VERSION_REGEX).matcher(rawKernelVersion);
        if (!m.matches()) {
            Log.e(LOG_TAG, "Regex did not match on /proc/version: " + rawKernelVersion);
            return "Unavailable";
        } else if (m.groupCount() < 4) {
            Log.e(LOG_TAG, "Regex match on /proc/version only returned " + m.groupCount()
                    + " groups");
            return "Unavailable";
        }
        return m.group(1) + "\n" +                 // 3.0.31-g6fb96c9
                m.group(2) + " " + m.group(3) + "\n" + // x@y.com #1
                m.group(4);                            // Thu Jun 28 11:02:39 PDT 2012
    }

    private String getMemInfo() {
        String result = null;
        BufferedReader reader = null;

        try {
            /* /proc/meminfo entries follow this format:
             * MemTotal:         362096 kB
             * MemFree:           29144 kB
             * Buffers:            5236 kB
             * Cached:            81652 kB
             */
            String firstLine = readLine(FILENAME_PROC_MEMINFO);
            if (firstLine != null) {
                String parts[] = firstLine.split("\\s+");
                if (parts.length == 3) {
                    result = Long.parseLong(parts[1]) / 1024 + " MB";
                }
            }
        } catch (IOException e) {
        }

        return result;
    }

    private String getCPUInfo() {
        String result = null;

        try {
            /* The expected /proc/cpuinfo output is as follows:
             * Processor	: ARMv7 Processor rev 2 (v7l)
             * BogoMIPS	: 272.62
             */
            String firstLine = readLine(FILENAME_PROC_CPUINFO);
            if (firstLine != null) {
                result = firstLine.split(":")[1].trim();
            }
        } catch (IOException e) {
        }

        return result;
    }

    private String getCpuFeatures() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(FILENAME_PROC_CPUINFO), 256);
            try {
                String tmp;
                while ((tmp = reader.readLine().toLowerCase()) != null) {
                    if (tmp.contains("features")) {
                        return tmp.split(":")[1].trim();
                    }
                }
                return null;
            } finally {
                reader.close();
            }
        } catch (IOException exc) {
            return null;
        }
    }
}
