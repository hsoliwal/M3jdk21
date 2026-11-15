#include "jdk_m3_graphics_resources_M3FileResourceProvider.h"
#include <jni.h>
#include "../interop.hh"
#include "SkResources.h"

extern "C" JNIEXPORT jlong JNICALL Java_jdk_m3_graphics_resources_M3FileResourceProvider__1nMake
  (JNIEnv* env, jclass jclass, jstring basePathStr, jboolean predecode) {
    SkString basePath = *skString(env, basePathStr);
    auto strategy = predecode ? skresources::ImageDecodeStrategy::kPreDecode : skresources::ImageDecodeStrategy::kLazyDecode;
    auto instance = skresources::FileResourceProvider::Make(basePath, strategy);
    return ptrToJlong(instance.release());
}