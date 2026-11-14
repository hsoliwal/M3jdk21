/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project */
/* SPDX-License-Identifier: Apache-2.0 */
#include "m3_ncm_codebook.h"

#include <float.h>
#include <limits.h>
#include <math.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

static size_t checks;

static void check(int condition, const char *message) {
  checks++;
  if (!condition) {
    fprintf(stderr, "NCM_CODEBOOK_FAILURE: %s\n", message);
    exit(1);
  }
}

static uint64_t random_state = UINT64_C(0x4d334e434d);

static uint32_t next_random(void) {
  random_state ^= random_state << 13;
  random_state ^= random_state >> 7;
  random_state ^= random_state << 17;
  return (uint32_t) random_state;
}

static void invalid_cases(void) {
  const double zero[] = {0.0};
  const double nan_value[] = {NAN};
  const double infinity[] = {INFINITY};
  const double huge[] = {DBL_MAX};
  const double minus_huge[] = {-DBL_MAX};
  int32_t out[] = {91};
  check(m3_ncm_nearest(zero, 1, 0, -1, zero, 1, 0, 1, 1, out, 1) == M3_NCM_INVALID_ARGUMENT, "negative rows");
  check(m3_ncm_nearest(zero, 1, 0, 1, zero, 1, 0, 0, 1, out, 1) == M3_NCM_INVALID_ARGUMENT, "empty codebook");
  check(m3_ncm_nearest(zero, 1, 0, 1, zero, 1, 0, 1, 0, out, 1) == M3_NCM_INVALID_ARGUMENT, "zero dimensions");
  check(m3_ncm_nearest(zero, 1, -1, 1, zero, 1, 0, 1, 1, out, 1) == M3_NCM_OUT_OF_RANGE, "negative offset");
  check(m3_ncm_nearest(zero, 1, 2, 0, zero, 1, 0, 1, 1, NULL, 0) == M3_NCM_OUT_OF_RANGE, "empty range outside array");
  check(m3_ncm_nearest(zero, 1, 0, INT32_MAX, zero, 1, 0, 1, INT32_MAX, out, 1) == M3_NCM_OUT_OF_RANGE, "shape product overflow");
  check(m3_ncm_nearest(zero, 1, 0, 1, zero, 1, INT32_MAX, INT32_MAX, 1, out, 1) == M3_NCM_OUT_OF_RANGE, "code range overflow");
  check(m3_ncm_nearest(zero, 1, 0, 1, zero, 1, 0, 1, 1, out, 0) == M3_NCM_INVALID_ARGUMENT, "output length");
  check(m3_ncm_nearest(NULL, 1, 0, 1, zero, 1, 0, 1, 1, out, 1) == M3_NCM_INVALID_ARGUMENT, "null selected input");
  check(m3_ncm_nearest(zero, 1, 0, 1, NULL, 1, 0, 1, 1, out, 1) == M3_NCM_INVALID_ARGUMENT, "null selected codebook");
  check(m3_ncm_nearest(zero, 1, 0, 1, zero, 1, 0, 1, 1, NULL, 1) == M3_NCM_INVALID_ARGUMENT, "null output");
  check(m3_ncm_nearest(nan_value, 1, 0, 1, zero, 1, 0, 1, 1, out, 1) == M3_NCM_NONFINITE_INPUT, "NaN vector");
  check(m3_ncm_nearest(zero, 1, 0, 1, infinity, 1, 0, 1, 1, out, 1) == M3_NCM_NONFINITE_INPUT, "infinite code");
  check(m3_ncm_nearest(NULL, 0, 0, 0, nan_value, 1, 0, 1, 1, NULL, 0) == M3_NCM_NONFINITE_INPUT, "empty batch validates selected codes");
  check(out[0] == 91, "invalid input wrote output");
  check(m3_ncm_nearest(huge, 1, 0, 1, minus_huge, 1, 0, 1, 1, out, 1) == M3_NCM_DISTANCE_OVERFLOW, "subtraction overflow");
  check(m3_ncm_nearest(huge, 1, 0, 1, zero, 1, 0, 1, 1, out, 1) == M3_NCM_DISTANCE_OVERFLOW, "square overflow");
  const double large[] = {0x1p511, 0x1p511, 0x1p511, 0x1p511};
  const double zero4[] = {0, 0, 0, 0};
  check(m3_ncm_nearest(large, 4, 0, 1, zero4, 4, 0, 1, 4, out, 1) == M3_NCM_DISTANCE_OVERFLOW, "sum overflow");
  check(m3_ncm_nearest(NULL, 0, 0, 0, NULL, 0, 0, 0, 1, NULL, 0) == M3_NCM_OK, "empty batch");
  check(m3_ncm_nearest(nan_value, 1, 1, 0, infinity, 1, 1, 0, INT32_MAX, NULL, 0) == M3_NCM_OK, "empty range at end");
  const double signed_zero[] = {-0.0, DBL_TRUE_MIN};
  const double near_zero[] = {0.0, -0.0, DBL_TRUE_MIN};
  int32_t tied[2] = {-1, -1};
  check(m3_ncm_nearest(signed_zero, 2, 0, 2, near_zero, 3, 0, 3, 1, tied, 2) == M3_NCM_OK, "subnormal inputs");
  check(tied[0] == 0 && tied[1] == 0, "first underflow tie");
}

