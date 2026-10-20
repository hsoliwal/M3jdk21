// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
// Exact mapped-file KMP kernel and helper from pinned indexstring_jni.c; JNI owner mapped.
#include <jni.h>
#include <stdint.h>
#include <stdlib.h>
#include <limits.h>
static uint32_t read_be32(const uint8_t *p) {
  return ((uint32_t)p[0] << 24)
      | ((uint32_t)p[1] << 16)
      | ((uint32_t)p[2] << 8)
      | (uint32_t)p[3];
}

static jint mindex_read_mapped_handle(
    const uint8_t *data, jint token_base, jint token_width, jint token_index) {
  const uint8_t *p = data + token_base + (int64_t)token_index * token_width;
  if (token_width == 1) return p[0];
  if (token_width == 2) return ((jint)p[0] << 8) | p[1];
  return (jint)read_be32(p);
}

JNIEXPORT jint JNICALL
Java_com_m3_file_M3FileNative_nativeMappedFileTokenIndexOf(
    JNIEnv *env,
    jclass clazz,
    jobject data_buffer,
    jint token_base,
    jint token_width,
    jint token_count,
    jintArray pattern_array,
    jint from_token) {
  (void)clazz;
  uint8_t *data = (uint8_t *)(*env)->GetDirectBufferAddress(env, data_buffer);
  jlong capacity64 = (*env)->GetDirectBufferCapacity(env, data_buffer);
  if (data == NULL || capacity64 < 0 || capacity64 > INT32_MAX) return -2;
  jsize plen = (*env)->GetArrayLength(env, pattern_array);
  if (plen == 0) return from_token;
  if (token_base < 0 || token_count < 0 || from_token < 0 || from_token > token_count
      || (token_width != 1 && token_width != 2 && token_width != 4)
      || plen > token_count - from_token
      || (int64_t)token_base + (int64_t)token_count * token_width > capacity64) return -2;

  jint *pattern = (jint *)(*env)->GetPrimitiveArrayCritical(env, pattern_array, NULL);
  if (pattern == NULL) return -2;
  jint *failure = (jint *)calloc((size_t)plen, sizeof(jint));
  if (failure == NULL) {
    (*env)->ReleasePrimitiveArrayCritical(env, pattern_array, pattern, JNI_ABORT);
    return -2;
  }
  for (jint i = 1, matched = 0; i < plen;) {
    if (pattern[i] == pattern[matched]) failure[i++] = ++matched;
    else if (matched > 0) matched = failure[matched - 1];
    else i++;
  }
  jint result = -1;
  for (jint token = from_token, matched = 0; token < token_count; token++) {
    jint value = mindex_read_mapped_handle(data, token_base, token_width, token);
    while (matched > 0 && value != pattern[matched]) matched = failure[matched - 1];
    if (value == pattern[matched]) matched++;
    if (matched == plen) { result = token - plen + 1; break; }
  }
  free(failure);
  (*env)->ReleasePrimitiveArrayCritical(env, pattern_array, pattern, JNI_ABORT);
  return result;
}

