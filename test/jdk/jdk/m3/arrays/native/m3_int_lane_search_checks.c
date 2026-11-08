/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
#include "m3_int_lane_search.h"
#include <limits.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>

static uint32_t state = UINT32_C(0x4d334c53);
static uint32_t next_random(void) {
  state ^= state << 13U;
  state ^= state >> 17U;
  state ^= state << 5U;
  return state;
}

int main(void) {
  size_t cases = 0U;
  int32_t lane[1025];
  for (size_t length = 0U; length <= 1025U; length++) {
    for (size_t i = 0U; i < length; i++) {
      int32_t next = (int32_t)(next_random() % UINT32_C(15));
      lane[i] = (i == 0U) ? INT32_MIN : lane[i - 1U] + next;
    }
    if (length != 0U) lane[length - 1U] = INT32_MAX;
    for (int iteration = 0; iteration < 80; iteration++) {
      int32_t key = (iteration % 11 == 0) ? INT32_MIN :
          (iteration % 13 == 0) ? INT32_MAX :
          (int32_t)(next_random() % UINT32_C(1000)) + INT32_MIN;
      size_t lower = 0U;
      while (lower < length && lane[lower] < key) lower++;
      size_t upper = 0U;
      while (upper < length && lane[upper] <= key) upper++;
      if (m3_int_lane_bound(lane, length, key, 0) != lower ||
          m3_int_lane_bound(lane, length, key, 1) != upper) {
        (void)fprintf(stderr, "M3_INT_LANE_BOUND_FAIL length=%zu iteration=%d\n", length, iteration);
        return EXIT_FAILURE;
      }
      cases += 2U;
    }
  }
  (void)printf("M3_INT_LANE_BOUND_PASS checked=%zu\n", cases);
  return EXIT_SUCCESS;
}
