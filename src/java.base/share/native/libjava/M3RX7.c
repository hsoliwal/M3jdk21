/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors.
 * GPLv2 with the Classpath exception (OpenJDK java.base native destination).
 * M3JDK21 DFA token transition precompute and composition: primitive arrays only.
 */
#include <jni.h>
#include <stdint.h>
#include <stddef.h>
#include <stdlib.h>
#include "jdk_internal_mindex_M3RX7.h"
#include "jdk_internal_mindex_M3RX7Transitions.h"

#define M3_ASCII 128
#define M3_MAX_STATES 4096
#define M3_MAX_PACKED (1 << 20)
#define M3_MAX_TOKEN 4096
#define M3_MAX_CELLS (1 << 20)
#define M3_MAX_WORK (32 << 20)

static void invalid(JNIEnv *env, const char *detail) {
    jclass clazz = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
    if (clazz != NULL) {
        (*env)->ThrowNew(env, clazz, detail);
        (*env)->DeleteLocalRef(env, clazz);
    }
}

static void allocation_failed(JNIEnv *env) {
    jclass clazz = (*env)->FindClass(env, "java/lang/OutOfMemoryError");
    if (clazz != NULL) {
        (*env)->ThrowNew(env, clazz, "bounded DFA transfer workspace");
        (*env)->DeleteLocalRef(env, clazz);
    }
}

static int valid_transfer(const jshort *rows, jsize states) {
    for (jsize state = 0; state < states; state++) {
        if (rows[state] < -1 || rows[state] >= states) return 0;
    }
    return 1;
}

/* Inputs may coincide for squaring. Output is always separate storage. */
static void compose_transfer(const jshort *left, const jshort *right,
        jshort *out, jsize states) {
    for (jsize state = 0; state < states; state++) {
        jint intermediate = left[state];
        out[state] = intermediate < 0 ? (jshort) -1 : right[intermediate];
    }
}

