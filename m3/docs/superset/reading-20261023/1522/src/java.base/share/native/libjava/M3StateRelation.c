/* M3JDK21 adaptation: C/JNI/header identifiers only; Synexia remains the source owner. */
/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0 */
#include "m3_state_relation.h"
#include <jni.h>
#include <string.h>

<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
/* Only called for nonzero state masks; the builtin is undefined at zero.
 * The portable lane has at most six tests, independent of the highest set bit. */
static unsigned int relation_trailing_zeros(uint64_t bits) {
#if (defined(__GNUC__) || defined(__clang__)) && !defined(M3_STATE_RELATION_PORTABLE_CTZ)
  return (unsigned int) __builtin_ctzll((unsigned long long) bits);
#else
  unsigned int count = 0;
  if ((bits & UINT64_C(0xffffffff)) == 0) { count += 32; bits >>= 32; }
  if ((bits & UINT64_C(0xffff)) == 0) { count += 16; bits >>= 16; }
  if ((bits & UINT64_C(0xff)) == 0) { count += 8; bits >>= 8; }
  if ((bits & UINT64_C(0xf)) == 0) { count += 4; bits >>= 4; }
  if ((bits & UINT64_C(0x3)) == 0) { count += 2; bits >>= 2; }
  if ((bits & UINT64_C(0x1)) == 0) ++count;
  return count;
#endif
}


=======
#if !defined(M3_RELATION_PORTABLE_SCAN) && (defined(__GNUC__) || defined(__clang__))
/* Reuse the existing regex atom bit-scan on nonzero masks. Other compilers keep
 * this owner's original shift loop, avoiding repeated portable prefix scans. */
static uint32_t relation_lowest_state(uint64_t bits) {
  return (uint32_t)__builtin_ctzll((unsigned long long)bits);
}

int m3_state_relation_compose(const uint64_t *left, const uint64_t *right,
                                 uint64_t *output, size_t states) {
  uint64_t temporary[64];
  uint64_t mask;
  uint64_t ceiling = 0;
  uint64_t previous = 0;
  uint64_t value = 0;
  if (left == NULL || right == NULL || output == NULL || states == 0 || states > 64) {
    return -1;
  }
  mask = states == 64 ? UINT64_MAX : (UINT64_C(1) << states) - UINT64_C(1);
  for (size_t row = 0; row < states; ++row) {
    if (((left[row] | right[row]) & ~mask) != 0) return -1;
    ceiling |= right[row];
  }
  for (size_t row = 0; row < states; ++row) {
    /* No source row is written before complete validation/computation, preserving aliases.
     * Repeated frontiers reuse an exact invocation-local result, never a hash/filter hit. */
    const uint64_t active = left[row];
    if (row == 0 || active != previous) {
      uint64_t bits = active;
      /* The full incoming frontier selects exactly the precomputed union. */
      value = active == mask ? ceiling : 0;
      while (bits != 0 && value != ceiling) {
        value |= right[relation_lowest_state(bits)];
        bits &= bits - UINT64_C(1);
      }

      previous = active;
    }
    temporary[row] = value;
  }
  memcpy(output, temporary, states * sizeof(uint64_t));
  return 0;
}

#else
/* Exact original composition body on compilers without the intrinsic. */
>>>>>>> d4a1c2d6844e8cc636dcc25ba98c85eb9c437971
int m3_state_relation_compose(const uint64_t *left, const uint64_t *right,
                                 uint64_t *output, size_t states) {
  uint64_t temporary[64];
  uint64_t mask;
  uint64_t ceiling = 0;
  uint64_t previous = 0;
  uint64_t value = 0;
  if (left == NULL || right == NULL || output == NULL || states == 0 || states > 64) {
    return -1;
  }
  mask = states == 64 ? UINT64_MAX : (UINT64_C(1) << states) - UINT64_C(1);
  for (size_t row = 0; row < states; ++row) {
    if (((left[row] | right[row]) & ~mask) != 0) return -1;
    ceiling |= right[row];
  }
  for (size_t row = 0; row < states; ++row) {
    /* No source row is written before complete validation/computation, preserving aliases.
     * Repeated frontiers reuse an exact invocation-local result, never a hash/filter hit. */
    const uint64_t active = left[row];
    if (row == 0 || active != previous) {
      uint64_t bits = active;
      value = 0;
      while (bits != 0 && value != ceiling) {
        value |= right[relation_trailing_zeros(bits)];
        bits &= bits - UINT64_C(1);
      }
      previous = active;
    }
    temporary[row] = value;
  }
  memcpy(output, temporary, states * sizeof(uint64_t));
  return 0;
}

#endif

static void relation_error(JNIEnv *env, const char *type, const char *message) {
  jclass exception = (*env)->FindClass(env, type);
  if (exception != NULL) {
    (void) (*env)->ThrowNew(env, exception, message);
    (*env)->DeleteLocalRef(env, exception);
  }
}

