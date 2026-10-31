/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 * Generated from the source-sealed Synexia M3 long-path JNI recipe.
 *
 * Derived from the first-party M3IndexPathMasks int-row JNI Region pattern,
 * Git blob 086b49293257fd17cc7fbd1ebbb98460aa677e35.
 * No pinned heap array, retained pointer, global reference or Java callback.
 * This general gather backend does not claim to outperform the Java implementation.
 */
#include <jni.h>
#include <stdint.h>
#include <stddef.h>
#include "../../../../../../src/java.base/share/native/libjava/m3_jni_util.h"

#define M3_LONG_PATH_MAX_WINDOW 4096

typedef struct m3_path_shape {
    jsize rows;
    jsize stages;
    jsize capacity;
} m3_path_shape;

static void m3_throw(JNIEnv *env, const char *type, const char *message) {
    m3_jni_throw_by_name(env, type, message);
}

static jlong m3_pack(jint next, jint written) {
    return ((jlong)next << 32) | ((jlong)written & INT64_C(0xffffffff));
}

/* Validate every plane, including ones not consulted by short-circuit matching. */
static int m3_shape(JNIEnv *env, jlongArray rows, jintArray offsets,
                    jobjectArray planes, jintArray needs, jintArray denies,
                    jint first, jint count, jint stride, m3_path_shape *out) {
    if (rows == NULL || offsets == NULL || planes == NULL || needs == NULL || denies == NULL) {
        m3_throw(env, "java/lang/NullPointerException", "null long path argument");
        return 0;
    }
    jsize size = (*env)->GetArrayLength(env, rows);
    jsize offset_count = (*env)->GetArrayLength(env, offsets);
    jsize stages = (*env)->GetArrayLength(env, planes);
    jsize n_needs = (*env)->GetArrayLength(env, needs);
    jsize n_denies = (*env)->GetArrayLength(env, denies);
    if ((*env)->ExceptionCheck(env)) return 0;
    if (size != offset_count || stages == 0 || n_needs != stages || n_denies != stages) {
        m3_throw(env, "java/lang/IllegalArgumentException", "unaligned long path planes");
        return 0;
    }
    jsize capacity = INT32_MAX;
    for (jsize s = 0; s < stages; s++) {
        jobject plane = (*env)->GetObjectArrayElement(env, planes, s);
        if ((*env)->ExceptionCheck(env)) return 0;
        if (plane == NULL) {
            m3_throw(env, "java/lang/NullPointerException", "null long path plane");
            return 0;
        }
        jsize length = (*env)->GetArrayLength(env, (jintArray)plane);
        (*env)->DeleteLocalRef(env, plane);
        if ((*env)->ExceptionCheck(env)) return 0;
        if (length < capacity) capacity = length;
    }
    if (count < 0 || stride == 0 || first < 0 || first > size) {
        m3_throw(env, "java/lang/IndexOutOfBoundsException", "invalid long path shape");
        return 0;
    }
    if (count != 0) {
        jlong last = (jlong)first + ((jlong)count - 1) * (jlong)stride;
        if (first == size || last < 0 || last >= size) {
            m3_throw(env, "java/lang/IndexOutOfBoundsException", "long path leaves row domain");
            return 0;
        }
    }
    out->rows = size;
    out->stages = stages;
    out->capacity = capacity;
    return 1;
}

/* -1 is a pending exception, 0 rejects, 1 admits. */
static int m3_matches(JNIEnv *env, jobjectArray planes, jintArray needs,
                      jintArray denies, jsize stages, jint physical) {
    for (jsize s = 0; s < stages; s++) {
        jint required;
        jint forbidden;
        jint flags;
        (*env)->GetIntArrayRegion(env, needs, s, 1, &required);
        if ((*env)->ExceptionCheck(env)) return -1;
        (*env)->GetIntArrayRegion(env, denies, s, 1, &forbidden);
        if ((*env)->ExceptionCheck(env)) return -1;
        jobject plane = (*env)->GetObjectArrayElement(env, planes, s);
        if ((*env)->ExceptionCheck(env)) return -1;
        if (plane == NULL) {
            m3_throw(env, "java/lang/NullPointerException", "null long path plane");
            return -1;
        }
        jsize length = (*env)->GetArrayLength(env, (jintArray)plane);
        if ((*env)->ExceptionCheck(env)) {
            (*env)->DeleteLocalRef(env, plane);
            return -1;
        }
        if (physical < 0 || physical >= length) {
            (*env)->DeleteLocalRef(env, plane);
            m3_throw(env, "java/lang/IndexOutOfBoundsException", "physical offset outside state plane");
            return -1;
        }
        (*env)->GetIntArrayRegion(env, (jintArray)plane, physical, 1, &flags);
        (*env)->DeleteLocalRef(env, plane);
        if ((*env)->ExceptionCheck(env)) return -1;
        if ((flags & required) != required || (flags & forbidden) != 0) return 0;
    }
    return 1;
}

/* Check logical/physical offsets independently from the state-mask result. */
static int m3_physical(JNIEnv *env, jintArray offsets,
                       jlong logical, jsize capacity, jint *physical) {
    (*env)->GetIntArrayRegion(env, offsets, (jsize)logical, 1, physical);
    if ((*env)->ExceptionCheck(env)) return 0;
    if (*physical < 0 || *physical >= capacity) {
        m3_throw(env, "java/lang/IndexOutOfBoundsException", "physical offset outside state plane");
        return 0;
    }
    return 1;
}

