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
