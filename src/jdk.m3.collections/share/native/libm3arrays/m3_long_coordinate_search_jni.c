/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
#include <jni.h>
#include <stdint.h>
#include "m3_long_coordinate_search.h"

#define M3_LONG_BOUND_REGION 1024

/* No GetLongArrayElements, Critical borrow, global refs, or retained pointers.
 * Each GetLongArrayRegion copy is complete before consulting the next pivot.
 */
#define M3_LONG_QUERY_BATCH 128

static void m3_throw_null(JNIEnv *env, const char *message) {
  jclass kind = (*env)->FindClass(env, "java/lang/NullPointerException");
  if (kind != NULL) {
    (void)(*env)->ThrowNew(env, kind, message);
    (*env)->DeleteLocalRef(env, kind);
  }
}

/* The caller already checked the non-null input and queried its length.
 * On any failed region access the pending Java exception is left intact.
 */
static jint m3_bound_region(JNIEnv *env, jlongArray input, jsize length,
    jlong key, jboolean upper) {
  jint low = 0;
  jint high = length;
  while (high - low > M3_LONG_BOUND_REGION) {
    jint middle = low + ((high - low) >> 1);
    jlong pivot = 0;
    (*env)->GetLongArrayRegion(env, input, middle, 1, &pivot);
    if ((*env)->ExceptionCheck(env)) return 0;
    if (upper != JNI_FALSE ? pivot <= key : pivot < key) low = middle + 1;
    else high = middle;
  }
  if (low == high) return low;
  jlong region[M3_LONG_BOUND_REGION];
  jsize count = high - low;
  (*env)->GetLongArrayRegion(env, input, low, count, region);
  if ((*env)->ExceptionCheck(env)) return 0;
  /* JNI jlong and C int64_t may differ in effective type on some ABIs.
   * Assign elements explicitly before giving the C kernel its own type.
   */
  int64_t signed_values[M3_LONG_BOUND_REGION];
  for (jsize i = 0; i < count; i++) signed_values[i] = (int64_t)region[i];
  size_t ordinal = m3_long_coordinate_bound(signed_values,
      (size_t)count, (int64_t)key, upper != JNI_FALSE);
  return low + (jint)ordinal;
}

JNIEXPORT jint JNICALL
Java_jdk_m3_arrays_LongCoordinateIndex_nativeBound(
    JNIEnv *env, jclass owner, jlongArray input, jlong key, jboolean upper) {
  (void)owner;
  if (input == NULL) {
    m3_throw_null(env, "null sorted long coordinates");
    return 0;
  }
  return m3_bound_region(env, input, (*env)->GetArrayLength(env, input), key, upper);
}

/* A single per-invocation cache for at most 1,024 signed-long coordinates.
 * Reading a key never stores a borrowed Java heap address; both the reader
 * and its JNI region copy are stack-local. The batch may be arbitrarily
 * ordered; a descent resets the ordinal hint to an ordinary binary bound.
 */
typedef struct {
  JNIEnv *env;
  jlongArray input;
  jsize length;
  jsize cached_start;
  jsize cached_count;
  jlong cached[M3_LONG_BOUND_REGION];
} m3_long_reader;

static int m3_long_at(m3_long_reader *reader, jint ordinal, jlong *value) {
  if (reader->cached_count == 0 || ordinal < reader->cached_start
      || ordinal - reader->cached_start >= reader->cached_count) {
    jsize start = (ordinal / M3_LONG_BOUND_REGION) * M3_LONG_BOUND_REGION;
    jsize remaining = reader->length - start;
    jsize count = remaining < M3_LONG_BOUND_REGION ? remaining : M3_LONG_BOUND_REGION;
    (*reader->env)->GetLongArrayRegion(reader->env, reader->input,
        start, count, reader->cached);
    if ((*reader->env)->ExceptionCheck(reader->env)) return 0;
    reader->cached_start = start;
    reader->cached_count = count;
  }
  *value = reader->cached[ordinal - reader->cached_start];
  return 1;
}

