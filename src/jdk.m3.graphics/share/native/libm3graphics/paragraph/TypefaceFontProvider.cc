#include "jdk_m3_graphics_paragraph_M3TypefaceFontProvider.h"
#include <iostream>
#include <jni.h>
#include "../interop.hh"
#include "TypefaceFontProvider.h"
#include "SkTypeface.h"

using namespace skia::textlayout;

extern "C" JNIEXPORT jlong JNICALL Java_jdk_m3_graphics_paragraph_M3TypefaceFontProvider__1nMake
  (JNIEnv* env, jclass jclass) {
    TypefaceFontProvider* instance = new TypefaceFontProvider();
    return reinterpret_cast<jlong>(instance);
}

extern "C" JNIEXPORT jlong JNICALL Java_jdk_m3_graphics_paragraph_M3TypefaceFontProvider__1nRegisterTypeface
  (JNIEnv* env, jclass jclass, jlong ptr, jlong typefacePtr, jstring aliasStr) {
    TypefaceFontProvider* instance = reinterpret_cast<TypefaceFontProvider*>(static_cast<uintptr_t>(ptr));
    SkTypeface* typeface = reinterpret_cast<SkTypeface*>(static_cast<uintptr_t>(typefacePtr));
    std::optional<SkString> alias = skString(env, aliasStr);
    if (alias) {
        return instance->registerTypeface(sk_ref_sp(typeface), *alias);
    } else {
        return instance->registerTypeface(sk_ref_sp(typeface));
    }
}
