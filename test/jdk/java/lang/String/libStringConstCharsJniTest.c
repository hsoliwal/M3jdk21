/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Synexia, Hitesh Soliwal and others
 */
#include <jni.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>

/* Copies expected into a malloc'd buffer before any critical region is entered. */
static jchar *expected_chars(JNIEnv *env, jcharArray expected, jsize *length) {
    *length = (*env)->GetArrayLength(env, expected);
    jchar *chars = (jchar *) malloc(((size_t) *length + 1) * sizeof(jchar));
    if (chars != NULL) {
        (*env)->GetCharArrayRegion(env, expected, 0, *length, chars);
        if ((*env)->ExceptionCheck(env)) {
            free(chars);
            return NULL;
        }
    }
    return chars;
}

/*
 * Two nested GetStringCritical calls on one String. out[0], out[1]: the addresses; out[2],
 * out[3]: the isCopy flags. Returns bit 0 / bit 1: the first / second chars equal expected,
 * bit 2: both calls returned one address. Negative: a setup failure.
 */
JNIEXPORT jint JNICALL Java_StringConstCharsJniTest_criticalTwice(
        JNIEnv *env, jclass ignored, jstring value, jcharArray expected, jlongArray out) {
    jsize length;
    jchar *want = expected_chars(env, expected, &length);
    if (want == NULL) return -1;
    size_t bytes = (size_t) length * sizeof(jchar);
    jboolean copy1 = JNI_TRUE;
    jboolean copy2 = JNI_TRUE;
    const jchar *first = (*env)->GetStringCritical(env, value, &copy1);
    if (first == NULL) {
        free(want);
        return -2;
    }
    const jchar *second = (*env)->GetStringCritical(env, value, &copy2);
    if (second == NULL) {
        (*env)->ReleaseStringCritical(env, value, first);
        free(want);
        return -3;
    }
    /* Inside the critical region: no allocation, no blocking, no other JNI call. */
    jint result = 0;
    if (memcmp(first, want, bytes) == 0) result |= 1;
    if (memcmp(second, want, bytes) == 0) result |= 2;
    if (first == second) result |= 4;
    jlong report[4];
    report[0] = (jlong) (intptr_t) first;
    report[1] = (jlong) (intptr_t) second;
    report[2] = copy1;
    report[3] = copy2;
    (*env)->ReleaseStringCritical(env, value, second);
    (*env)->ReleaseStringCritical(env, value, first);
    free(want);
    (*env)->SetLongArrayRegion(env, out, 0, 4, report);
    return (*env)->ExceptionCheck(env) ? -4 : result;
}

/*
 * GetStringCritical of inner while the critical region of outer is held. Returns bit 0: the
 * inner chars equal innerExpected, bit 2: the outer chars equal outerExpected; innerCopy[0]
 * receives the inner isCopy flag. Negative: a setup failure.
 */
JNIEXPORT jint JNICALL Java_StringConstCharsJniTest_criticalNested(
        JNIEnv *env, jclass ignored, jstring outer, jcharArray outerExpected,
        jstring inner, jcharArray innerExpected, jbooleanArray innerCopy) {
    jsize outerLength;
    jsize innerLength;
    jchar *outerWant = expected_chars(env, outerExpected, &outerLength);
    jchar *innerWant = expected_chars(env, innerExpected, &innerLength);
    if (outerWant == NULL || innerWant == NULL) {
        free(outerWant);
        free(innerWant);
        return -1;
    }
    const jchar *outerChars = (*env)->GetStringCritical(env, outer, NULL);
    if (outerChars == NULL) {
        free(outerWant);
        free(innerWant);
        return -2;
    }
    jboolean copy = JNI_FALSE;
    const jchar *innerChars = (*env)->GetStringCritical(env, inner, &copy);
    jint result = 0;
    if (innerChars != NULL) {
        if (memcmp(innerChars, innerWant, (size_t) innerLength * sizeof(jchar)) == 0) {
            result |= 1;
        }
        (*env)->ReleaseStringCritical(env, inner, innerChars);
    }
    if (memcmp(outerChars, outerWant, (size_t) outerLength * sizeof(jchar)) == 0) result |= 4;
    (*env)->ReleaseStringCritical(env, outer, outerChars);
    free(outerWant);
    free(innerWant);
    if (innerChars == NULL) return -3;
    (*env)->SetBooleanArrayRegion(env, innerCopy, 0, 1, &copy);
    return (*env)->ExceptionCheck(env) ? -4 : result;
}

