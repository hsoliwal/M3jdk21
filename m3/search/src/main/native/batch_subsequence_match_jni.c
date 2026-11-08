/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0 */
// Relocated from com.synexia m3-fast-search/src/main/native/batch_subsequence_match_jni.c (blob 8463caba2101) by M3_SEARCH_FORWARD v1.
#include <jni.h>
#include <stdint.h>
#include <stdlib.h>

/* Local-only JNI proof, not a replacement for the existing native search ABI. */
enum { M3_SOURCE_LIMIT = 1000000, M3_PATTERN_LIMIT = 65536,
       M3_UNIT_LIMIT = 1000000, M3_ALPHABET = 65536 };

static void m3_throw(JNIEnv *env, const char *type, const char *message) {
    jclass klass = (*env)->FindClass(env, type);
    if (klass != NULL) {
        (*env)->ThrowNew(env, klass, message);
        (*env)->DeleteLocalRef(env, klass);
    }
}

JNIEXPORT jlong JNICALL
Java_jdk_m3_search_BatchSubsequenceMatch_countNative(
        JNIEnv *env, jclass type, jcharArray sourceArray,
        jcharArray packedArray, jintArray offsetArray) {
    (void) type;
    jint *offsets = NULL;
    jchar *source = NULL;
    jchar *packed = NULL;
    jint *head = NULL;
    jint *next = NULL;
    jint *cursor = NULL;
    jlong matches = -1;
    if (sourceArray == NULL || packedArray == NULL || offsetArray == NULL) {
        m3_throw(env, "java/lang/NullPointerException", "batch subsequence input");
        return (jlong)-1;
    }
    jsize sourceLength = (*env)->GetArrayLength(env, sourceArray);
    jsize packedLength = (*env)->GetArrayLength(env, packedArray);
    jsize offsetCount = (*env)->GetArrayLength(env, offsetArray);
    if (sourceLength > M3_SOURCE_LIMIT || packedLength > M3_UNIT_LIMIT ||
            offsetCount < 1 || offsetCount - 1 > M3_PATTERN_LIMIT) {
        m3_throw(env, "java/lang/IllegalArgumentException", "batch subsequence budget");
        return (jlong)-1;
    }
    jsize patternCount = offsetCount - 1;
    offsets = (*env)->GetIntArrayElements(env, offsetArray, NULL);
    if (offsets == NULL) goto done;
    if (offsets[0] != 0 || offsets[patternCount] != packedLength) {
        m3_throw(env, "java/lang/IllegalArgumentException", "batch pattern bounds");
        goto done;
    }
    for (jsize i = 1; i <= patternCount; i++) {
        if (offsets[i] < offsets[i-1] || offsets[i] > packedLength) {
            m3_throw(env, "java/lang/IllegalArgumentException", "batch pattern offsets");
            goto done;
        }
    }
    head = (jint *)malloc((size_t)M3_ALPHABET * sizeof(*head));
    next = (jint *)malloc((size_t)(patternCount == 0 ? 1 : patternCount) * sizeof(*next));
    cursor = (jint *)calloc((size_t)(patternCount == 0 ? 1 : patternCount), sizeof(*cursor));
    if (head == NULL || next == NULL || cursor == NULL) {
        m3_throw(env, "java/lang/OutOfMemoryError", "batch subsequence workspace");
        goto done;
    }
    for (size_t i = 0; i < (size_t)M3_ALPHABET; i++) head[i] = -1;
    source = (*env)->GetCharArrayElements(env, sourceArray, NULL);
    if (source == NULL) goto done;
    packed = (*env)->GetCharArrayElements(env, packedArray, NULL);
    if (packed == NULL) goto done;
    matches = 0;
    jint remaining = 0; /* feasible nonempty entries; cleanup remains mandatory */
    for (jsize i = 0; i < patternCount; i++) {
        if (offsets[i] == offsets[i+1]) {
            matches++;
        } else if (offsets[i+1] - offsets[i] <= sourceLength) {
            remaining++;
            unsigned int c = (unsigned int)packed[offsets[i]];
            next[i] = head[c];
            head[c] = i;
        }
    }
    if (remaining == 0) goto done;
    for (jsize i = 0; i < sourceLength; i++) {
        unsigned int c = (unsigned int)source[i];
        jint pending = head[c];
        head[c] = -1;
        while (pending >= 0) {
            jint id = pending;
            pending = next[id];
            jint position = ++cursor[id];
            jint start = offsets[id];
            if (position == offsets[id+1] - start) {
                matches++;
                if (--remaining == 0) goto done;
            } else {
                unsigned int target = (unsigned int)packed[start + position];
                next[id] = head[target];
                head[target] = id;
            }
        }
    }
done:
    if (packed != NULL) (*env)->ReleaseCharArrayElements(env, packedArray, packed, JNI_ABORT);
    if (source != NULL) (*env)->ReleaseCharArrayElements(env, sourceArray, source, JNI_ABORT);
    if (offsets != NULL) (*env)->ReleaseIntArrayElements(env, offsetArray, offsets, JNI_ABORT);
    free(head);
    free(next);
    free(cursor);
    return (*env)->ExceptionCheck(env) ? (jlong)-1 : matches;
}
