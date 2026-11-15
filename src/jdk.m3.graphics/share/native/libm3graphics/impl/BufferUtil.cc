#include "jdk_m3_graphics_impl_M3BufferUtil.h"
#include <jni.h>
#include "../interop.hh"

namespace {
    jclass klass_IllegalArgumentException = nullptr;
    jmethodID method_Ctor = nullptr;
}

extern "C" {
    JNIEXPORT jobject JNICALL Java_jdk_m3_graphics_impl_M3BufferUtil__1nGetByteBufferFromPointer
  (JNIEnv* env, jclass s, jlong ptr, jint size) {
        return env->NewDirectByteBuffer(jlongToPtr<void*>(ptr), size);
    }

    JNIEXPORT jlong JNICALL Java_jdk_m3_graphics_impl_M3BufferUtil__1nGetPointerFromByteBuffer
  (JNIEnv* env, jclass s, jobject buffer) {
        return ptrToJlong(env->GetDirectBufferAddress(buffer));
    }
}