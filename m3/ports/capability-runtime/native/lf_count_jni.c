// SPDX-License-Identifier: Apache-2.0
#include <jni.h>
#include <stdint.h>
#include <stddef.h>

/* JNI is an optional, operation-local scalar leaf. Never retain a JVM pointer. */
static void illegal(JNIEnv *env, const char *message) {
    jclass klass = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
    if (klass != NULL) (void)(*env)->ThrowNew(env, klass, message);
}

JNIEXPORT jint JNICALL
Java_com_m3_runtime_capability_LfCount_abi0(JNIEnv *env, jclass owner) {
    (void) env;
    (void) owner;
    return 1;
}

JNIEXPORT jlong JNICALL
Java_com_m3_runtime_capability_LfCount_countLf0(
        JNIEnv *env, jclass owner, jobject buffer, jint start, jint length) {
    (void) owner;
    if (buffer == NULL || start < 0 || length < 0) {
        illegal(env, "invalid direct LF range");
        return 0;
    }
    jlong capacity = (*env)->GetDirectBufferCapacity(env, buffer);
    if (capacity < 0 || (jlong)start + (jlong)length > capacity) {
        illegal(env, "direct buffer capacity/range mismatch");
        return 0;
    }
    if (length == 0) return 0;
    const uint8_t *bytes = (const uint8_t *)(*env)->GetDirectBufferAddress(env, buffer);
    if (bytes == NULL) {
        illegal(env, "missing direct buffer address");
        return 0;
    }
    jlong count = 0;
    for (size_t index = (size_t)start; index < (size_t)start + (size_t)length; index++) {
        count += bytes[index] == (uint8_t)'\n';
    }
    return count;
}

/*
 * Bounded source-byte line-start positions, relative to [start, start+length).
 * Two passes guarantee exact result allocation. JNI writes small stack chunks
 * with SetIntArrayRegion; no Java array or direct address is retained or pinned.
 * This new symbol does not change the v1 abi0()/countLf0() contract.
 */
JNIEXPORT jintArray JNICALL
Java_com_m3_runtime_capability_LfCount_lineEnds0(
        JNIEnv *env, jclass owner, jobject buffer,
        jint start, jint length, jint max_breaks) {
    (void)owner;
    if (buffer == NULL || start < 0 || length < 0 || max_breaks < 0
            || max_breaks > 1000000 || length > 16777216) {
        illegal(env, "invalid bounded LF boundary scan");
        return NULL;
    }
    jlong capacity = (*env)->GetDirectBufferCapacity(env, buffer);
    if (capacity < 0 || (jlong)start + (jlong)length > capacity) {
        illegal(env, "LF boundary direct-buffer capacity");
        return NULL;
    }
    const uint8_t *bytes = (const uint8_t *)(*env)->GetDirectBufferAddress(env, buffer);
    if (bytes == NULL && length != 0) {
        illegal(env, "LF boundary direct-buffer address");
        return NULL;
    }
    jint count = 0;
    for (jint i = 0; i < length; i++) {
        if (bytes[start + i] == (uint8_t)'\n') {
            if (count == max_breaks) {
                illegal(env, "LF boundary index memory budget");
                return NULL;
            }
            count++;
        }
    }
    jintArray result = (*env)->NewIntArray(env, count);
    if (result == NULL) return NULL;
    jint chunk[1024];
    jint written = 0;
    jint size = 0;
    for (jint i = 0; i < length; i++) {
        if (bytes[start + i] == (uint8_t)'\n') {
            chunk[size++] = i + 1;
            if (size == 1024) {
                (*env)->SetIntArrayRegion(env, result, written, size, chunk);
                if ((*env)->ExceptionCheck(env)) return NULL;
                written += size;
                size = 0;
            }
        }
    }
    if (size != 0) {
        (*env)->SetIntArrayRegion(env, result, written, size, chunk);
        if ((*env)->ExceptionCheck(env)) return NULL;
        written += size;
    }
    if (written != count) {
        illegal(env, "LF boundary count mutated while scanning");
        return NULL;
    }
    return result;
}
