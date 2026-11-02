/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
#include <jni.h>
#include <stdint.h>
#include "m3_int_lane_search.h"

/* Region copies, not GetPrimitiveArrayCritical/GetIntArrayElements: zero
 * outstanding native borrows and no native-owned Java object references.
 */
#define M3_INT_LANE_CHUNK 1024

static void invalid(JNIEnv *env, const char *message) {
  jclass exception = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
  if (exception != NULL) {
    (void)(*env)->ThrowNew(env, exception, message);
    (*env)->DeleteLocalRef(env, exception);
  }
}

JNIEXPORT jint JNICALL
Java_com_m3_arrays_M3IntLaneAlgorithms_nativeBound(
    JNIEnv *env, jclass owner, jintArray lane, jint from, jint to, jint key, jboolean upper) {
  (void)owner;
  if (lane == NULL) {
    invalid(env, "null sorted lane");
    return 0;
  }
  jsize length = (*env)->GetArrayLength(env, lane);
  if (from < 0 || to < from || to > length) {
    invalid(env, "sorted lane range");
    return 0;
  }

  jint low = from;
  jint high = to;
  while (high - low > M3_INT_LANE_CHUNK) {
    jint middle = low + ((high - low) >> 1);
    jint value = 0;
    (*env)->GetIntArrayRegion(env, lane, middle, 1, &value);
    if ((*env)->ExceptionCheck(env)) return 0;
    if (upper != JNI_FALSE ? value <= key : value < key) low = middle + 1;
    else high = middle;
  }
  if (low == high) return low;
  jint window[M3_INT_LANE_CHUNK];
  jsize count = high - low;
  (*env)->GetIntArrayRegion(env, lane, low, count, window);
  if ((*env)->ExceptionCheck(env)) return 0;
  size_t offset = m3_int_lane_bound((const int32_t *)window, (size_t)count,
      (int32_t)key, upper != JNI_FALSE);
  return low + (jint)offset;
}
