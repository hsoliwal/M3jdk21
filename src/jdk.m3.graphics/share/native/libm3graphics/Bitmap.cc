#include "jdk_m3_graphics_M3Bitmap.h"
#include <jni.h>
#include "SkBitmap.h"
#include "SkPixelRef.h"
#include "SkSamplingOptions.h"
#include "SkShader.h"
#include "interop.hh"
#include <algorithm>
#include <functional>
#include <mutex>
#include <vector>

// Skia removed the per-bitmap volatility hint after m75. Keep the retained JNI
// contract without changing SkBitmap's ABI or sharing the hint with SkPixelRef.
// Only explicitly volatile live bitmaps occupy this owner-local registry.
static std::mutex bitmapVolatilityMutex;
static std::vector<const SkBitmap*> volatileBitmaps;

static auto findVolatileBitmap(const SkBitmap* bitmap) {
    return std::lower_bound(volatileBitmaps.begin(), volatileBitmaps.end(), bitmap,
                           std::less<const SkBitmap*>());
}

static bool isBitmapVolatileLocked(const SkBitmap* bitmap) {
    auto position = findVolatileBitmap(bitmap);
    return position != volatileBitmaps.end() && *position == bitmap;
}

static void setBitmapVolatileLocked(const SkBitmap* bitmap, bool value) {
    auto position = findVolatileBitmap(bitmap);
    bool found = position != volatileBitmaps.end() && *position == bitmap;
    if (value && !found) {
        volatileBitmaps.insert(position, bitmap);
    } else if (!value && found) {
        volatileBitmaps.erase(position);
        if (volatileBitmaps.empty()) {
            std::vector<const SkBitmap*>().swap(volatileBitmaps);
        }
    }
}

static bool isBitmapVolatile(const SkBitmap* bitmap) {
    std::lock_guard<std::mutex> lock(bitmapVolatilityMutex);
    return isBitmapVolatileLocked(bitmap);
}

static void setBitmapVolatile(const SkBitmap* bitmap, bool value) {
    std::lock_guard<std::mutex> lock(bitmapVolatilityMutex);
    setBitmapVolatileLocked(bitmap, value);
}

static void swapBitmapVolatility(const SkBitmap* bitmap, const SkBitmap* other) {
    std::lock_guard<std::mutex> lock(bitmapVolatilityMutex);
    bool first = isBitmapVolatileLocked(bitmap);
    bool second = isBitmapVolatileLocked(other);
    setBitmapVolatileLocked(bitmap, second);
    setBitmapVolatileLocked(other, first);
}

static void deleteBitmap(SkBitmap* instance) {
    // std::cout << "Deleting [SkBitmap " << instance << "]" << std::endl;
    setBitmapVolatile(instance, false);
    delete instance;
}

extern "C" JNIEXPORT jlong JNICALL Java_jdk_m3_graphics_M3Bitmap__1nGetFinalizer(JNIEnv* env, jclass jclass) {
    return static_cast<jlong>(reinterpret_cast<uintptr_t>(&deleteBitmap));
}

extern "C" JNIEXPORT jlong JNICALL Java_jdk_m3_graphics_M3Bitmap__1nMake
  (JNIEnv* env, jclass jclass) {
    return reinterpret_cast<jlong>(new SkBitmap());
}

extern "C" JNIEXPORT jlong JNICALL Java_jdk_m3_graphics_M3Bitmap__1nMakeClone
  (JNIEnv* env, jclass jclass, jlong ptr) {
    SkBitmap* instance = reinterpret_cast<SkBitmap*>(static_cast<uintptr_t>(ptr));
    SkBitmap* clone = new SkBitmap(*instance);
    setBitmapVolatile(clone, isBitmapVolatile(instance));
    return reinterpret_cast<jlong>(clone);
}

