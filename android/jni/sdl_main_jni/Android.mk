LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := sdl_main_jni

ifndef SDL_JAVA_PACKAGE_PATH
$(error Please define SDL_JAVA_PACKAGE_PATH to the path of your Java package with dots replaced with underscores, for example "com_example_SanAngeles")
endif

LOCAL_C_INCLUDES := $(LOCAL_PATH)/../sdl-1.2_jni/include
LOCAL_CFLAGS := -DSDL_JAVA_PACKAGE_PATH=$(SDL_JAVA_PACKAGE_PATH) -DSDL_ANDROID_PACKAGE_NAME=\"$(SDL_ANDROID_PACKAGE_NAME)\"

LOCAL_CPP_EXTENSION := .cpp

LOCAL_SRC_FILES := sdl_main.c

LOCAL_SHARED_LIBRARIES := sdl-1.2_jni
LOCAL_LDLIBS := -llog -ldl

include $(BUILD_SHARED_LIBRARY)
