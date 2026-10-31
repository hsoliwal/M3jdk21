// SPDX-License-Identifier: Apache-2.0
#include <jni.h>
#include <stdint.h>
#include <string.h>
#include <stddef.h>

static uint16_t m3_be16(const uint8_t *p) {
  return (uint16_t)(((uint16_t)p[0] << 8) | (uint16_t)p[1]);
}

static void m3_throw(JNIEnv *env, const char *type, const char *message) {
  if ((*env)->ExceptionCheck(env)) return;
  jclass cls = (*env)->FindClass(env, type);
  if (cls != NULL) (void)(*env)->ThrowNew(env, cls, message);
}

static uint32_t m3_be32(const uint8_t *p) {
  return ((uint32_t)p[0] << 24)
      | ((uint32_t)p[1] << 16)
      | ((uint32_t)p[2] << 8)
      | (uint32_t)p[3];
}

static uint64_t m3_be64(const uint8_t *p) {
  return ((uint64_t)p[0] << 56)
      | ((uint64_t)p[1] << 48)
      | ((uint64_t)p[2] << 40)
      | ((uint64_t)p[3] << 32)
      | ((uint64_t)p[4] << 24)
      | ((uint64_t)p[5] << 16)
      | ((uint64_t)p[6] << 8)
      | (uint64_t)p[7];
}

static int m3_contains(
    const uint8_t *source,
    size_t length,
    const uint8_t *needle,
    size_t needle_length) {
  if (needle_length == 0U || needle_length > length) return 0;
  size_t limit = length - needle_length;
  for (size_t start = 0U; start <= limit; start++) {
    if (source[start] == needle[0]
        && memcmp(source + start, needle, needle_length) == 0) {
      return 1;
    }
  }
  return 0;
}


JNIEXPORT jcharArray JNICALL
Java_com_m3_fs_M3NativeContent_nativeMaterializeUtf16Segments(
    JNIEnv *env,
    jclass type,
    jobjectArray parents,
    jobject parent_indexes,
    jobject parent_starts,
    jobject lengths,
    jint segment_count,
    jint total_chars) {
  (void)type;
  if (parents == NULL
      || parent_indexes == NULL
      || parent_starts == NULL
      || lengths == NULL
      || segment_count < 0
      || total_chars < 0) {
    m3_throw(env, "java/lang/IllegalArgumentException", "segmented UTF-16 geometry");
    return NULL;
  }

  jsize parent_count = (*env)->GetArrayLength(env, parents);
  jlong index_capacity = (*env)->GetDirectBufferCapacity(env, parent_indexes);
  jlong start_capacity = (*env)->GetDirectBufferCapacity(env, parent_starts);
  jlong length_capacity = (*env)->GetDirectBufferCapacity(env, lengths);
  const uint8_t *index_bytes =
      (const uint8_t *)(*env)->GetDirectBufferAddress(env, parent_indexes);
  const uint8_t *start_bytes =
      (const uint8_t *)(*env)->GetDirectBufferAddress(env, parent_starts);
  const uint8_t *length_bytes =
      (const uint8_t *)(*env)->GetDirectBufferAddress(env, lengths);
  if (parent_count < 1
      || index_capacity < segment_count
      || start_capacity < segment_count
      || length_capacity < segment_count
      || index_bytes == NULL
      || start_bytes == NULL
      || length_bytes == NULL) {
    m3_throw(env, "java/lang/IllegalArgumentException", "direct UTF-16 geometry");
    return NULL;
  }

  jcharArray result = (*env)->NewCharArray(env, total_chars);
  if (result == NULL) return NULL;
  jchar *out = (*env)->GetCharArrayElements(env, result, NULL);
  if (out == NULL) return NULL;

  jint cursor = 0;
  for (jint segment = 0; segment < segment_count; segment++) {
    uint32_t parent = m3_be32(index_bytes + (size_t)(uint32_t)segment * 4U);
    uint32_t start = m3_be32(start_bytes + (size_t)(uint32_t)segment * 4U);
    uint32_t count = m3_be32(length_bytes + (size_t)(uint32_t)segment * 4U);
    if (parent >= (uint32_t)parent_count
        || count > (uint32_t)(total_chars - cursor)) {
      (*env)->ReleaseCharArrayElements(env, result, out, JNI_ABORT);
      m3_throw(env, "java/lang/IllegalArgumentException", "UTF-16 segment range");
      return NULL;
    }

    jobject parent_buffer =
        (*env)->GetObjectArrayElement(env, parents, (jsize)parent);
    if (parent_buffer == NULL) {
      (*env)->ReleaseCharArrayElements(env, result, out, JNI_ABORT);
      return NULL;
    }
    jlong parent_capacity =
        (*env)->GetDirectBufferCapacity(env, parent_buffer);
    const uint8_t *parent_bytes =
        (const uint8_t *)(*env)->GetDirectBufferAddress(env, parent_buffer);
    uint64_t end_chars = (uint64_t)start + (uint64_t)count;
    if (parent_capacity < 0
        || parent_bytes == NULL
        || end_chars > (uint64_t)parent_capacity / 2U) {
      (*env)->DeleteLocalRef(env, parent_buffer);
      (*env)->ReleaseCharArrayElements(env, result, out, JNI_ABORT);
      m3_throw(env, "java/lang/IllegalArgumentException", "UTF-16 parent range");
      return NULL;
    }
    for (uint32_t offset = 0U; offset < count; offset++) {
      out[cursor++] =
          (jchar)m3_be16(
              parent_bytes + ((size_t)start + (size_t)offset) * 2U);
    }
    (*env)->DeleteLocalRef(env, parent_buffer);
  }

  if (cursor != total_chars) {
    (*env)->ReleaseCharArrayElements(env, result, out, JNI_ABORT);
    m3_throw(env, "java/lang/IllegalArgumentException", "UTF-16 terminal length");
    return NULL;
  }
  (*env)->ReleaseCharArrayElements(env, result, out, 0);
  return result;
}

