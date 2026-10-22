/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
#include <jni.h>
#include <stdint.h>
#include <stdlib.h>

/* Reuse the existing mask-window owner's bounded-region pattern, not its
 * byte-mask/alias contract. These inputs retain all 32 state bits. */
#define M3_VIEWPORT_CHUNK 128

static void raise(JNIEnv *env, const char *type, const char *message) {
    jclass cls = (*env)->FindClass(env, type);
    if (cls != NULL) {
        (*env)->ThrowNew(env, cls, message);
        (*env)->DeleteLocalRef(env, cls);
    }
}

static int matches(jint state, jint required, jint forbidden) {
    uint32_t value = (uint32_t) state;
    uint32_t yes = (uint32_t) required;
    uint32_t no = (uint32_t) forbidden;
    return (value & yes) == yes && (value & no) == 0U;
}

/* Never acquire or retain a pointer to either complete Java input array. */
static int read_chunk(JNIEnv *env, jintArray offsets, jintArray flags,
        jint first, jint length, jint *src, jint *states) {
    (*env)->GetIntArrayRegion(env, offsets, first, length, src);
    if ((*env)->ExceptionCheck(env)) return 0;
    (*env)->GetIntArrayRegion(env, flags, first, length, states);
    return !(*env)->ExceptionCheck(env);
}

JNIEXPORT jint JNICALL
Java_com_m3_collections_M3MaskViewportNative_project(JNIEnv *env, jclass cls,
        jintArray offsets, jintArray flags, jint first, jint length,
        jint required, jint forbidden, jintArray target, jint targetOffset) {
    (void) cls;
    if (offsets == NULL || flags == NULL || target == NULL) {
        raise(env, "java/lang/NullPointerException", "null projection array");
        return 0;
    }
    const jsize sourceSize = (*env)->GetArrayLength(env, offsets);
    const jsize flagsSize = (*env)->GetArrayLength(env, flags);
    const jsize outputSize = (*env)->GetArrayLength(env, target);
    if (sourceSize != flagsSize) {
        raise(env, "java/lang/IllegalArgumentException", "offset and state array lengths differ");
        return 0;
    }
    if (first < 0 || length < 0 || first > sourceSize - length ||
            targetOffset < 0 || targetOffset > outputSize) {
        raise(env, "java/lang/IndexOutOfBoundsException", "invalid array window");
        return 0;
    }
    const jint end = first + length;
    jint src[M3_VIEWPORT_CHUNK];
    jint states[M3_VIEWPORT_CHUNK];
    jint selected = 0;
    for (jint begin = first; begin < end;) {
        jint take = end - begin;
        if (take > M3_VIEWPORT_CHUNK) take = M3_VIEWPORT_CHUNK;
        if (!read_chunk(env, offsets, flags, begin, take, src, states)) return 0;
        for (jint i = 0; i < take; ++i) {
            if (src[i] < 0) {
                raise(env, "java/lang/IllegalArgumentException", "negative logical offset");
                return 0;
            }
            if (matches(states[i], required, forbidden)) ++selected;
        }
        begin += take;
    }
    if (selected > outputSize - targetOffset) {
        raise(env, "java/lang/IndexOutOfBoundsException", "destination capacity");
        return 0;
    }
    if (selected == 0) return 0;
    if ((size_t) selected > SIZE_MAX / sizeof(jint)) {
        raise(env, "java/lang/OutOfMemoryError", "projection size overflow");
        return 0;
    }
    /* Keep the original O(selected) scratch and single-commit writeback.
     * Alias and JNI-read failures must not publish a partially selected result. */
    jint *scratch = (jint *) malloc((size_t) selected * sizeof(jint));
    if (scratch == NULL) {
        raise(env, "java/lang/OutOfMemoryError", "projection scratch allocation");
        return 0;
    }
    jint cursor = 0;
    for (jint begin = first; begin < end;) {
        jint take = end - begin;
        if (take > M3_VIEWPORT_CHUNK) take = M3_VIEWPORT_CHUNK;
        /* Complete preflight proves a dense window under the existing stable-
         * input contract. Do not fetch state bytes twice when every row passes. */
        if (selected == length) {
            (*env)->GetIntArrayRegion(env, offsets, begin, take, scratch + cursor);
            if ((*env)->ExceptionCheck(env)) {
                free(scratch);
                return 0;
            }
            cursor += take;
            begin += take;
            continue;
        }
        if (!read_chunk(env, offsets, flags, begin, take, src, states)) {
            free(scratch);
            return 0;
        }
        for (jint i = 0; i < take; ++i) {
            if (matches(states[i], required, forbidden)) {
                /* Stable inputs are the caller's contract; a violated count must
                 * nevertheless never write beyond the native allocation. */
                if (cursor == selected) {
                    free(scratch);
                    raise(env, "java/util/ConcurrentModificationException", "projection inputs changed");
                    return 0;
                }
                scratch[cursor++] = src[i];
            }
        }
        begin += take;
    }
    if (cursor != selected) {
        free(scratch);
        raise(env, "java/util/ConcurrentModificationException", "projection inputs changed");
        return 0;
    }
    (*env)->SetIntArrayRegion(env, target, targetOffset, selected, scratch);
    free(scratch);
    return (*env)->ExceptionCheck(env) ? 0 : selected;
}