static int m3_long_before(jlong coordinate, jlong query, jboolean upper) {
  return upper != JNI_FALSE ? coordinate <= query : coordinate < query;
}

/* Returns -1 only when a pending Java exception makes the result unusable. */
static jint m3_long_binary(m3_long_reader *reader, jint low, jint high,
    jlong query, jboolean upper) {
  while (low < high) {
    jint middle = low + ((high - low) >> 1);
    jlong value = 0;
    if (!m3_long_at(reader, middle, &value)) return -1;
    if (m3_long_before(value, query, upper)) low = middle + 1;
    else high = middle;
  }
  return low;
}

/* Nondecreasing query run: exponential discovery then binary refinement.
 * A jump of delta key ordinals takes O(log(1 + delta)) probes; repeated
 * queries reuse the prior ordinal without inspecting any key cells.
 */
static jint m3_long_gallop(m3_long_reader *reader, jint from,
    jlong query, jboolean upper) {
  if (from == reader->length) return from;
  jlong first = 0;
  if (!m3_long_at(reader, from, &first)) return -1;
  if (!m3_long_before(first, query, upper)) return from;
  jint low = from + 1;
  jint jump = 1;
  while (low < reader->length) {
    jint remaining = reader->length - low;
    jint span = jump < remaining ? jump : remaining;
    jint high = low + span;
    jlong end_value = 0;
    if (!m3_long_at(reader, high - 1, &end_value)) return -1;
    if (m3_long_before(end_value, query, upper)) {
      low = high;
      jump = jump >= reader->length - jump ? reader->length : jump + jump;
    } else {
      return m3_long_binary(reader, low, high, query, upper);
    }
  }
  return reader->length;
}

/* Bounded-batch native path: one JNI transition for q queries. Sorted runs
 * use a cursor; falling queries revert to ordinary bisection. Query order,
 * duplicates, and detached output identity remain unchanged.
 */
JNIEXPORT jintArray JNICALL
Java_jdk_m3_arrays_LongCoordinateIndex_nativeBounds(
    JNIEnv *env, jclass owner, jlongArray input, jlongArray queries, jboolean upper) {
  (void)owner;
  if (input == NULL || queries == NULL) {
    m3_throw_null(env, input == NULL ? "null sorted long coordinates" : "null batch queries");
    return NULL;
  }
  jsize key_count = (*env)->GetArrayLength(env, input);
  jsize query_count = (*env)->GetArrayLength(env, queries);
  jintArray results = (*env)->NewIntArray(env, query_count);
  if (results == NULL) return NULL;
  m3_long_reader reader = {env, input, key_count, 0, 0, {0}};
  jlong query_region[M3_LONG_QUERY_BATCH];
  jint ordinal_region[M3_LONG_QUERY_BATCH];
  jlong previous = 0;
  jint hint = 0;
  jsize processed = 0;
  for (jsize first = 0; first < query_count;) {
    jsize count = query_count - first;
    if (count > M3_LONG_QUERY_BATCH) count = M3_LONG_QUERY_BATCH;
    (*env)->GetLongArrayRegion(env, queries, first, count, query_region);
    if ((*env)->ExceptionCheck(env)) return NULL;
    for (jsize i = 0; i < count; i++) {
      jlong query = query_region[i];
      if (processed == 0 || query < previous) {
        hint = m3_long_binary(&reader, 0, key_count, query, upper);
      } else if (query != previous) {
        hint = m3_long_gallop(&reader, hint, query, upper);
      }
      if ((*env)->ExceptionCheck(env)) return NULL;
      ordinal_region[i] = hint;
      previous = query;
      processed++;
    }
    (*env)->SetIntArrayRegion(env, results, first, count, ordinal_region);
    if ((*env)->ExceptionCheck(env)) return NULL;
    first += count;
  }
  return results;
}
