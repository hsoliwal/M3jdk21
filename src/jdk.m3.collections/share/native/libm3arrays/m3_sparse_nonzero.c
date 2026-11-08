/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
#include "m3_sparse_nonzero.h"

size_t m3_count_nonzero_i32(const int32_t *values, size_t count) {
  if (values == NULL) return 0U;
  size_t result = 0U;
  for (size_t index = 0U; index < count; index++) {
    if (values[index] != 0) result++;
  }
  return result;
}
