LOCAL_PATH := $(call my-dir)

KITTY_ROOT  := $(LOCAL_PATH)/KittyMemoryEx
KITTY_INNER := $(KITTY_ROOT)/KittyMemoryEx

KITTY_SRC := $(patsubst $(LOCAL_PATH)/%,%,$(shell find $(KITTY_INNER) -name '*.cpp' 2>/dev/null))
KITTY_INC := $(shell find $(KITTY_INNER) -type d 2>/dev/null)

include $(CLEAR_VARS)
LOCAL_MODULE     := memscan
LOCAL_SRC_FILES  := MemScan.cpp $(KITTY_SRC)
LOCAL_C_INCLUDES := $(LOCAL_PATH) $(KITTY_ROOT) $(KITTY_INC)
LOCAL_LDLIBS     := -llog -ldl -landroid
LOCAL_CPPFLAGS   := -std=c++17 -fexceptions -frtti -DkNO_KEYSTONE -O2
include $(BUILD_SHARED_LIBRARY)
