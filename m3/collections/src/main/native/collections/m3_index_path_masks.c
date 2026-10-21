/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 * Optional JNI parity backend for precomputed integer mask stages.
 * Only primitive Region APIs; no GetPrimitiveArrayCritical, retained Java pointers,
 * Java callbacks, persistent references, or per-row heap allocations.
 * This general gather kernel is not claimed faster than the Java reference path.
 */
#include <jni.h>
#include <stddef.h>

static void m3_throw(JNIEnv *env, const char *type, const char *message) {
    jclass exception = (*env)->FindClass(env, type);
    if (exception != NULL) {
        (void)(*env)->ThrowNew(env, exception, message);
        (*env)->DeleteLocalRef(env, exception);
    }
}

/* -1 = pending JNI exception, 0 = rejected, 1 = accepted. */
static int m3_matches(JNIEnv *env, jobjectArray planes,
                      jintArray needs, jintArray denies,
                      jsize stages, jint slot) {
    for (jsize stage = 0; stage < stages; ++stage) {
        jint required = 0;
        jint forbidden = 0;
        jint state = 0;
        (*env)->GetIntArrayRegion(env, needs, stage, 1, &required);
        if ((*env)->ExceptionCheck(env)) return -1;
        (*env)->GetIntArrayRegion(env, denies, stage, 1, &forbidden);
        if ((*env)->ExceptionCheck(env)) return -1;
        jobject plane_object = (*env)->GetObjectArrayElement(env, planes, stage);
        if ((*env)->ExceptionCheck(env)) return -1;
        if (plane_object == NULL) {
            m3_throw(env, "java/lang/NullPointerException", "null state plane");
            return -1;
        }
        jintArray plane = (jintArray)plane_object;
        jsize length = (*env)->GetArrayLength(env, plane);
        if ((*env)->ExceptionCheck(env)) {
            (*env)->DeleteLocalRef(env, plane_object);
            return -1;
        }
        if (slot < 0 || slot >= length) {
            (*env)->DeleteLocalRef(env, plane_object);
            m3_throw(env, "java/lang/IndexOutOfBoundsException", "physical offset outside state plane");
            return -1;
        }
        (*env)->GetIntArrayRegion(env, plane, slot, 1, &state);
        (*env)->DeleteLocalRef(env, plane_object);
        if ((*env)->ExceptionCheck(env)) return -1;
        if ((state & required) != required || (state & forbidden) != 0) return 0;
    }
    return 1;
}

/* ART-friendly bounded metadata cache: no heap allocation or pinned Java pointers.
 * Stage count above the bound follows the unchanged original JNI reference kernel.
 * All local references are released on every normal/exceptional exit.
 */
#define M3_CACHED_STAGE_LIMIT 8
/* At most 8 x 256 x sizeof(jint) = 8192 bytes of call-local state data.
 * The row threshold amortizes an eager plane snapshot on constrained devices.
 */
#define M3_PLANE_SNAPSHOT_CAPACITY 256
#define M3_PLANE_SNAPSHOT_MIN_ROWS 512

typedef struct {
    jintArray plane[M3_CACHED_STAGE_LIMIT];
    jsize length[M3_CACHED_STAGE_LIMIT];
    jint needs[M3_CACHED_STAGE_LIMIT];
    jint denies[M3_CACHED_STAGE_LIMIT];
    jsize count;
    jboolean snapshot_ready;
    jint snapshot_values[M3_CACHED_STAGE_LIMIT][M3_PLANE_SNAPSHOT_CAPACITY];
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

/* Snapshot only small, complete primitive state planes when repeated indexed
 * lookups are likely to amortize the initial bulk JNI region reads. Every
 * physical offset is still checked against its original per-stage capacity.
 * No Java array address is retained; mutable inputs must remain stable for the
 * duration of the call, as required by the existing M3IndexPathMasks contract.
 */
static int m3_prepare_small_plane_snapshot(JNIEnv *env, M3StageCache *cache,
        jint logical_count) {
    if (logical_count < M3_PLANE_SNAPSHOT_MIN_ROWS) return 1;
    for (jsize stage = 0; stage < cache->count; ++stage) {
        if (cache->length[stage] > M3_PLANE_SNAPSHOT_CAPACITY) return 1;
    }
    for (jsize stage = 0; stage < cache->count; ++stage) {
        jsize length = cache->length[stage];
        if (length == 0) continue;
        (*env)->GetIntArrayRegion(env, cache->plane[stage], 0, length,
                cache->snapshot_values[stage]);
        if ((*env)->ExceptionCheck(env)) return 0;
    }
    cache->snapshot_ready = JNI_TRUE;
    return 1;
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
        if (cache->snapshot_ready) {
            state = cache->snapshot_values[i][slot];
        } else {
            (*env)->GetIntArrayRegion(env, cache->plane[i], slot, 1, &state);
            if ((*env)->ExceptionCheck(env)) return -1;
        }
        if ((state & cache->needs[i]) != cache->needs[i]
                || (state & cache->denies[i]) != 0) return 0;
    }
    return 1;
}


