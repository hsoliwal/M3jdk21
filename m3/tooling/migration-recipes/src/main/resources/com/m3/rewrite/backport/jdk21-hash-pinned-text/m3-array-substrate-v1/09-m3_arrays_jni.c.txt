/* SPDX-License-Identifier: Apache-2.0 */
#include <jni.h>
#include <stdint.h>
#include <stdlib.h>
#include "m3_arrays.h"

#define M3_ARRAY_CHUNK 1024

static void throw_illegal(JNIEnv *env, const char *message) {
  if ((*env)->ExceptionCheck(env)) return;
  jclass type = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
  if (type != NULL) {
    (void)(*env)->ThrowNew(env, type, message);
    (*env)->DeleteLocalRef(env, type);
  }
}

static int checked_range(jint start, jint count, jsize length) {
  return start >= 0 && count >= 0 && (int64_t)start + (int64_t)count <= (int64_t)length;
}

JNIEXPORT jint JNICALL
Java_com_m3_arrays_M3ArrayNative_nativeCompareUtf16(
    JNIEnv *env,
    jclass type,
    jcharArray left_array,
    jint left_start,
    jcharArray right_array,
    jint right_start,
    jint count) {
  (void)type;
  if (left_array == NULL || right_array == NULL) {
    throw_illegal(env, "null UTF-16 array");
    return 0;
  }
  jsize left_length = (*env)->GetArrayLength(env, left_array);
  jsize right_length = (*env)->GetArrayLength(env, right_array);
  if (!checked_range(left_start, count, left_length)
      || !checked_range(right_start, count, right_length)) {
    throw_illegal(env, "UTF-16 compare range");
    return 0;
  }

  jchar left[M3_ARRAY_CHUNK];
  jchar right[M3_ARRAY_CHUNK];
  jint compared = 0;
  while (compared < count) {
    jint take = count - compared;
    if (take > M3_ARRAY_CHUNK) take = M3_ARRAY_CHUNK;
    (*env)->GetCharArrayRegion(env, left_array, left_start + compared, take, left);
    if ((*env)->ExceptionCheck(env)) return 0;
    (*env)->GetCharArrayRegion(env, right_array, right_start + compared, take, right);
    if ((*env)->ExceptionCheck(env)) return 0;
    int result = m3_compare_utf16(
        (const uint16_t *)left, 0U, (const uint16_t *)right, 0U, (size_t)take);
    if (result != 0) return (jint)result;
    compared += take;
  }
  return 0;
}

JNIEXPORT jcharArray JNICALL
Java_com_m3_arrays_M3ArrayNative_nativeConcatenateUtf16(
    JNIEnv *env,
    jclass type,
    jobjectArray segments) {
  (void)type;
  if (segments == NULL) {
    throw_illegal(env, "null UTF-16 segment array");
    return NULL;
  }
  jsize count = (*env)->GetArrayLength(env, segments);
  int64_t total = 0;
  for (jsize index = 0; index < count; index++) {
    jcharArray segment = (jcharArray)(*env)->GetObjectArrayElement(env, segments, index);
    if (segment == NULL) {
      throw_illegal(env, "null UTF-16 segment");
      return NULL;
    }
    total += (*env)->GetArrayLength(env, segment);
    (*env)->DeleteLocalRef(env, segment);
    if (total > INT32_MAX) {
      jclass oom = (*env)->FindClass(env, "java/lang/OutOfMemoryError");
      if (oom != NULL) {
        (void)(*env)->ThrowNew(env, oom, "required UTF-16 array size too large");
        (*env)->DeleteLocalRef(env, oom);
      }
      return NULL;
    }
  }

  jcharArray result = (*env)->NewCharArray(env, (jsize)total);
  if (result == NULL) return NULL;

  jchar buffer[M3_ARRAY_CHUNK];
  jsize destination = 0;
  for (jsize index = 0; index < count; index++) {
    jcharArray segment = (jcharArray)(*env)->GetObjectArrayElement(env, segments, index);
    if (segment == NULL) return NULL;
    jsize length = (*env)->GetArrayLength(env, segment);
    jsize source = 0;
    while (source < length) {
      jsize take = length - source;
      if (take > M3_ARRAY_CHUNK) take = M3_ARRAY_CHUNK;
      (*env)->GetCharArrayRegion(env, segment, source, take, buffer);
      if ((*env)->ExceptionCheck(env)) {
        (*env)->DeleteLocalRef(env, segment);
        return NULL;
      }
      (*env)->SetCharArrayRegion(env, result, destination, take, buffer);
      if ((*env)->ExceptionCheck(env)) {
        (*env)->DeleteLocalRef(env, segment);
        return NULL;
      }
      source += take;
      destination += take;
    }
    (*env)->DeleteLocalRef(env, segment);
  }
  return result;
}
