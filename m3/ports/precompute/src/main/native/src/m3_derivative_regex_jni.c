// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
#include <jni.h>

#include <stdint.h>

#include "m3_derivative_regex.h"

static void throw_illegal_argument(JNIEnv *env, const char *message) {
  jclass type = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
  if (type != NULL) (void) (*env)->ThrowNew(env, type, message);
}

static void throw_illegal_state(JNIEnv *env, const char *message) {
  jclass type = (*env)->FindClass(env, "java/lang/IllegalStateException");
  if (type != NULL) (void) (*env)->ThrowNew(env, type, message);
}

JNIEXPORT jbyteArray JNICALL
Java_com_m3_precompute_M3RegexDerivativeNative_nativeMatchRunsBatch(
    JNIEnv *env,
    jclass owner,
    jint classes,
    jint states,
    jint start_state,
    jintArray power_array,
    jint power_bits,
    jbyteArray accepting_array,
    jbyteArray live_array,
    jintArray class_array,
    jintArray length_array,
    jintArray offset_array) {
  (void) owner;
  if (classes <= 0
      || states <= 0
      || start_state < 0
      || start_state >= states
      || power_bits <= 0
      || power_bits > 31
      || power_array == NULL
      || accepting_array == NULL
      || live_array == NULL
      || class_array == NULL
      || length_array == NULL
      || offset_array == NULL) {
    throw_illegal_argument(env, "invalid derivative regex JNI geometry");
    return NULL;
  }

  jsize power_count = (*env)->GetArrayLength(env, power_array);
  jsize accepting_count = (*env)->GetArrayLength(env, accepting_array);
  jsize live_count = (*env)->GetArrayLength(env, live_array);
  jsize run_count = (*env)->GetArrayLength(env, class_array);
  jsize length_count = (*env)->GetArrayLength(env, length_array);
  jsize offset_count = (*env)->GetArrayLength(env, offset_array);
  int64_t expected_powers =
      (int64_t) power_bits * (int64_t) states * (int64_t) classes;
  if (offset_count < 1
      || accepting_count != states
      || live_count != states
      || length_count != run_count
      || expected_powers != (int64_t) power_count) {
    throw_illegal_argument(env, "inconsistent derivative regex JNI arrays");
    return NULL;
  }

  jint *powers = (*env)->GetIntArrayElements(env, power_array, NULL);
  if (powers == NULL) return NULL;
  jbyte *accepting = (*env)->GetByteArrayElements(env, accepting_array, NULL);
  if (accepting == NULL) {
    (*env)->ReleaseIntArrayElements(env, power_array, powers, JNI_ABORT);
    return NULL;
  }
  jbyte *live = (*env)->GetByteArrayElements(env, live_array, NULL);
  if (live == NULL) {
    (*env)->ReleaseByteArrayElements(env, accepting_array, accepting, JNI_ABORT);
    (*env)->ReleaseIntArrayElements(env, power_array, powers, JNI_ABORT);
    return NULL;
  }
  jint *classes_values = (*env)->GetIntArrayElements(env, class_array, NULL);
  if (classes_values == NULL) {
    (*env)->ReleaseByteArrayElements(env, live_array, live, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, accepting_array, accepting, JNI_ABORT);
    (*env)->ReleaseIntArrayElements(env, power_array, powers, JNI_ABORT);
    return NULL;
  }
  jint *lengths = (*env)->GetIntArrayElements(env, length_array, NULL);
  if (lengths == NULL) {
    (*env)->ReleaseIntArrayElements(env, class_array, classes_values, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, live_array, live, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, accepting_array, accepting, JNI_ABORT);
    (*env)->ReleaseIntArrayElements(env, power_array, powers, JNI_ABORT);
    return NULL;
  }
  jint *offsets = (*env)->GetIntArrayElements(env, offset_array, NULL);
  if (offsets == NULL) {
    (*env)->ReleaseIntArrayElements(env, length_array, lengths, JNI_ABORT);
    (*env)->ReleaseIntArrayElements(env, class_array, classes_values, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, live_array, live, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, accepting_array, accepting, JNI_ABORT);
    (*env)->ReleaseIntArrayElements(env, power_array, powers, JNI_ABORT);
    return NULL;
  }

  jsize row_count = offset_count - 1;
  jbyteArray result = (*env)->NewByteArray(env, row_count);
  if (result == NULL) {
    (*env)->ReleaseIntArrayElements(env, offset_array, offsets, JNI_ABORT);
    (*env)->ReleaseIntArrayElements(env, length_array, lengths, JNI_ABORT);
    (*env)->ReleaseIntArrayElements(env, class_array, classes_values, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, live_array, live, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, accepting_array, accepting, JNI_ABORT);
    (*env)->ReleaseIntArrayElements(env, power_array, powers, JNI_ABORT);
    return NULL;
  }
  jbyte *output = (*env)->GetByteArrayElements(env, result, NULL);
  if (output == NULL) {
    (*env)->ReleaseIntArrayElements(env, offset_array, offsets, JNI_ABORT);
    (*env)->ReleaseIntArrayElements(env, length_array, lengths, JNI_ABORT);
    (*env)->ReleaseIntArrayElements(env, class_array, classes_values, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, live_array, live, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, accepting_array, accepting, JNI_ABORT);
    (*env)->ReleaseIntArrayElements(env, power_array, powers, JNI_ABORT);
    return NULL;
  }

  int status =
      m3_derivative_regex_match_runs_batch(
          (const int32_t *) powers,
          (size_t) power_count,
          (uint32_t) power_bits,
          (size_t) states,
          (size_t) classes,
          (uint32_t) start_state,
          (const uint8_t *) accepting,
          (const uint8_t *) live,
          (const int32_t *) classes_values,
          (const int32_t *) lengths,
          (size_t) run_count,
          (const int32_t *) offsets,
          (size_t) row_count,
          (uint8_t *) output);

  (*env)->ReleaseByteArrayElements(
      env, result, output, status == M3_DERIVATIVE_REGEX_OK ? 0 : JNI_ABORT);
  (*env)->ReleaseIntArrayElements(env, offset_array, offsets, JNI_ABORT);
  (*env)->ReleaseIntArrayElements(env, length_array, lengths, JNI_ABORT);
  (*env)->ReleaseIntArrayElements(env, class_array, classes_values, JNI_ABORT);
  (*env)->ReleaseByteArrayElements(env, live_array, live, JNI_ABORT);
  (*env)->ReleaseByteArrayElements(env, accepting_array, accepting, JNI_ABORT);
  (*env)->ReleaseIntArrayElements(env, power_array, powers, JNI_ABORT);

  if (status != M3_DERIVATIVE_REGEX_OK) {
    throw_illegal_state(
        env, "native derivative regex rejected compiled power table or input");
    return NULL;
  }
  return result;
}
