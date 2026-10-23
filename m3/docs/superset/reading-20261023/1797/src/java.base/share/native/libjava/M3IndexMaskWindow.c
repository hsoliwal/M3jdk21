/* Modified 2026 by Hitesh Soliwal and contributors: trim JNI row spans to admitted ordinals. */
/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
/* Internal mask-window ABI in the JDK-owned libjava shared library.
 * Fixed 128-element JNI regions; no persistent pins, Java callbacks, or native heap storage.
 */
#include "jdk_internal_m3_M3IndexMaskWindow.h"
#include <stdint.h>

#define M3_MASK_WINDOW_CHUNK 128
#define M3_MASK_WINDOW_MAX 4096

static jlong m3_window_result(jint next, jint written) {
    return (((jlong) next) << 32) | ((jlong) written & INT64_C(0xffffffff));
}

static jlong m3_window_invalid(JNIEnv *env, const char *message) {
    jclass type = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
    if (type != NULL) {
        (*env)->ThrowNew(env, type, message);
        (*env)->DeleteLocalRef(env, type);
    }
    return (jlong) -1;
}

/* Pure selection atom over caller-owned scratch, shared by both JNI entry points.
 * Preconditions: 0 <= take <= M3_MASK_WINDOW_CHUNK, remaining > 0, valid
 * unsigned-byte masks. Offsets are stable source ordinals, not row values.
 * No JNI calls, allocation, retained state, or payload mutation occur here.
 * Preserve the incumbent two-condition predicate: its fused native alternative
 * was rejected by the recorded resource gate. */
static jint m3_window_select_offsets(const jbyte *marks, jint take,
        jint required, jint excluded, jint remaining, unsigned char *offsets,
        jint *consumed_out) {
    jint matches = 0;
    jint consumed = 0;
    while (consumed < take && matches < remaining) {
        unsigned int flags = (unsigned int) (unsigned char) marks[consumed];
        if ((flags & (unsigned int) required) == (unsigned int) required
                && (flags & (unsigned int) excluded) == 0U) {
            offsets[matches++] = (unsigned char) consumed;
        }
        consumed++;
    }
    *consumed_out = consumed;
    return matches;
}

