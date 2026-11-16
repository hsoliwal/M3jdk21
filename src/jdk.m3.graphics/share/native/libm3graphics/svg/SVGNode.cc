#include "jdk_m3_graphics_svg_M3SVGNode.h"
#include <jni.h>
#include "../interop.hh"
#include "SkSVGNode.h"

extern "C" JNIEXPORT jint JNICALL Java_jdk_m3_graphics_svg_M3SVGNode__1nGetTag
  (JNIEnv* env, jclass jclass, jlong ptr) {
    SkSVGNode* instance = reinterpret_cast<SkSVGNode*>(static_cast<uintptr_t>(ptr));
    return static_cast<jint>(instance->tag());
}
