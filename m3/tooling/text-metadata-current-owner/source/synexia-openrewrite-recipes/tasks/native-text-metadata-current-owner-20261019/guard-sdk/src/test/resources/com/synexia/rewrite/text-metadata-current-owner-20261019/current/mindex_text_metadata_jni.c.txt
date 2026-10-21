/* SPDX-License-Identifier: Apache-2.0 */
/* Scalar metadata only. No text payload, persistent address, pool or loader is owned here. */
#include <jni.h>
#include <stdint.h>
#include <limits.h>

#define ROW_WIDTH 5

static void fail(JNIEnv *env, const char *type, const char *message) {
    jclass exception = (*env)->FindClass(env, type);
    if (exception != NULL) {
        (void)(*env)->ThrowNew(env, exception, message);
        (*env)->DeleteLocalRef(env, exception);
    }
}

static uint32_t pow31(uint32_t length) {
    uint32_t result = UINT32_C(1);
    uint32_t base = UINT32_C(31);
    while (length != 0) {
        if ((length & UINT32_C(1)) != 0) result *= base;
        base *= base;
        length >>= 1;
    }
    return result;
}

/* Avoid an implementation-defined out-of-range unsigned-to-signed conversion. */
static jint signed_hash(uint32_t hash) {
    int64_t value = (int64_t)hash;
    if (value > INT32_MAX) value -= INT64_C(4294967296);
    return (jint)value;
}

static int valid(const jint *row) {
    jint length = row[0], points = row[2], first = row[3], last = row[4];
    if (length < 0 || points < 0 || points > length
            || points < length / 2 + length % 2
            || first < 0 || first > 65535 || last < 0 || last > 65535) return 0;
    if (length == 0 && (row[1] != 0 || first != 0 || last != 0)) return 0;
    if (length == 1 && (first != last || row[1] != first)) return 0;
    return 1;
}

JNIEXPORT jintArray JNICALL
Java_com_synexia_mindex_TextMetadata_nativeConcat(JNIEnv *env, jclass owner, jintArray input) {
    (void)owner;
    if (input == NULL) {
        fail(env, "java/lang/NullPointerException", "rows");
        return NULL;
    }
    jsize size = (*env)->GetArrayLength(env, input);
    if (size % ROW_WIDTH != 0) {
        fail(env, "java/lang/IllegalArgumentException", "invalid summary row width");
        return NULL;
    }
    jint result[ROW_WIDTH] = {0, 0, 0, 0, 0};
    if (size != 0) {
        jint *rows = (*env)->GetIntArrayElements(env, input, NULL);
        if (rows == NULL) return NULL;
        const char *error = NULL;
        const char *message = NULL;
        /* Reject all malformed rows before evaluating any result. */
        for (jsize i = 0; i < size; i += ROW_WIDTH) {
            if (!valid(rows + i)) {
                error = "java/lang/IllegalArgumentException";
                message = "invalid UTF-16 summary";
                break;
            }
        }
        uint32_t hash = 0;
        for (jsize i = 0; error == NULL && i < size; i += ROW_WIDTH) {
            const jint *row = rows + i;
            if (row[0] == 0) continue;
            if (row[0] > INT32_MAX - result[0]) {
                error = "java/lang/ArithmeticException";
                message = "UTF-16 summary length overflow";
                break;
            }
            jint points = result[2] + row[2]; /* bounded by the checked total length */
            if (result[0] != 0 && result[4] >= 0xd800 && result[4] <= 0xdbff
                    && row[3] >= 0xdc00 && row[3] <= 0xdfff) points--;
            if (result[0] == 0) result[3] = row[3];
            hash = hash * pow31((uint32_t)row[0]) + (uint32_t)row[1];
            result[0] += row[0];
            result[2] = points;
            result[4] = row[4];
        }
        result[1] = signed_hash(hash);
        (*env)->ReleaseIntArrayElements(env, input, rows, JNI_ABORT);
        if (error != NULL) {
            fail(env, error, message);
            return NULL;
        }
    }
    jintArray output = (*env)->NewIntArray(env, ROW_WIDTH);
    if (output == NULL) return NULL;
    (*env)->SetIntArrayRegion(env, output, 0, ROW_WIDTH, result);
    if ((*env)->ExceptionCheck(env)) return NULL;
    return output;
}
