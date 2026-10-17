LOCAL_PATH := $(call my-dir)

# KittyMemoryEx sources
KITTY_PATH := $(LOCAL_PATH)/KittyMemoryEx
KITTY_SRC  := $(wildcard $(KITTY_PATH)/*.cpp)

include $(CLEAR_VARS)
LOCAL_MODULE           := memscan
LOCAL_SRC_FILES        := MemScan.cpp $(KITTY_SRC:$(LOCAL_PATH)/%=%)
LOCAL_LDLIBS           := -llog -ldl
LOCAL_CPPFLAGS         := -std=c++17 -fexceptions -frtti -DkNO_KEYSTONE
LOCAL_C_INCLUDES       := $(LOCAL_PATH) $(KITTY_PATH)
include $(BUILD_SHARED_LIBRARY)
