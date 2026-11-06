/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
#include "m3_long_coordinate_search.h"
#include <limits.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>

static uint32_t state = UINT32_C(0x4d334c43);
static uint32_t next_random(void) {
  state ^= state << 13U;
  state ^= state >> 17U;
  state ^= state << 5U;
  return state;
}

int main(void) {
  int64_t keys[2049];
  size_t cases = 0U;
  for (size_t count = 0U; count <= 2049U; count++) {
    for (size_t i = 0U; i < count; i++) {
      keys[i] = (INT64_MIN / INT64_C(2)) + (int64_t)(i / 3U);
    }
    if (count != 0U) {
      keys[0] = INT64_MIN;
      keys[count - 1U] = INT64_MAX;
    }
    for (int trial = 0; trial < 64; trial++) {
      int64_t key = trial % 7 == 0 ? INT64_MIN :
          trial % 11 == 0 ? INT64_MAX :
          trial % 13 == 0 ? INT64_MIN / INT64_C(2) + (int64_t)(next_random() % UINT32_C(800)) :
          (int64_t)(int32_t)next_random();
      size_t lower = 0U;
      size_t upper = 0U;
      while (lower < count && keys[lower] < key) lower++;
      while (upper < count && keys[upper] <= key) upper++;
      if (m3_long_coordinate_bound(keys, count, key, 0) != lower ||
          m3_long_coordinate_bound(keys, count, key, 1) != upper) {
        (void)fprintf(stderr, "M3_LONG_BOUND_FAIL count=%zu trial=%d\n", count, trial);
        return EXIT_FAILURE;
      }
      cases += 2U;
    }
  }
  (void)printf("M3_LONG_COORDINATE_C_PASS checked=%zu\n", cases);
  return EXIT_SUCCESS;
}
