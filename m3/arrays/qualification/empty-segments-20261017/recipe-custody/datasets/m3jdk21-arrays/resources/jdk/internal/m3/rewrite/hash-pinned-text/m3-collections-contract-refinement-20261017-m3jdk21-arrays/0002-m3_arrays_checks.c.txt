/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 * Modified 2026-10-17: checks remain executable when Release defines NDEBUG.
 */
#include "m3_arrays.h"
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>

static int failures;

static void check(int condition, const char *name) {
  if (condition) return;
  (void)fprintf(stderr, "FAIL: %s\n", name);
  failures++;
}

int main(void) {
  const uint16_t left[] = {'a','b','c',0xd83d,0xde00};
  const uint16_t same[] = {'a','b','c',0xd83d,0xde00};
  const uint16_t greater[] = {'a','b','d',0xd83d,0xde00};
  const uint16_t extrema[] = {0, UINT16_MAX};

  check(m3_compare_utf16(left, 0U, same, 0U, 5U) == 0, "equal units");
  check(m3_compare_utf16(left, 0U, greater, 0U, 5U) == -1, "exact negative difference");
  check(m3_compare_utf16(greater, 0U, left, 0U, 5U) == 1, "exact positive difference");
  check(m3_compare_utf16(left, 3U, same, 3U, 2U) == 0, "surrogate range");
  check(m3_compare_utf16(NULL, 0U, NULL, 0U, 0U) == 0, "empty null range");
  check(m3_compare_utf16(extrema, 0U, extrema, 1U, 1U) == -65535, "unsigned negative extreme");
  check(m3_compare_utf16(extrema, 1U, extrema, 0U, 1U) == 65535, "unsigned positive extreme");
  check(m3_compare_utf16(left, 1U, same, 0U, 1U) == 1, "independent offsets");
  return failures == 0 ? EXIT_SUCCESS : EXIT_FAILURE;
}