JNIEXPORT void JNICALL Java_jdk_internal_mindex_M3RX7_buildPacked0(
        JNIEnv *env, jclass ignored, jshortArray dfaArray, jbyteArray packedArray,
        jintArray startArray, jintArray lengthArray, jshortArray outArray) {
    jshort *dfa = NULL;
    jbyte *packed = NULL;
    jint *starts = NULL, *lengths = NULL;
    jshort *out = NULL;
    jsize states, count, size;
    int valid = 1;
    int written = 0;
    (void) ignored;

    if (dfaArray == NULL || packedArray == NULL || startArray == NULL
            || lengthArray == NULL || outArray == NULL) {
        invalid(env, "missing DFA token arrays");
        return;
    }
    if ((*env)->IsSameObject(env, dfaArray, outArray)) {
        invalid(env, "DFA token output aliases input");
        return;
    }
    size = (*env)->GetArrayLength(env, dfaArray);
    states = size / M3_ASCII;
    count = (*env)->GetArrayLength(env, startArray);
    jsize packedSize = (*env)->GetArrayLength(env, packedArray);
    if (size <= 0 || size % M3_ASCII != 0 || states > M3_MAX_STATES
            || packedSize > M3_MAX_PACKED || count != (*env)->GetArrayLength(env, lengthArray)
            || (int64_t) states * count > M3_MAX_CELLS
            || (*env)->GetArrayLength(env, outArray) != (int64_t) states * count) {
        invalid(env, "invalid DFA token array dimensions");
        return;
    }
    dfa = (*env)->GetShortArrayElements(env, dfaArray, NULL);
    if (dfa == NULL) goto done;
    packed = (*env)->GetByteArrayElements(env, packedArray, NULL);
    if (packed == NULL) goto done;
    starts = (*env)->GetIntArrayElements(env, startArray, NULL);
    if (starts == NULL) goto done;
    lengths = (*env)->GetIntArrayElements(env, lengthArray, NULL);
    if (lengths == NULL) goto done;

    int64_t totalBytes = 0;
    for (jsize i = 0; i < size; i++) {
        if (dfa[i] < -1 || dfa[i] >= states) { valid = 0; break; }
    }
    for (jsize i = 0; valid && i < count; i++) {
        if (starts[i] < 0 || lengths[i] < 1 || lengths[i] > M3_MAX_TOKEN
                || starts[i] > packedSize || lengths[i] > packedSize - starts[i]) {
            valid = 0;
            break;
        }
        totalBytes += lengths[i];
        if (totalBytes * states > M3_MAX_WORK) { valid = 0; break; }
        for (jint p = starts[i], end = p + lengths[i]; p < end; p++) {
            if (packed[p] < 0) { valid = 0; break; }
        }
    }
    if (!valid) {
        invalid(env, "invalid DFA or token bytes");
        goto done;
    }
    out = (*env)->GetShortArrayElements(env, outArray, NULL);
    if (out == NULL) goto done;

    for (jsize state = 0; state < states; state++) {
        for (jsize token = 0; token < count; token++) {
            jint cursor = state;
            jint end = starts[token] + lengths[token];
            for (jint at = starts[token]; at < end && cursor >= 0; at++) {
                cursor = dfa[cursor * M3_ASCII + (unsigned char) packed[at]];
            }
            out[(size_t) state * (size_t) count + (size_t) token] = (jshort) cursor;
        }
    }
    written = 1;
done:
    if (out != NULL) (*env)->ReleaseShortArrayElements(env, outArray, out, written ? 0 : JNI_ABORT);
    if (lengths != NULL) (*env)->ReleaseIntArrayElements(env, lengthArray, lengths, JNI_ABORT);
    if (starts != NULL) (*env)->ReleaseIntArrayElements(env, startArray, starts, JNI_ABORT);
    if (packed != NULL) (*env)->ReleaseByteArrayElements(env, packedArray, packed, JNI_ABORT);
    if (dfa != NULL) (*env)->ReleaseShortArrayElements(env, dfaArray, dfa, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_jdk_internal_mindex_M3RX7Transitions_compose0(
        JNIEnv *env, jclass ignored, jshortArray leftArray,
        jshortArray rightArray, jshortArray outArray) {
    jshort *left = NULL, *right = NULL, *out = NULL;
    int written = 0;
    (void) ignored;
    if (leftArray == NULL || rightArray == NULL || outArray == NULL) {
        invalid(env, "missing DFA transfer arrays");
        return;
    }
    jsize states = (*env)->GetArrayLength(env, leftArray);
    if (states < 1 || states > M3_MAX_STATES
            || states != (*env)->GetArrayLength(env, rightArray)
            || states != (*env)->GetArrayLength(env, outArray)) {
        invalid(env, "invalid DFA transfer dimensions");
        return;
    }
    if ((*env)->IsSameObject(env, leftArray, outArray)
            || (*env)->IsSameObject(env, rightArray, outArray)) {
        invalid(env, "DFA transfer output aliases input");
        return;
    }
    left = (*env)->GetShortArrayElements(env, leftArray, NULL);
    if (left == NULL) goto done;
    right = (*env)->GetShortArrayElements(env, rightArray, NULL);
    if (right == NULL) goto done;
    if (!valid_transfer(left, states) || !valid_transfer(right, states)) {
        invalid(env, "invalid DFA transfer state");
        goto done;
    }
    out = (*env)->GetShortArrayElements(env, outArray, NULL);
    if (out == NULL) goto done;
    compose_transfer(left, right, out, states);
    written = 1;
done:
    if (out != NULL) (*env)->ReleaseShortArrayElements(env, outArray, out, written ? 0 : JNI_ABORT);
    if (right != NULL) (*env)->ReleaseShortArrayElements(env, rightArray, right, JNI_ABORT);
    if (left != NULL) (*env)->ReleaseShortArrayElements(env, leftArray, left, JNI_ABORT);
}

JNIEXPORT void JNICALL Java_jdk_internal_mindex_M3RX7Transitions_repeat0(
        JNIEnv *env, jclass ignored, jshortArray rowsArray,
        jlong count, jshortArray outArray) {
    jshort *rows = NULL, *out = NULL, *workspace = NULL;
    int written = 0;
    (void) ignored;
    if (rowsArray == NULL || outArray == NULL) {
        invalid(env, "missing DFA transfer arrays");
        return;
    }
    jsize states = (*env)->GetArrayLength(env, rowsArray);
    if (count < 0 || states < 1 || states > M3_MAX_STATES
            || states != (*env)->GetArrayLength(env, outArray)) {
        invalid(env, "invalid DFA repetition dimensions or count");
        return;
    }
    if ((*env)->IsSameObject(env, rowsArray, outArray)) {
        invalid(env, "DFA repetition output aliases input");
        return;
    }
    rows = (*env)->GetShortArrayElements(env, rowsArray, NULL);
    if (rows == NULL) goto done;
    if (!valid_transfer(rows, states)) {
        invalid(env, "invalid DFA transfer state");
        goto done;
    }
    workspace = (jshort *) malloc((size_t) states * 3 * sizeof(jshort));
    if (workspace == NULL) {
        allocation_failed(env);
        goto done;
    }
    jshort *result = workspace;
    jshort *power = workspace + states;
    jshort *scratch = workspace + 2 * states;
    for (jsize state = 0; state < states; state++) {
        result[state] = (jshort) state;
        power[state] = rows[state];
    }
    for (uint64_t remaining = (uint64_t) count; remaining != 0; remaining >>= 1) {
        if ((remaining & 1) != 0) {
            compose_transfer(result, power, scratch, states);
            jshort *previous = result;
            result = scratch;
            scratch = previous;
        }
        if (remaining > 1) {
            compose_transfer(power, power, scratch, states);
            jshort *previous = power;
            power = scratch;
            scratch = previous;
        }
    }
    out = (*env)->GetShortArrayElements(env, outArray, NULL);
    if (out == NULL) goto done;
    for (jsize state = 0; state < states; state++) out[state] = result[state];
    written = 1;
done:
    if (out != NULL) (*env)->ReleaseShortArrayElements(env, outArray, out, written ? 0 : JNI_ABORT);
    free(workspace);
    if (rows != NULL) (*env)->ReleaseShortArrayElements(env, rowsArray, rows, JNI_ABORT);
}
