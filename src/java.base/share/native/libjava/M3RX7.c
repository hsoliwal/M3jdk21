/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors.
 * GPLv2 with the Classpath exception (OpenJDK java.base native destination).
 * M3JDK21 DFA token transition precompute: bounded primitive array arithmetic only.
 */
#include <jni.h>
#include <stdint.h>
#include <stddef.h>
#include "jdk_internal_mindex_M3RX7.h"

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

/* Bounded token-symbol execution; no text decoding or retained Java addresses. */
JNIEXPORT jint JNICALL Java_jdk_internal_mindex_M3RX7_advanceTokens0(
        JNIEnv *env, jclass ignored, jshortArray rowArray, jint tokens, jint state,
        jintArray idArray, jint offset, jint length) {
    (void) ignored;
    if (rowArray == NULL || idArray == NULL) {
        invalid(env, "missing token-symbol arrays");
        return -1;
    }
    jsize cells = (*env)->GetArrayLength(env, rowArray);
    jsize ids = (*env)->GetArrayLength(env, idArray);
    if (tokens <= 0 || cells <= 0 || cells > M3_MAX_CELLS || cells % tokens != 0
            || cells / tokens > M3_MAX_STATES || state < -1 || state >= cells / tokens
            || offset < 0 || length < 0 || length > 4096 || offset > ids
            || length > ids - offset) {
        invalid(env, "invalid token-symbol page geometry");
        return -1;
    }
    jshort *rows = (*env)->GetShortArrayElements(env, rowArray, NULL);
    if (rows == NULL) return -1;
    jint page[256];
    int valid = 1;
    for (jint done = 0; done < length && valid;) {
        jint count = length - done < 256 ? length - done : 256;
        (*env)->GetIntArrayRegion(env, idArray, offset + done, count, page);
        if ((*env)->ExceptionCheck(env)) {
            (*env)->ReleaseShortArrayElements(env, rowArray, rows, JNI_ABORT);
            return -1;
        }
        for (jint i = 0; i < count; i++) {
            if (page[i] < 0 || page[i] >= tokens) { valid = 0; break; }
            if (state >= 0) {
                state = rows[(size_t) state * (size_t) tokens + (size_t) page[i]];
                if (state < -1 || state >= cells / tokens) { valid = 0; break; }
            }
        }
        done += count;
    }
    (*env)->ReleaseShortArrayElements(env, rowArray, rows, JNI_ABORT);
    if (!valid) {
        invalid(env, "invalid token-symbol ID or transition");
        return -1;
    }
    return state;
}
