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
JNIEXPORT jint JNICALL
Java_com_m3_arrays_M3LongCoordinateIndex_nativeBound(
    JNIEnv *env, jclass owner, jlongArray input, jlong key, jboolean upper) {
  (void)owner;
  if (input == NULL) {
    jclass kind = (*env)->FindClass(env, "java/lang/NullPointerException");
    if (kind != NULL) {
      (void)(*env)->ThrowNew(env, kind, "null sorted long coordinates");
      (*env)->DeleteLocalRef(env, kind);
    }
    return 0;
  }
  jsize length = (*env)->GetArrayLength(env, input);
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
  jlong window[M3_LONG_BOUND_REGION];
  jsize count = high - low;
  (*env)->GetLongArrayRegion(env, input, low, count, window);
  if ((*env)->ExceptionCheck(env)) return 0;
  /* jlong and int64_t need not have the same effective C type on every ABI.
   * Copy signed values, never type-pun a JNI region through an incompatible pointer.
   */
  int64_t signed_values[M3_LONG_BOUND_REGION];
  for (jsize i = 0; i < count; i++) signed_values[i] = (int64_t)window[i];
  size_t ordinal = m3_long_coordinate_bound(signed_values,
      (size_t)count, (int64_t)key, upper != JNI_FALSE);
  return low + (jint)ordinal;
}
