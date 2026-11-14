/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project */
/* SPDX-License-Identifier: Apache-2.0 */
#include <jni.h>
#include <stdint.h>
#include "m3_ncm_codebook.h"

_Static_assert(sizeof(jint) == sizeof(int32_t), "JNI int width must be 32 bits");
_Static_assert(sizeof(jdouble) == sizeof(double), "JNI double must be binary64");

static void throw_named(JNIEnv *env, const char *name, const char *message) {
  if ((*env)->ExceptionCheck(env)) return;
  jclass type = (*env)->FindClass(env, name);
  if (type == NULL) return;
  (void) (*env)->ThrowNew(env, type, message);
  (*env)->DeleteLocalRef(env, type);
}

static void throw_status(JNIEnv *env, m3_ncm_status status) {
  switch (status) {
    case M3_NCM_OK: break;
    case M3_NCM_OUT_OF_RANGE:
      throw_named(env, "java/lang/IndexOutOfBoundsException", "codebook batch range outside array");
      break;
    case M3_NCM_NONFINITE_INPUT:
      throw_named(env, "java/lang/IllegalArgumentException", "nonfinite codebook batch input");
      break;
    case M3_NCM_DISTANCE_OVERFLOW:
      throw_named(env, "java/lang/ArithmeticException", "squared codebook distance overflow");
      break;
    case M3_NCM_INVALID_ARGUMENT:
      throw_named(env, "java/lang/IllegalArgumentException", "invalid codebook batch shape");
      break;
    default:
      throw_named(env, "java/lang/IllegalStateException", "unknown codebook kernel status");
      break;
  }
}

JNIEXPORT jint JNICALL
Java_jdk_m3_precompute_NativePrecomputeOps_ncmCodebookAbiVersionNative(
    JNIEnv *env, jclass type) {
  (void) env;
  (void) type;
  return 1;
}

JNIEXPORT jintArray JNICALL
Java_jdk_m3_precompute_NativePrecomputeOps_nearestNcmCodesNative(
    JNIEnv *env, jclass type, jdoubleArray vectors_array, jint vector_offset, jint rows,
    jdoubleArray codebook_array, jint codebook_offset, jint code_count, jint dimensions) {
  (void) type;
  if (vectors_array == NULL || codebook_array == NULL) {
    throw_named(env, "java/lang/NullPointerException",
        vectors_array == NULL ? "vectors" : "codebook");
    return NULL;
  }
  jsize vector_length = (*env)->GetArrayLength(env, vectors_array);
  jsize codebook_length = (*env)->GetArrayLength(env, codebook_array);
  m3_ncm_status status = m3_ncm_validate_layout((size_t) vector_length,
      vector_offset, rows, (size_t) codebook_length, codebook_offset, code_count,
      dimensions, rows < 0 ? 0 : (size_t) rows);
  if (status != M3_NCM_OK) {
    throw_status(env, status);
    return NULL;
  }

  jdouble *vectors = NULL;
  jdouble *codebook = NULL;
  jint *output = NULL;
  jintArray result = NULL;
  if (rows > 0) {
    vectors = (*env)->GetDoubleArrayElements(env, vectors_array, NULL);
    if (vectors == NULL) goto cleanup;
  }
  if (code_count > 0) {
    codebook = (*env)->GetDoubleArrayElements(env, codebook_array, NULL);
    if (codebook == NULL) goto cleanup;
  }
  result = (*env)->NewIntArray(env, rows);
  if (result == NULL) goto cleanup;
  if (rows > 0) {
    output = (*env)->GetIntArrayElements(env, result, NULL);
    if (output == NULL) goto cleanup;
  }

  status = m3_ncm_nearest(vectors, (size_t) vector_length, vector_offset, rows,
      codebook, (size_t) codebook_length, codebook_offset, code_count, dimensions,
      (int32_t *) output, (size_t) rows);

cleanup:
  if (output != NULL) (*env)->ReleaseIntArrayElements(env, result, output,
      status == M3_NCM_OK ? 0 : JNI_ABORT);
  if (codebook != NULL) (*env)->ReleaseDoubleArrayElements(env, codebook_array, codebook, JNI_ABORT);
  if (vectors != NULL) (*env)->ReleaseDoubleArrayElements(env, vectors_array, vectors, JNI_ABORT);
  if (status != M3_NCM_OK) throw_status(env, status);
  if ((*env)->ExceptionCheck(env)) return NULL;
  return result;
}
