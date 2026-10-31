/* SPDX-License-Identifier: Apache-2.0 */
#include <jni.h>
#include <stdint.h>
#include <stdlib.h>
#include "m3_arrays.h"
#include "../../../../native/include/m3_jni_core.h"

#define M3_ARRAY_CHUNK 1024

static void throw_illegal(JNIEnv *env, const char *message) {
  m3_jni_throw_new(env, "java/lang/IllegalArgumentException", message);
}

static int checked_range(jint start, jint count, jsize length) {
  return m3_jni_valid_window(length, start, count);
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

/* Required-native collection backing allocation through the existing array owner.
 * Standard JNI APIs create ordinary GC-managed Java arrays. No heap pointer,
 * global reference, native payload allocator or alternate storage owner is added.
 */
static void m3_backing_throw(JNIEnv *env, const char *name, const char *message) {
  if ((*env)->ExceptionCheck(env)) return;
  jclass type = (*env)->FindClass(env, name);
  if (type != NULL) {
    (void)(*env)->ThrowNew(env, type, message);
    (*env)->DeleteLocalRef(env, type);
  }
}

static int m3_backing_length(JNIEnv *env, jint length) {
  if ((*env)->ExceptionCheck(env)) return 0;
  if (length < 0) {
    m3_backing_throw(env, "java/lang/NegativeArraySizeException", "negative backing array length");
    return 0;
  }
  return 1;
}

static void m3_backing_failed(JNIEnv *env) {
  /* Preserve an existing allocation exception. Java also guards silent NULL if
   * native exception reporting itself cannot install an exception.
   */
  m3_backing_throw(env, "java/lang/OutOfMemoryError", "native backing array allocation failed");
}

JNIEXPORT jobjectArray JNICALL
Java_com_m3_arrays_M3ArrayNative_nativeAllocateObjects(
    JNIEnv *env, jclass owner, jint length) {
  (void)owner;
  if (!m3_backing_length(env, length)) return NULL;
  jclass component = (*env)->FindClass(env, "java/lang/Object");
  if (component == NULL) {
    m3_backing_failed(env);
    return NULL;
  }
  jobjectArray result = (*env)->NewObjectArray(env, length, component, NULL);
  (*env)->DeleteLocalRef(env, component);
  if (result == NULL) m3_backing_failed(env);
  return result;
}

JNIEXPORT jbyteArray JNICALL
Java_com_m3_arrays_M3ArrayNative_nativeAllocateBytes(
    JNIEnv *env, jclass owner, jint length) {
  (void)owner;
  if (!m3_backing_length(env, length)) return NULL;
  jbyteArray result = (*env)->NewByteArray(env, length);
  if (result == NULL) m3_backing_failed(env);
  return result;
}

JNIEXPORT jintArray JNICALL
Java_com_m3_arrays_M3ArrayNative_nativeAllocateInts(
    JNIEnv *env, jclass owner, jint length) {
  (void)owner;
  if (!m3_backing_length(env, length)) return NULL;
  jintArray result = (*env)->NewIntArray(env, length);
  if (result == NULL) m3_backing_failed(env);
  return result;
}

JNIEXPORT jlongArray JNICALL
Java_com_m3_arrays_M3ArrayNative_nativeAllocateLongs(
    JNIEnv *env, jclass owner, jint length) {
  (void)owner;
  if (!m3_backing_length(env, length)) return NULL;
  jlongArray result = (*env)->NewLongArray(env, length);
  if (result == NULL) m3_backing_failed(env);
  return result;
}
