/* SPDX-License-Identifier: Apache-2.0 */
#include "m3_arrays.h"
#include <assert.h>
#include <stdint.h>

int main(void) {
  const uint16_t left[] = {'a','b','c',0xd83d,0xde00};
  const uint16_t same[] = {'a','b','c',0xd83d,0xde00};
  const uint16_t greater[] = {'a','b','d',0xd83d,0xde00};

  assert(m3_compare_utf16(left, 0U, same, 0U, 5U) == 0);
  assert(m3_compare_utf16(left, 0U, greater, 0U, 5U) < 0);
  assert(m3_compare_utf16(greater, 0U, left, 0U, 5U) > 0);
  assert(m3_compare_utf16(left, 3U, same, 3U, 2U) == 0);
  assert(m3_compare_utf16(NULL, 0U, NULL, 0U, 0U) == 0);
  return 0;
}
