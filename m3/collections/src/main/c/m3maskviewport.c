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
        jint first, jint length, int read_states, jint *src, jint *states) {
    (*env)->GetIntArrayRegion(env, offsets, first, length, src);
    if ((*env)->ExceptionCheck(env)) return 0;
    if (!read_states) return 1;
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
    /* State-independent queries still validate every logical offset. */
    const int all_states = required == 0 && forbidden == 0;
    const int read_states = !all_states
            && (((uint32_t) required & (uint32_t) forbidden) == 0U);
    const jint end = first + length;
    jint src[M3_VIEWPORT_CHUNK];
    jint states[M3_VIEWPORT_CHUNK];
    /* Reuse page capture for a small full selection. Keep all input reads and
     * validation before publication, including late errors and aliased output. */
    jint inlineSelection[M3_VIEWPORT_CHUNK];
    jint selected = 0;
    for (jint begin = first; begin < end;) {
        jint take = end - begin;
        if (take > M3_VIEWPORT_CHUNK) take = M3_VIEWPORT_CHUNK;
        if (!read_chunk(env, offsets, flags, begin, take, read_states, src, states)) return 0;
        for (jint i = 0; i < take; ++i) {
            if (src[i] < 0) {
                raise(env, "java/lang/IllegalArgumentException", "negative logical offset");
                return 0;
            }
            if (all_states || (read_states && matches(states[i], required, forbidden))) {
                if (selected < M3_VIEWPORT_CHUNK) inlineSelection[selected] = src[i];
                ++selected;
            }
        }
        begin += take;
    }
    if (selected > outputSize - targetOffset) {
        raise(env, "java/lang/IndexOutOfBoundsException", "destination capacity");
        return 0;
    }
    if (selected == 0) return 0;
    if (selected <= M3_VIEWPORT_CHUNK) {
        (*env)->SetIntArrayRegion(env, target, targetOffset, selected, inlineSelection);
        return (*env)->ExceptionCheck(env) ? 0 : selected;
    }
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
        if (!read_chunk(env, offsets, flags, begin, take, read_states, src, states)) {
            free(scratch);
            return 0;
        }
        for (jint i = 0; i < take; ++i) {
            if (all_states || (read_states && matches(states[i], required, forbidden))) {
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
    /* State-independent queries still validate every logical offset. */
    const int all_states = required == 0 && forbidden == 0;
    const int read_states = !all_states
            && (((uint32_t) required & (uint32_t) forbidden) == 0U);
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
        if (!read_chunk(env, offsets, flags, begin, take, read_states, src, states)) return 0;
        for (jint i = 0; i < take; ++i) {
            if (src[i] < 0) {
                raise(env, "java/lang/IllegalArgumentException", "negative logical offset");
                return 0;
            }
            if (all_states || (read_states && matches(states[i], required, forbidden))) {
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
            if (!read_chunk(env, offsets, flags, begin, take, read_states, src, states)) {
                free(scratch);
                return 0;
            }
            for (jint i = 0; i < take; ++i) {
                if (all_states || (read_states && matches(states[i], required, forbidden))) {
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

/* Additive JNI original-owner CPU viewport output. Old project symbol unchanged. */
#define M3_FRAME_REGION 128

static int m3_read_frame(JNIEnv *env, jintArray offsets, jintArray flags,
        jint position, jint count, int fetchStates, jint *ids, jint *masks) {
    (*env)->GetIntArrayRegion(env, offsets, position, count, ids);
    if ((*env)->ExceptionCheck(env)) return 0;
    if (fetchStates) {
        (*env)->GetIntArrayRegion(env, flags, position, count, masks);
        if ((*env)->ExceptionCheck(env)) return 0;
    }
    return 1;
}

JNIEXPORT jint JNICALL
Java_com_m3_collections_M3MaskViewportNative_projectFramePage(JNIEnv *env, jclass cls,
        jintArray offsets, jintArray flags, jint first, jint length,
        jint required, jint forbidden, jint skipVisible, jint maxVisible,
        jintArray rows, jint rowOffset, jintArray slots, jint slotOffset,
        jintArray words, jint wordOffset) {
    (void) cls;
    if (offsets == NULL || flags == NULL || rows == NULL || slots == NULL || words == NULL) {
        raise(env, "java/lang/NullPointerException", "null frame lane");
        return 0;
    }
    const jsize size = (*env)->GetArrayLength(env, offsets);
    const jsize flagSize = (*env)->GetArrayLength(env, flags);
    const jsize rowSize = (*env)->GetArrayLength(env, rows);
    const jsize slotSize = (*env)->GetArrayLength(env, slots);
    const jsize wordSize = (*env)->GetArrayLength(env, words);
    if (size != flagSize) {
        raise(env, "java/lang/IllegalArgumentException", "offset and state array lengths differ");
        return 0;
    }
    if ((*env)->IsSameObject(env, rows, offsets) || (*env)->IsSameObject(env, rows, flags)
            || (*env)->IsSameObject(env, slots, offsets) || (*env)->IsSameObject(env, slots, flags)
            || (*env)->IsSameObject(env, words, offsets) || (*env)->IsSameObject(env, words, flags)
            || (*env)->IsSameObject(env, rows, slots) || (*env)->IsSameObject(env, rows, words)
            || (*env)->IsSameObject(env, slots, words)) {
        raise(env, "java/lang/IllegalArgumentException", "frame outputs must be disjoint");
        return 0;
    }
    if (first < 0 || length < 0 || first > size - length
            || skipVisible < 0 || maxVisible < 0) {
        raise(env, "java/lang/IndexOutOfBoundsException", "invalid source window");
        return 0;
    }

    const int allStates = required == 0 && forbidden == 0;
    const int contradictory = (((uint32_t) required & (uint32_t) forbidden) != 0U);
    const int fetchStates = !allStates && !contradictory;
    const jint end = first + length;
    /* For a bounded filtered frame, stage every selected result during the
     * mandatory source validation pass. No second JNI input-region traversal. */
    const int inlineFast = fetchStates && maxVisible <= M3_FRAME_REGION;
    jint small[M3_FRAME_REGION * 3];
    jint inlineWritten = 0;
    jint inlineLeft = skipVisible;
    jint ids[M3_FRAME_REGION];
    jint masks[M3_FRAME_REGION];
    jint available = 0;
    for (jint start = first; start < end;) {
        jint take = end - start;
        if (take > M3_FRAME_REGION) take = M3_FRAME_REGION;
        if (!m3_read_frame(env, offsets, flags, start, take, fetchStates, ids, masks)) return 0;
        for (jint i = 0; i < take; ++i) {
            if (ids[i] < 0) {
                raise(env, "java/lang/IllegalArgumentException", "negative logical offset");
                return 0;
            }
            if (allStates || (fetchStates && matches(masks[i], required, forbidden))) {
                if (inlineFast && inlineWritten < maxVisible) {
                    if (inlineLeft > 0) {
                        --inlineLeft;
                    } else {
                        small[inlineWritten] = ids[i];
                        small[M3_FRAME_REGION + inlineWritten] = start + i;
                        small[M3_FRAME_REGION * 2 + inlineWritten] = masks[i];
                        ++inlineWritten;
                    }
                }
                ++available;
            }
        }
        start += take;
    }
    const jint needed = skipVisible >= available ? 0
            : (available - skipVisible < maxVisible ? available - skipVisible : maxVisible);
    if (rowOffset < 0 || slotOffset < 0 || wordOffset < 0
            || needed > rowSize || rowOffset > rowSize - needed
            || needed > slotSize || slotOffset > slotSize - needed
            || needed > wordSize || wordOffset > wordSize - needed) {
        raise(env, "java/lang/IndexOutOfBoundsException", "destination capacity");
        return 0;
    }
    if (needed == 0) return 0;
    if (inlineFast) {
        if (inlineWritten != needed) {
            raise(env, "java/util/ConcurrentModificationException", "frame input changed");
            return 0;
        }
        (*env)->SetIntArrayRegion(env, rows, rowOffset, needed, small);
        if (!(*env)->ExceptionCheck(env)) {
            (*env)->SetIntArrayRegion(env, slots, slotOffset, needed, small + M3_FRAME_REGION);
        }
        if (!(*env)->ExceptionCheck(env)) {
            (*env)->SetIntArrayRegion(env, words, wordOffset, needed, small + M3_FRAME_REGION * 2);
        }
        return (*env)->ExceptionCheck(env) ? 0 : needed;
    }
    if ((size_t) needed > SIZE_MAX / (3U * sizeof(jint))) {
        raise(env, "java/lang/OutOfMemoryError", "scratch size overflow");
        return 0;
    }
    jint *scratch = small;
    if (needed > M3_FRAME_REGION) {
        scratch = (jint *) malloc(3U * (size_t) needed * sizeof(jint));
        if (scratch == NULL) {
            raise(env, "java/lang/OutOfMemoryError", "scratch allocation");
            return 0;
        }
    }
    jint *ordinals = scratch + needed;
    jint *values = scratch + ((size_t) needed * 2U);
    jint left = skipVisible;
    jint written = 0;
    if (allStates) {
        /* All offsets pass the mask. The first pass validated the complete
         * source; the requested page is now a contiguous physical interval.
         * Fetch only page offsets and original state words, even for a huge
         * skipVisible. Do not acquire whole Java arrays or publish early. */
        const jint pageFirst = first + skipVisible;
        while (written < needed) {
            jint take = needed - written;
            if (take > M3_FRAME_REGION) take = M3_FRAME_REGION;
            const jint pageStart = pageFirst + written;
            (*env)->GetIntArrayRegion(env, offsets, pageStart, take, scratch + written);
            if ((*env)->ExceptionCheck(env)) {
                if (scratch != small) free(scratch);
                return 0;
            }
            (*env)->GetIntArrayRegion(env, flags, pageStart, take, values + written);
            if ((*env)->ExceptionCheck(env)) {
                if (scratch != small) free(scratch);
                return 0;
            }
            for (jint i = 0; i < take; ++i) {
                ordinals[written + i] = pageStart + i;
            }
            written += take;
        }
    } else for (jint start = first; start < end && written < needed;) {
        jint take = end - start;
        if (take > M3_FRAME_REGION) take = M3_FRAME_REGION;
        if (!m3_read_frame(env, offsets, flags, start, take, 1, ids, masks)) {
            if (scratch != small) free(scratch);
            return 0;
        }
        for (jint i = 0; i < take && written < needed; ++i) {
            if (allStates || (fetchStates && matches(masks[i], required, forbidden))) {
                if (left > 0) {
                    --left;
                } else {
                    scratch[written] = ids[i];
                    ordinals[written] = start + i;
                    values[written] = masks[i];
                    ++written;
                }
            }
        }
        start += take;
    }
    if (written != needed) {
        if (scratch != small) free(scratch);
        raise(env, "java/util/ConcurrentModificationException", "frame input changed");
        return 0;
    }
    (*env)->SetIntArrayRegion(env, rows, rowOffset, needed, scratch);
    if (!(*env)->ExceptionCheck(env)) (*env)->SetIntArrayRegion(env, slots, slotOffset, needed, ordinals);
    if (!(*env)->ExceptionCheck(env)) (*env)->SetIntArrayRegion(env, words, wordOffset, needed, values);
    if (scratch != small) free(scratch);
    return (*env)->ExceptionCheck(env) ? 0 : needed;
}
