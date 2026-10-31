/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
/* Optional, isolated C17/JNI mask histogram; only unsigned-byte metadata crosses JNI.
 * No Java callbacks, retained JNI array elements, unbounded stack allocations or JVM pins.
 */
#include <jni.h>
#include <stdint.h>

#define M3_STATE_COUNT 256
#define M3_STATE_CHUNK 256

static void m3_invalid(JNIEnv *env, const char *message) {
    jclass type = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
    if (type != NULL) {
        (*env)->ThrowNew(env, type, message);
        (*env)->DeleteLocalRef(env, type);
    }
}

JNIEXPORT void JNICALL
Java_com_m3_collections_M3StateMaskHistogram_tallyNative(
        JNIEnv *env, jclass owner, jbyteArray source, jint first,
        jint count, jint stride, jintArray result) {
    (void) owner;
    if (source == NULL || result == NULL || count < 0 || stride == 0) {
        m3_invalid(env, "invalid native state histogram inputs");
        return;
    }
    jsize length = (*env)->GetArrayLength(env, source);
    if ((*env)->ExceptionCheck(env)) return;
    jsize output_size = (*env)->GetArrayLength(env, result);
    if ((*env)->ExceptionCheck(env)) return;
    if (first < 0 || first > length || output_size != M3_STATE_COUNT) {
        m3_invalid(env, "invalid native histogram window or output size");
        return;
    }
    if (count > 0) {
        jlong last = (jlong) first + ((jlong) count - INT64_C(1)) * (jlong) stride;
        if (first == length || last < 0 || last >= (jlong) length) {
            m3_invalid(env, "native histogram path outside state lane");
            return;
        }
    }
    jint buckets[M3_STATE_COUNT] = {0};
    jbyte chunk[M3_STATE_CHUNK];
    if (stride == 1 || stride == -1) {
        for (jint offset = 0; offset < count;) {
            jint take = count - offset;
            if (take > M3_STATE_CHUNK) take = M3_STATE_CHUNK;
            jint begin = stride == 1 ? first + offset : first - offset - take + 1;
            (*env)->GetByteArrayRegion(env, source, begin, take, chunk);
            if ((*env)->ExceptionCheck(env)) return;
            for (jint i = 0; i < take; i++) {
                jint cell = stride == 1 ? i : take - i - 1;
                buckets[(unsigned char) chunk[cell]]++;
            }
            offset += take;
        }
    } else {
        jlong at = first;
        for (jint i = 0; i < count; i++, at += stride) {
            jbyte state = 0;
            (*env)->GetByteArrayRegion(env, source, (jsize) at, 1, &state);
            if ((*env)->ExceptionCheck(env)) return;
            buckets[(unsigned char) state]++;
        }
    }
    (*env)->SetIntArrayRegion(env, result, 0, M3_STATE_COUNT, buckets);
}
