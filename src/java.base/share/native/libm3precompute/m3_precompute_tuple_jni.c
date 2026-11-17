/* Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 * Extends the existing Synexia frozen-index hash/layout, with no external code transplant.
 * Pair-only ABI entry points remain in precompute_jni.c, unchanged.
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/78cf2ad4c7e56f7b0ade5aa206c491bc866178a6/synexia-openrewrite-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-text/m3-frozen-tuple-jni-alias-20261101/tuple_lookup_jni.c.txt blob 7b06eefa1d92; recipe m3/ports/precompute/recipe/planner-native-20261107/relocate.py v4
#include <jni.h>
#include <stdint.h>

static void invalid(JNIEnv *env, const char *message) {
  jclass error = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
  if (error != NULL) {
    (void) (*env)->ThrowNew(env, error, message);
    (*env)->DeleteLocalRef(env, error);
  }
}

static uint64_t tuple_mix(uint64_t z) {
  z = (z ^ (z >> 30)) * UINT64_C(0xbf58476d1ce4e5b9);
  z = (z ^ (z >> 27)) * UINT64_C(0x94d049bb133111eb);
  return z ^ (z >> 31);
}

static uint64_t tuple_hash(const jlong *query, jint arity) {
  if (arity == 2) {
    uint64_t right = tuple_mix((uint64_t) query[1]);
    return tuple_mix((uint64_t) query[0]) ^ ((right << 29) | (right >> 35));
  }
  uint64_t hash = UINT64_C(0x9e3779b97f4a7c15) ^ (uint64_t) arity;
  for (jint role = 0; role < arity; role++) {
    hash = tuple_mix(hash ^ (uint64_t) query[role] ^ ((uint64_t) role << 32));
  }
  return hash;
}

JNIEXPORT jint JNICALL
Java_jdk_m3_precompute_NativePrecomputeOps_tupleLookupAbiVersionNative(JNIEnv *env, jclass type) {
  (void) env;
  (void) type;
  return 1;
}

JNIEXPORT void JNICALL
Java_jdk_m3_precompute_NativePrecomputeOps_lookupTuplesNative(
    JNIEnv *env, jclass type, jlongArray queries_array, jint arity,
    jlongArray keys_array, jintArray values_array, jintArray slots_array,
    jint default_value, jintArray output_array) {
  (void) type;
  /* A Java exception already in flight owns the JNI return boundary. */
  if ((*env)->ExceptionCheck(env)) return;
  if (queries_array == NULL || keys_array == NULL || values_array == NULL
      || slots_array == NULL || output_array == NULL || arity < 1 || arity > 4) {
    invalid(env, "INVALID_TUPLE_NATIVE_ARGUMENT");
    return;
  }
  jsize query_longs = (*env)->GetArrayLength(env, queries_array);
  jsize key_longs = (*env)->GetArrayLength(env, keys_array);
  jsize value_count = (*env)->GetArrayLength(env, values_array);
  jsize slot_count = (*env)->GetArrayLength(env, slots_array);
  jsize output_count = (*env)->GetArrayLength(env, output_array);
  if (query_longs % arity != 0 || output_count != query_longs / arity
      || (int64_t) key_longs != (int64_t) value_count * arity || slot_count <= 0
      || ((uint32_t) slot_count & ((uint32_t) slot_count - 1U)) != 0U) {
    invalid(env, "INVALID_TUPLE_NATIVE_LAYOUT");
    return;
  }
  if (output_count == 0) return;
  /* A raw JNI caller must not make a frozen index lane the writable result.
   * Public Java lookupTuples already allocates its output; this check preserves
   * that ownership contract before borrowing any native array elements. */
  if ((*env)->IsSameObject(env, output_array, values_array)
      || (*env)->IsSameObject(env, output_array, slots_array)) {
    invalid(env, "INVALID_TUPLE_NATIVE_OUTPUT_ALIAS");
    return;
  }

  jlong *queries = NULL;
  jlong *keys = NULL;
  jint *values = NULL;
  jint *slots = NULL;
  jint *output = NULL;
  int bad_slot = 0;
  queries = (*env)->GetLongArrayElements(env, queries_array, NULL);
  if (queries == NULL) goto cleanup;
  keys = (*env)->GetLongArrayElements(env, keys_array, NULL);
  if (keys == NULL) goto cleanup;
  values = (*env)->GetIntArrayElements(env, values_array, NULL);
  if (values == NULL) goto cleanup;
  slots = (*env)->GetIntArrayElements(env, slots_array, NULL);
  if (slots == NULL) goto cleanup;
  output = (*env)->GetIntArrayElements(env, output_array, NULL);
  if (output == NULL) goto cleanup;

  uint32_t mask = (uint32_t) slot_count - 1U;
  for (jsize query_row = 0; query_row < output_count; query_row++) {
    const jlong *query = queries + query_row * arity;
    uint32_t slot = (uint32_t) tuple_hash(query, arity) & mask;
    jint result = default_value;
    for (jsize probe = 0; probe < slot_count; probe++) {
      jint encoded = slots[slot];
      if (encoded == 0) break;
      if (encoded < 0 || encoded > value_count) { bad_slot = 1; goto cleanup; }
      jint row = encoded - 1;
      jsize base = row * arity;
      int equal = 1;
      for (jint role = 0; role < arity; role++) {
        if (keys[base + role] != query[role]) { equal = 0; break; }
      }
      if (equal != 0) { result = values[row]; break; }
      slot = (slot + 1U) & mask;
    }
    output[query_row] = result;
  }

cleanup:
  if (output != NULL) (*env)->ReleaseIntArrayElements(env, output_array, output, bad_slot != 0 ? JNI_ABORT : 0);
  if (slots != NULL) (*env)->ReleaseIntArrayElements(env, slots_array, slots, JNI_ABORT);
  if (values != NULL) (*env)->ReleaseIntArrayElements(env, values_array, values, JNI_ABORT);
  if (keys != NULL) (*env)->ReleaseLongArrayElements(env, keys_array, keys, JNI_ABORT);
  if (queries != NULL) (*env)->ReleaseLongArrayElements(env, queries_array, queries, JNI_ABORT);
  if (bad_slot != 0) invalid(env, "INVALID_TUPLE_NATIVE_SLOT");
}
