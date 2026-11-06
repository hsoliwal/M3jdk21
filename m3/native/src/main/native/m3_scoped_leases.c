/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/64652588f0d4c23b1f6e0411090b2a796325ae0e/synexia-primitives/src/main/native/m3_scoped_leases.c blob 996e19908933; recipe m3/native/recipe/scoped-leases-20261106/relocate.py v1
#include <jni.h>
#include <stddef.h>
#include <stdint.h>
#include "jdk_m3_jni_M3JniByteConcat.h"

/* No native ownership, global JNI references, retained Java array pointers or off-heap state. */
JNIEXPORT jint JNICALL
Java_jdk_m3_jni_M3NativeLeaseScan_collectNative(
        JNIEnv *env, jclass type, jlongArray idsArray, jintArray leasesArray,
        jintArray pinsArray, jbyteArray statesArray, jint count, jlongArray outputArray) {
    (void)type;
    if (count < 0 || idsArray == NULL || leasesArray == NULL || pinsArray == NULL
            || statesArray == NULL || outputArray == NULL) {
        return -1; /* Java already validates public requests. */
    }
    /* Direct JNI entry is independently bounds-checked before any array borrowing.
     * Public Java validation cannot protect a caller that invokes this symbol directly. */
    if ((*env)->IsSameObject(env, idsArray, outputArray) == JNI_TRUE
            || (*env)->GetArrayLength(env, idsArray) < count
            || (*env)->GetArrayLength(env, leasesArray) < count
            || (*env)->GetArrayLength(env, pinsArray) < count
            || (*env)->GetArrayLength(env, statesArray) < count
            || (*env)->GetArrayLength(env, outputArray) < count) {
        return -1;
    }
    if (count == 0) return 0;
    jlong *ids = (*env)->GetLongArrayElements(env, idsArray, NULL);
    if (ids == NULL) return -1;
    jint *leases = (*env)->GetIntArrayElements(env, leasesArray, NULL);
    if (leases == NULL) {
        (*env)->ReleaseLongArrayElements(env, idsArray, ids, JNI_ABORT);
        return -1;
    }
    jint *pins = (*env)->GetIntArrayElements(env, pinsArray, NULL);
    if (pins == NULL) {
        (*env)->ReleaseIntArrayElements(env, leasesArray, leases, JNI_ABORT);
        (*env)->ReleaseLongArrayElements(env, idsArray, ids, JNI_ABORT);
        return -1;
    }
    jbyte *states = (*env)->GetByteArrayElements(env, statesArray, NULL);
    if (states == NULL) {
        (*env)->ReleaseIntArrayElements(env, pinsArray, pins, JNI_ABORT);
        (*env)->ReleaseIntArrayElements(env, leasesArray, leases, JNI_ABORT);
        (*env)->ReleaseLongArrayElements(env, idsArray, ids, JNI_ABORT);
        return -1;
    }
    jlong *output = (*env)->GetLongArrayElements(env, outputArray, NULL);
    if (output == NULL) {
        (*env)->ReleaseByteArrayElements(env, statesArray, states, JNI_ABORT);
        (*env)->ReleaseIntArrayElements(env, pinsArray, pins, JNI_ABORT);
        (*env)->ReleaseIntArrayElements(env, leasesArray, leases, JNI_ABORT);
        (*env)->ReleaseLongArrayElements(env, idsArray, ids, JNI_ABORT);
        return -1;
    }
    jint written = 0;
    for (jint i = 0; i < count; i++) {
        if (states[i] == 0 && leases[i] == 0 && pins[i] == 0) {
            output[written++] = ids[i];
        }
    }
    (*env)->ReleaseLongArrayElements(env, outputArray, output, 0);
    (*env)->ReleaseByteArrayElements(env, statesArray, states, JNI_ABORT);
    (*env)->ReleaseIntArrayElements(env, pinsArray, pins, JNI_ABORT);
    (*env)->ReleaseIntArrayElements(env, leasesArray, leases, JNI_ABORT);
    (*env)->ReleaseLongArrayElements(env, idsArray, ids, JNI_ABORT);
    return written;
}