/*
 * GetStringChars and GetStringRegion. Returns bit 0: the chars equal expected and end with NUL,
 * bit 1: GetStringChars isCopy, bit 2: the region [start, start + len) equals expected, bit 3:
 * a region past the end throws StringIndexOutOfBoundsException. Negative: a setup failure.
 */
JNIEXPORT jint JNICALL Java_StringConstCharsJniTest_charsAndRegion(
        JNIEnv *env, jclass ignored, jstring value, jcharArray expected, jint start, jint len) {
    jsize length;
    jchar *want = expected_chars(env, expected, &length);
    if (want == NULL) return -1;
    jchar *region = (jchar *) malloc(((size_t) len + 1) * sizeof(jchar));
    if (region == NULL) {
        free(want);
        return -2;
    }
    jint result = 0;
    jboolean copy = JNI_FALSE;
    const jchar *chars = (*env)->GetStringChars(env, value, &copy);
    if (chars != NULL) {
        if (memcmp(chars, want, (size_t) length * sizeof(jchar)) == 0 && chars[length] == 0) {
            result |= 1;
        }
        if (copy == JNI_TRUE) result |= 2;
        (*env)->ReleaseStringChars(env, value, chars);
    }
    (*env)->GetStringRegion(env, value, start, len, region);
    if ((*env)->ExceptionCheck(env)) {
        (*env)->ExceptionClear(env);
    } else if (memcmp(region, want + start, (size_t) len * sizeof(jchar)) == 0) {
        result |= 4;
    }
    (*env)->GetStringRegion(env, value, length, 1, region);
    jthrowable thrown = (*env)->ExceptionOccurred(env);
    if (thrown != NULL) {
        (*env)->ExceptionClear(env);
        jclass bounds = (*env)->FindClass(env, "java/lang/StringIndexOutOfBoundsException");
        if (bounds != NULL && (*env)->IsInstanceOf(env, thrown, bounds)) result |= 8;
    }
    free(region);
    free(want);
    return result;
}

/* A String made with AllocObject: its constructor never ran, so its value is null. */
JNIEXPORT jstring JNICALL Java_StringConstCharsJniTest_allocUnconstructed(
        JNIEnv *env, jclass ignored) {
    jclass string = (*env)->FindClass(env, "java/lang/String");
    return string == NULL ? NULL : (jstring) (*env)->AllocObject(env, string);
}

/*
 * An unconstructed String. Returns bit 0: GetStringChars is NULL (JDK 21), bit 1:
 * GetStringCritical is not NULL, bit 2: its isCopy is JNI_TRUE (a native copy), bit 3:
 * GetStringLength is 0.
 */
JNIEXPORT jint JNICALL Java_StringConstCharsJniTest_unconstructed(
        JNIEnv *env, jclass ignored, jstring value) {
    jint result = 0;
    const jchar *chars = (*env)->GetStringChars(env, value, NULL);
    if (chars == NULL) {
        result |= 1;
    } else {
        (*env)->ReleaseStringChars(env, value, chars);
    }
    jboolean copy = JNI_FALSE;
    const jchar *critical = (*env)->GetStringCritical(env, value, &copy);
    if (critical != NULL) {
        result |= 2;
        if (copy == JNI_TRUE) result |= 4;
        (*env)->ReleaseStringCritical(env, value, critical);
    }
    if ((*env)->GetStringLength(env, value) == 0) result |= 8;
    return result;
}
