/*
 *  Copyright (C) 2014 The OmniROM Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.android.settings.omni.sounds;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import android.content.ContentResolver;
import android.os.Bundle;
import android.os.Environment;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class SoundPackageSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "SoundPackageSettings";

    private static final String INSTALL_SOUND_PACK = "install_sound_pack";
    private static final String SELECT_SOUND_PACK = "select_sound_pack";

    private static final String SOUND_PACKS_LOCATION = Environment
            .getExternalStorageDirectory().getAbsolutePath() + "/Amra/SoundPacks/";
    private static final String INSTALLED_PACKS_LOCATION = "/data/system/soundpacks/";

    private ListPreference mInstallSoundPack;
    private ListPreference mSelectSoundPack;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.sound_package_settings);

        PreferenceScreen prefSet = getPreferenceScreen();

        mInstallSoundPack = (ListPreference) prefSet
                .findPreference(INSTALL_SOUND_PACK);
        mInstallSoundPack.setOnPreferenceChangeListener(this);

        mSelectSoundPack = (ListPreference) prefSet
                .findPreference(SELECT_SOUND_PACK);
        mSelectSoundPack.setOnPreferenceChangeListener(this);

        updatePacks();
    }

    private void updatePacks() {
        final ContentResolver resolver = getActivity().getContentResolver();
        List<String> installablePacks = getInstallablePacks();

        if (installablePacks != null && installablePacks.size() > 0) {
            CharSequence[] packs = listToCharSeqArray(installablePacks, false);
            mInstallSoundPack.setEntries(packs);
            mInstallSoundPack.setEntryValues(packs);
            mInstallSoundPack.setEnabled(true);
        } else {
            mInstallSoundPack.setSummary(
                    getString(R.string.install_sound_pack_summary_disabled, SOUND_PACKS_LOCATION));
            mInstallSoundPack.setEnabled(false);
        }

        List<String> selectablePacks = getInstalledPacks();

        if (selectablePacks != null && selectablePacks.size() > 0) {
            CharSequence[] packs = listToCharSeqArray(selectablePacks, true);
            mSelectSoundPack.setEntries(packs);
            mSelectSoundPack.setEntryValues(packs);
            mSelectSoundPack.setEnabled(true);
        } else {
            mSelectSoundPack.setEnabled(false);
        }

        String activePack = Settings.System.getString(resolver,
                Settings.System.CUSTOM_SOUND_EFFECTS_PATH);
        if (activePack == null
                || activePack.equals(getResources().getString(
                        R.string.default_sound_pack)) || activePack.isEmpty()) {
            mSelectSoundPack.setValueIndex(0);
        } else {
            mSelectSoundPack.setValue(packPathToName(activePack));
        }
    }

    private static String packPathToName(String path) {
        String output = path.substring(0, path.length() - 1);
        output = output.substring(output.lastIndexOf('/') + 1);
        return output;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        return true;
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference == mInstallSoundPack) {
            try {
                installPack((String) objValue);
            } catch (IOException e) {
                Toast.makeText(getActivity(),
                        getResources().getString(R.string.error_pack_install),
                        Toast.LENGTH_LONG).show();
                Log.e(TAG, "Unable to install sound pack", e);
            }
            return true;
        } else if (preference == mSelectSoundPack) {
            selectPack((String) objValue);
            return true;
        }

        return false;
    }

    private CharSequence[] listToCharSeqArray(List<String> list,
            boolean addDefault) {
        CharSequence[] packs = new CharSequence[addDefault ? list.size() + 1
                : list.size()];
        int i = addDefault ? 1 : 0;

        if (addDefault) {
            packs[0] = getResources().getString(R.string.default_sound_pack);
        }

        for (String pack : list) {
            packs[i] = pack;
            i++;
        }

        return packs;
    }

    public static String stripExtension(String str) {
        if (str == null)
            return null;
        int pos = str.lastIndexOf(".");
        if (pos == -1)
            return str;
        return str.substring(0, pos);
    }

    private List<String> getInstallablePacks() {
        File packsDir = new File(SOUND_PACKS_LOCATION);

        Log.d(TAG, "Looking for sound packs in " + SOUND_PACKS_LOCATION);

        if (!packsDir.exists()) {
            final int msgId = packsDir.mkdirs()
                    ? R.string.sound_pack_folder_created_success
                    : R.string.sound_pack_folder_created_failure;
            Toast.makeText(getActivity(), getString(msgId, SOUND_PACKS_LOCATION), Toast.LENGTH_LONG)
                    .show();
            return null; // jump out, directory didn't exist, so why should there be files in?
        }

        // Get all ZIP files in our soundpack install location
        File[] availPacks = packsDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                // Must be lowercase
                return filename.endsWith(".zip");
            }
        });

        if (availPacks == null || availPacks.length == 0) {
            Log.d(TAG, "No installable packs found!");
            return null;
        }

        // Filter the pack we already have
        List<String> installedPacks = getInstalledPacks();
        List<String> filteredPacks = new ArrayList<String>();

        for (File pack : availPacks) {
            String packName = stripExtension(pack.getName());

            // XXX: originally, we had this. However, this can cause troubles
            // when designing
            // your sound package as you cannot reinstall a package that has
            // already been
            // installed (unless you change name every time).
            // We keep on displaying installable already installed packs instead
            // until we
            // implement package uninstaller
            /*
             * if (installedPacks == null || (installedPacks.size() > 0 &&
             * !installedPacks .contains(packName))) {
             * filteredPacks.add(packName); }
             */
            filteredPacks.add(packName);
        }

        return filteredPacks;
    }

    private List<String> getInstalledPacks() {
        File packsDir = new File(INSTALLED_PACKS_LOCATION);
        if (packsDir.list() != null) {
            return Arrays.asList(packsDir.list());
        } else {
            return null;
        }
    }

    private void extractFile(ZipInputStream zipIn, String filePath)
            throws IOException {
        BufferedOutputStream bos = new BufferedOutputStream(
                new FileOutputStream(filePath));
        byte[] bytesIn = new byte[8192];
        int read = 0;
        while ((read = zipIn.read(bytesIn)) != -1) {
            bos.write(bytesIn, 0, read);
        }
        bos.close();
    }

    private void selectPack(String packName) {
        final ContentResolver resolver = getActivity().getContentResolver();

        if (packName.equals(getResources().getString(
                R.string.default_sound_pack))) {
            Settings.System.putString(resolver,
                    Settings.System.CUSTOM_SOUND_EFFECTS_PATH, null);
        } else {
            Settings.System.putString(resolver,
                    Settings.System.CUSTOM_SOUND_EFFECTS_PATH,
                    INSTALLED_PACKS_LOCATION + packName + "/");
        }

    }

    private void installPack(String packName) throws IOException {
        // Copy package contents (.ogg and .xml only) to
        // /data/system/soundpacks/<name>/
        File rootDir = new File(INSTALLED_PACKS_LOCATION);
        if (!rootDir.exists()) {
            rootDir.mkdir();
            rootDir.setWritable(true, true);
            rootDir.setReadable(true, false);
            rootDir.setExecutable(true, false);
        }
        File destDir = new File(INSTALLED_PACKS_LOCATION + packName);
        if (!destDir.exists()) {
            destDir.mkdir();
            destDir.setWritable(true, true);
            destDir.setReadable(true, false);
            destDir.setExecutable(true, false);
        }

        ZipInputStream zipIn = new ZipInputStream(new FileInputStream(
                SOUND_PACKS_LOCATION + packName + ".zip"));

        ZipEntry entry = zipIn.getNextEntry();

        while (entry != null) {
            if (!entry.getName().endsWith(".xml")
                    && !entry.getName().endsWith(".ogg")) {
                // We only care about xml and ogg files
                zipIn.closeEntry();
                entry = zipIn.getNextEntry();
                continue;
            }

            String filePath = INSTALLED_PACKS_LOCATION + packName + "/"
                    + entry.getName();
            if (!entry.isDirectory()) {
                extractFile(zipIn, filePath);

                // Update permissions (644 / rw-r--r--)
                File copied = new File(filePath);
                copied.setExecutable(false);
                copied.setReadable(true, false);
                copied.setWritable(true, true);
            } else {
                File dir = new File(filePath);
                dir.mkdir();
                destDir.setWritable(true, true);
                destDir.setReadable(true, false);
                destDir.setExecutable(true, false);
            }

            zipIn.closeEntry();
            entry = zipIn.getNextEntry();
        }
        zipIn.close();

        updatePacks();
    }
}