extern "C" JNIEXPORT void JNICALL Java_jdk_m3_graphics_M3Bitmap__1nSwap
  (JNIEnv* env, jclass jclass, jlong ptr, jlong otherPtr) {
    SkBitmap* instance = reinterpret_cast<SkBitmap*>(static_cast<uintptr_t>(ptr));
    SkBitmap* other = reinterpret_cast<SkBitmap*>(static_cast<uintptr_t>(otherPtr));
    instance->swap(*other);
    swapBitmapVolatility(instance, other);
}

extern "C" JNIEXPORT jobject JNICALL Java_jdk_m3_graphics_M3Bitmap__1nGetImageInfo
  (JNIEnv* env, jclass jclass, jlong ptr) {
    SkBitmap* instance = reinterpret_cast<SkBitmap*>(static_cast<uintptr_t>(ptr));
    return skija::ImageInfo::toJava(env, instance->info());
}

extern "C" JNIEXPORT jint JNICALL Java_jdk_m3_graphics_M3Bitmap__1nGetRowBytesAsPixels
  (JNIEnv* env, jclass jclass, jlong ptr) {
    SkBitmap* instance = reinterpret_cast<SkBitmap*>(static_cast<uintptr_t>(ptr));
    return instance->rowBytesAsPixels();
}

extern "C" JNIEXPORT jboolean JNICALL Java_jdk_m3_graphics_M3Bitmap__1nIsNull
  (JNIEnv* env, jclass jclass, jlong ptr) {
    SkBitmap* instance = reinterpret_cast<SkBitmap*>(static_cast<uintptr_t>(ptr));
    return instance->isNull();
}

extern "C" JNIEXPORT jlong JNICALL Java_jdk_m3_graphics_M3Bitmap__1nGetRowBytes
  (JNIEnv* env, jclass jclass, jlong ptr) {
    SkBitmap* instance = reinterpret_cast<SkBitmap*>(static_cast<uintptr_t>(ptr));
    return instance->rowBytes();
}

extern "C" JNIEXPORT jboolean JNICALL Java_jdk_m3_graphics_M3Bitmap__1nSetAlphaType
  (JNIEnv* env, jclass jclass, jlong ptr, jint alphaType) {
    SkBitmap* instance = reinterpret_cast<SkBitmap*>(static_cast<uintptr_t>(ptr));
    return instance->setAlphaType(static_cast<SkAlphaType>(alphaType));
}

extern "C" JNIEXPORT void JNICALL Java_jdk_m3_graphics_M3Bitmap__1nSetColorSpace
  (JNIEnv* env, jclass jclass, jlong ptr, jlong colorSpacePtr) {
    SkBitmap* instance = reinterpret_cast<SkBitmap*>(static_cast<uintptr_t>(ptr));
    SkColorSpace* colorSpace = reinterpret_cast<SkColorSpace*>(static_cast<uintptr_t>(colorSpacePtr));
    instance->setColorSpace(sk_ref_sp<SkColorSpace>(colorSpace));
}

extern "C" JNIEXPORT jlong JNICALL Java_jdk_m3_graphics_M3Bitmap__1nComputeByteSize
  (JNIEnv* env, jclass jclass, jlong ptr) {
    SkBitmap* instance = reinterpret_cast<SkBitmap*>(static_cast<uintptr_t>(ptr));
    return instance->computeByteSize();
}

extern "C" JNIEXPORT jboolean JNICALL Java_jdk_m3_graphics_M3Bitmap__1nIsImmutable
  (JNIEnv* env, jclass jclass, jlong ptr) {
    SkBitmap* instance = reinterpret_cast<SkBitmap*>(static_cast<uintptr_t>(ptr));
    return instance->isImmutable();
}

extern "C" JNIEXPORT void JNICALL Java_jdk_m3_graphics_M3Bitmap__1nSetImmutable
  (JNIEnv* env, jclass jclass, jlong ptr) {
    SkBitmap* instance = reinterpret_cast<SkBitmap*>(static_cast<uintptr_t>(ptr));
    instance->setImmutable();
}

