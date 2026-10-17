// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
#include <jni.h>

#include <stdint.h>

#include "m3_derivative_regex.h"

static void throw_illegal_argument(JNIEnv *env, const char *message) {
  jclass type = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
  if (type != NULL) (void) (*env)->ThrowNew(env, type, message);
}

static void throw_illegal_state(JNIEnv *env, const char *message) {
  jclass type = (*env)->FindClass(env, "java/lang/IllegalStateException");
  if (type != NULL) (void) (*env)->ThrowNew(env, type, message);
}

JNIEXPORT jbyteArray JNICALL
Java_com_m3_precompute_M3RegexDerivativeNative_nativeMatchRunsBatch(
    JNIEnv *env,
    jclass owner,
    jint classes,
    jint states,
    jint start_state,
    jintArray power_array,
    jint power_bits,
    jbyteArray accepting_array,
    jbyteArray live_array,
    jintArray class_array,
    jintArray length_array,
    jintArray offset_array) {
  (void) owner;
  if (classes <= 0
      || states <= 0
      || start_state < 0
      || start_state >= states
      || power_bits <= 0
      || power_bits > 31
      || power_array == NULL
      || accepting_array == NULL
      || live_array == NULL
      || class_array == NULL
      || length_array == NULL
      || offset_array == NULL) {
    throw_illegal_argument(env, "invalid derivative regex JNI geometry");
    return NULL;
  }

  jsize power_count = (*env)->GetArrayLength(env, power_array);
  jsize accepting_count = (*env)->GetArrayLength(env, accepting_array);
  jsize live_count = (*env)->GetArrayLength(env, live_array);
  jsize run_count = (*env)->GetArrayLength(env, class_array);
  jsize length_count = (*env)->GetArrayLength(env, length_array);
  jsize offset_count = (*env)->GetArrayLength(env, offset_array);
  int64_t expected_powers =
      (int64_t) power_bits * (int64_t) states * (int64_t) classes;
  if (offset_count < 1
      || accepting_count != states
      || live_count != states
      || length_count != run_count
      || expected_powers != (int64_t) power_count) {
    throw_illegal_argument(env, "inconsistent derivative regex JNI arrays");
    return NULL;
  }

  jint *powers = (*env)->GetIntArrayElements(env, power_array, NULL);
  if (powers == NULL) return NULL;
  jbyte *accepting = (*env)->GetByteArrayElements(env, accepting_array, NULL);
  if (accepting == NULL) {
    (*env)->ReleaseIntArrayElements(env, power_array, powers, JNI_ABORT);
    return NULL;
  }
  jbyte *live = (*env)->GetByteArrayElements(env, live_array, NULL);
  if (live == NULL) {
    (*env)->ReleaseByteArrayElements(env, accepting_array, accepting, JNI_ABORT);
    (*env)->ReleaseIntArrayElements(env, power_array, powers, JNI_ABORT);
    return NULL;
  }
  jint *classes_values = (*env)->GetIntArrayElements(env, class_array, NULL);
  if (classes_values == NULL) {
    (*env)->ReleaseByteArrayElements(env, live_array, live, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, accepting_array, accepting, JNI_ABORT);
    (*env)->ReleaseIntArrayElements(env, power_array, powers, JNI_ABORT);
    return NULL;
  }
  jint *lengths = (*env)->GetIntArrayElements(env, length_array, NULL);
  if (lengths == NULL) {
    (*env)->ReleaseIntArrayElements(env, class_array, classes_values, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, live_array, live, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, accepting_array, accepting, JNI_ABORT);
    (*env)->ReleaseIntArrayElements(env, power_array, powers, JNI_ABORT);
    return NULL;
  }
  jint *offsets = (*env)->GetIntArrayElements(env, offset_array, NULL);
  if (offsets == NULL) {
    (*env)->ReleaseIntArrayElements(env, length_array, lengths, JNI_ABORT);
    (*env)->ReleaseIntArrayElements(env, class_array, classes_values, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, live_array, live, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, accepting_array, accepting, JNI_ABORT);
    (*env)->ReleaseIntArrayElements(env, power_array, powers, JNI_ABORT);
    return NULL;
  }

  jsize row_count = offset_count - 1;
  jbyteArray result = (*env)->NewByteArray(env, row_count);
  if (result == NULL) {
    (*env)->ReleaseIntArrayElements(env, offset_array, offsets, JNI_ABORT);
    (*env)->ReleaseIntArrayElements(env, length_array, lengths, JNI_ABORT);
    (*env)->ReleaseIntArrayElements(env, class_array, classes_values, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, live_array, live, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, accepting_array, accepting, JNI_ABORT);
    (*env)->ReleaseIntArrayElements(env, power_array, powers, JNI_ABORT);
    return NULL;
  }
  jbyte *output = (*env)->GetByteArrayElements(env, result, NULL);
  if (output == NULL) {
    (*env)->ReleaseIntArrayElements(env, offset_array, offsets, JNI_ABORT);
    (*env)->ReleaseIntArrayElements(env, length_array, lengths, JNI_ABORT);
    (*env)->ReleaseIntArrayElements(env, class_array, classes_values, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, live_array, live, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, accepting_array, accepting, JNI_ABORT);
    (*env)->ReleaseIntArrayElements(env, power_array, powers, JNI_ABORT);
    return NULL;
  }

  int status =
      m3_derivative_regex_match_runs_batch(
          (const int32_t *) powers,
          (size_t) power_count,
          (uint32_t) power_bits,
          (size_t) states,
          (size_t) classes,
          (uint32_t) start_state,
          (const uint8_t *) accepting,
          (const uint8_t *) live,
          (const int32_t *) classes_values,
          (const int32_t *) lengths,
          (size_t) run_count,
          (const int32_t *) offsets,
          (size_t) row_count,
          (uint8_t *) output);

  (*env)->ReleaseByteArrayElements(
      env, result, output, status == M3_DERIVATIVE_REGEX_OK ? 0 : JNI_ABORT);
  (*env)->ReleaseIntArrayElements(env, offset_array, offsets, JNI_ABORT);
  (*env)->ReleaseIntArrayElements(env, length_array, lengths, JNI_ABORT);
  (*env)->ReleaseIntArrayElements(env, class_array, classes_values, JNI_ABORT);
  (*env)->ReleaseByteArrayElements(env, live_array, live, JNI_ABORT);
  (*env)->ReleaseByteArrayElements(env, accepting_array, accepting, JNI_ABORT);
  (*env)->ReleaseIntArrayElements(env, power_array, powers, JNI_ABORT);

  if (status != M3_DERIVATIVE_REGEX_OK) {
    throw_illegal_state(
        env, "native derivative regex rejected compiled power table or input");
    return NULL;
  }
  return result;
}

