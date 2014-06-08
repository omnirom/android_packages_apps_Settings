/*
 * Performance Control - An Android CPU Control application Copyright (C) 2012
 * James Roberts
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.android.settings.util;

public interface Constants {

    //hide flashing kernel/recovery options
    // NO_FLASH=true > hide flash options
    // NO_FLASH=false > show flash options
    public static final Boolean NO_FLASH = false;

    // Fragment IDs
    public static final int FRAGMENT_ID_CPUSETTINGS = 0;
    public static final int FRAGMENT_ID_BATTERYINFO = 1;
    public static final int FRAGMENT_ID_OOMSETTINGS = 2;
    public static final int FRAGMENT_ID_VM = 3;
    public static final int FRAGMENT_ID_VOLTAGECONROL = 4;
    public static final int FRAGMENT_ID_ADVANCED = 5;
    public static final int FRAGMENT_ID_TIMEINSTATE = 6;
    public static final int FRAGMENT_ID_CPUINFO = 7;
    public static final int FRAGMENT_ID_DISKINFO = 8;
    public static final int FRAGMENT_ID_TOOLS = 9;

    // CPU settings
    public static final String CPU_PATH = "/sys/devices/system/cpu/cpu";
    public static final String CPU_FREQ_TAIL = "/cpufreq/scaling_cur_freq";
    public static final String CUR_CPU_PATH = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq";
    public static final String MAX_FREQ_PATH = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq";
    public static final String TEGRA_MAX_FREQ_PATH = "/sys/module/cpu_tegra/parameters/cpu_user_cap";
    public static final String MIN_FREQ_PATH = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq";
    public static final String STEPS_PATH = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_available_frequencies";
    public static final String GOVERNORS_LIST_PATH = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_available_governors";
    public static final String GOVERNOR_PATH = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor";
    public static final String[] IO_SCHEDULER_PATH = {"/sys/block/mmcblk0/queue/scheduler", "/sys/block/mmcblk1/queue/scheduler"};
    //Dynamic frequency scaling
    public static final String DYN_MAX_FREQ_PATH = "/sys/power/cpufreq_max_limit";
    public static final String DYN_MIN_FREQ_PATH = "/sys/power/cpufreq_min_limit";

    public static final String NUM_OF_CPUS_PATH = "/sys/devices/system/cpu/present";

    public static final String PREF_MAX_CPU = "pref_max_cpu";
    public static final String PREF_MIN_CPU = "pref_min_cpu";
    public static final String PREF_GOV = "pref_gov";
    public static final String PREF_IO = "pref_io";
    public static final String CPU_SOB = "cpu_sob";
    public static final String GOV_SOB = "gov_settings_sob";
    public static final String GOV_SETTINGS = "gov_settings";
    public static final String GOV_NAME = "gov_name";
    public static final String GOV_SETTINGS_PATH = "/sys/devices/system/cpu/cpufreq/";

    // CPU info
    public static String KERNEL_INFO_PATH = "/proc/version";
    public static String CPU_INFO_PATH = "/proc/cpuinfo";
    public static String MEM_INFO_PATH = "/proc/meminfo";

    // Time in state
    public static final String TIME_IN_STATE_PATH = "/sys/devices/system/cpu/cpu0/cpufreq/stats/time_in_state";
    public static final String TIME_IN_STATE_TAIL = "/cpufreq/stats/time_in_state";
    public static final String PREF_OFFSETS = "pref_offsets";
    // Battery
    public static final String BAT_VOLT_PATH = "/sys/class/power_supply/battery/voltage_now";

    // Other settings
    public static final String MINFREE_PATH = "/sys/module/lowmemorykiller/parameters/minfree";
    public static final String MINFREE_ADJ_PATH = "/sys/module/lowmemorykiller/parameters/adj";
    public static final String READ_AHEAD_PATH = "/sys/block/mmcblk0/bdi/read_ahead_kb";
    //"/sys/devices/virtual/bdi/default/read_ahead_kb"

    public static final String INTENT_ACTION_FASTCHARGE = "com.aokp.romcontrol.FCHARGE_CHANGED";
    public static final String PREF_MINFREE = "pref_minfree";
    public static final String PREF_MINFREE_BOOT = "pref_minfree_boot";
    public static final String PREF_READ_AHEAD = "pref_read_ahead";
    public static final String PREF_READ_AHEAD_BOOT = "pref_read_ahead_boot";
    public static final String PREF_FASTCHARGE = "pref_fast_charge";
    //------ MinFree ------
    public static final String OOM_FOREGROUND_APP = "oom_foreground_app";
    public static final String OOM_VISIBLE_APP = "oom_visible_app";
    public static final String OOM_SECONDARY_SERVER = "oom_secondary_server";
    public static final String OOM_HIDDEN_APP = "oom_hidden_app";
    public static final String OOM_CONTENT_PROVIDERS = "oom_content_providers";
    public static final String OOM_EMPTY_APP = "oom_empty_app";
    //------ KSM
    public static final String KSM_RUN_PATH = "/sys/kernel/mm/ksm/run";
    public static final String KSM_FULLSCANS_PATH = "/sys/kernel/mm/ksm/full_scans";
    public static final String KSM_PAGESSHARED_PATH = "/sys/kernel/mm/ksm/pages_shared";
    public static final String KSM_PAGESSHARING_PATH = "/sys/kernel/mm/ksm/pages_sharing";
    public static final String KSM_PAGESTOSCAN_PATH = "/sys/kernel/mm/ksm/pages_to_scan";
    public static final String KSM_PAGESUNSHERED_PATH = "/sys/kernel/mm/ksm/pages_unshared";
    public static final String KSM_PAGESVOLATILE_PATH = "/sys/kernel/mm/ksm/pages_volatile";
    public static final String KSM_SLEEP_PATH = "/sys/kernel/mm/ksm/sleep_millisecs";
    public static final String PREF_RUN_KSM = "pref_run_ksm";
    public static final String KSM_SOB = "ksm_boot";

    //------ DoNotKillProc
    public static final String USER_PROC_PATH = "/sys/module/lowmemorykiller/parameters/donotkill_proc";
    public static final String SYS_PROC_PATH = "/sys/module/lowmemorykiller/parameters/donotkill_sysproc";
    public static final String USER_PROC_NAMES_PATH = "/sys/module/lowmemorykiller/parameters/donotkill_proc_names";
    public static final String USER_SYS_NAMES_PATH = "/sys/module/lowmemorykiller/parameters/donotkill_sysproc_names";
    public static final String USER_PROC_SOB = "user_proc_boot";
    public static final String SYS_PROC_SOB = "sys_proc_boot";
    public static final String PREF_USER_PROC = "pref_user_proc";
    public static final String PREF_SYS_PROC = "pref_sys_proc";
    public static final String PREF_USER_NAMES = "pref_user_names_proc";
    public static final String PREF_SYS_NAMES = "pref_sys_names_proc";
    //-------BLX---------
    public static final String PREF_BLX = "pref_blx";
    public static final String BLX_PATH = "/sys/class/misc/batterylifeextender/charging_limit";
    public static final String BLX_SOB = "blx_sob";
    //-------DFsync---------
    public static final String DSYNC_PATH = "/sys/kernel/dyn_fsync/Dyn_fsync_active";
    public static final String PREF_DSYNC = "pref_dsync";
    //-------BL----
    public static final String PREF_BLTIMEOUT = "pref_bltimeout";
    public static final String BLTIMEOUT_SOB = "bltimeout_sob";
    public static final String PREF_BLTOUCH = "pref_bltouch";
    public static final String BL_TIMEOUT_PATH = "/sys/class/misc/notification/bl_timeout";
    public static final String BL_TOUCH_ON_PATH = "/sys/class/misc/notification/touchlight_enabled";
    //-------BLN---------
    public static final String PREF_BLN = "pref_bln";
    //-------PFK---------
    public static final String PFK_VER = "/sys/class/misc/phantom_kp_filter/version";
    public static final String PFK_HOME_ON = "pfk_home_on";
    public static final String PREF_HOME_ALLOWED_IRQ = "pref_home_allowed_irq";
    public static final String PREF_HOME_REPORT_WAIT = "pref_home_report_wait";
    public static final String PFK_MENUBACK_ON = "pfk_menuback_on";
    public static final String PREF_MENUBACK_INTERRUPT_CHECKS = "pref_menuback_interrupt_checks";
    public static final String PREF_MENUBACK_FIRST_ERR_WAIT = "pref_menuback_first_err_wait";
    public static final String PREF_MENUBACK_LAST_ERR_WAIT = "pref_menuback_last_err_wait";

    public static final String PFK_HOME_ENABLED = "/sys/class/misc/phantom_kp_filter/home_enabled";
    public static final String PFK_HOME_ALLOWED_IRQ = "/sys/class/misc/phantom_kp_filter/home_allowed_irqs";
    public static final String PFK_HOME_REPORT_WAIT = "/sys/class/misc/phantom_kp_filter/home_report_wait";
    public static final String PFK_HOME_IGNORED_KP = "/sys/class/misc/phantom_kp_filter/home_ignored_kp";
    public static final String PFK_MENUBACK_ENABLED = "/sys/class/misc/phantom_kp_filter/menuback_enabled";
    public static final String PFK_MENUBACK_INTERRUPT_CHECKS = "/sys/class/misc/phantom_kp_filter/menuback_interrupt_checks";
    public static final String PFK_MENUBACK_FIRST_ERR_WAIT = "/sys/class/misc/phantom_kp_filter/menuback_first_err_wait";
    public static final String PFK_MENUBACK_LAST_ERR_WAIT = "/sys/class/misc/phantom_kp_filter/menuback_last_err_wait";
    public static final String PFK_MENUBACK_IGNORED_KP = "/sys/class/misc/phantom_kp_filter/menuback_ignored_kp";
    public static final String PFK_SOB = "pfk_sob";
    //------------------
    public static final String DYNAMIC_DIRTY_WRITEBACK_PATH = "/proc/sys/vm/dynamic_dirty_writeback";
    public static final String DIRTY_WRITEBACK_ACTIVE_PATH = "/proc/sys/vm/dirty_writeback_active_centisecs";
    public static final String DIRTY_WRITEBACK_SUSPEND_PATH = "/proc/sys/vm/dirty_writeback_suspend_centisecs";
    public static final String PREF_DYNAMIC_DIRTY_WRITEBACK = "pref_dynamic_dirty_writeback";
    public static final String PREF_DIRTY_WRITEBACK_ACTIVE = "pref_dynamic_writeback_active";
    public static final String PREF_DIRTY_WRITEBACK_SUSPEND = "pref_dynamic_writeback_suspend";
    public static final String DYNAMIC_DIRTY_WRITEBACK_SOB = "dynamic_write_back_sob";

    // VM settings
    public static final String PREF_DIRTY_RATIO = "pref_dirty_ratio";
    public static final String PREF_DIRTY_BACKGROUND = "pref_dirty_background";
    public static final String PREF_DIRTY_EXPIRE = "pref_dirty_expire";
    public static final String PREF_DIRTY_WRITEBACK = "pref_dirty_writeback";
    public static final String PREF_MIN_FREE_KB = "pref_min_free_kb";
    public static final String PREF_OVERCOMMIT = "pref_overcommit";
    public static final String PREF_SWAPPINESS = "pref_swappiness";
    public static final String PREF_VFS = "pref_vfs";
    public static final String DIRTY_RATIO_PATH = "/proc/sys/vm/dirty_ratio";
    public static final String DIRTY_BACKGROUND_PATH = "/proc/sys/vm/dirty_background_ratio";
    public static final String DIRTY_EXPIRE_PATH = "/proc/sys/vm/dirty_expire_centisecs";
    public static final String DIRTY_WRITEBACK_PATH = "/proc/sys/vm/dirty_writeback_centisecs";
    public static final String MIN_FREE_PATH = "/proc/sys/vm/min_free_kbytes";
    public static final String OVERCOMMIT_PATH = "/proc/sys/vm/overcommit_ratio";
    public static final String SWAPPINESS_PATH = "/proc/sys/vm/swappiness";
    public static final String VFS_CACHE_PRESSURE_PATH = "/proc/sys/vm/vfs_cache_pressure";
    public static final String VM_SOB = "vm_sob";

    // Voltage control
    public static final String VOLTAGE_SOB = "voltage_sob";
    public static final String UV_MV_PATH = "/sys/devices/system/cpu/cpu0/cpufreq/UV_mV_table";
    public static final String VDD_PATH = "/sys/devices/system/cpu/cpu0/cpufreq/vdd_levels";
    public static final String COMMON_VDD_PATH = "/sys/devices/system/cpu/cpufreq/vdd_levels";
    public static final String VDD_SYSFS_PATH = "/sys/devices/system/cpu/cpu0/cpufreq/vdd_sysfs_levels";

    //Tools
    public static final String PREF_SH = "pref_sh";
    public static final String PREF_WIPE_CACHE = "pref_wipe_cache";
    public static final String NOT_FOUND = "not found";
    public static final String FLASH_KERNEL = "pref_kernel_img";
    public static final String FLASH_RECOVERY = "pref_recovery_img";
    public static final String RESIDUAL_FILES = "pref_residual_files";
    public static final String residualfiles[] = {"/data/log", "/data/tombstones", "/data/system/dropbox", "/data/system/usagestats", "/data/anr", "/data/local/tmp"};//add coresponding info in strings
    public static final String PREF_FIX_PERMS = "pref_fix_perms";
    public static final String PREF_LOG = "pref_log";
    public static final String PREF_OPTIM_DB = "pref_optim_db";

    //Freezer
    public static final String PREF_FRREZE = "freeze_packs";
    public static final String PREF_UNFRREZE = "unfreeze_packs";

    //zRam
    public static final String ISZRAM = "busybox echo `busybox zcat /proc/config.gz | busybox grep ZRAM | busybox grep -v ^#'`";
    public static final String ZRAM_DEV = "/dev/block/zram0";
    public static final String ZRAM_SIZE_PATH = "/sys/block/zram0/disksize";
    public static final String ZRAM_RESET_PATH = "/sys/block/zram0/reset";
    public static final String ZRAM_COMPR_PATH = "/sys/block/zram0/compr_data_size";
    public static final String ZRAM_ORIG_PATH = "/sys/block/zram0/orig_data_size";
    public static final String ZRAM_MEMTOT_PATH = "/sys/block/zram0/mem_used_total";

    // PC Settings
    public static final String PREF_USE_LIGHT_THEME = "use_light_theme";
    public static final String PREF_WIDGET_BG_COLOR = "widget_bg_color";
    public static final String PREF_WIDGET_TEXT_COLOR = "widget_text_color";

}