JNIEXPORT jint JNICALL
Java_com_m3_fs_M3NativeContent_nativeEqualDirect(
    JNIEnv *env,
    jclass type,
    jobject left,
    jint left_offset,
    jobject right,
    jint right_offset,
    jint length) {
  (void)type;
  if (left == NULL
      || right == NULL
      || left_offset < 0
      || right_offset < 0
      || length < 0) {
    return -2;
  }

  jlong left_capacity =
      (*env)->GetDirectBufferCapacity(env, left);
  jlong right_capacity =
      (*env)->GetDirectBufferCapacity(env, right);
  if (left_capacity < 0 || right_capacity < 0) {
    return -2;
  }

  jlong left_at = (jlong)left_offset;
  jlong right_at = (jlong)right_offset;
  jlong span = (jlong)length;
  if (left_at > left_capacity
      || right_at > right_capacity
      || span > left_capacity - left_at
      || span > right_capacity - right_at) {
    return -2;
  }
  if (length == 0) return 1;

  void *left_raw = (*env)->GetDirectBufferAddress(env, left);
  void *right_raw = (*env)->GetDirectBufferAddress(env, right);
  if (left_raw == NULL || right_raw == NULL) return -2;

  const uint8_t *a =
      (const uint8_t *)left_raw
      + (size_t)(uint32_t)left_offset;
  const uint8_t *b =
      (const uint8_t *)right_raw
      + (size_t)(uint32_t)right_offset;
  return memcmp(
             a,
             b,
             (size_t)(uint32_t)length)
          == 0
      ? 1
      : 0;
}

JNIEXPORT jint JNICALL
Java_com_m3_fs_M3NativeContent_nativeGrepSegments(
    JNIEnv *env,
    jclass type,
    jobject corpus,
    jobject offsets,
    jobject lengths,
    jint segment_count,
    jbyteArray needle,
    jintArray output_rows) {
  (void)type;
  if (corpus == NULL
      || offsets == NULL
      || lengths == NULL
      || needle == NULL
      || output_rows == NULL
      || segment_count < 0) {
    return -2;
  }

  jlong corpus_capacity = (*env)->GetDirectBufferCapacity(env, corpus);
  jlong offset_capacity = (*env)->GetDirectBufferCapacity(env, offsets);
  jlong length_capacity = (*env)->GetDirectBufferCapacity(env, lengths);
  jsize needle_length = (*env)->GetArrayLength(env, needle);
  jsize output_length = (*env)->GetArrayLength(env, output_rows);
  if (corpus_capacity < 0
      || offset_capacity < (jlong)segment_count + 1
      || length_capacity < (jlong)segment_count
      || needle_length <= 0
      || output_length < segment_count) {
    return -2;
  }

  const uint8_t *corpus_bytes =
      (const uint8_t *)(*env)->GetDirectBufferAddress(env, corpus);
  const uint8_t *offset_bytes =
      (const uint8_t *)(*env)->GetDirectBufferAddress(env, offsets);
  const uint8_t *length_bytes =
      (const uint8_t *)(*env)->GetDirectBufferAddress(env, lengths);
  if (corpus_bytes == NULL || offset_bytes == NULL || length_bytes == NULL) return -2;

  jbyte *needle_values = (*env)->GetByteArrayElements(env, needle, NULL);
  if (needle_values == NULL) return -2;
  jint *output_values = (*env)->GetIntArrayElements(env, output_rows, NULL);
  if (output_values == NULL) {
    (*env)->ReleaseByteArrayElements(env, needle, needle_values, JNI_ABORT);
    return -2;
  }

  jint matches = 0;
  uint64_t prior = m3_be64(offset_bytes);
  if (prior != 0U) {
    (*env)->ReleaseIntArrayElements(env, output_rows, output_values, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, needle, needle_values, JNI_ABORT);
    return -2;
  }

  for (jint segment = 0; segment < segment_count; segment++) {
    uint64_t from = m3_be64(offset_bytes + (size_t)(uint32_t)segment * 8U);
    uint64_t to = m3_be64(offset_bytes + ((size_t)(uint32_t)segment + 1U) * 8U);
    uint32_t length = m3_be32(length_bytes + (size_t)(uint32_t)segment * 4U);
    if (from != prior
        || to < from
        || to - from != (uint64_t)length
        || to > (uint64_t)corpus_capacity) {
      (*env)->ReleaseIntArrayElements(env, output_rows, output_values, JNI_ABORT);
      (*env)->ReleaseByteArrayElements(env, needle, needle_values, JNI_ABORT);
      return -2;
    }
    if (m3_contains(
            corpus_bytes + (size_t)from,
            (size_t)length,
            (const uint8_t *)needle_values,
            (size_t)needle_length)) {
      output_values[matches++] = segment;
    }
    prior = to;
  }

  if (prior != (uint64_t)corpus_capacity) {
    (*env)->ReleaseIntArrayElements(env, output_rows, output_values, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, needle, needle_values, JNI_ABORT);
    return -2;
  }

  (*env)->ReleaseIntArrayElements(env, output_rows, output_values, 0);
  (*env)->ReleaseByteArrayElements(env, needle, needle_values, JNI_ABORT);
  return matches;
}
