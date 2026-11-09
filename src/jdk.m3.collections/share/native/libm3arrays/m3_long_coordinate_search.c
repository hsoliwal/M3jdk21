/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
#include "m3_long_coordinate_search.h"

size_t m3_long_coordinate_bound(const int64_t *lane, size_t length, int64_t key, int upper) {
  size_t low = 0U;
  size_t high = length;
  while (low < high) {
    size_t middle = low + ((high - low) >> 1U);
    int64_t value = lane[middle];
    if (upper != 0 ? value <= key : value < key) low = middle + 1U;
    else high = middle;
  }
  return low;
}
