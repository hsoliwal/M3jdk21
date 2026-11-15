#include "jdk_m3_graphics_impl_M3Managed.h"
#include <iostream>
#include <jni.h>
#include <cstdint>

typedef void (*FreeFunction)(void*);

extern "C" JNIEXPORT void JNICALL Java_jdk_m3_graphics_impl_M3Managed__1nInvokeFinalizer
  (JNIEnv* env, jclass jclass, jlong finalizerPtr, jlong ptr) {
    void* instance = reinterpret_cast<void*>(static_cast<uintptr_t>(ptr));
    FreeFunction finalizer = reinterpret_cast<FreeFunction>(static_cast<uintptr_t>(finalizerPtr));
    finalizer(instance);
}