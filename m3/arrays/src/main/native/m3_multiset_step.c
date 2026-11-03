/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
#include "m3_multiset_step.h"

int m3_multiset_step(int mode, int has_left, int32_t left,
    int has_right, int32_t right, m3_multiset_action *result) {
  if (result == 0 || mode < M3_MULTI_UNION || mode > M3_MULTI_SYMDIFF
      || (has_left == 0 && has_right == 0)) return 0;
  result->take_left = 0;
  result->take_right = 0;
  result->emit = 0;
  result->value = 0;
  if (has_left == 0 || (has_right != 0 && right < left)) {
    result->take_right = 1;
    result->value = right;
    result->emit = mode == M3_MULTI_UNION || mode == M3_MULTI_SYMDIFF;
  } else if (has_right == 0 || left < right) {
    result->take_left = 1;
    result->value = left;
    result->emit = mode != M3_MULTI_INTERSECT;
  } else {
    result->take_left = 1;
    result->take_right = 1;
    result->value = left;
    result->emit = mode == M3_MULTI_UNION || mode == M3_MULTI_INTERSECT;
  }
  return 1;
}