extern "C" JNIEXPORT jboolean JNICALL Java_jdk_m3_graphics_M3Bitmap__1nIsVolatile
  (JNIEnv* env, jclass jclass, jlong ptr) {
    return isBitmapVolatile(reinterpret_cast<SkBitmap*>(static_cast<uintptr_t>(ptr)));
}

extern "C" JNIEXPORT void JNICALL Java_jdk_m3_graphics_M3Bitmap__1nSetVolatile
  (JNIEnv* env, jclass jclass, jlong ptr, jboolean value) {
    setBitmapVolatile(reinterpret_cast<SkBitmap*>(static_cast<uintptr_t>(ptr)), value);
}

extern "C" JNIEXPORT void JNICALL Java_jdk_m3_graphics_M3Bitmap__1nReset
  (JNIEnv* env, jclass jclass, jlong ptr) {
    SkBitmap* instance = reinterpret_cast<SkBitmap*>(static_cast<uintptr_t>(ptr));
    instance->reset();
    setBitmapVolatile(instance, false);
}

extern "C" JNIEXPORT jboolean JNICALL Java_jdk_m3_graphics_M3Bitmap__1nComputeIsOpaque
  (JNIEnv* env, jclass jclass, jlong ptr) {
    SkBitmap* instance = reinterpret_cast<SkBitmap*>(static_cast<uintptr_t>(ptr));
    return SkBitmap::ComputeIsOpaque(*instance);
}

extern "C" JNIEXPORT jboolean JNICALL Java_jdk_m3_graphics_M3Bitmap__1nSetImageInfo
  (JNIEnv* env, jclass jclass, jlong ptr, jint width, jint height, jint colorType, jint alphaType, jlong colorSpacePtr, jlong rowBytes) {
    SkBitmap* instance = reinterpret_cast<SkBitmap*>(static_cast<uintptr_t>(ptr));
    SkColorSpace* colorSpace = reinterpret_cast<SkColorSpace*>(static_cast<uintptr_t>(colorSpacePtr));
    SkImageInfo imageInfo = SkImageInfo::Make(width,
                                              height,
                                              static_cast<SkColorType>(colorType),
                                              static_cast<SkAlphaType>(alphaType),
                                              sk_ref_sp<SkColorSpace>(colorSpace));
    bool result = instance->setInfo(imageInfo, rowBytes);
    if (!result) setBitmapVolatile(instance, false);
    return result;
}

extern "C" JNIEXPORT jboolean JNICALL Java_jdk_m3_graphics_M3Bitmap__1nAllocPixelsFlags
  (JNIEnv* env, jclass jclass, jlong ptr, jint width, jint height, jint colorType, jint alphaType, jlong colorSpacePtr, jint flags) {
    SkBitmap* instance = reinterpret_cast<SkBitmap*>(static_cast<uintptr_t>(ptr));
    SkColorSpace* colorSpace = reinterpret_cast<SkColorSpace*>(static_cast<uintptr_t>(colorSpacePtr));
    SkImageInfo imageInfo = SkImageInfo::Make(width,
                                              height,
                                              static_cast<SkColorType>(colorType),
                                              static_cast<SkAlphaType>(alphaType),
                                              sk_ref_sp<SkColorSpace>(colorSpace));
    bool result = instance->tryAllocPixelsFlags(imageInfo, flags);
    if (!result) setBitmapVolatile(instance, false);
    return result;
}

