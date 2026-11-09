/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
#include <jni.h>
#include <stdint.h>
#include <limits.h>
#include "m3_multiset_step.h"

#define M3_MULTI_REGION 1024
#define M3_MULTI_MAX_RESULT (INT32_MAX - 8)

typedef struct {
  jintArray array;
  jsize length;
  jsize pos;
  jsize cached_start;
  jsize cached_count;
  jint region[M3_MULTI_REGION];
} m3_int_reader;

typedef struct {
  jintArray array;
  jsize next;
  jsize size;
  jint region[M3_MULTI_REGION];
} m3_int_writer;

static void m3_throw(JNIEnv *env, const char *type, const char *message) {
  if ((*env)->ExceptionCheck(env)) return;
  jclass kind = (*env)->FindClass(env, type);
  if (kind != NULL) {
    (void)(*env)->ThrowNew(env, kind, message);
    (*env)->DeleteLocalRef(env, kind);
  }
}

static int m3_read_at(JNIEnv *env, m3_int_reader *reader, jint *value) {
  if (reader->pos >= reader->length) return 0;
  if (reader->cached_count == 0 || reader->pos < reader->cached_start
      || reader->pos - reader->cached_start >= reader->cached_count) {
    jsize start = (reader->pos / M3_MULTI_REGION) * M3_MULTI_REGION;
    jsize size = reader->length - start;
    if (size > M3_MULTI_REGION) size = M3_MULTI_REGION;
    (*env)->GetIntArrayRegion(env, reader->array, start, size, reader->region);
    if ((*env)->ExceptionCheck(env)) return 0;
    reader->cached_start = start;
    reader->cached_count = size;
  }
  *value = reader->region[reader->pos - reader->cached_start];
  return 1;
}

static int m3_flush(JNIEnv *env, m3_int_writer *writer) {
  if (writer->size == 0) return 1;
  (*env)->SetIntArrayRegion(env, writer->array,
      writer->next, writer->size, writer->region);
  if ((*env)->ExceptionCheck(env)) return 0;
  writer->next += writer->size;
  writer->size = 0;
  return 1;
}

/* Return -1 on error (pending Java exception), otherwise exact count.
 * The first pass never allocates output. The second uses a 1024-int writer.
 */
static jlong m3_merge_pass(JNIEnv *env, jintArray left, jintArray right,
    int mode, jintArray result) {
  m3_int_reader a = {left, (*env)->GetArrayLength(env, left), 0, 0, 0, {0}};
  m3_int_reader b = {right, (*env)->GetArrayLength(env, right), 0, 0, 0, {0}};
  m3_int_writer out = {result, 0, 0, {0}};
  jlong emitted = 0;
  while (a.pos < a.length || b.pos < b.length) {
    jint av = 0, bv = 0;
    if (a.pos < a.length && !m3_read_at(env, &a, &av)) return -1;
    if (b.pos < b.length && !m3_read_at(env, &b, &bv)) return -1;
    m3_multiset_action step;
    if (!m3_multiset_step(mode, a.pos < a.length, (int32_t)av,
            b.pos < b.length, (int32_t)bv, &step)) {
      m3_throw(env, "java/lang/IllegalArgumentException", "invalid multiset operation");
      return -1;
    }
    a.pos += step.take_left;
    b.pos += step.take_right;
    if (step.emit != 0) {
      if (emitted == M3_MULTI_MAX_RESULT) {
        m3_throw(env, "java/lang/OutOfMemoryError", "multiset result exceeds Java array limit");
        return -1;
      }
      if (result != NULL) {
        out.region[out.size++] = (jint)step.value;
        if (out.size == M3_MULTI_REGION && !m3_flush(env, &out)) return -1;
      }
      emitted++;
    }
  }
  if (result != NULL && !m3_flush(env, &out)) return -1;
  return emitted;
}

JNIEXPORT jintArray JNICALL
Java_jdk_m3_arrays_SortedIntMultisets_nativeMerge(
    JNIEnv *env, jclass owner, jintArray left, jintArray right, jint mode) {
  (void)owner;
  if (left == NULL || right == NULL) {
    m3_throw(env, "java/lang/NullPointerException", "null primitive multiset lane");
    return NULL;
  }
  if (mode < M3_MULTI_UNION || mode > M3_MULTI_SYMDIFF) {
    m3_throw(env, "java/lang/IllegalArgumentException", "invalid multiset operation");
    return NULL;
  }
  jlong length = m3_merge_pass(env, left, right, mode, NULL);
  if (length < 0 || (*env)->ExceptionCheck(env)) return NULL;
  jintArray out = (*env)->NewIntArray(env, (jsize)length);
  if (out == NULL) return NULL;
  jlong populated = m3_merge_pass(env, left, right, mode, out);
  if (populated < 0 || (*env)->ExceptionCheck(env)) return NULL;
  if (populated != length) {
    m3_throw(env, "java/util/ConcurrentModificationException",
        "input lanes changed during native multiset merge");
    return NULL;
  }
  return out;
}
