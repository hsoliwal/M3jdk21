/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 * Modified 2026-10-17: checks remain executable when Release defines NDEBUG.
 * Modified 2026-10-18: exact lane, offset, allocation-end and seeded differential corpus.
 */
#include "m3_arrays.h"
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

static int failures;

static void check(int condition, const char *name) {
  if (condition) return;
  (void)fprintf(stderr, "FAIL: %s\n", name);
  failures++;
}


static size_t compared_cases;
static uint32_t random_state = UINT32_C(0x4d335554);

static uint32_t next_random(void) {
  random_state ^= random_state << 13U;
  random_state ^= random_state >> 17U;
  random_state ^= random_state << 5U;
  return random_state;
}

static int scalar_oracle(
    const uint16_t *left, size_t left_start,
    const uint16_t *right, size_t right_start, size_t count) {
  if (count != 0U && (left == NULL || right == NULL)) return 0;
  for (size_t at = 0U; at < count; at++) {
    int difference = (int)left[left_start + at] - (int)right[right_start + at];
    if (difference != 0) return difference;
  }
  return 0;
}

static void check_comparison(
    const uint16_t *left, size_t left_start,
    const uint16_t *right, size_t right_start, size_t count) {
  int expected = scalar_oracle(left, left_start, right, right_start, count);
  int actual = m3_compare_utf16(left, left_start, right, right_start, count);
  compared_cases++;
  if (actual == expected) return;
  (void)fprintf(stderr, "CASE: count=%zu left_start=%zu right_start=%zu expected=%d actual=%d\n",
                count, left_start, right_start, expected, actual);
  check(0, "scalar differential comparison");
}

static void check_mismatch(
    uint16_t *left, size_t left_start, uint16_t *right, size_t right_start,
    size_t count, size_t changed) {
  uint16_t original_left = left[left_start + changed];
  uint16_t original_right = right[right_start + changed];
  left[left_start + changed] = 0U;
  right[right_start + changed] = UINT16_MAX;
  check_comparison(left, left_start, right, right_start, count);
  check_comparison(right, right_start, left, left_start, count);
  left[left_start + changed] = UINT16_C(0x00ff);
  right[right_start + changed] = UINT16_C(0x0100);
  check_comparison(left, left_start, right, right_start, count);
  check_comparison(right, right_start, left, left_start, count);
  left[left_start + changed] = original_left;
  right[right_start + changed] = original_right;
}

/* The allocation ends exactly at the admitted range end, including offset cases.
 * Sanitized builds therefore reject a grouped load that crosses a tail boundary.
 */
static void exercise_range(size_t count, size_t left_start, size_t right_start, int mode) {
  size_t left_slots = left_start + count;
  size_t right_slots = right_start + count;
  if (left_slots == 0U) left_slots = 1U;
  if (right_slots == 0U) right_slots = 1U;
  uint16_t *left = malloc(left_slots * sizeof(*left));
  uint16_t *right = malloc(right_slots * sizeof(*right));
  uint16_t *saved_left = malloc(left_slots * sizeof(*saved_left));
  uint16_t *saved_right = malloc(right_slots * sizeof(*saved_right));
  if (left == NULL || right == NULL || saved_left == NULL || saved_right == NULL) {
    free(left);
    free(right);
    free(saved_left);
    free(saved_right);
    (void)fprintf(stderr, "FAIL: bounded corpus allocation\n");
    exit(EXIT_FAILURE);
  }
  for (size_t at = 0U; at < left_slots; at++) left[at] = (uint16_t)next_random();
  for (size_t at = 0U; at < right_slots; at++) right[at] = (uint16_t)next_random();
  for (size_t at = 0U; at < count; at++) right[right_start + at] = left[left_start + at];
  memcpy(saved_left, left, left_slots * sizeof(*left));
  memcpy(saved_right, right, right_slots * sizeof(*right));
  check_comparison(left, left_start, right, right_start, count);
  if (mode == 0) {
    for (size_t at = 0U; at < count; at++) {
      check_mismatch(left, left_start, right, right_start, count, at);
    }
  } else if (count != 0U) {
    if (mode == 1) {
      const size_t positions[] = {
          0U, 1U, 2U, 3U, 4U, 7U, 8U, 15U, 16U, 31U, 32U, 63U, 64U,
          1022U, 1023U, 1024U, count / 2U, count - 1U};
      for (size_t at = 0U; at < sizeof(positions) / sizeof(positions[0]); at++) {
        if (positions[at] < count) {
          check_mismatch(left, left_start, right, right_start, count, positions[at]);
        }
      }
    } else {
      size_t changed = (size_t)next_random() % count;
      right[right_start + changed] =
          (uint16_t)(right[right_start + changed] ^ UINT16_C(0x8021));
      check_comparison(left, left_start, right, right_start, count);
      check_comparison(right, right_start, left, left_start, count);
      right[right_start + changed] = saved_right[right_start + changed];
    }
  }
  check(memcmp(left, saved_left, left_slots * sizeof(*left)) == 0, "left input unchanged");
  check(memcmp(right, saved_right, right_slots * sizeof(*right)) == 0, "right input unchanged");
  free(left);
  free(right);
  free(saved_left);
  free(saved_right);
}

