#include "jdk_m3_graphics_impl_M3Library.h"
#include <jni.h>
#include "../interop.hh"
#include "../shaper/interop.hh"
#include "../skottie/interop.hh"
#include "../paragraph/interop.hh"
#include "../svg/interop.hh"

JavaVM* gJavaVM = nullptr;

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    JNIEnv* env;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK)
        return JNI_ERR;

    gJavaVM = vm;

    return JNI_VERSION_1_6;
}

extern "C" JNIEXPORT void JNICALL Java_jdk_m3_graphics_impl_M3Library__1nAfterLoad
  (JNIEnv* env, jclass jclass) {
    if (env->EnsureLocalCapacity(128) < 0) return;
    java::onLoad(env);
    if (env->ExceptionCheck()) return;
    types::onLoad(env);
    if (env->ExceptionCheck()) return;
    skija::onLoad(env);
    if (env->ExceptionCheck()) return;
    skija::shaper::onLoad(env);
    if (env->ExceptionCheck()) return;
    skija::skottie::onLoad(env);
    if (env->ExceptionCheck()) return;
    skija::paragraph::onLoad(env);
    if (env->ExceptionCheck()) return;
    skija::svg::onLoad(env);
}

JNIEXPORT void JNICALL JNI_OnUnload(JavaVM* vm, void* reserved) {
    JNIEnv* env;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK)
        return;

    skija::svg::onUnload(env);
    skija::paragraph::onUnload(env);
    skija::skottie::onUnload(env);
    skija::shaper::onUnload(env);
    skija::onUnload(env);
    types::onUnload(env);
    java::onUnload(env);

    gJavaVM = nullptr;
}
