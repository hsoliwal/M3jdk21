#include "jdk_m3_graphics_resources_M3DataURIResourceProviderProxy.h"
#include <jni.h>
#include "../interop.hh"
#include "SkResources.h"

extern "C" JNIEXPORT jlong JNICALL Java_jdk_m3_graphics_resources_M3DataURIResourceProviderProxy__1nMake
  (JNIEnv* env, jclass jclass, jlong resourceProviderPtr, jboolean predecode) {
    skresources::ResourceProvider* resourceProvider = reinterpret_cast<skresources::ResourceProvider*>(static_cast<uintptr_t>(resourceProviderPtr));
    auto strategy = predecode ? skresources::ImageDecodeStrategy::kPreDecode : skresources::ImageDecodeStrategy::kLazyDecode;
    auto instance = skresources::DataURIResourceProviderProxy::Make(sk_ref_sp(resourceProvider), strategy);
    return ptrToJlong(instance.release());
}