JNIEXPORT jlongArray JNICALL
Java_jdk_internal_mindex_M3StateRelation_composeNative(
    JNIEnv *env, jclass owner, jlongArray left, jlongArray right) {
  /* Reuse one JNI transfer lane; never type-pun jlong as uint64_t. */
  jlong transfer[64];
  uint64_t lhs[64];
  uint64_t rhs[64];
  jsize states;
  jlongArray answer;
  (void) owner;
  _Static_assert(sizeof(jlong) == sizeof(uint64_t), "JNI long must be 64 bits");
  if (left == NULL || right == NULL) {
    relation_error(env, "java/lang/NullPointerException", "null relation rows");
    return NULL;
  }
  states = (*env)->GetArrayLength(env, left);
  if (states < 1 || states > 64 || states != (*env)->GetArrayLength(env, right)) {
    relation_error(env, "java/lang/IllegalArgumentException", "invalid relation dimensions");
    return NULL;
  }
  (*env)->GetLongArrayRegion(env, left, 0, states, transfer);
  if ((*env)->ExceptionCheck(env)) return NULL;
  memcpy(lhs, transfer, (size_t) states * sizeof(uint64_t));
  (*env)->GetLongArrayRegion(env, right, 0, states, transfer);
  if ((*env)->ExceptionCheck(env)) return NULL;
  memcpy(rhs, transfer, (size_t) states * sizeof(uint64_t));
  /* The existing kernel validates and stages every row before any aliased output write. */
  if (m3_state_relation_compose(lhs, rhs, lhs, (size_t) states) != 0) {
    relation_error(env, "java/lang/IllegalArgumentException", "foreign relation state bits");
    return NULL;
  }
  memcpy(transfer, lhs, (size_t) states * sizeof(uint64_t));
  answer = (*env)->NewLongArray(env, states);
  if (answer == NULL) return NULL;
  (*env)->SetLongArrayRegion(env, answer, 0, states, transfer);
  if ((*env)->ExceptionCheck(env)) return NULL;
  return answer;
}

/* Reuse the alias-safe composition atom; all validation precedes external output writes. */
int m3_state_relation_repeat(const uint64_t *rows, uint64_t *output,
                                size_t states, uint64_t count) {
  uint64_t power[64];
  uint64_t result[64];
  uint64_t mask;
  if (rows == NULL || output == NULL || states == 0 || states > 64) return -1;
  mask = states == 64 ? UINT64_MAX : (UINT64_C(1) << states) - UINT64_C(1);
  for (size_t row = 0; row < states; ++row) {
    if ((rows[row] & ~mask) != 0) return -1;
    power[row] = rows[row];
    result[row] = UINT64_C(1) << row;
  }
  for (uint64_t remaining = count; remaining != 0; remaining >>= 1) {
    if ((remaining & UINT64_C(1)) != 0
        && m3_state_relation_compose(result, power, result, states) != 0) return -1;
    if (remaining > 1
        && m3_state_relation_compose(power, power, power, states) != 0) return -1;
  }
  memcpy(output, result, states * sizeof(uint64_t));
  return 0;
}

JNIEXPORT jlongArray JNICALL
Java_jdk_internal_mindex_M3StateRelation_repeatNative(
    JNIEnv *env, jclass owner, jlongArray rows, jlong count) {
  jlong transfer[64];
  uint64_t bits[64];
  jsize states;
  jlongArray answer;
  (void) owner;
  _Static_assert(sizeof(jlong) == sizeof(uint64_t), "JNI long must be 64 bits");
  if (rows == NULL) {
    relation_error(env, "java/lang/NullPointerException", "null relation rows");
    return NULL;
  }
  if (count < 0) {
    relation_error(env, "java/lang/IllegalArgumentException", "negative repeat count");
    return NULL;
  }
  states = (*env)->GetArrayLength(env, rows);
  if (states < 1 || states > 64) {
    relation_error(env, "java/lang/IllegalArgumentException", "invalid relation dimensions");
    return NULL;
  }
  (*env)->GetLongArrayRegion(env, rows, 0, states, transfer);
  if ((*env)->ExceptionCheck(env)) return NULL;
  memcpy(bits, transfer, (size_t) states * sizeof(uint64_t));
  if (m3_state_relation_repeat(bits, bits, (size_t) states, (uint64_t) count) != 0) {
    relation_error(env, "java/lang/IllegalArgumentException", "foreign relation state bits");
    return NULL;
  }
  memcpy(transfer, bits, (size_t) states * sizeof(uint64_t));
  answer = (*env)->NewLongArray(env, states);
  if (answer == NULL) return NULL;
  (*env)->SetLongArrayRegion(env, answer, 0, states, transfer);
  if ((*env)->ExceptionCheck(env)) return NULL;
  return answer;
}
