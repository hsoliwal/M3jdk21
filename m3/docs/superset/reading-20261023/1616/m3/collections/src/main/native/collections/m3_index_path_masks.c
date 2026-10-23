/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 * Modified 2026-10-21: bounded chunked gather/filter/compact in the existing JNI owner.
 * Region APIs only; no retained heap address, callback, or per-row heap allocation.
 * Inputs must remain stable for the call. This is not a concurrent collection protocol.
 */
#include <jni.h>
#include <stddef.h>
#include "../../../../../native/include/m3_jni_core.h"

/* Scratch capacity, not a limit on logical paths, state stages, or recipe atoms. */
#define M3_CHUNK 256

static void m3_throw(JNIEnv *env, const char *type, const char *message) {
    m3_jni_throw_new(env, type, message);
}

/* Gather a valid signed-stride segment into call-local primitive scratch. */
static int m3_gather(JNIEnv *env, jintArray source, jlong first, jint stride,
                     jsize count, jint *output) {
    if (stride == 1 || stride == -1) {
        jlong low = stride == 1 ? first : first - (jlong)count + 1;
        (*env)->GetIntArrayRegion(env, source, (jsize)low, count, output);
        if ((*env)->ExceptionCheck(env)) return 0;
        if (stride == -1) {
            for (jsize i = 0; i < count / 2; ++i) {
                jint swap = output[i];
                output[i] = output[count - 1 - i];
                output[count - 1 - i] = swap;
            }
        }
    } else {
        for (jsize i = 0; i < count; ++i) {
            jlong at = first + (jlong)i * stride;
            (*env)->GetIntArrayRegion(env, source, (jsize)at, 1, output + i);
            if ((*env)->ExceptionCheck(env)) return 0;
        }
    }
    return 1;
}

/* Preserve each row's left-to-right stage short circuit; filter primitive metadata only. */
static int m3_filter(JNIEnv *env, jobjectArray planes, jintArray needs, jintArray denies,
                     jsize stages, const jint *physical, jsize count, unsigned char *alive) {
    jint states[M3_CHUNK];
    int remaining = count;
    for (jsize i = 0; i < count; ++i) alive[i] = 1;
    for (jsize stage = 0; stage < stages && remaining != 0; ++stage) {
        jint required = 0, forbidden = 0;
        (*env)->GetIntArrayRegion(env, needs, stage, 1, &required);
        if ((*env)->ExceptionCheck(env)) return -1;
        (*env)->GetIntArrayRegion(env, denies, stage, 1, &forbidden);
        if ((*env)->ExceptionCheck(env)) return -1;
        jobject object = (*env)->GetObjectArrayElement(env, planes, stage);
        if ((*env)->ExceptionCheck(env)) return -1;
        if (object == NULL) {
            m3_throw(env, "java/lang/NullPointerException", "null state plane");
            return -1;
        }
        jintArray plane = (jintArray)object;
        for (jsize begin = 0; begin < count;) {
            if (!alive[begin]) { ++begin; continue; }
            jint direction = 1;
            jsize end = begin + 1;
            if (end < count && alive[end]
                    && (jlong)physical[end] == (jlong)physical[begin] - 1) direction = -1;
            while (end < count && alive[end]
                    && (jlong)physical[end] == (jlong)physical[begin]
                            + (jlong)(end - begin) * direction) ++end;
            if (!m3_gather(env, plane, physical[begin], direction, end - begin, states)) {
                (*env)->DeleteLocalRef(env, object);
                return -1;
            }
            for (jsize i = begin; i < end; ++i) {
                jint state = states[i - begin];
                if ((state & required) != required || (state & forbidden) != 0) {
                    alive[i] = 0;
                    --remaining;
                }
            }
            begin = end;
        }
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
        if (slot < 0 || slot >= length) {
            (*env)->DeleteLocalRef(env, plane_object);
            m3_throw(env, "java/lang/IndexOutOfBoundsException", "physical offset outside state plane");
            return -1;
        }
        (*env)->GetIntArrayRegion(env, plane, slot, 1, &state);
        (*env)->DeleteLocalRef(env, plane_object);
        if ((*env)->ExceptionCheck(env)) return -1;
        if (!m3_jni_matches_u32(state, required, forbidden)) return 0;
=======
        (*env)->DeleteLocalRef(env, object);
>>>>>>> 683d8738ecec081cc554b1c2652f75f32f63cbb3
    }
    return remaining;
}

/* ART-friendly bounded metadata cache: no heap allocation or pinned Java pointers.
 * Stage count above the bound follows the unchanged original JNI reference kernel.
 * All local references are released on every normal/exceptional exit.
 */
#define M3_CACHED_STAGE_LIMIT 8

