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

/*
 * Optional byte-pattern extension of the existing SHA-pinned LF C11 JNI library.
 * The pattern is copied once into bounded stack scratch. Two KMP passes produce
 * exact overlapping source-relative hit offsets without retaining Java pointers
 * or pinning arrays. No changes to ABI v1, countLf0 or lineEnds0.
 */
JNIEXPORT jintArray JNICALL
Java_com_m3_runtime_capability_BytePatternSearch_findKmp0(
        JNIEnv *env, jclass owner, jobject buffer, jint start, jint length,
        jbyteArray needle_array, jint max_matches) {
    (void)owner;
    if (buffer == NULL || needle_array == NULL || start < 0 || length < 0
            || length > 16777216 || max_matches < 0 || max_matches > 1000000) {
        illegal(env, "invalid byte-pattern JNI geometry");
        return NULL;
    }
    jsize width = (*env)->GetArrayLength(env, needle_array);
    if (width < 1 || width > 4096) {
        illegal(env, "native byte-pattern length budget");
        return NULL;
    }
    jlong capacity = (*env)->GetDirectBufferCapacity(env, buffer);
    if (capacity < 0 || (jlong)start + (jlong)length > capacity) {
        illegal(env, "byte-search direct buffer capacity mismatch");
        return NULL;
    }
    const uint8_t *data = (const uint8_t *)(*env)->GetDirectBufferAddress(env, buffer);
    if (data == NULL && length != 0) {
        illegal(env, "byte-search direct buffer address absent");
        return NULL;
    }
    uint8_t pattern[4096];
    jint failure[4096];
    (*env)->GetByteArrayRegion(env, needle_array, 0, width, (jbyte *)pattern);
    if ((*env)->ExceptionCheck(env)) return NULL;
    failure[0] = 0;
    for (jint i = 1, matched = 0; i < width; i++) {
        while (matched != 0 && pattern[i] != pattern[matched]) {
            matched = failure[matched - 1];
        }
        if (pattern[i] == pattern[matched]) matched++;
        failure[i] = matched;
    }
    jint count = 0;
    for (jint pass = 0; pass < 2; pass++) {
        jint found = 0;
        jint matched = 0;
        jint chunk[512];
        jint staged = 0;
        jint written = 0;
        jintArray output = NULL;
        if (pass == 1) {
            output = (*env)->NewIntArray(env, count);
            if (output == NULL) return NULL;
        }
        for (jint i = 0; i < length; i++) {
            uint8_t unit = data[(size_t)start + (size_t)i];
            while (matched != 0 && pattern[matched] != unit) {
                matched = failure[matched - 1];
            }
            if (pattern[matched] == unit) matched++;
            if (matched == width) {
                if (found == max_matches) {
                    illegal(env, "native byte-search result budget");
                    return NULL;
                }
                if (pass == 1) {
                    chunk[staged++] = i + 1 - width;
                    if (staged == 512) {
                        (*env)->SetIntArrayRegion(env, output, written, staged, chunk);
                        if ((*env)->ExceptionCheck(env)) return NULL;
                        written += staged;
                        staged = 0;
                    }
                }
                found++;
                matched = failure[width - 1];
            }
        }
        if (pass == 0) {
            count = found;
        } else {
            if (staged > 0) {
                (*env)->SetIntArrayRegion(env, output, written, staged, chunk);
                if ((*env)->ExceptionCheck(env)) return NULL;
                written += staged;
            }
            if (found != count || written != count) {
                illegal(env, "native source bytes changed between KMP passes");
                return NULL;
            }
            return output;
        }
    }
    illegal(env, "byte-search state unreachable");
    return NULL;
}