static void run_corpus(void) {
  for (size_t count = 0U; count <= 33U; count++) {
    for (size_t left_start = 0U; left_start < 8U; left_start++) {
      for (size_t right_start = 0U; right_start < 8U; right_start++) {
        exercise_range(count, left_start, right_start, 0);
      }
    }
  }
  const size_t lengths[] = {63U, 64U, 65U, 1023U, 1024U, 1025U, 4097U};
  for (size_t at = 0U; at < sizeof(lengths) / sizeof(lengths[0]); at++) {
    for (size_t left_start = 0U; left_start < 8U; left_start++) {
      for (size_t right_start = 0U; right_start < 8U; right_start++) {
        exercise_range(lengths[at], left_start, right_start, 1);
      }
    }
  }
  for (size_t at = 0U; at < 4096U; at++) {
    size_t count = (size_t)(next_random() % UINT32_C(4098));
    size_t left_start = (size_t)(next_random() % UINT32_C(8));
    size_t right_start = (size_t)(next_random() % UINT32_C(8));
    exercise_range(count, left_start, right_start, 2);
  }
  uint16_t overlap[72];
  uint16_t saved[72];
  for (size_t at = 0U; at < 72U; at++) overlap[at] = (uint16_t)(at % 4U);
  memcpy(saved, overlap, sizeof(saved));
  for (size_t left_start = 0U; left_start < 8U; left_start++) {
    for (size_t right_start = 0U; right_start < 8U; right_start++) {
      check_comparison(overlap, left_start, overlap, right_start, 64U);
    }
  }
  check(memcmp(overlap, saved, sizeof(saved)) == 0, "overlapping inputs unchanged");
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
  check(m3_compare_utf16(NULL, SIZE_MAX, NULL, SIZE_MAX, 0U) == 0, "empty null arbitrary starts");
  check(m3_compare_utf16(left, SIZE_MAX, same, SIZE_MAX, 0U) == 0, "empty nonnull arbitrary starts");
  check(m3_compare_utf16(NULL, SIZE_MAX, same, 0U, 1U) == 0, "nonempty left null sentinel");
  check(m3_compare_utf16(left, 0U, NULL, SIZE_MAX, 1U) == 0, "nonempty right null sentinel");
  const uint16_t byte_left[] = {0x00ff, 0xffff, 0xd800, 0x0000};
  const uint16_t byte_right[] = {0x0100, 0x0000, 0x0000, 0xffff};
  check(m3_compare_utf16(byte_left, 0U, byte_right, 0U, 4U) == -1, "unit ordering before later differences");
  check(m3_compare_utf16(byte_right, 0U, byte_left, 0U, 4U) == 1, "reverse unit ordering");
  run_corpus();
  (void)printf("M3_UTF16_NATIVE_CORPUS seed=0x4d335554 differential_cases=%zu failures=%d\n",
               compared_cases, failures);
  return failures == 0 ? EXIT_SUCCESS : EXIT_FAILURE;
}
