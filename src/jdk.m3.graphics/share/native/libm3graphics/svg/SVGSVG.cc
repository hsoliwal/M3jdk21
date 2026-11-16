#include "jdk_m3_graphics_svg_M3SVGSVG.h"
#include <jni.h>
#include "../interop.hh"
#include "interop.hh"
#include "SkSVGSVG.h"
#include "SkSVGRenderContext.h"

extern "C" JNIEXPORT jobject JNICALL Java_jdk_m3_graphics_svg_M3SVGSVG__1nGetX
  (JNIEnv* env, jclass jclass, jlong ptr) {
    SkSVGSVG* instance = reinterpret_cast<SkSVGSVG*>(static_cast<uintptr_t>(ptr));
    return skija::svg::SVGLength::toJava(env, instance->getX());
}

extern "C" JNIEXPORT jobject JNICALL Java_jdk_m3_graphics_svg_M3SVGSVG__1nGetY
  (JNIEnv* env, jclass jclass, jlong ptr) {
    SkSVGSVG* instance = reinterpret_cast<SkSVGSVG*>(static_cast<uintptr_t>(ptr));
    return skija::svg::SVGLength::toJava(env, instance->getY());
}

extern "C" JNIEXPORT jobject JNICALL Java_jdk_m3_graphics_svg_M3SVGSVG__1nGetWidth
  (JNIEnv* env, jclass jclass, jlong ptr) {
    SkSVGSVG* instance = reinterpret_cast<SkSVGSVG*>(static_cast<uintptr_t>(ptr));
    return skija::svg::SVGLength::toJava(env, instance->getWidth());
}

extern "C" JNIEXPORT jobject JNICALL Java_jdk_m3_graphics_svg_M3SVGSVG__1nGetHeight
  (JNIEnv* env, jclass jclass, jlong ptr) {
    SkSVGSVG* instance = reinterpret_cast<SkSVGSVG*>(static_cast<uintptr_t>(ptr));
    return skija::svg::SVGLength::toJava(env, instance->getHeight());
}

extern "C" JNIEXPORT jobject JNICALL Java_jdk_m3_graphics_svg_M3SVGSVG__1nGetPreserveAspectRatio
  (JNIEnv* env, jclass jclass, jlong ptr) {
    SkSVGSVG* instance = reinterpret_cast<SkSVGSVG*>(static_cast<uintptr_t>(ptr));
    return skija::svg::SVGPreserveAspectRatio::toJava(env, instance->getPreserveAspectRatio());
}

extern "C" JNIEXPORT jobject JNICALL Java_jdk_m3_graphics_svg_M3SVGSVG__1nGetViewBox
  (JNIEnv* env, jclass jclass, jlong ptr) {
    SkSVGSVG* instance = reinterpret_cast<SkSVGSVG*>(static_cast<uintptr_t>(ptr));
    if (std::optional<SkSVGViewBoxType> viewBox = instance->getViewBox()) {
      return types::Rect::fromSkRect(env, *viewBox);
    } else {
      return nullptr;
    }
}

extern "C" JNIEXPORT jobject JNICALL Java_jdk_m3_graphics_svg_M3SVGSVG__1nGetIntrinsicSize
  (JNIEnv* env, jclass jclass, jlong ptr, jfloat width, jfloat height, jfloat dpi) {
    SkSVGSVG* instance = reinterpret_cast<SkSVGSVG*>(static_cast<uintptr_t>(ptr));
    SkSVGLengthContext lc({width, height}, dpi);
    SkSize size = instance->intrinsicSize(lc);
    return types::Point::fromSkPoint(env, {size.width(), size.height()});
}

extern "C" JNIEXPORT void JNICALL Java_jdk_m3_graphics_svg_M3SVGSVG__1nSetX
  (JNIEnv* env, jclass jclass, jlong ptr, jfloat value, jint unit) {
    SkSVGSVG* instance = reinterpret_cast<SkSVGSVG*>(static_cast<uintptr_t>(ptr));
    SkSVGLength lenght(value, static_cast<SkSVGLength::Unit>(unit));
    instance->setX(lenght);
}

extern "C" JNIEXPORT void JNICALL Java_jdk_m3_graphics_svg_M3SVGSVG__1nSetY
  (JNIEnv* env, jclass jclass, jlong ptr, jfloat value, jint unit) {
    SkSVGSVG* instance = reinterpret_cast<SkSVGSVG*>(static_cast<uintptr_t>(ptr));
    SkSVGLength lenght(value, static_cast<SkSVGLength::Unit>(unit));
    instance->setY(lenght);
}

extern "C" JNIEXPORT void JNICALL Java_jdk_m3_graphics_svg_M3SVGSVG__1nSetWidth
  (JNIEnv* env, jclass jclass, jlong ptr, jfloat value, jint unit) {
    SkSVGSVG* instance = reinterpret_cast<SkSVGSVG*>(static_cast<uintptr_t>(ptr));
    SkSVGLength lenght(value, static_cast<SkSVGLength::Unit>(unit));
    instance->setWidth(lenght);
}

extern "C" JNIEXPORT void JNICALL Java_jdk_m3_graphics_svg_M3SVGSVG__1nSetHeight
  (JNIEnv* env, jclass jclass, jlong ptr, jfloat value, jint unit) {
    SkSVGSVG* instance = reinterpret_cast<SkSVGSVG*>(static_cast<uintptr_t>(ptr));
    SkSVGLength lenght(value, static_cast<SkSVGLength::Unit>(unit));
    instance->setHeight(lenght);
}

extern "C" JNIEXPORT void JNICALL Java_jdk_m3_graphics_svg_M3SVGSVG__1nSetPreserveAspectRatio
  (JNIEnv* env, jclass jclass, jlong ptr, jint align, jint scale) {
    SkSVGSVG* instance = reinterpret_cast<SkSVGSVG*>(static_cast<uintptr_t>(ptr));
    instance->setPreserveAspectRatio(SkSVGPreserveAspectRatio { static_cast<SkSVGPreserveAspectRatio::Align>(align),
                                                                static_cast<SkSVGPreserveAspectRatio::Scale>(scale) });
}

extern "C" JNIEXPORT void JNICALL Java_jdk_m3_graphics_svg_M3SVGSVG__1nSetViewBox
  (JNIEnv* env, jclass jclass, jlong ptr, jfloat l, jfloat t, jfloat r, jfloat b) {
    SkSVGSVG* instance = reinterpret_cast<SkSVGSVG*>(static_cast<uintptr_t>(ptr));
    instance->setViewBox(SkRect::MakeLTRB(l, t, r, b));
}
