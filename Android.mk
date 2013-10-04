LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_JAVA_LIBRARIES := bouncycastle telephony-common
LOCAL_STATIC_JAVA_LIBRARIES := android-support-v4 android-support-v13 jsr305

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_SRC_FILES += $(call all-java-files-under, ../OmniGears/src)
LOCAL_SRC_FILES += $(call all-java-files-under, ../PerformanceControl/src)

LOCAL_RESOURCE_DIR += $(LOCAL_PATH)/res
LOCAL_RESOURCE_DIR += packages/apps/OmniGears/res
LOCAL_RESOURCE_DIR += packages/apps/PerformanceControl/res

LOCAL_ASSET_DIR += packages/apps/PerformanceControl/assets

LOCAL_AAPT_FLAGS := --auto-add-overlay \
	--extra-packages org.omnirom.omnigears \
	--extra-packages com.brewcrewfoo.performance

LOCAL_PACKAGE_NAME := Settings
LOCAL_CERTIFICATE := platform

LOCAL_PROGUARD_FLAG_FILES := proguard.flags

LOCAL_AAPT_FLAGS += -c zz_ZZ

include $(BUILD_PACKAGE)

# Use the folloing include to make our test apk.
include $(call all-makefiles-under,$(LOCAL_PATH))