typedef struct {
    jintArray plane[M3_CACHED_STAGE_LIMIT];
    jsize length[M3_CACHED_STAGE_LIMIT];
    jint needs[M3_CACHED_STAGE_LIMIT];
    jint denies[M3_CACHED_STAGE_LIMIT];
    jsize count;
} M3StageCache;

static void m3_release_stages(JNIEnv *env, M3StageCache *cache) {
    for (jsize i = 0; i < cache->count; ++i) {
        if (cache->plane[i] != NULL) {
            (*env)->DeleteLocalRef(env, cache->plane[i]);
            cache->plane[i] = NULL;
        }
    }
}

static int m3_prepare_stages(JNIEnv *env, jobjectArray planes,
        jintArray required, jintArray forbidden, jsize count, M3StageCache *cache) {
    cache->count = count;
    for (jsize i = 0; i < count; ++i) {
        (*env)->GetIntArrayRegion(env, required, i, 1, &cache->needs[i]);
        if ((*env)->ExceptionCheck(env)) goto failure;
        (*env)->GetIntArrayRegion(env, forbidden, i, 1, &cache->denies[i]);
        if ((*env)->ExceptionCheck(env)) goto failure;
        jobject reference = (*env)->GetObjectArrayElement(env, planes, i);
        if ((*env)->ExceptionCheck(env)) goto failure;
        if (reference == NULL) {
            m3_throw(env, "java/lang/NullPointerException", "null state plane");
            goto failure;
        }
        cache->plane[i] = (jintArray) reference;
        cache->length[i] = (*env)->GetArrayLength(env, cache->plane[i]);
        if ((*env)->ExceptionCheck(env)) goto failure;
    }
    return 1;
failure:
    m3_release_stages(env, cache);
    return 0;
}

/* -1 is an exception; 0 and 1 are rejected and admitted respectively. */
static int m3_matches_cached(JNIEnv *env, const M3StageCache *cache, jint slot) {
    for (jsize i = 0; i < cache->count; ++i) {
        if (slot < 0 || slot >= cache->length[i]) {
            m3_throw(env, "java/lang/IndexOutOfBoundsException",
                    "physical offset outside state plane");
            return -1;
        }
        jint state;
        (*env)->GetIntArrayRegion(env, cache->plane[i], slot, 1, &state);
        if ((*env)->ExceptionCheck(env)) return -1;
        if ((state & cache->needs[i]) != cache->needs[i]
                || (state & cache->denies[i]) != 0) return 0;
    }
    return 1;
}

static jintArray m3_select_cached(JNIEnv *env, jintArray rows, jintArray offsets,
        jobjectArray planes, jintArray required, jintArray forbidden,
        jint first, jint count, jint stride, jsize stages) {
    M3StageCache cache = {0};
    if (!m3_prepare_stages(env, planes, required, forbidden, stages, &cache)) return NULL;
    jintArray result = NULL;
    jsize matching = 0;
    for (jint pass = 0; pass < 2; ++pass) {
        if (pass == 1) {
            result = (*env)->NewIntArray(env, matching);
            if (result == NULL) goto failure;
        }
        jsize written = 0;
        jlong at = first;
        for (jint ordinal = 0; ordinal < count; ++ordinal, at += stride) {
            jint physical;
            (*env)->GetIntArrayRegion(env, offsets, (jsize)at, 1, &physical);
            if ((*env)->ExceptionCheck(env)) goto failure;
            int keep = m3_matches_cached(env, &cache, physical);
            if (keep < 0) goto failure;
            if (keep == 0) continue;
            if (pass == 0) {
                ++matching;
            } else {
                if (written >= matching) {
                    m3_throw(env, "java/util/ConcurrentModificationException",
                            "mask stages changed during JNI selection");
                    goto failure;
                }
                jint row;
                (*env)->GetIntArrayRegion(env, rows, (jsize)at, 1, &row);
                if ((*env)->ExceptionCheck(env)) goto failure;
                (*env)->SetIntArrayRegion(env, result, written++, 1, &row);
                if ((*env)->ExceptionCheck(env)) goto failure;
            }
        }
        if (pass == 1) {
            if (written != matching) {
                m3_throw(env, "java/util/ConcurrentModificationException",
                        "mask stages changed during JNI selection");
                goto failure;
            }
            m3_release_stages(env, &cache);
            return result;
        }
    }
failure:
    m3_release_stages(env, &cache);
    return NULL;
}

