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
Java_com_m3_arrays_M3LongCoordinateIndex_nativeBound(
    JNIEnv *env, jclass owner, jlongArray input, jlong key, jboolean upper) {
  (void)owner;
  if (input == NULL) {
    m3_throw_null(env, "null sorted long coordinates");
    return 0;
  }
  return m3_bound_region(env, input, (*env)->GetArrayLength(env, input), key, upper);
}

/* Bounded-batch native path: one JNI transition for q queries. All inputs
 * are read through GetLongArrayRegion; the output is detached and written
 * through SetIntArrayRegion in batches, with no retained native borrow.
 */
JNIEXPORT jintArray JNICALL
Java_com_m3_arrays_M3LongCoordinateIndex_nativeBounds(
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
  jlong query_region[M3_LONG_QUERY_BATCH];
  jint ordinal_region[M3_LONG_QUERY_BATCH];
  for (jsize first = 0; first < query_count;) {
    jsize count = query_count - first;
    if (count > M3_LONG_QUERY_BATCH) count = M3_LONG_QUERY_BATCH;
    (*env)->GetLongArrayRegion(env, queries, first, count, query_region);
    if ((*env)->ExceptionCheck(env)) return NULL;
    for (jsize i = 0; i < count; i++) {
      ordinal_region[i] = m3_bound_region(env, input, key_count, query_region[i], upper);
      if ((*env)->ExceptionCheck(env)) return NULL;
    }
    (*env)->SetIntArrayRegion(env, results, first, count, ordinal_region);
    if ((*env)->ExceptionCheck(env)) return NULL;
    first += count;
  }
  return results;
}