extern "C" JNIEXPORT jboolean JNICALL Java_jdk_m3_graphics_M3Bitmap__1nAllocPixelsRowBytes
  (JNIEnv* env, jclass jclass, jlong ptr, jint width, jint height, jint colorType, jint alphaType, jlong colorSpacePtr, jlong rowBytes) {
    SkBitmap* instance = reinterpret_cast<SkBitmap*>(static_cast<uintptr_t>(ptr));
    SkColorSpace* colorSpace = reinterpret_cast<SkColorSpace*>(static_cast<uintptr_t>(colorSpacePtr));
    SkImageInfo imageInfo = SkImageInfo::Make(width,
                                              height,
                                              static_cast<SkColorType>(colorType),
                                              static_cast<SkAlphaType>(alphaType),
                                              sk_ref_sp<SkColorSpace>(colorSpace));
    bool result = instance->tryAllocPixels(imageInfo, rowBytes);
    if (!result) setBitmapVolatile(instance, false);
    return result;
}

extern "C" JNIEXPORT jboolean JNICALL Java_jdk_m3_graphics_M3Bitmap__1nInstallPixels
  (JNIEnv* env, jclass jclass, jlong ptr, jint width, jint height, jint colorType, jint alphaType, jlong colorSpacePtr, jbyteArray pixelsArr, jlong rowBytes) {
    SkBitmap* instance = reinterpret_cast<SkBitmap*>(static_cast<uintptr_t>(ptr));
    SkColorSpace* colorSpace = reinterpret_cast<SkColorSpace*>(static_cast<uintptr_t>(colorSpacePtr));
    SkImageInfo imageInfo = SkImageInfo::Make(width,
                                              height,
                                              static_cast<SkColorType>(colorType),
                                              static_cast<SkAlphaType>(alphaType),
                                              sk_ref_sp<SkColorSpace>(colorSpace));

    jsize len = env->GetArrayLength(pixelsArr);
    jbyte* pixels = new jbyte[len];
    env->GetByteArrayRegion(pixelsArr, 0, len, pixels);
    bool result = instance->installPixels(imageInfo, pixels, rowBytes, deleteJBytes, nullptr);
    if (!result) setBitmapVolatile(instance, false);
    return result;
}

extern "C" JNIEXPORT jboolean JNICALL Java_jdk_m3_graphics_M3Bitmap__1nAllocPixels
  (JNIEnv* env, jclass jclass, jlong ptr) {
    SkBitmap* instance = reinterpret_cast<SkBitmap*>(static_cast<uintptr_t>(ptr));
    return instance->tryAllocPixels();
}

extern "C" JNIEXPORT jlong JNICALL Java_jdk_m3_graphics_M3Bitmap__1nGetPixelRef
  (JNIEnv* env, jclass jclass, jlong ptr) {
    SkBitmap* instance = reinterpret_cast<SkBitmap*>(static_cast<uintptr_t>(ptr));
    SkPixelRef* pixelRef = instance->pixelRef();
    pixelRef->ref();
    return reinterpret_cast<jlong>(pixelRef);
}

extern "C" JNIEXPORT jlong JNICALL Java_jdk_m3_graphics_M3Bitmap__1nGetPixelRefOrigin
  (JNIEnv* env, jclass jclass, jlong ptr) {
    SkBitmap* instance = reinterpret_cast<SkBitmap*>(static_cast<uintptr_t>(ptr));
    SkIPoint origin = instance->pixelRefOrigin();
    return packIPoint(origin);
}

extern "C" JNIEXPORT void JNICALL Java_jdk_m3_graphics_M3Bitmap__1nSetPixelRef
  (JNIEnv* env, jclass jclass, jlong ptr, jlong pixelRefPtr, jint dx, jint dy) {
    SkBitmap* instance = reinterpret_cast<SkBitmap*>(static_cast<uintptr_t>(ptr));
    SkPixelRef* pixelRef = reinterpret_cast<SkPixelRef*>(static_cast<uintptr_t>(pixelRefPtr));
    instance->setPixelRef(sk_ref_sp<SkPixelRef>(pixelRef), dx, dy);
}

extern "C" JNIEXPORT jboolean JNICALL Java_jdk_m3_graphics_M3Bitmap__1nIsReadyToDraw
  (JNIEnv* env, jclass jclass, jlong ptr) {
    SkBitmap* instance = reinterpret_cast<SkBitmap*>(static_cast<uintptr_t>(ptr));
    return instance->readyToDraw();
}

