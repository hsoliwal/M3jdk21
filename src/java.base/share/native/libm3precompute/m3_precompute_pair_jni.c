/* Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/78cf2ad4c7e56f7b0ade5aa206c491bc866178a6/synexia-openrewrite-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-text/m3-primitive-pair-empty-index-20261103/precompute_jni.c.txt blob 90a95e2de2ab; recipe m3/ports/precompute/recipe/planner-native-20261107/relocate.py v4
#include <jni.h>
#include <stdint.h>

static uint64_t mix64(uint64_t z) {
  z = (z ^ (z >> 30)) * UINT64_C(0xbf58476d1ce4e5b9);
  z = (z ^ (z >> 27)) * UINT64_C(0x94d049bb133111eb);
  return z ^ (z >> 31);
}

static uint64_t rotl64(uint64_t value, unsigned distance) {
  return (value << distance) | (value >> (64U - distance));
}

static uint64_t pair_hash(uint64_t left, uint64_t right) {
  return mix64(left) ^ rotl64(mix64(right), 29U);
}

JNIEXPORT jint JNICALL
Java_jdk_m3_precompute_NativePrecomputeOps_nativeAbiVersion(
    JNIEnv *env, jclass type) {
  (void) env;
  (void) type;
  return 1;
}

/* Preserve a pending Java exception while rejecting malformed private JNI calls. */
static void invalid_pair(JNIEnv *env, const char *message) {
  if ((*env)->ExceptionCheck(env)) return;
  jclass exception = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
  if (exception != NULL) {
    (void) (*env)->ThrowNew(env, exception, message);
    (*env)->DeleteLocalRef(env, exception);
  }
}

/* Get*ArrayElements may fail after a JVM exception; never replace that
 * exception. An exceptional embedded JVM which returns NULL without an
 * exception must not make a JNI void call look like successful completion. */
static void failed_borrow(JNIEnv *env) {
  if ((*env)->ExceptionCheck(env)) return;
  jclass exception = (*env)->FindClass(env, "java/lang/OutOfMemoryError");
  if (exception != NULL) {
    (void) (*env)->ThrowNew(env, exception, "INVALID_PAIR_NATIVE_ARRAY_BORROW");
    (*env)->DeleteLocalRef(env, exception);
  }
}

