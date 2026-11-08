/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
#include "m3_sparse_nonzero.h"
#include <limits.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>

static uint32_t rng = UINT32_C(0x4d33534e);
static uint32_t next_random(void) {
  rng ^= rng << 13U;
  rng ^= rng >> 17U;
  rng ^= rng << 5U;
  return rng;
}

int main(void) {
  int32_t values[4096] = {0};
  size_t checks = 0U;
  for (size_t count = 0U; count <= 4096U; count++) {
    size_t expected = 0U;
    for (size_t i = 0U; i < count; i++) {
      int32_t v = (i % 7U == 0U) ? INT32_MIN :
          (i % 11U == 0U) ? INT32_MAX : (int32_t)(next_random() % 9U) - 4;
      values[i] = v;
      if (v != 0) expected++;
    }
    size_t actual = m3_count_nonzero_i32(values, count);
    if (actual != expected) {
      (void)fprintf(stderr, "M3_SPARSE_NATIVE_REFUSAL count=%zu actual=%zu expected=%zu\n",
          count, actual, expected);
      return EXIT_FAILURE;
    }
    checks++;
  }
  if (m3_count_nonzero_i32(NULL, 0U) != 0U) return EXIT_FAILURE;
  (void)printf("M3_SPARSE_NONZERO_C_PASS checks=%zu\n", checks);
  return EXIT_SUCCESS;
}