JNIEXPORT jintArray JNICALL Java_com_m3_collections_M3IndexPathMasks_selectNative(
        JNIEnv *env, jclass owner, jintArray rows, jintArray offsets,
        jobjectArray planes, jintArray required, jintArray forbidden,
        jint first, jint count, jint stride) {
    (void)owner;
    if (rows == NULL || offsets == NULL || planes == NULL || required == NULL || forbidden == NULL) {
        m3_throw(env, "java/lang/NullPointerException", "null native mask path argument");
        return NULL;
    }
    jsize size = (*env)->GetArrayLength(env, rows);
    if ((*env)->ExceptionCheck(env)) return NULL;
    jsize position_count = (*env)->GetArrayLength(env, offsets);
    if ((*env)->ExceptionCheck(env)) return NULL;
    jsize stages = (*env)->GetArrayLength(env, planes);
    if ((*env)->ExceptionCheck(env)) return NULL;
    jsize n_required = (*env)->GetArrayLength(env, required);
    if ((*env)->ExceptionCheck(env)) return NULL;
    jsize n_forbidden = (*env)->GetArrayLength(env, forbidden);
    if ((*env)->ExceptionCheck(env)) return NULL;
    if (size != position_count || stages == 0 || stages != n_required || stages != n_forbidden) {
        m3_throw(env, "java/lang/IllegalArgumentException", "unaligned native mask planes");
        return NULL;
    }
    /* Defensive native admission also checks planes an empty/early-reject path never reads. */
    jsize capacity = 0;
    for (jsize stage = 0; stage < stages; ++stage) {
        jobject plane = (*env)->GetObjectArrayElement(env, planes, stage);
        if ((*env)->ExceptionCheck(env)) return NULL;
        if (plane == NULL) {
            m3_throw(env, "java/lang/NullPointerException", "null state plane");
            return NULL;
        }
        jsize length = (*env)->GetArrayLength(env, plane);
        (*env)->DeleteLocalRef(env, plane);
        if ((*env)->ExceptionCheck(env)) return NULL;
        if (stage == 0 || length < capacity) capacity = length;
    }
    if (count < 0 || stride == 0 || first < 0 || first > size) {
        m3_throw(env, "java/lang/IndexOutOfBoundsException", "invalid native mask path");
        return NULL;
    }
    if (count > 0) {
        jlong last = (jlong)first + ((jlong)count - 1) * (jlong)stride;
        if (first == size || last < 0 || last >= size) {
            m3_throw(env, "java/lang/IndexOutOfBoundsException", "native path out of bounds");
            return NULL;
        }
    }
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
    if (stages <= M3_CACHED_STAGE_LIMIT && count >= 8) {
        return m3_select_cached(env, rows, offsets, planes, required, forbidden,
                first, count, stride, stages);
    }
=======
    jint physical[M3_CHUNK], row_buffer[M3_CHUNK], selected[M3_CHUNK];
    unsigned char alive[M3_CHUNK];
>>>>>>> 683d8738ecec081cc554b1c2652f75f32f63cbb3
    jsize matching = 0;
    jintArray result = NULL;
    for (int pass = 0; pass < 2; ++pass) {
        if (pass == 1) {
            result = (*env)->NewIntArray(env, matching);
            if (result == NULL || matching == 0) return result;
        }
        jsize written = 0;
        for (jlong ordinal = 0; ordinal < count;) {
            jsize chunk = (jsize)(((jlong)count - ordinal) < M3_CHUNK
                    ? (jlong)count - ordinal : M3_CHUNK);
            jlong at = (jlong)first + ordinal * stride;
            if (!m3_gather(env, offsets, at, stride, chunk, physical)) return NULL;
            for (jsize i = 0; i < chunk; ++i) {
                if (physical[i] < 0 || physical[i] >= capacity) {
                    m3_throw(env, "java/lang/IndexOutOfBoundsException", "physical offset outside state plane");
                    return NULL;
                }
            }
            int kept = m3_filter(env, planes, required, forbidden, stages, physical, chunk, alive);
            if (kept < 0) return NULL;
            if (pass == 0) {
                matching += kept;
            } else if (kept != 0) {
                if (kept > matching - written) {
                    m3_throw(env, "java/util/ConcurrentModificationException", "mask stages changed during JNI selection");
                    return NULL;
                }
                if (!m3_gather(env, rows, at, stride, chunk, row_buffer)) return NULL;
                jsize output = 0;
                for (jsize i = 0; i < chunk; ++i) if (alive[i]) selected[output++] = row_buffer[i];
                (*env)->SetIntArrayRegion(env, result, written, output, selected);
                if ((*env)->ExceptionCheck(env)) return NULL;
                written += output;
            }
            ordinal += chunk;
        }
        if (pass == 1 && written != matching) {
            m3_throw(env, "java/util/ConcurrentModificationException", "mask stages changed during JNI selection");
            return NULL;
        }
    }
    return result;
}
