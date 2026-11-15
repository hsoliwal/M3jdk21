#include "jdk_m3_graphics_resources_M3CachingResourceProvider.h"
#include <jni.h>
#include "../interop.hh"
#include "SkResources.h"

extern "C" JNIEXPORT jlong JNICALL Java_jdk_m3_graphics_resources_M3CachingResourceProvider__1nMake
  (JNIEnv* env, jclass jclass, jlong resourceProviderPtr) {
    skresources::ResourceProvider* resourceProvider = reinterpret_cast<skresources::ResourceProvider*>(static_cast<uintptr_t>(resourceProviderPtr));
    auto instance = skresources::CachingResourceProvider::Make(sk_ref_sp(resourceProvider));
    return ptrToJlong(instance.release());
}