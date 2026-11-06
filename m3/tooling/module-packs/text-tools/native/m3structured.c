/* SPDX-License-Identifier: Apache-2.0
 * Scalar predicate over the existing Java owner's immutable admitted CSV ranges.
 * This is not a second CSV parser. All JNI inputs are borrowed only for this call.
 */
#include "jdk_m3_structured_StructuredTextTools.h"
#include <stddef.h>

static void invalid(JNIEnv *env, const char *message) {
    jclass type = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
    if (type != NULL) {
        (void) (*env)->ThrowNew(env, type, message);
        (*env)->DeleteLocalRef(env, type);
    }
}

JNIEXPORT jint JNICALL Java_jdk_m3_structured_StructuredTextTools_nativeDelimitedAbi(
        JNIEnv *env, jclass owner) {
    (void) env; (void) owner;
    return 20261104;
}

JNIEXPORT jint JNICALL Java_jdk_m3_structured_StructuredTextTools_nativeCountDelimitedEquals(
        JNIEnv *env, jclass owner, jstring source, jintArray field_array,
        jintArray row_array, jint column, jstring expected, jint limit) {
    (void) owner;
    if (source == NULL || field_array == NULL || row_array == NULL || expected == NULL
            || column < 0 || limit < 0) {
        invalid(env, "invalid delimited JNI arguments");
        return 0;
    }
    const jsize source_length = (*env)->GetStringLength(env, source);
    const jsize expected_length = (*env)->GetStringLength(env, expected);
    const jsize field_cells = (*env)->GetArrayLength(env, field_array);
    const jsize row_cells = (*env)->GetArrayLength(env, row_array);
    if (field_cells % 3 != 0 || row_cells < 1) {
        invalid(env, "invalid delimited JNI geometry");
        return 0;
    }
    const jint field_count = field_cells / 3;
    const jchar *text = NULL, *value = NULL;
    jint *fields = NULL, *rows = NULL;
    const char *error = NULL;
    jint result = 0;
    text = (*env)->GetStringChars(env, source, NULL);
    if (text == NULL) goto cleanup;
    value = (*env)->GetStringChars(env, expected, NULL);
    if (value == NULL) goto cleanup;
    if (field_cells != 0) {
        fields = (*env)->GetIntArrayElements(env, field_array, NULL);
        if (fields == NULL) goto cleanup;
    }
    rows = (*env)->GetIntArrayElements(env, row_array, NULL);
    if (rows == NULL) goto cleanup;
    if (rows[0] != 0 || rows[row_cells - 1] != field_count) {
        error = "invalid delimited JNI row bounds";
        goto cleanup;
    }
    for (jint row = 0; row < row_cells - 1; row++) {
        if (rows[row] < 0 || rows[row + 1] <= rows[row] || rows[row + 1] > field_count
                || column >= rows[row + 1] - rows[row]) {
            error = "invalid delimited JNI row or column";
            goto cleanup;
        }
    }
    /* Validate even unselected fields and late rows, including at limit zero.
     * Quote pairs/decoded lengths guard the raw ABI, not cross-owner text identity. */
    jint previous_end = 0;
    for (jint base = 0; base < field_cells; base += 3) {
        const jint start = fields[base], end = fields[base + 1];
        if (start < previous_end || start < 0 || end < start || end > source_length
                || fields[base + 2] < 0 || fields[base + 2] > end - start) {
            error = "invalid delimited JNI field bounds";
            goto cleanup;
        }
        jint decoded = 0;
        for (jint at = start; at < end; at++) {
            if (text[at] == (jchar) '"') {
                if (at + 1 == end || text[at + 1] != (jchar) '"') {
                    error = "invalid delimited JNI quote pair";
                    goto cleanup;
                }
                at++;
            }
            decoded++;
        }
        if (decoded != fields[base + 2]) {
            error = "invalid delimited JNI decoded length";
            goto cleanup;
        }
        previous_end = end;
    }
    for (jint row = 0; row < row_cells - 1 && result < limit; row++) {
        const jint base = (rows[row] + column) * 3;
        if (fields[base + 2] != expected_length) continue;
        jint unit = 0;
        int equal = 1;
        for (jint at = fields[base]; at < fields[base + 1]; at++) {
            const jchar c = text[at];
            if (value[unit++] != c) { equal = 0; break; }
            if (c == (jchar) '"') at++;
        }
        if (equal) result++;
    }
cleanup:
    if (rows != NULL) (*env)->ReleaseIntArrayElements(env, row_array, rows, JNI_ABORT);
    if (fields != NULL) (*env)->ReleaseIntArrayElements(env, field_array, fields, JNI_ABORT);
    if (value != NULL) (*env)->ReleaseStringChars(env, expected, value);
    if (text != NULL) (*env)->ReleaseStringChars(env, source, text);
    if (error != NULL && !(*env)->ExceptionCheck(env)) invalid(env, error);
    return result;
}