/*
 * Bounded JNI window scan. Get*ArrayRegion copies only fixed-size stack chunks,
 * never the entire Java backing arrays. No retained pointers, native allocations,
 * global references, or JNI-owned handle lifecycle.
 */
#define M3_SCAN_CHUNK 128

static jlong m3_pack_window(jint next, jint written) {
    return (((jlong)next) << 32) | (((jlong)written) & 0xffffffffLL);
}

static jlong m3_invalid_window(JNIEnv *env, const char *message) {
    jclass error = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
    if (error != NULL) {
        (*env)->ThrowNew(env, error, message);
        (*env)->DeleteLocalRef(env, error);
    }
    return (jlong)-1;
}

JNIEXPORT jlong JNICALL
Java_jdk_m3_jni_M3NativeLeaseScan_collectNativeWindow(
        JNIEnv *env, jclass type, jlongArray idsArray, jintArray leasesArray,
        jintArray pinsArray, jbyteArray statesArray, jint count, jint first,
        jlongArray outputArray) {
    (void)type;
    if (idsArray == NULL || leasesArray == NULL || pinsArray == NULL
            || statesArray == NULL || outputArray == NULL || count < 0
            || first < 0 || first > count) {
        return m3_invalid_window(env, "invalid JNI window arguments");
    }
    if ((*env)->IsSameObject(env, idsArray, outputArray) == JNI_TRUE) {
        return m3_invalid_window(env, "output must not alias ids");
    }
    jsize cap = (*env)->GetArrayLength(env, outputArray);
    if (cap <= 0 || (*env)->GetArrayLength(env, idsArray) < count
            || (*env)->GetArrayLength(env, leasesArray) < count
            || (*env)->GetArrayLength(env, pinsArray) < count
            || (*env)->GetArrayLength(env, statesArray) < count) {
        return m3_invalid_window(env, "invalid JNI array capacity");
    }
    jlong ids[M3_SCAN_CHUNK];
    jint leases[M3_SCAN_CHUNK];
    jint pins[M3_SCAN_CHUNK];
    jbyte states[M3_SCAN_CHUNK];
    jlong buffered[M3_SCAN_CHUNK];
    jint next = first;
    jint written = 0;
    jint bufferedCount = 0;
    while (next < count && written < cap) {
        jint take = count - next;
        if (take > M3_SCAN_CHUNK) take = M3_SCAN_CHUNK;
        (*env)->GetLongArrayRegion(env, idsArray, next, take, ids);
        if ((*env)->ExceptionCheck(env) == JNI_TRUE) return (jlong)-1;
        (*env)->GetIntArrayRegion(env, leasesArray, next, take, leases);
        if ((*env)->ExceptionCheck(env) == JNI_TRUE) return (jlong)-1;
        (*env)->GetIntArrayRegion(env, pinsArray, next, take, pins);
        if ((*env)->ExceptionCheck(env) == JNI_TRUE) return (jlong)-1;
        (*env)->GetByteArrayRegion(env, statesArray, next, take, states);
        if ((*env)->ExceptionCheck(env) == JNI_TRUE) return (jlong)-1;
        for (jint i = 0; i < take; i++) {
            if (ids[i] <= 0 || leases[i] < 0 || pins[i] < 0
                    || states[i] < 0 || states[i] > 2) {
                /* Match Java's visible output prefix before rejecting the bad slot.
                 * Earlier complete chunks are already committed; flush only the
                 * outstanding eligible IDs. Never write the unchecked slot. */
                if (bufferedCount != 0) {
                    (*env)->SetLongArrayRegion(env, outputArray,
                            written - bufferedCount, bufferedCount, buffered);
                    if ((*env)->ExceptionCheck(env) == JNI_TRUE) return (jlong)-1;
                }
                return m3_invalid_window(env, "invalid JNI lease slot");
            }
            if (states[i] == 0 && leases[i] == 0 && pins[i] == 0) {
                buffered[bufferedCount++] = ids[i];
                written++;
                if (bufferedCount == M3_SCAN_CHUNK || written == cap) {
                    (*env)->SetLongArrayRegion(env, outputArray,
                            written - bufferedCount, bufferedCount, buffered);
                    if ((*env)->ExceptionCheck(env) == JNI_TRUE) return (jlong)-1;
                    bufferedCount = 0;
                }
            }
            next++;
            if (written == cap) break;
        }
    }
    if (bufferedCount != 0) {
        (*env)->SetLongArrayRegion(env, outputArray, written - bufferedCount,
                bufferedCount, buffered);
        if ((*env)->ExceptionCheck(env) == JNI_TRUE) return (jlong)-1;
    }
    return m3_pack_window(next, written);
}

