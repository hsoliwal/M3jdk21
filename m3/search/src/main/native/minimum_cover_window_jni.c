/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0 */
// Relocated from com.synexia m3-fast-search/src/main/native/minimum_cover_window_jni.c (blob 1d0f245570c5) by M3_SEARCH_RELOCATE v2.
#include <jni.h>
#include <stdint.h>
#include <stdlib.h>

/* Independent private Java 21 contract owner. No native address survives this call. */
static void m3_cover_throw(JNIEnv *env, const char *class_name, const char *message) {
    jclass exception_class = (*env)->FindClass(env, class_name);
    if (exception_class != NULL) {
        (*env)->ThrowNew(env, exception_class, message);
        (*env)->DeleteLocalRef(env, exception_class);
    }
}

JNIEXPORT jlong JNICALL
Java_jdk_m3_search_M3MinimumCoverWindow_nativeSpan(
        JNIEnv *env, jclass owner, jcharArray source_array, jcharArray required_array) {
    (void) owner;
    if (source_array == NULL || required_array == NULL) {
        m3_cover_throw(env, "java/lang/NullPointerException", "minimum-cover inputs");
        return (jlong) -1;
    }
    jsize length = (*env)->GetArrayLength(env, source_array);
    jsize required_length = (*env)->GetArrayLength(env, required_array);
    if (length > 1000000 || required_length > 1000000) {
        m3_cover_throw(env, "java/lang/IllegalArgumentException",
                       "minimum-cover input budget exceeded");
        return (jlong) -1;
    }
    if (required_length == 0) return (jlong) 0;
    if (required_length > length) return (jlong) -1;
    /* Singleton cover: first matching UTF-16 unit; no frequency table or
       second pinned/copied JNI array. Read the target through a region. */
    if (required_length == 1) {
        jchar target = 0;
        (*env)->GetCharArrayRegion(env, required_array, 0, 1, &target);
        if ((*env)->ExceptionCheck(env)) return (jlong) -1;
        jchar *haystack = (*env)->GetCharArrayElements(env, source_array, NULL);
        if (haystack == NULL) return (jlong) -1;
        jlong first = (jlong) -1;
        for (jsize index = 0; index < length; index++) {
            if (haystack[index] == target) {
                first = ((jlong) index << 32) | (jlong) (index + 1);
                break;
            }
        }
        (*env)->ReleaseCharArrayElements(env, source_array, haystack, JNI_ABORT);
        return (*env)->ExceptionCheck(env) ? (jlong) -1 : first;
    }
    /* Two-code-unit multiset: track the nearest previous required unit.
       Borrow only the source array; the requirement is read by region. */
    if (required_length == 2) {
        jchar pair[2] = {0, 0};
        (*env)->GetCharArrayRegion(env, required_array, 0, 2, pair);
        if ((*env)->ExceptionCheck(env)) return (jlong) -1;
        jchar *haystack = (*env)->GetCharArrayElements(env, source_array, NULL);
        if (haystack == NULL) return (jlong) -1;
        jsize previous_first = -1;
        jsize previous_second = -1;
        jsize best_start = -1;
        jsize best_width = length + 1;
        for (jsize index = 0; index < length; index++) {
            const jchar unit = haystack[index];
            jsize start = -1;
            if (unit == pair[0]) {
                start = pair[0] == pair[1] ? previous_first : previous_second;
                previous_first = index;
            } else if (unit == pair[1]) {
                start = previous_first;
                previous_second = index;
            }
            if (start >= 0) {
                const jsize width = index - start + 1;
                if (width < best_width) {
                    best_start = start;
                    best_width = width;
                    if (width == 2) break;
                }
            }
        }
        const jlong result = best_start < 0 ? (jlong) -1
                : ((jlong) best_start << 32) | (jlong) ((uint32_t) (best_start + best_width));
        (*env)->ReleaseCharArrayElements(env, source_array, haystack, JNI_ABORT);
        return (*env)->ExceptionCheck(env) ? (jlong) -1 : result;
    }

    jchar *source = NULL;
    jchar *required = NULL;
    jint *deficit = NULL;
    jlong result = (jlong) -1;
    source = (*env)->GetCharArrayElements(env, source_array, NULL);
    if (source == NULL) goto done;
    required = (*env)->GetCharArrayElements(env, required_array, NULL);
    if (required == NULL) goto done;
    /* The required UTF-16 interval is enough; exclude the unused lower prefix. */
    uint32_t lowest = UINT32_MAX;
    uint32_t highest = 0;
    for (jsize index = 0; index < required_length; index++) {
        const uint32_t unit = (uint32_t) required[index];
        if (unit < lowest) lowest = unit;
        if (unit > highest) highest = unit;
    }
    const size_t table_size = (size_t) (highest - lowest) + 1;
    deficit = (jint *) calloc(table_size, sizeof(jint));
    if (deficit == NULL) {
        m3_cover_throw(env, "java/lang/OutOfMemoryError", "minimum-cover frequencies");
        goto done;
    }
    for (jsize index = 0; index < required_length; index++) {
        deficit[(size_t) ((uint32_t) required[index] - lowest)]++;
    }
    jsize missing = required_length;
    jsize left = 0;
    jsize best_left = -1;
    jsize best_right = 0;
    jsize best_size = length + 1;
    for (jsize right = 0; right < length; right++) {
        const uint32_t slot = (uint32_t) source[right] - lowest;
        if ((size_t) slot < table_size) {
            if (deficit[slot] > 0) missing--;
            deficit[slot]--;
        }
        while (missing == 0) {
            const jsize candidate = right - left + 1;
            if (candidate < best_size) {
                best_size = candidate;
                best_left = left;
                best_right = right + 1;
            }
            const uint32_t dropped_slot = (uint32_t) source[left++] - lowest;
            if ((size_t) dropped_slot < table_size) {
                deficit[dropped_slot]++;
                if (deficit[dropped_slot] > 0) missing++;
            }
        }
    }
    if (best_left >= 0) {
        result = ((jlong) best_left << 32) | (jlong) ((uint32_t) best_right);
    }
done:
    free(deficit);
    if (required != NULL) {
        (*env)->ReleaseCharArrayElements(env, required_array, required, JNI_ABORT);
    }
    if (source != NULL) {
        (*env)->ReleaseCharArrayElements(env, source_array, source, JNI_ABORT);
    }
    return (*env)->ExceptionCheck(env) ? (jlong) -1 : result;
}