/* Reuse #1131's sparse JNI scan atom for legacy and bounded source windows. */
static jlong m3_window_scan_range(JNIEnv *env, jintArray rows, jbyteArray states,
        jint first, jint end, jboolean full_source, jint required, jint excluded,
        jintArray output, jint output_offset, jint limit) {
    if (rows == NULL || states == NULL || output == NULL || required < 0
            || required > 255 || excluded < 0 || excluded > 255 || limit < 0
            || limit > M3_MASK_WINDOW_MAX) {
        return m3_window_invalid(env, "invalid mask-window arguments");
    }
    jsize n = (*env)->GetArrayLength(env, rows);
    if ((*env)->ExceptionCheck(env)) return (jlong) -1;
    if (full_source) end = n;
    if (first < 0 || first > n || end < first || end > n
            || (*env)->GetArrayLength(env, states) != n
            || (*env)->IsSameObject(env, rows, output) == JNI_TRUE) {
        return m3_window_invalid(env, "invalid source/alias geometry");
    }
    if ((*env)->ExceptionCheck(env)) return (jlong) -1;
    jsize capacity = (*env)->GetArrayLength(env, output);
    if ((*env)->ExceptionCheck(env)) return (jlong) -1;
    if (output_offset < 0 || output_offset > capacity || limit > capacity - output_offset) {
        return m3_window_invalid(env, "invalid output geometry");
    }
    if (limit == 0) return m3_window_result(first, 0);

    /* Remain inside this ABI: an unavailable required native symbol must still refuse. */
    if ((required & excluded) != 0) return m3_window_result(end, 0);
    jint values[M3_MASK_WINDOW_CHUNK];
    if ((required | excluded) == 0) {
        jint count = end - first;
        if (count > limit) count = limit;
        for (jint copied = 0; copied < count;) {
            jint take = count - copied;
            if (take > M3_MASK_WINDOW_CHUNK) take = M3_MASK_WINDOW_CHUNK;
            (*env)->GetIntArrayRegion(env, rows, first + copied, take, values);
            if ((*env)->ExceptionCheck(env)) return (jlong) -1;
            (*env)->SetIntArrayRegion(env, output, output_offset + copied, take, values);
            if ((*env)->ExceptionCheck(env)) return (jlong) -1;
            copied += take;
        }
        return m3_window_result(first + count, count);
    }

    jbyte marks[M3_MASK_WINDOW_CHUNK];
    jint selected[M3_MASK_WINDOW_CHUNK];
    unsigned char offsets[M3_MASK_WINDOW_CHUNK];
    jint next = first;
    jint written = 0;
    jint pending = 0;
    while (next < end && written < limit) {
        jint take = end - next;
        if (take > M3_MASK_WINDOW_CHUNK) take = M3_MASK_WINDOW_CHUNK;
        (*env)->GetByteArrayRegion(env, states, next, take, marks);
        if ((*env)->ExceptionCheck(env)) return (jlong) -1;
        /* One state pass records relative ordinals, stopping when the output fills.
         * Only the enclosing admitted row span crosses JNI. Offsets fit in one byte
         * because the fixed chunk is 128; no retained index or native heap is added. */
        jint consumed;
        jint remaining = limit - written;
        jint matches = m3_window_select_offsets(marks, take, required, excluded,
                remaining, offsets, &consumed);
        if (matches != 0) {
            jint first_match = offsets[0];
            jint span = (jint) offsets[matches - 1] - first_match + 1;
            (*env)->GetIntArrayRegion(env, rows, next + first_match, span, values);
            if ((*env)->ExceptionCheck(env)) return (jlong) -1;
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
            if (matches == M3_MASK_WINDOW_CHUNK && pending == 0) {
                /* Every ordinal in this chunk is selected; values is already contiguous. */
                written += matches;
                (*env)->SetIntArrayRegion(env, output,
                        output_offset + written - matches, matches, values);
                if ((*env)->ExceptionCheck(env)) return (jlong) -1;
                next += consumed;
                continue;
            }
            for (jint i = 0; i < matches; i++) {
                selected[pending++] = values[(jint) offsets[i] - first_match];
                written++;
                if (pending == M3_MASK_WINDOW_CHUNK || written == limit) {
                    (*env)->SetIntArrayRegion(env, output,
                            output_offset + written - pending, pending, selected);
                    if ((*env)->ExceptionCheck(env)) return (jlong) -1;
                    pending = 0;
=======
            /* A contiguous selected span already is the compacted output. Bypass
             * identity compaction only when the original loop would flush exactly
             * this batch here. JNI call order, ranges and failure points stay equal. */
            if (matches == span && pending == 0
                    && (matches == M3_MASK_WINDOW_CHUNK || matches == remaining)) {
                (*env)->SetIntArrayRegion(env, output, output_offset + written, matches, values);
                if ((*env)->ExceptionCheck(env)) return (jlong) -1;
                written += matches;
            } else {
                for (jint i = 0; i < matches; i++) {
                    selected[pending++] = values[(jint) offsets[i] - first_match];
                    written++;
                    if (pending == M3_MASK_WINDOW_CHUNK || written == limit) {
                        (*env)->SetIntArrayRegion(env, output,
                                output_offset + written - pending, pending, selected);
                        if ((*env)->ExceptionCheck(env)) return (jlong) -1;
                        pending = 0;
                    }
>>>>>>> e93868c34f5e3f6857ed64892bafa143862429a9
                }
            }
        }
        next += consumed;
    }
    if (pending != 0) {
        (*env)->SetIntArrayRegion(env, output,
                output_offset + written - pending, pending, selected);
        if ((*env)->ExceptionCheck(env)) return (jlong) -1;
    }
    return m3_window_result(next, written);

}

/* Internal M3JDK21 ABI: retains the original full-remainder semantics. */
JNIEXPORT jlong JNICALL
Java_jdk_internal_m3_M3IndexMaskWindow_scanNative(
        JNIEnv *env, jclass owner, jintArray rows, jbyteArray states, jint first,
        jint required, jint excluded, jintArray output, jint output_offset, jint limit) {
    (void) owner;
    return m3_window_scan_range(env, rows, states, first, 0, JNI_TRUE,
            required, excluded, output, output_offset, limit);
}

/* JDK libjava bounded JNI ABI: caller-admitted exclusive end ordinal. */
JNIEXPORT jlong JNICALL
Java_jdk_internal_m3_M3IndexMaskWindow_scanBoundedNative(
        JNIEnv *env, jclass owner, jintArray rows, jbyteArray states, jint first,
        jint end, jint required, jint excluded, jintArray output,
        jint output_offset, jint limit) {
    (void) owner;
    return m3_window_scan_range(env, rows, states, first, end, JNI_FALSE,
            required, excluded, output, output_offset, limit);
}


/* State-only reduction. Keep the established two-condition native predicate;
 * the prior selection predicate-fusion candidate remains rejected. */
static jint m3_window_count_matches(const jbyte *marks, jint take,
        jint required, jint excluded) {
    jint count = 0;
    for (jint i = 0; i < take; i++) {
        unsigned int flags = (unsigned int) (unsigned char) marks[i];
        if ((flags & (unsigned int) required) == (unsigned int) required
                && (flags & (unsigned int) excluded) == 0U) {
            count++;
        }
    }
    return count;
}

/* Additive count ABI: only bounded state reads, 128 scratch bytes plus locals.
 * No row-ID reads, output writes, allocation, callbacks or critical-array pins. */
JNIEXPORT jlong JNICALL
Java_jdk_internal_m3_M3IndexMaskWindow_countBoundedNative(
        JNIEnv *env, jclass owner, jbyteArray states, jint first, jint end,
        jint required, jint excluded) {
    (void) owner;
    if ((*env)->ExceptionCheck(env)) return (jlong) -1;
    if (states == NULL || required < 0 || required > 255
            || excluded < 0 || excluded > 255) {
        return m3_window_invalid(env, "invalid mask-window count arguments");
    }
    jsize length = (*env)->GetArrayLength(env, states);
    if ((*env)->ExceptionCheck(env)) return (jlong) -1;
    if (first < 0 || first > length || end < first || end > length) {
        return m3_window_invalid(env, "invalid mask-window count geometry");
    }
    if ((required & excluded) != 0) return m3_window_result(end, 0);
    if ((required | excluded) == 0) return m3_window_result(end, end - first);
    jbyte marks[M3_MASK_WINDOW_CHUNK];
    jint count = 0;
    for (jint next = first; next < end;) {
        jint take = end - next;
        if (take > M3_MASK_WINDOW_CHUNK) take = M3_MASK_WINDOW_CHUNK;
        (*env)->GetByteArrayRegion(env, states, next, take, marks);
        if ((*env)->ExceptionCheck(env)) return (jlong) -1;
        count += m3_window_count_matches(marks, take, required, excluded);
        next += take;
    }
    return m3_window_result(end, count);
}