/*
 * JNI materialized byte-array concatenation. The Java caller retains both
 * registered array borrows across this entire native call, including alias
 * occurrences. Never retain a Java heap pointer, global JNI reference, or
 * off-heap backing; use bounded Get/SetByteArrayRegion scratch instead of
 * critical pointers, which forbid arbitrary JNI calls in their scope.
 */
/*
 * This 4096-byte native transfer window is separate from the 128-slot
 * JNI eligibility scan: fewer Get/SetByteArrayRegion calls without any
 * critical pointers, heap allocations or retained source references.
 */
#define M3_CONCAT_REGION_CHUNK 4096

static jboolean m3_concat_copy_region(JNIEnv *env, jbyteArray source,
        jbyteArray output, jsize outputOffset, jsize count) {
    jbyte scratch[M3_CONCAT_REGION_CHUNK];
    jsize at = 0;
    while (at < count) {
        const jsize remaining = count - at;
        const jsize take = remaining > (jsize)M3_CONCAT_REGION_CHUNK
                ? (jsize)M3_CONCAT_REGION_CHUNK : remaining;
        (*env)->GetByteArrayRegion(env, source, at, take, scratch);
        if ((*env)->ExceptionCheck(env) == JNI_TRUE) return JNI_FALSE;
        (*env)->SetByteArrayRegion(env, output, outputOffset + at, take, scratch);
        if ((*env)->ExceptionCheck(env) == JNI_TRUE) return JNI_FALSE;
        at += take;
    }
    return JNI_TRUE;
}

JNIEXPORT jbyteArray JNICALL
Java_jdk_m3_jni_M3JniByteConcat_concatNative(
        JNIEnv *env, jclass type, jbyteArray left, jbyteArray right) {
    (void)type;
    if (left == NULL || right == NULL) {
        jclass error = (*env)->FindClass(env, "java/lang/NullPointerException");
        if (error != NULL) {
            (*env)->ThrowNew(env, error, "M3 JNI concat source is null");
            (*env)->DeleteLocalRef(env, error);
        }
        return NULL;
    }

    const jsize leftLength = (*env)->GetArrayLength(env, left);
    const jsize rightLength = (*env)->GetArrayLength(env, right);
    const jlong total = (jlong)leftLength + (jlong)rightLength;
    if (total > (jlong)INT32_MAX - 8LL) {
        jclass error = (*env)->FindClass(env, "java/lang/OutOfMemoryError");
        if (error != NULL) {
            (*env)->ThrowNew(env, error, "M3 JNI byte concat length limit");
            (*env)->DeleteLocalRef(env, error);
        }
        return NULL;
    }

    jbyteArray output = (*env)->NewByteArray(env, (jsize)total);
    if (output == NULL) return NULL; /* Pending OOME. */
    if (m3_concat_copy_region(env, left, output, 0, leftLength) == JNI_FALSE
            || m3_concat_copy_region(env, right, output,
                    leftLength, rightLength) == JNI_FALSE) {
        return NULL; /* Preserve pending exception. */
    }
    return output;
}
