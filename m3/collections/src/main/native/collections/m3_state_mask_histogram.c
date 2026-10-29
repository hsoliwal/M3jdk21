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
            /* Histogram counts are independent of the copied chunk's visit order. */
            for (jint i = 0; i < take; i++) {
                buckets[(unsigned char) chunk[i]]++;
            }
            offset += take;
        }
    } else if (count < 2 || stride >= M3_STATE_CHUNK || stride <= -M3_STATE_CHUNK) {
        /* No two selected points fit in one scratch region: retain the scalar path. */
        jlong at = first;
        for (jint i = 0; i < count; i++, at += stride) {
            jbyte state = 0;
            (*env)->GetByteArrayRegion(env, source, (jsize) at, 1, &state);
            if ((*env)->ExceptionCheck(env)) return;
            buckets[(unsigned char) state]++;
        }
<<<<<<< HEAD
    } else {
        /* Batch selected points into the existing bounded scratch region. Cast before
         * negation so even INT_MIN strides are safe for empty/singleton paths. */
        jlong magnitude = stride > 0 ? (jlong) stride : -(jlong) stride;
        jint batch_limit = (jint) (INT64_C(1) + (M3_STATE_CHUNK - 1) / magnitude);
        for (jint offset = 0; offset < count;) {
            jint take = count - offset;
            if (take > batch_limit) take = batch_limit;
            jlong span = INT64_C(1) + ((jlong) take - INT64_C(1)) * magnitude;
            jlong at = (jlong) first + (jlong) offset * (jlong) stride;
            jlong begin = stride > 0 ? at : at - (span - INT64_C(1));
            (*env)->GetByteArrayRegion(env, source, (jsize) begin, (jsize) span, chunk);
            if ((*env)->ExceptionCheck(env)) return;
            for (jint i = 0; i < take; i++) {
                jlong cell = stride > 0 ? (jlong) i * magnitude
                        : span - INT64_C(1) - (jlong) i * magnitude;
                buckets[(unsigned char) chunk[(jsize) cell]]++;
            }
            offset += take;
        }
    }
    (*env)->SetIntArrayRegion(env, result, 0, M3_STATE_COUNT, buckets);
}

/* Additive physical-index histogram ABI. Source offsets and state masks retain
 * Java parent ownership. Region copies are bounded to 256 scalars, no critical
 * pins, heap buffers, callbacks, or persistent native pointers.
 */
#define M3_INDEX_HIST_CHUNK 256

JNIEXPORT jint JNICALL
Java_com_m3_collections_M3StateMaskHistogram_indexedAbiNative(
        JNIEnv *env, jclass owner) {
    (void) env;
    (void) owner;
    return 1;
}

JNIEXPORT void JNICALL
Java_com_m3_collections_M3StateMaskHistogram_tallyIndexedNative(
        JNIEnv *env, jclass owner, jintArray offsets, jbyteArray states,
        jint first, jint count, jint stride, jintArray result) {
    (void) owner;
    if (offsets == NULL || states == NULL || result == NULL || count < 0 || stride == 0) {
        m3_invalid(env, "invalid indexed histogram inputs");
        return;
    }
    jsize offset_length = (*env)->GetArrayLength(env, offsets);
    if ((*env)->ExceptionCheck(env)) return;
    jsize state_length = (*env)->GetArrayLength(env, states);
    if ((*env)->ExceptionCheck(env)) return;
    jsize result_length = (*env)->GetArrayLength(env, result);
    if ((*env)->ExceptionCheck(env)) return;
    if (first < 0 || first > offset_length || result_length != M3_STATE_COUNT) {
        m3_invalid(env, "invalid indexed histogram geometry");
        return;
    }
    if (count > 0) {
        jlong last = (jlong) first + ((jlong) count - 1) * (jlong) stride;
        if (first == offset_length || last < 0 || last >= offset_length) {
            m3_invalid(env, "indexed histogram path outside offset lane");
            return;
        }
    }

    jint buckets[M3_STATE_COUNT] = {0};
    jint index_region[M3_INDEX_HIST_CHUNK];
    jbyte state_region[M3_INDEX_HIST_CHUNK];
    jlong cached_page = -1;
    jlong magnitude = stride < 0 ? -(jlong) stride : (jlong) stride;

    for (jint processed = 0; processed < count;) {
        jint take = count - processed;
        if (take > M3_INDEX_HIST_CHUNK) take = M3_INDEX_HIST_CHUNK;
        jlong at = (jlong) first + (jlong) processed * (jlong) stride;
        jlong span = 1 + ((jlong) take - 1) * magnitude;
        jlong index_start = stride > 0 ? at : at - span + 1;
        int dense = span <= M3_INDEX_HIST_CHUNK;
        if (dense) {
            (*env)->GetIntArrayRegion(env, offsets, (jsize) index_start,
                    (jsize) span, index_region);
            if ((*env)->ExceptionCheck(env)) return;
        }
        for (jint i = 0; i < take; i++) {
            jint physical;
            if (dense) {
                jsize element = (jsize) (stride > 0 ? (jlong) i * magnitude
                        : span - 1 - (jlong) i * magnitude);
                physical = index_region[element];
            } else {
                jlong selected = at + (jlong) i * (jlong) stride;
                (*env)->GetIntArrayRegion(env, offsets, (jsize) selected, 1, &physical);
                if ((*env)->ExceptionCheck(env)) return;
            }
            if (physical < 0 || physical >= state_length) {
                m3_invalid(env, "physical offset outside indexed state lane");
                return;
            }
            jlong page = ((jlong) physical / M3_INDEX_HIST_CHUNK) * M3_INDEX_HIST_CHUNK;
            if (page != cached_page) {
                jsize remaining = state_length - (jsize) page;
                jsize chunk = remaining < M3_INDEX_HIST_CHUNK
                        ? remaining : M3_INDEX_HIST_CHUNK;
                (*env)->GetByteArrayRegion(env, states, (jsize) page, chunk, state_region);
                if ((*env)->ExceptionCheck(env)) return;
                cached_page = page;
            }
            buckets[(unsigned char) state_region[(jsize) (physical - cached_page)]]++;
        }
        processed += take;
||||||| merged common ancestors
=======
    } else {
        /* Batch selected points into the existing bounded scratch region. Cast before
         * negation so even INT_MIN strides are safe for empty/singleton paths. */
        jlong magnitude = stride > 0 ? (jlong) stride : -(jlong) stride;
        jint batch_limit = (jint) (INT64_C(1) + (M3_STATE_CHUNK - 1) / magnitude);
        for (jint offset = 0; offset < count;) {
            jint take = count - offset;
            if (take > batch_limit) take = batch_limit;
            jlong span = INT64_C(1) + ((jlong) take - INT64_C(1)) * magnitude;
            jlong at = (jlong) first + (jlong) offset * (jlong) stride;
            jlong begin = stride > 0 ? at : at - (span - INT64_C(1));
            (*env)->GetByteArrayRegion(env, source, (jsize) begin, (jsize) span, chunk);
            if ((*env)->ExceptionCheck(env)) return;
            for (jint i = 0; i < take; i++) {
                jlong cell = stride > 0 ? (jlong) i * magnitude
                        : span - INT64_C(1) - (jlong) i * magnitude;
                buckets[(unsigned char) chunk[(jsize) cell]]++;
            }
            offset += take;
        }
>>>>>>> 9b155e730c19162ea2f2fa97ba003a880ee2c5cf
    }
    (*env)->SetIntArrayRegion(env, result, 0, M3_STATE_COUNT, buckets);
}
