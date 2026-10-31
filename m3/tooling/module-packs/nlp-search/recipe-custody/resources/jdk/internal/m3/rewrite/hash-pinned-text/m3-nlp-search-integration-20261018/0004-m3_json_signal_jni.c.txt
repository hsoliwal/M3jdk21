// SPDX-License-Identifier: Apache-2.0
#include <jni.h>
#include <stdint.h>
#include <stddef.h>

JNIEXPORT jint JNICALL
Java_com_m3_file_M3JsonStructuralNative_nativeJsonSignalAbiVersion(
    JNIEnv *env, jclass type) {
  (void)env;
  (void)type;
  return 1;
}

JNIEXPORT jint JNICALL
Java_com_m3_file_M3JsonStructuralNative_nativeClassifyJsonBytes(
    JNIEnv *env,
    jclass type,
    jobject source,
    jint source_offset,
    jint length,
    jobject target,
    jint target_offset) {
  (void)type;
  if (source == NULL
      || target == NULL
      || source_offset < 0
      || target_offset < 0
      || length < 0) {
    return -1;
  }

  jlong source_capacity = (*env)->GetDirectBufferCapacity(env, source);
  jlong target_capacity = (*env)->GetDirectBufferCapacity(env, target);
  if (source_capacity < 0 || target_capacity < 0
      || (jlong)source_offset > source_capacity
      || (jlong)target_offset > target_capacity
      || (jlong)length > source_capacity - (jlong)source_offset
      || (jlong)length > target_capacity - (jlong)target_offset) {
    return -1;
  }
  if (length == 0) return 0;

  uint8_t *source_raw = (uint8_t *)(*env)->GetDirectBufferAddress(env, source);
  uint8_t *target_raw = (uint8_t *)(*env)->GetDirectBufferAddress(env, target);
  if (source_raw == NULL || target_raw == NULL) return -1;

  uintptr_t source_base = (uintptr_t)source_raw;
  uintptr_t target_base = (uintptr_t)target_raw;
  uintptr_t source_delta = (uintptr_t)(uint32_t)source_offset;
  uintptr_t target_delta = (uintptr_t)(uint32_t)target_offset;
  uintptr_t span = (uintptr_t)(uint32_t)length;
  if (source_base > UINTPTR_MAX - source_delta
      || target_base > UINTPTR_MAX - target_delta) {
    return -1;
  }
  uintptr_t source_start = source_base + source_delta;
  uintptr_t target_start = target_base + target_delta;
  if (source_start > UINTPTR_MAX - span || target_start > UINTPTR_MAX - span) return -1;
  uintptr_t source_end = source_start + span;
  uintptr_t target_end = target_start + span;
  if (source_start < target_end && target_start < source_end) return -2;

  const uint8_t *input = source_raw + (size_t)(uint32_t)source_offset;
  uint8_t *output = target_raw + (size_t)(uint32_t)target_offset;
  for (jint index = 0; index < length; ++index) {
    uint8_t value = input[(size_t)(uint32_t)index];
    uint8_t flags = 0;
    if (value == (uint8_t)'{'
        || value == (uint8_t)'}'
        || value == (uint8_t)'['
        || value == (uint8_t)']'
        || value == (uint8_t)':'
        || value == (uint8_t)',') {
      flags |= (uint8_t)1;
    }
    if (value == (uint8_t)'"') flags |= (uint8_t)(1u << 1);
    if (value == (uint8_t)'\\') flags |= (uint8_t)(1u << 2);
    if (value == (uint8_t)' '
        || value == (uint8_t)'\t'
        || value == (uint8_t)'\r'
        || value == (uint8_t)'\n') {
      flags |= (uint8_t)(1u << 3);
    }
    if (value == (uint8_t)'\n') flags |= (uint8_t)(1u << 4);
    if ((value & (uint8_t)0x80u) != 0) flags |= (uint8_t)(1u << 5);
    if (value < (uint8_t)0x20u) flags |= (uint8_t)(1u << 6);
    output[(size_t)(uint32_t)index] = flags;
  }
  return 0;
}
