/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
#include <jni.h>
#include <stdint.h>
#include "m3_sparse_nonzero.h"

#define M3_NONZERO_REGION 1024

JNIEXPORT jint JNICALL
Java_jdk_m3_arrays_SparseLongIntArray_nativeCountNonZero(
    JNIEnv *env, jclass owner, jintArray input) {
  (void)owner;
  if (input == NULL) {
    jclass kind = (*env)->FindClass(env, "java/lang/NullPointerException");
    if (kind != NULL) {
      (void)(*env)->ThrowNew(env, kind, "null sparse primitive block");
      (*env)->DeleteLocalRef(env, kind);
    }
    return 0;
  }
  jsize length = (*env)->GetArrayLength(env, input);
  jint local[M3_NONZERO_REGION];
  jint at = 0;
  jint matched = 0;
  while (at < length) {
    jint take = length - at;
    if (take > M3_NONZERO_REGION) take = M3_NONZERO_REGION;
    (*env)->GetIntArrayRegion(env, input, at, take, local);
    if ((*env)->ExceptionCheck(env)) return 0;
    matched += (jint)m3_count_nonzero_i32((const int32_t *)local, (size_t)take);
    at += take;
  }
  return matched;
}
