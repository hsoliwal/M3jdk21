/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
#include <jni.h>
#include <stdint.h>
#include <limits.h>

#define IDS_PER_CHUNK 1024
#define BYTES_PER_CHUNK (IDS_PER_CHUNK * 10)

static int encoded_size(uint64_t value) {
  int size = 1;
  while (value >= UINT64_C(128)) {
    value >>= 7;
    size++;
  }
  return size;
}

/* A primitive batch kernel only: no shared JVM pointers, no cached Java array address. */
JNIEXPORT jbyteArray JNICALL
Java_com_m3_file_M3FmtVarints_packNative(
    JNIEnv *env, jclass owner, jlongArray ids) {
  jlong batch[IDS_PER_CHUNK];
  jbyte encoded[BYTES_PER_CHUNK];
  jsize count, offset, total = 0;
  jbyteArray result;
  (void)owner;
  if (ids == NULL) {
    jclass npe = (*env)->FindClass(env, "java/lang/NullPointerException");
    if (npe != NULL) (*env)->ThrowNew(env, npe, "ids");
    return NULL;
  }
  count = (*env)->GetArrayLength(env, ids);
  for (offset = 0; offset < count;) {
    jsize limit = count - offset < IDS_PER_CHUNK ? count - offset : IDS_PER_CHUNK;
    (*env)->GetLongArrayRegion(env, ids, offset, limit, batch);
    if ((*env)->ExceptionCheck(env)) return NULL;
    for (jsize i = 0; i < limit; i++) {
      int width = encoded_size((uint64_t)batch[i]);
      if (total > INT32_MAX - width) {
        jclass oom = (*env)->FindClass(env, "java/lang/OutOfMemoryError");
        if (oom != NULL) (*env)->ThrowNew(env, oom, "packed IDs exceed Java byte array");
        return NULL;
      }
      total += width;
    }
    offset += limit;
  }
  result = (*env)->NewByteArray(env, total);
  if (result == NULL) return NULL;
  jsize position = 0;
  for (offset = 0; offset < count;) {
    jsize limit = count - offset < IDS_PER_CHUNK ? count - offset : IDS_PER_CHUNK;
    jsize used = 0;
    (*env)->GetLongArrayRegion(env, ids, offset, limit, batch);
    if ((*env)->ExceptionCheck(env)) return NULL;
    for (jsize i = 0; i < limit; i++) {
      uint64_t value = (uint64_t)batch[i];
      while (value >= UINT64_C(128)) {
        encoded[used++] = (jbyte)((value & UINT64_C(127)) | UINT64_C(128));
        value >>= 7;
      }
      encoded[used++] = (jbyte)value;
    }
    if (position > total - used) {
      jclass failure = (*env)->FindClass(env, "java/lang/IllegalStateException");
      if (failure != NULL) (*env)->ThrowNew(env, failure, "mutable input changed encoding width");
      return NULL;
    }
    (*env)->SetByteArrayRegion(env, result, position, used, encoded);
    if ((*env)->ExceptionCheck(env)) return NULL;
    position += used;
    offset += limit;
  }
  if (position != total) {
    jclass failure = (*env)->FindClass(env, "java/lang/IllegalStateException");
    if (failure != NULL) (*env)->ThrowNew(env, failure, "mutable input changed encoding width");
    return NULL;
  }
  return result;
}