static void exact_properties(void) {
  for (int trial = 0; trial < 4096; trial++) {
    int32_t rows = (int32_t) (next_random() % 8U);
    int32_t codes = 1 + (int32_t) (next_random() % 10U);
    int32_t dimensions = 1 + (int32_t) (next_random() % 7U);
    int32_t vector_offset = (int32_t) (next_random() % 4U);
    int32_t code_offset = (int32_t) (next_random() % 4U);
    double vectors[64], codebook[80], before_vectors[64], before_codes[80];
    int32_t result[8], expected[8];
    for (size_t i = 0; i < 64; i++) vectors[i] = NAN;
    for (size_t i = 0; i < 80; i++) codebook[i] = NAN;
    for (int32_t i = 0; i < rows * dimensions; i++) {
      vectors[vector_offset + i] = (double) ((int32_t) (next_random() % 129U) - 64);
    }
    for (int32_t i = 0; i < codes * dimensions; i++) {
      codebook[code_offset + i] = (double) ((int32_t) (next_random() % 129U) - 64);
    }
    if (codes > 1 && trial % 3 == 0) {
      memcpy(codebook + code_offset + dimensions, codebook + code_offset, (size_t) dimensions * sizeof(double));
    }
    memcpy(before_vectors, vectors, sizeof(vectors));
    memcpy(before_codes, codebook, sizeof(codebook));
    for (int32_t row = 0; row < rows; row++) {
      int64_t best = INT64_MAX;
      expected[row] = -1;
      for (int32_t code = 0; code < codes; code++) {
        int64_t distance = 0;
        for (int32_t d = 0; d < dimensions; d++) {
          int64_t delta = (int64_t) vectors[vector_offset + row * dimensions + d]
              - (int64_t) codebook[code_offset + code * dimensions + d];
          distance += delta * delta;
        }
        if (distance < best) { best = distance; expected[row] = code; }
      }
    }
    check(m3_ncm_nearest(vectors, 64, vector_offset, rows, codebook, 80, code_offset,
        codes, dimensions, result, (size_t) rows) == M3_NCM_OK, "random batch status");
    for (int32_t row = 0; row < rows; row++) check(result[row] == expected[row], "exact integer oracle mismatch");
    check(memcmp(vectors, before_vectors, sizeof(vectors)) == 0
        && memcmp(codebook, before_codes, sizeof(codebook)) == 0, "input mutated");
  }
}

int main(void) {
  invalid_cases();
  exact_properties();
  printf("NCM_CODEBOOK_C_PASS checks=%zu\n", checks);
  return 0;
}
