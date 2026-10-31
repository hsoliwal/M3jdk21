/* SPDX-License-Identifier: Apache-2.0 */
#include <jni.h>
#include <stdint.h>
#include "com_m3_collections_M3ResidencyStateMaskIndex.h"

/* Only ephemeral primitive index/word/state snapshots cross JNI; no Java object handles. */
#define M3_CHUNK 256

static void fail(JNIEnv *env, const char *name, const char *message) {
    if ((*env)->ExceptionCheck(env)) return;
    jclass kind = (*env)->FindClass(env, name);
    if (kind == NULL) return;
    (void) (*env)->ThrowNew(env, kind, message);
    (*env)->DeleteLocalRef(env, kind);
}

static int selected(jlong bits, jbyte state, jlong required,
                    jlong forbidden, jint allowed_states) {
    int life = state;
    if (life < 0 || life >= 6) return 0;
    return ((allowed_states & (1 << life)) != 0)
            && ((bits & required) == required) && ((bits & forbidden) == 0);
}

JNIEXPORT jintArray JNICALL Java_com_m3_collections_M3ResidencyStateMaskIndex_select0(
        JNIEnv *env, jclass owner, jintArray coordinates, jlongArray words,
        jbyteArray lifecycle, jlong required, jlong forbidden, jint allowed_states) {
    (void) owner;
    if (coordinates == NULL || words == NULL || lifecycle == NULL) {
        fail(env, "java/lang/NullPointerException", "null native state-snapshot array");
        return NULL;
    }
    if ((required & forbidden) != 0 || (allowed_states & ~63) != 0) {
        fail(env, "java/lang/IllegalArgumentException", "invalid state mask");
        return NULL;
    }
    jsize length = (*env)->GetArrayLength(env, coordinates);
    if ((*env)->ExceptionCheck(env)) return NULL;
    if ((*env)->GetArrayLength(env, words) != length
            || (*env)->GetArrayLength(env, lifecycle) != length) {
        fail(env, "java/lang/IllegalArgumentException", "unaligned primitive snapshot arrays");
        return NULL;
    }
    if ((*env)->ExceptionCheck(env)) return NULL;
    jlong values[M3_CHUNK];
    jbyte statuses[M3_CHUNK];
    jint original[M3_CHUNK];
    jint out[M3_CHUNK];
    jsize matches = 0;
    for (jsize base = 0; base < length;) {
        jsize take = length - base < M3_CHUNK ? length - base : M3_CHUNK;
        (*env)->GetLongArrayRegion(env, words, base, take, values);
        if ((*env)->ExceptionCheck(env)) return NULL;
        (*env)->GetByteArrayRegion(env, lifecycle, base, take, statuses);
        if ((*env)->ExceptionCheck(env)) return NULL;
        for (jsize i = 0; i < take; i++) {
            if (statuses[i] < 0 || statuses[i] >= 6) {
                fail(env, "java/lang/IllegalArgumentException", "invalid lifecycle ordinal");
                return NULL;
            }
            if (selected(values[i], statuses[i], required, forbidden, allowed_states)) ++matches;
        }
        base += take;
    }
    jintArray result = (*env)->NewIntArray(env, matches);
    if (result == NULL || (*env)->ExceptionCheck(env)) return NULL;
    jsize written = 0, buffered = 0;
    for (jsize base = 0; base < length;) {
        jsize take = length - base < M3_CHUNK ? length - base : M3_CHUNK;
        (*env)->GetLongArrayRegion(env, words, base, take, values);
        if ((*env)->ExceptionCheck(env)) return NULL;
        (*env)->GetByteArrayRegion(env, lifecycle, base, take, statuses);
        if ((*env)->ExceptionCheck(env)) return NULL;
        (*env)->GetIntArrayRegion(env, coordinates, base, take, original);
        if ((*env)->ExceptionCheck(env)) return NULL;
        for (jsize i = 0; i < take; i++) {
            if (!selected(values[i], statuses[i], required, forbidden, allowed_states)) continue;
            if ((jlong) written + buffered >= matches) {
                fail(env, "java/util/ConcurrentModificationException", "native primitive snapshot changed");
                return NULL;
            }
            out[buffered++] = original[i];
            if (buffered == M3_CHUNK) {
                (*env)->SetIntArrayRegion(env, result, written, buffered, out);
                if ((*env)->ExceptionCheck(env)) return NULL;
                written += buffered;
                buffered = 0;
            }
        }
        base += take;
    }
    if ((jlong) written + buffered != matches) {
        fail(env, "java/util/ConcurrentModificationException", "native primitive snapshot changed");
        return NULL;
    }
    if (buffered != 0) (*env)->SetIntArrayRegion(env, result, written, buffered, out);
    if ((*env)->ExceptionCheck(env)) return NULL;
    return result;
}