extern "C" JNIEXPORT jint JNICALL Java_jdk_m3_graphics_M3Bitmap__1nGetGenerationId
  (JNIEnv* env, jclass jclass, jlong ptr) {
    SkBitmap* instance = reinterpret_cast<SkBitmap*>(static_cast<uintptr_t>(ptr));
    return instance->getGenerationID();
}

extern "C" JNIEXPORT void JNICALL Java_jdk_m3_graphics_M3Bitmap__1nNotifyPixelsChanged
  (JNIEnv* env, jclass jclass, jlong ptr) {
    SkBitmap* instance = reinterpret_cast<SkBitmap*>(static_cast<uintptr_t>(ptr));
    instance->notifyPixelsChanged();
}

extern "C" JNIEXPORT void JNICALL Java_jdk_m3_graphics_M3Bitmap__1nErase4f
  (JNIEnv* env, jclass jclass, jlong ptr, jfloat r, jfloat g, jfloat b, jfloat a) {
    SkBitmap* instance = reinterpret_cast<SkBitmap*>(static_cast<uintptr_t>(ptr));
    instance->eraseColor({r, g, b, a});
}

extern "C" JNIEXPORT void JNICALL Java_jdk_m3_graphics_M3Bitmap__1nEraseRect4f
  (JNIEnv* env, jclass jclass, jlong ptr, jfloat r, jfloat g, jfloat b, jfloat a, jint left, jint top, jint right, jint bottom) {
    SkBitmap* instance = reinterpret_cast<SkBitmap*>(static_cast<uintptr_t>(ptr));
    instance->erase({r, g, b, a}, {left, top, right, bottom});
}

extern "C" JNIEXPORT void JNICALL Java_jdk_m3_graphics_M3Bitmap__1nErase
  (JNIEnv* env, jclass jclass, jlong ptr, jint color) {
    SkBitmap* instance = reinterpret_cast<SkBitmap*>(static_cast<uintptr_t>(ptr));
    instance->eraseColor(color);
}

extern "C" JNIEXPORT void JNICALL Java_jdk_m3_graphics_M3Bitmap__1nEraseRect
  (JNIEnv* env, jclass jclass, jlong ptr, jint color, jint left, jint top, jint right, jint bottom) {
    SkBitmap* instance = reinterpret_cast<SkBitmap*>(static_cast<uintptr_t>(ptr));
    instance->erase(color, {left, top, right, bottom});
}

extern "C" JNIEXPORT jint JNICALL Java_jdk_m3_graphics_M3Bitmap__1nGetColor
  (JNIEnv* env, jclass jclass, jlong ptr, jint x, jint y) {
    SkBitmap* instance = reinterpret_cast<SkBitmap*>(static_cast<uintptr_t>(ptr));
    return instance->getColor(x, y);
}

extern "C" JNIEXPORT jobject JNICALL Java_jdk_m3_graphics_M3Bitmap__1nGetColor4f
  (JNIEnv* env, jclass jclass, jlong ptr, jint x, jint y) {
    SkBitmap* instance = reinterpret_cast<SkBitmap*>(static_cast<uintptr_t>(ptr));
    SkColor4f color = instance->getColor4f(x, y);
    return env->NewObject(skija::Color4f::cls, skija::Color4f::ctor, color.fR, color.fG, color.fB, color.fA);
}

extern "C" JNIEXPORT jfloat JNICALL Java_jdk_m3_graphics_M3Bitmap__1nGetAlphaf
  (JNIEnv* env, jclass jclass, jlong ptr, jint x, jint y) {
    SkBitmap* instance = reinterpret_cast<SkBitmap*>(static_cast<uintptr_t>(ptr));
    return instance->getAlphaf(x, y);
}

