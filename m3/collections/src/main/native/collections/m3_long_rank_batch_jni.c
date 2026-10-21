/* SPDX-License-Identifier: Apache-2.0 */
#include <jni.h>
#include <stddef.h>

/*
 * One JNI transition per batch, signed jlong order matching Java Long.compare.
 * Get/ReleaseArrayElements respects moving GC: no object addresses are retained.
 * Input arrays are released JNI_ABORT; only the preallocated output array is written.
 */
JNIEXPORT void JNICALL
Java_com_m3_collections_M3LongRankBatchNative_lowerBounds0(
        JNIEnv *env, jclass ignored, jlongArray sorted, jlongArray queries,
        jintArray results) {
    (void) ignored;
    if (sorted == NULL || queries == NULL || results == NULL) {
        jclass error = (*env)->FindClass(env, "java/lang/NullPointerException");
        if (error != NULL) (*env)->ThrowNew(env, error, "null batch argument");
        return;
    }
    jsize size = (*env)->GetArrayLength(env, sorted);
    jsize count = (*env)->GetArrayLength(env, queries);
    if (count != (*env)->GetArrayLength(env, results)) {
        jclass error = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
        if (error != NULL) (*env)->ThrowNew(env, error, "result length mismatch");
        return;
    }
    if (count == 0) return;

    jlong *values = (*env)->GetLongArrayElements(env, sorted, NULL);
    if (values == NULL) return;
    jlong *targets = (*env)->GetLongArrayElements(env, queries, NULL);
    if (targets == NULL) {
        (*env)->ReleaseLongArrayElements(env, sorted, values, JNI_ABORT);
        return;
    }
    jint *positions = (*env)->GetIntArrayElements(env, results, NULL);
    if (positions == NULL) {
        (*env)->ReleaseLongArrayElements(env, queries, targets, JNI_ABORT);
        (*env)->ReleaseLongArrayElements(env, sorted, values, JNI_ABORT);
        return;
    }

    for (jsize i = 0; i < count; i++) {
        jlong needle = targets[i];
        jsize lo = 0;
        jsize hi = size;
        while (lo < hi) {
            jsize mid = lo + ((hi - lo) >> 1);
            if (values[mid] < needle) lo = mid + 1;
            else hi = mid;
        }
        positions[i] = (jint) lo;
    }
    (*env)->ReleaseIntArrayElements(env, results, positions, 0);
    (*env)->ReleaseLongArrayElements(env, queries, targets, JNI_ABORT);
    (*env)->ReleaseLongArrayElements(env, sorted, values, JNI_ABORT);
}
