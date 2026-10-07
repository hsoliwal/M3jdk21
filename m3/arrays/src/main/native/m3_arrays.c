/* SPDX-License-Identifier: Apache-2.0 */
#include "m3_arrays.h"

int m3_compare_utf16(
    const uint16_t *left,
    size_t left_start,
    const uint16_t *right,
    size_t right_start,
    size_t count) {
  if (count != 0U && (left == NULL || right == NULL)) return 0;
  for (size_t index = 0U; index < count; index++) {
    uint16_t a = left[left_start + index];
    uint16_t b = right[right_start + index];
    if (a != b) return (int)a - (int)b;
  }
  return 0;
}
