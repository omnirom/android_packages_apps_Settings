LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_JAVA_LIBRARIES := bouncycastle conscrypt telephony-common
LOCAL_STATIC_JAVA_LIBRARIES := android-support-v4 android-support-v13 jsr305

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-java-files-under, src) \
        src/com/android/settings/EventLogTags.logtags
LOCAL_SRC_FILES += $(call all-java-files-under, ../AmraSettings/src)
LOCAL_SRC_FILES += $(call all-java-files-under, ../PerformanceControl/src)

LOCAL_SRC_FILES += \
       ../AmraSettings/src/org/regulus/amrasettings/screencolor/IPPService.aidl

LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res
LOCAL_RESOURCE_DIR += packages/apps/AmraSettings/res
LOCAL_RESOURCE_DIR += packages/apps/PerformanceControl/res

LOCAL_ASSET_DIR += packages/apps/PerformanceControl/assets

LOCAL_AAPT_INCLUDE_ALL_RESOURCES := true

LOCAL_AAPT_FLAGS := --auto-add-overlay \
	--extra-packages org.regulus.amrasettings \
	--extra-packages com.brewcrewfoo.performance

LOCAL_PACKAGE_NAME := Settings
LOCAL_CERTIFICATE := platform
LOCAL_PRIVILEGED_MODULE := true

LOCAL_PROGUARD_FLAG_FILES := proguard.flags

LOCAL_JAVA_LIBRARIES += org.cyanogenmod.hardware

LOCAL_AAPT_FLAGS += -c zz_ZZ

LOCAL_AAPT_INCLUDE_ALL_RESOURCES := true
LOCAL_AAPT_FLAGS += --extra-packages com.koushikdutta.superuser:com.koushikdutta.widgets --auto-add-overlay

LOCAL_SRC_FILES += $(call all-java-files-under,../../../external/amra/Superuser/Superuser/src) $(call all-java-files-under,../../../external/amra/Widgets/Widgets/src)
LOCAL_RESOURCE_DIR += $(LOCAL_PATH)/../../../external/amra/Widgets/Widgets/res $(LOCAL_PATH)/../../../external/amra/Superuser/Superuser/res

include $(BUILD_PACKAGE)

# Use the folloing include to make our test apk.
include $(call all-makefiles-under,$(LOCAL_PATH))
