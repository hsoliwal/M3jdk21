/* SPDX-License-Identifier: Apache-2.0 */
/* Original Synexia signed-INT8 block quantization JNI candidate. No model weights. */
#include <jni.h>
#include <math.h>
#include <stddef.h>
#include <stdint.h>

#define M3_BLOCK_MAX 4096
#define M3_ELEMENTS_MAX 4194304

static void illegal(JNIEnv *env, const char *message) {
    jclass type = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
    if (type != NULL) {
        (*env)->ThrowNew(env, type, message);
        (*env)->DeleteLocalRef(env, type);
    }
}

JNIEXPORT jint JNICALL Java_com_m3_tools_model_quantize_BlockInt8Quantizer_nativeAbi(
        JNIEnv *env, jclass type) {
    (void)env;
    (void)type;
    return 1;
}

/*
 * Mirrors Java's per-block double max/scale/nearest-even conversion.
 * A bounded stack buffer avoids GetPrimitiveArrayCritical and pinned GC arrays.
 * Java validates input and compares every output bit independently before
 * returning any JNI-produced answer. No JNI access to files or GPU devices.
 */
JNIEXPORT jobjectArray JNICALL Java_com_m3_tools_model_quantize_BlockInt8Quantizer_packNative(
        JNIEnv *env, jclass type, jfloatArray values, jint block_size) {
    (void)type;
    if (values == NULL || block_size < 32 || block_size > M3_BLOCK_MAX
            || (block_size & (block_size - 1)) != 0) {
        illegal(env, "invalid JNI block parameters");
        return NULL;
    }
    const jsize length = (*env)->GetArrayLength(env, values);
    if (length < 1 || length > M3_ELEMENTS_MAX) {
        illegal(env, "invalid JNI element count");
        return NULL;
    }
    const jsize block_count = (jsize)(((int64_t)length + block_size - 1) / block_size);
    jbyteArray codes = (*env)->NewByteArray(env, length);
    if (codes == NULL) return NULL;
    jdoubleArray scales = (*env)->NewDoubleArray(env, block_count);
    if (scales == NULL) {
        (*env)->DeleteLocalRef(env, codes);
        return NULL;
    }
    jfloat source[M3_BLOCK_MAX];
    jbyte result_codes[M3_BLOCK_MAX];
    for (jsize block = 0; block < block_count; ++block) {
        const jsize start = block * block_size;
        const jsize count = length - start < block_size ? length - start : block_size;
        (*env)->GetFloatArrayRegion(env, values, start, count, source);
        if ((*env)->ExceptionCheck(env)) goto error;
        double max_abs = 0.0;
        for (jsize i = 0; i < count; ++i) {
            double current = (double)source[i];
            if (!isfinite(current)) {
                illegal(env, "nonfinite JNI source weight");
                goto error;
            }
            const double absolute = fabs(current);
            max_abs = fmax(max_abs, absolute);
        }
        const double scale = max_abs == 0.0 ? 1.0 : max_abs / 127.0;
        if (!(scale > 0.0) || !isfinite(scale)) {
            illegal(env, "invalid JNI scale");
            goto error;
        }
        for (jsize i = 0; i < count; ++i) {
            /* Semantics: nearbyint ties-to-even in the host's normal FE_TONEAREST.
             * Under alternate floating-point rounding modes the exact Java
             * oracle detects and refuses a mismatch before publication. */
            const double rounded = nearbyint((double)source[i] / scale);
            const double clipped = fmax(-127.0, fmin(127.0, rounded));
            result_codes[i] = (jbyte)clipped;
        }
        (*env)->SetByteArrayRegion(env, codes, start, count, result_codes);
        if ((*env)->ExceptionCheck(env)) goto error;
        (*env)->SetDoubleArrayRegion(env, scales, block, 1, &scale);
        if ((*env)->ExceptionCheck(env)) goto error;
    }
    jclass objects = (*env)->FindClass(env, "java/lang/Object");
    if (objects == NULL) goto error;
    jobjectArray pair = (*env)->NewObjectArray(env, 2, objects, NULL);
    (*env)->DeleteLocalRef(env, objects);
    if (pair == NULL) goto error;
    (*env)->SetObjectArrayElement(env, pair, 0, codes);
    if ((*env)->ExceptionCheck(env)) goto error;
    (*env)->SetObjectArrayElement(env, pair, 1, scales);
    if ((*env)->ExceptionCheck(env)) goto error;
    (*env)->DeleteLocalRef(env, codes);
    (*env)->DeleteLocalRef(env, scales);
    return pair;
error:
    (*env)->DeleteLocalRef(env, codes);
    (*env)->DeleteLocalRef(env, scales);
    return NULL;
}