JNIEXPORT jlongArray JNICALL
Java_com_m3_collections_M3LongIndexPathMasksJNI_selectNative(
        JNIEnv *env, jclass owner, jlongArray rows, jintArray offsets,
        jobjectArray planes, jintArray needs, jintArray denies,
        jint first, jint count, jint stride) {
    (void)owner;
    m3_path_shape shape;
    if (!m3_shape(env, rows, offsets, planes, needs, denies,
                  first, count, stride, &shape)) return NULL;
    /* The Java owner checks all offsets before matching or output allocation. */
    jlong logical = (jlong)first;
    for (jint ordinal = 0; ordinal < count; ordinal++, logical += stride) {
        jint physical;
        if (!m3_physical(env, offsets, logical, shape.capacity, &physical)) return NULL;
    }
    jsize matches = 0;
    logical = (jlong)first;
    for (jint ordinal = 0; ordinal < count; ordinal++, logical += stride) {
        jint physical;
        if (!m3_physical(env, offsets, logical, shape.capacity, &physical)) return NULL;
        int keep = m3_matches(env, planes, needs, denies, shape.stages, physical);
        if (keep < 0) return NULL;
        if (keep == 1) matches++;
    }
    jlongArray result = (*env)->NewLongArray(env, matches);
    if (result == NULL) return NULL;
    jsize written = 0;
    logical = (jlong)first;
    for (jint ordinal = 0; ordinal < count; ordinal++, logical += stride) {
        jint physical;
        if (!m3_physical(env, offsets, logical, shape.capacity, &physical)) return NULL;
        int keep = m3_matches(env, planes, needs, denies, shape.stages, physical);
        if (keep < 0) return NULL;
        if (keep == 0) continue;
        if (written == matches) {
            m3_throw(env, "java/util/ConcurrentModificationException", "long path masks changed during native select");
            return NULL;
        }
        jlong value;
        (*env)->GetLongArrayRegion(env, rows, (jsize)logical, 1, &value);
        if ((*env)->ExceptionCheck(env)) return NULL;
        (*env)->SetLongArrayRegion(env, result, written++, 1, &value);
        if ((*env)->ExceptionCheck(env)) return NULL;
    }
    if (written != matches) {
        m3_throw(env, "java/util/ConcurrentModificationException", "long path masks changed during native select");
        return NULL;
    }
    return result;
}

JNIEXPORT jlong JNICALL
Java_com_m3_collections_M3LongIndexPathMasksJNI_scanWindowNative(
        JNIEnv *env, jclass owner, jlongArray rows, jintArray offsets,
        jobjectArray planes, jintArray needs, jintArray denies,
        jint first, jint count, jint stride, jint next,
        jlongArray output, jint output_offset, jint limit) {
    (void)owner;
    if (output == NULL) {
        m3_throw(env, "java/lang/NullPointerException", "null native long output");
        return (jlong)-1;
    }
    m3_path_shape shape;
    if (!m3_shape(env, rows, offsets, planes, needs, denies,
                  first, count, stride, &shape)) return (jlong)-1;
    if (next < 0 || next > count) {
        m3_throw(env, "java/lang/IndexOutOfBoundsException", "invalid path ordinal");
        return (jlong)-1;
    }
    if (limit < 0 || limit > M3_LONG_PATH_MAX_WINDOW) {
        m3_throw(env, "java/lang/IllegalArgumentException", "native window limit exceeds 4096");
        return (jlong)-1;
    }
    jsize output_len = (*env)->GetArrayLength(env, output);
    if ((*env)->ExceptionCheck(env)) return (jlong)-1;
    if (output_offset < 0 || output_offset > output_len || limit > output_len - output_offset) {
        m3_throw(env, "java/lang/IndexOutOfBoundsException", "native output window out of range");
        return (jlong)-1;
    }
    if ((*env)->IsSameObject(env, rows, output) == JNI_TRUE) {
        m3_throw(env, "java/lang/IllegalArgumentException", "long row/output alias");
        return (jlong)-1;
    }
    if ((*env)->ExceptionCheck(env)) return (jlong)-1;
    if (next == count || limit == 0) return m3_pack(next, 0);

    /* Preflight precisely the inspected prefix before any output writes. */
    jint stop = next;
    jint selected_count = 0;
    jlong logical = (jlong)first + (jlong)next * (jlong)stride;
    while (stop < count && selected_count < limit) {
        jint physical;
        if (!m3_physical(env, offsets, logical, shape.capacity, &physical)) return (jlong)-1;
        int keep = m3_matches(env, planes, needs, denies, shape.stages, physical);
        if (keep < 0) return (jlong)-1;
        if (keep == 1) selected_count++;
        stop++;
        logical += stride;
    }
    /* Bounded stack result, written to Java only after the second pass agrees. */
    jlong selected[M3_LONG_PATH_MAX_WINDOW];
    jint written = 0;
    logical = (jlong)first + (jlong)next * (jlong)stride;
    for (jint ordinal = next; ordinal < stop; ordinal++, logical += stride) {
        jint physical;
        if (!m3_physical(env, offsets, logical, shape.capacity, &physical)) return (jlong)-1;
        int keep = m3_matches(env, planes, needs, denies, shape.stages, physical);
        if (keep < 0) return (jlong)-1;
        if (keep == 0) continue;
        if (written == selected_count) {
            m3_throw(env, "java/util/ConcurrentModificationException", "long path masks changed during native window");
            return (jlong)-1;
        }
        (*env)->GetLongArrayRegion(env, rows, (jsize)logical, 1, &selected[written]);
        if ((*env)->ExceptionCheck(env)) return (jlong)-1;
        written++;
    }
    if (written != selected_count) {
        m3_throw(env, "java/util/ConcurrentModificationException", "long path masks changed during native window");
        return (jlong)-1;
    }
    if (written != 0) {
        (*env)->SetLongArrayRegion(env, output, output_offset, written, selected);
        if ((*env)->ExceptionCheck(env)) return (jlong)-1;
    }
    return m3_pack(stop, written);
}