/* The Java page contract validates the full input even for an empty result.
 * Publication is one region write, so overlapping owners need only page scratch. */
JNIEXPORT jint JNICALL
Java_com_m3_collections_M3MaskViewportNative_projectPage(JNIEnv *env, jclass cls,
        jintArray offsets, jintArray flags, jint first, jint length,
        jint required, jint forbidden, jint skipVisible, jint maxVisible,
        jintArray target, jint targetOffset) {
    (void) cls;
    if (offsets == NULL) {
        raise(env, "java/lang/NullPointerException", "logicalOffsets");
        return 0;
    }
    if (flags == NULL) {
        raise(env, "java/lang/NullPointerException", "states");
        return 0;
    }
    if (target == NULL) {
        raise(env, "java/lang/NullPointerException", "target");
        return 0;
    }
    const jsize sourceSize = (*env)->GetArrayLength(env, offsets);
    const jsize flagsSize = (*env)->GetArrayLength(env, flags);
    const jsize outputSize = (*env)->GetArrayLength(env, target);
    if (sourceSize != flagsSize) {
        raise(env, "java/lang/IllegalArgumentException", "offset and state array lengths differ");
        return 0;
    }
    if (first < 0 || length < 0 || first > sourceSize - length ||
            targetOffset < 0 || targetOffset > outputSize) {
        raise(env, "java/lang/IndexOutOfBoundsException", "invalid array window");
        return 0;
    }
    if (skipVisible < 0 || maxVisible < 0) {
        raise(env, "java/lang/IndexOutOfBoundsException", "negative visible page bounds");
        return 0;
    }
    const jint end = first + length;
    jint src[M3_VIEWPORT_CHUNK];
    jint states[M3_VIEWPORT_CHUNK];
    /* Capture a bounded page while validating. This buffer is call-local,
     * never retained and never published before complete preflight succeeds. */
    jint inlinePage[M3_VIEWPORT_CHUNK];
    jint available = 0;
    jint needed = 0;
    jint pageFirst = end;
    jint pageEnd = end;
    for (jint begin = first; begin < end;) {
        jint take = end - begin;
        if (take > M3_VIEWPORT_CHUNK) take = M3_VIEWPORT_CHUNK;
        if (!read_chunk(env, offsets, flags, begin, take, src, states)) return 0;
        for (jint i = 0; i < take; ++i) {
            if (src[i] < 0) {
                raise(env, "java/lang/IllegalArgumentException", "negative logical offset");
                return 0;
            }
            if (matches(states[i], required, forbidden)) {
                /* No skip+limit arithmetic: both can independently be INT_MAX. */
                if (available >= skipVisible && needed < maxVisible) {
                    if (needed == 0) pageFirst = begin + i;
                    pageEnd = begin + i + 1;
                    if (needed < M3_VIEWPORT_CHUNK) inlinePage[needed] = src[i];
                    ++needed;
                }
                ++available;
            }
        }
        begin += take;
    }
    if (needed > outputSize - targetOffset) {
        raise(env, "java/lang/IndexOutOfBoundsException", "destination capacity");
        return 0;
    }
    if (needed == 0) return 0;
    if (needed <= M3_VIEWPORT_CHUNK) {
        /* A small returned page needs neither heap scratch nor a second input
         * pass. Source/target aliases are safe: all input reads are complete. */
        (*env)->SetIntArrayRegion(env, target, targetOffset, needed, inlinePage);
        return (*env)->ExceptionCheck(env) ? 0 : needed;
    }
    if ((size_t) needed > SIZE_MAX / sizeof(jint)) {
        raise(env, "java/lang/OutOfMemoryError", "projection size overflow");
        return 0;
    }
    jint *scratch = (jint *) malloc((size_t) needed * sizeof(jint));
    if (scratch == NULL) {
        raise(env, "java/lang/OutOfMemoryError", "projection scratch allocation");
        return 0;
    }
    jint cursor = 0;
    for (jint begin = pageFirst; begin < pageEnd;) {
        jint take = pageEnd - begin;
        if (take > M3_VIEWPORT_CHUNK) take = M3_VIEWPORT_CHUNK;
        if (pageEnd - pageFirst == needed) {
            /* Preflight proves this page is contiguous even in a sparse window.
             * Only its offsets are needed on the second pass. */
            (*env)->GetIntArrayRegion(env, offsets, begin, take, scratch + cursor);
            if ((*env)->ExceptionCheck(env)) {
                free(scratch);
                return 0;
            }
            cursor += take;
        } else {
            if (!read_chunk(env, offsets, flags, begin, take, src, states)) {
                free(scratch);
                return 0;
            }
            for (jint i = 0; i < take; ++i) {
                if (matches(states[i], required, forbidden)) {
                    if (cursor == needed) {
                        free(scratch);
                        raise(env, "java/util/ConcurrentModificationException", "projection inputs changed");
                        return 0;
                    }
                    scratch[cursor++] = src[i];
                }
            }
        }
        begin += take;
    }
    if (cursor != needed) {
        free(scratch);
        raise(env, "java/util/ConcurrentModificationException", "projection inputs changed");
        return 0;
    }
    (*env)->SetIntArrayRegion(env, target, targetOffset, needed, scratch);
    free(scratch);
    return (*env)->ExceptionCheck(env) ? 0 : needed;
}