JNIEXPORT void JNICALL
Java_jdk_m3_precompute_NativePrecomputeOps_lookupPairsNative(
    JNIEnv *env,
    jclass type,
    jlongArray query_left_array,
    jlongArray query_right_array,
    jlongArray keys_array,
    jintArray values_array,
    jintArray slots_array,
    jint default_value,
    jintArray output_array) {
  (void) type;
  /* The Java entry point supplies a frozen index; the private raw JNI symbol
   * must nevertheless refuse malformed arrays before dereferencing them. */
  if ((*env)->ExceptionCheck(env)) return;
  if (query_left_array == NULL || query_right_array == NULL || keys_array == NULL
      || values_array == NULL || slots_array == NULL || output_array == NULL) {
    invalid_pair(env, "INVALID_PAIR_NATIVE_ARGUMENT");
    return;
  }

  jsize query_count = (*env)->GetArrayLength(env, query_left_array);
  jsize right_count = (*env)->GetArrayLength(env, query_right_array);
  jsize output_count = (*env)->GetArrayLength(env, output_array);
  jsize key_count = (*env)->GetArrayLength(env, keys_array);
  jsize value_count = (*env)->GetArrayLength(env, values_array);
  jsize slot_count = (*env)->GetArrayLength(env, slots_array);
  if (query_count != right_count || query_count != output_count
      || (int64_t) key_count != (int64_t) value_count * 2
      || slot_count <= 0
      || (((uint32_t) slot_count & ((uint32_t) slot_count - 1U)) != 0U)) {
    invalid_pair(env, "INVALID_PAIR_NATIVE_LAYOUT");
    return;
  }
  /* A privately invoked JNI caller must not reuse a read-only frozen index lane
   * as the writable result: it could corrupt a frozen array even on a hit. */
  if ((*env)->IsSameObject(env, output_array, values_array)
      || (*env)->IsSameObject(env, output_array, slots_array)) {
    invalid_pair(env, "INVALID_PAIR_NATIVE_OUTPUT_ALIAS");
    return;
  }
  if (query_count == 0) return;

  jlong *query_left = NULL;
  jlong *query_right = NULL;
  jlong *keys = NULL;
  jint *values = NULL;
  jint *slots = NULL;
  jint *output = NULL;
  const char *invalid_slot = NULL;
  int missing_borrow = 0;

  query_left = (*env)->GetLongArrayElements(env, query_left_array, NULL);
  if (query_left == NULL) { missing_borrow = 1; goto cleanup; }
  query_right = (*env)->GetLongArrayElements(env, query_right_array, NULL);
  if (query_right == NULL) { missing_borrow = 1; goto cleanup; }
  /* A frozen index with no rows has no key/value payload to read. Avoid
   * borrowing zero-length arrays: a JNI provider may return NULL without
   * an exception for such arrays. Valid empty indexes still return misses;
   * nonzero encoded slots are rejected by the normal slot preflight. */
  if (value_count != 0) {
    keys = (*env)->GetLongArrayElements(env, keys_array, NULL);
    if (keys == NULL) { missing_borrow = 1; goto cleanup; }
    values = (*env)->GetIntArrayElements(env, values_array, NULL);
    if (values == NULL) { missing_borrow = 1; goto cleanup; }
  }
  slots = (*env)->GetIntArrayElements(env, slots_array, NULL);
  if (slots == NULL) { missing_borrow = 1; goto cleanup; }

  /* Validate slot geometry BEFORE acquiring the writable output array. An
   * invalid later query must not leave earlier results visible on JVMs that
   * return pinned Java output storage (JNI_ABORT cannot undo pinned writes).
   * For large batches a full slot pass is cheaper than re-probing each query;
   * for small batches validate just the paths the queries may touch. Both
   * variants retain the original O(probes) lookup and allocate no objects. */
  uint32_t mask = (uint32_t) slot_count - 1U;
  if (query_count >= slot_count) {
    for (jsize position = 0; position < slot_count; position++) {
      jint encoded = slots[position];
      if (encoded < 0 || encoded > value_count) {
        invalid_slot = "INVALID_PAIR_NATIVE_SLOT";
        goto cleanup;
      }
    }
  } else {
    for (jsize i = 0; i < query_count; i++) {
      uint64_t left = (uint64_t) query_left[i];
      uint64_t right = (uint64_t) query_right[i];
      uint32_t slot = (uint32_t) pair_hash(left, right) & mask;
      for (jsize probe = 0; probe < slot_count; probe++) {
        jint encoded = slots[slot];
        if (encoded == 0) break;
        if (encoded < 0 || encoded > value_count) {
          invalid_slot = "INVALID_PAIR_NATIVE_SLOT";
          goto cleanup;
        }
        jint row = encoded - 1;
        jsize base = row * 2;
        if ((uint64_t) keys[base] == left
            && (uint64_t) keys[base + 1] == right) break;
        slot = (slot + 1U) & mask;
      }
    }
  }

  output = (*env)->GetIntArrayElements(env, output_array, NULL);
  if (output == NULL) { missing_borrow = 1; goto cleanup; }
  for (jsize i = 0; i < query_count; i++) {
    uint64_t left = (uint64_t) query_left[i];
    uint64_t right = (uint64_t) query_right[i];
    uint32_t slot = (uint32_t) pair_hash(left, right) & mask;
    jint result = default_value;
    for (jsize probe = 0; probe < slot_count; probe++) {
      jint encoded = slots[slot];
      if (encoded == 0) break;
      if (encoded < 0 || encoded > value_count) {
        invalid_slot = "INVALID_PAIR_NATIVE_SLOT";
        goto cleanup;
      }
      jint row = encoded - 1;
      jsize base = row * 2;
      if ((uint64_t) keys[base] == left && (uint64_t) keys[base + 1] == right) {
        result = values[row];
        break;
      }
      slot = (slot + 1U) & mask;
    }
    output[i] = result;
  }

cleanup:
  if (output != NULL)
    (*env)->ReleaseIntArrayElements(env, output_array, output, invalid_slot != NULL ? JNI_ABORT : 0);
  if (slots != NULL) (*env)->ReleaseIntArrayElements(env, slots_array, slots, JNI_ABORT);
  if (values != NULL) (*env)->ReleaseIntArrayElements(env, values_array, values, JNI_ABORT);
  if (keys != NULL) (*env)->ReleaseLongArrayElements(env, keys_array, keys, JNI_ABORT);
  if (query_right != NULL)
    (*env)->ReleaseLongArrayElements(env, query_right_array, query_right, JNI_ABORT);
  if (query_left != NULL)
    (*env)->ReleaseLongArrayElements(env, query_left_array, query_left, JNI_ABORT);
  if (invalid_slot != NULL) invalid_pair(env, invalid_slot);
  if (missing_borrow) failed_borrow(env);
}