/* Bulk contiguous physical-index path execution: one native entry for the whole
 * selection. Keep per-state reads checked (physical offsets may be arbitrary),
 * but bring logical offsets and accepted row IDs across JNI in bounded regions.
 * A source array is borrowed only during this operation; no GC-unsafe pointer
 * is retained, and the output remains a detached, exact-sized primitive array.
 */
enum { M3_PATH_BULK = 256 };

static jintArray m3_select_cached_contiguous(JNIEnv *env, jintArray rows,
        jintArray offsets, const M3StageCache *cache, jint first, jint count) {
    jint slots[M3_PATH_BULK];
    jint row_ids[M3_PATH_BULK];
    jint selected_indices[M3_PATH_BULK];
    jint selected_values[M3_PATH_BULK];
    jsize matching = 0;
    jintArray result = NULL;

    for (jint pass = 0; pass < 2; ++pass) {
        if (pass == 1) {
            result = (*env)->NewIntArray(env, matching);
            if (result == NULL) return NULL;
        }
        jsize written = 0;
        for (jint position = 0; position < count;) {
            jint remaining = count - position;
            jsize chunk = remaining < M3_PATH_BULK ? remaining : M3_PATH_BULK;
            (*env)->GetIntArrayRegion(env, offsets, first + position, chunk, slots);
            if ((*env)->ExceptionCheck(env)) return NULL;

            jsize selected = 0;
            for (jsize index = 0; index < chunk; ++index) {
                int keep = m3_matches_cached(env, cache, slots[index]);
                if (keep < 0) return NULL;
                if (keep == 0) continue;
                if (pass == 0) {
                    matching++;
                } else {
                    if (selected >= matching - written) {
                        m3_throw(env, "java/util/ConcurrentModificationException",
                                "mask stages changed during JNI selection");
                        return NULL;
                    }
                    selected_indices[selected++] = index;
                }
            }
            if (pass == 1 && selected > 0) {
                (*env)->GetIntArrayRegion(env, rows, first + position, chunk, row_ids);
                if ((*env)->ExceptionCheck(env)) return NULL;
                for (jsize index = 0; index < selected; ++index) {
                    selected_values[index] = row_ids[selected_indices[index]];
                }
                (*env)->SetIntArrayRegion(env, result, written, selected, selected_values);
                if ((*env)->ExceptionCheck(env)) return NULL;
                written += selected;
            }
            position += chunk;
        }
        if (pass == 1) {
            if (written != matching) {
                m3_throw(env, "java/util/ConcurrentModificationException",
                        "mask stages changed during JNI selection");
                return NULL;
            }
            return result;
        }
    }
    return NULL;
}

static jintArray m3_select_cached(JNIEnv *env, jintArray rows, jintArray offsets,
        jobjectArray planes, jintArray required, jintArray forbidden,
        jint first, jint count, jint stride, jsize stages) {
    M3StageCache cache = {0};
    if (!m3_prepare_stages(env, planes, required, forbidden, stages, &cache)) return NULL;
    if (!m3_prepare_small_plane_snapshot(env, &cache, count)) {
        m3_release_stages(env, &cache);
        return NULL;
    }
    if (stride == 1) {
        jintArray output = m3_select_cached_contiguous(
                env, rows, offsets, &cache, first, count);
        m3_release_stages(env, &cache);
        return output;
    }
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
    if (stages <= M3_CACHED_STAGE_LIMIT && count >= 8) {
        return m3_select_cached(env, rows, offsets, planes, required, forbidden,
                first, count, stride, stages);
    }
    jsize matching = 0;
    for (jint pass = 0; pass < 2; ++pass) {
        /* Output allocation is deferred until after exact counting. */
        jintArray result = NULL;
        if (pass == 1) {
            result = (*env)->NewIntArray(env, matching);
            if (result == NULL) return NULL;
        }
        jsize written = 0;
        jlong at = (jlong)first;
        for (jint ordinal = 0; ordinal < count; ++ordinal, at += stride) {
            jint physical = -1;
            (*env)->GetIntArrayRegion(env, offsets, (jsize)at, 1, &physical);
            if ((*env)->ExceptionCheck(env)) return NULL;
            int keep = m3_matches(env, planes, required, forbidden, stages, physical);
            if (keep < 0) return NULL;
            if (keep == 0) continue;
            if (pass == 0) {
                ++matching;
            } else {
                if (written >= matching) {
                    m3_throw(env, "java/util/ConcurrentModificationException", "mask stages changed during JNI selection");
                    return NULL;
                }
                jint row = 0;
                (*env)->GetIntArrayRegion(env, rows, (jsize)at, 1, &row);
                if ((*env)->ExceptionCheck(env)) return NULL;
                (*env)->SetIntArrayRegion(env, result, written++, 1, &row);
                if ((*env)->ExceptionCheck(env)) return NULL;
            }
        }
        if (pass == 1) {
            if (written != matching) {
                m3_throw(env, "java/util/ConcurrentModificationException", "mask stages changed during JNI selection");
                return NULL;
            }
            return result;
        }
    }
    return NULL;
}