/* Exact dual-reachability facts over the existing derivative image. No source text is retained.
 * Input copies are bounded and released on every exit; no critical pins or retained JVM address. */
#include <stdlib.h>

JNIEXPORT jbyteArray JNICALL
Java_com_synexia_indexstring_MIndexRegexDerivativeProgram_suffixFactsNative(
    JNIEnv *env, jclass owner, jintArray transition_array, jbyteArray accepting_array,
    jint classes) {
  (void) owner;
  if (transition_array == NULL || accepting_array == NULL || classes < 1 || classes > 4096) {
    throw_illegal_argument(env, "invalid suffix fact geometry");
    return NULL;
  }
  jsize states = (*env)->GetArrayLength(env, accepting_array);
  jsize edges = (*env)->GetArrayLength(env, transition_array);
  if (states < 1 || states > 4000000 || edges > 4000000
      || (int64_t) states * classes != edges) {
    throw_illegal_argument(env, "invalid suffix fact dimensions");
    return NULL;
  }
  jint *transitions = malloc((size_t) edges * sizeof(*transitions));
  jbyte *accepting = malloc((size_t) states * sizeof(*accepting));
  jint *offsets = calloc((size_t) states + 1U, sizeof(*offsets));
  jint *cursor = malloc((size_t) states * sizeof(*cursor));
  jint *predecessors = malloc((size_t) edges * sizeof(*predecessors));
  jint *queue = malloc((size_t) states * sizeof(*queue));
  jbyte *outcomes = calloc((size_t) states, sizeof(*outcomes));
  jbyteArray result = NULL;
  if (transitions == NULL || accepting == NULL || offsets == NULL || cursor == NULL
      || predecessors == NULL || queue == NULL || outcomes == NULL) {
    jclass error = (*env)->FindClass(env, "java/lang/OutOfMemoryError");
    if (error != NULL) (void) (*env)->ThrowNew(env, error, "suffix fact workspace");
    goto cleanup;
  }
  (*env)->GetIntArrayRegion(env, transition_array, 0, edges, transitions);
  if ((*env)->ExceptionCheck(env)) goto cleanup;
  (*env)->GetByteArrayRegion(env, accepting_array, 0, states, accepting);
  if ((*env)->ExceptionCheck(env)) goto cleanup;
  for (jint state = 0; state < states; state++) {
    if (accepting[state] != 0 && accepting[state] != 1) {
      throw_illegal_argument(env, "suffix acceptance lane");
      goto cleanup;
    }
  }
  for (jint edge = 0; edge < edges; edge++) {
    if (transitions[edge] < 0 || transitions[edge] >= states) {
      throw_illegal_argument(env, "suffix transition row");
      goto cleanup;
    }
    offsets[transitions[edge] + 1]++;
  }
  for (jint state = 0; state < states; state++) {
    offsets[state + 1] += offsets[state];
    cursor[state] = offsets[state];
  }
  for (jint edge = 0; edge < edges; edge++) {
    predecessors[cursor[transitions[edge]]++] = edge / classes;
  }
  for (jint bit = 1; bit <= 2; bit <<= 1) {
    jint head = 0, tail = 0;
    for (jint state = 0; state < states; state++) {
      if ((accepting[state] != 0) == (bit == 1)) {
        outcomes[state] = (jbyte) (outcomes[state] | bit);
        queue[tail++] = state;
      }
    }
    while (head < tail) {
      jint reached = queue[head++];
      for (jint at = offsets[reached]; at < offsets[reached + 1]; at++) {
        jint predecessor = predecessors[at];
        if ((outcomes[predecessor] & bit) == 0) {
          outcomes[predecessor] = (jbyte) (outcomes[predecessor] | bit);
          queue[tail++] = predecessor;
        }
      }
    }
  }
  /* Release the graph scratch BEFORE publication so the returned Java array does not
   * overlap the full native working set counted by the caller's primitive budget. */
  free(queue); queue = NULL;
  free(predecessors); predecessors = NULL;
  free(cursor); cursor = NULL;
  free(offsets); offsets = NULL;
  free(accepting); accepting = NULL;
  free(transitions); transitions = NULL;
  result = (*env)->NewByteArray(env, states);
  if (result != NULL) (*env)->SetByteArrayRegion(env, result, 0, states, outcomes);
cleanup:
  free(outcomes);
  free(queue);
  free(predecessors);
  free(cursor);
  free(offsets);
  free(accepting);
  free(transitions);
  return result;
}