extern "C" JNIEXPORT jboolean JNICALL Java_jdk_m3_graphics_M3Bitmap__1nExtractSubset
  (JNIEnv* env, jclass jclass, jlong ptr, jlong dstPtr, jint left, jint top, jint right, jint bottom) {
    SkBitmap* instance = reinterpret_cast<SkBitmap*>(static_cast<uintptr_t>(ptr));
    SkBitmap* dst = reinterpret_cast<SkBitmap*>(static_cast<uintptr_t>(dstPtr));
    bool result = instance->extractSubset(dst, {left, top, right, bottom});
    if (result) setBitmapVolatile(dst, isBitmapVolatile(instance));
    return result;
}

extern "C" JNIEXPORT jbyteArray JNICALL Java_jdk_m3_graphics_M3Bitmap__1nReadPixels
  (JNIEnv* env, jclass jclass, jlong ptr, jint width, jint height, jint colorType, jint alphaType, jlong colorSpacePtr, jlong rowBytes, jint srcX, jint srcY) {
    SkBitmap* instance = reinterpret_cast<SkBitmap*>(static_cast<uintptr_t>(ptr));
    SkColorSpace* colorSpace = reinterpret_cast<SkColorSpace*>(static_cast<uintptr_t>(colorSpacePtr));
    SkImageInfo imageInfo = SkImageInfo::Make(width,
                                              height,
                                              static_cast<SkColorType>(colorType),
                                              static_cast<SkAlphaType>(alphaType),
                                              sk_ref_sp<SkColorSpace>(colorSpace));
    std::vector<jbyte> pixels(std::min(height, instance->height() - srcY) * rowBytes);
    if (instance->readPixels(imageInfo, pixels.data(), rowBytes, srcX, srcY))
        return javaByteArray(env, pixels);
    else
        return nullptr;
}

extern "C" JNIEXPORT jobject JNICALL Java_jdk_m3_graphics_M3Bitmap__1nExtractAlpha
  (JNIEnv* env, jclass jclass, jlong ptr, jlong dstPtr, jlong paintPtr) {
    SkBitmap* instance = reinterpret_cast<SkBitmap*>(static_cast<uintptr_t>(ptr));
    SkBitmap* dst = reinterpret_cast<SkBitmap*>(static_cast<uintptr_t>(dstPtr));
    SkPaint* paint = reinterpret_cast<SkPaint*>(static_cast<uintptr_t>(paintPtr));
    SkIPoint offset;
    if (instance->extractAlpha(dst, paint, &offset)) {
        setBitmapVolatile(dst, false);
        return types::IPoint::fromSkIPoint(env, offset);
    } else
        return nullptr;
}

extern "C" JNIEXPORT jlong JNICALL Java_jdk_m3_graphics_M3Bitmap__1nPeekPixels
  (JNIEnv* env, jclass jclass, jlong ptr) {
    SkBitmap* instance = reinterpret_cast<SkBitmap*>(static_cast<uintptr_t>(ptr));
    std::unique_ptr<SkPixmap> pixmap(new SkPixmap());
    if (instance->peekPixels(pixmap.get()))
        return reinterpret_cast<jlong>(pixmap.release());
    else
        return 0;
}

extern "C" JNIEXPORT jlong JNICALL Java_jdk_m3_graphics_M3Bitmap__1nMakeShader
  (JNIEnv* env, jclass jclass, jlong ptr, jint tmx, jint tmy, jlong samplingMode, jfloatArray localMatrixArr) {
    SkBitmap* instance = reinterpret_cast<SkBitmap*>(static_cast<uintptr_t>(ptr));
    std::unique_ptr<SkMatrix> localMatrix = skMatrix(env, localMatrixArr);
    sk_sp<SkShader> shader = instance->makeShader(static_cast<SkTileMode>(tmx), static_cast<SkTileMode>(tmy), skija::SamplingMode::unpack(samplingMode), localMatrix.get());
    return reinterpret_cast<jlong>(shader.release());
}
