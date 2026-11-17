// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
#include "m3_derivative_regex.h"

#include <stddef.h>
#include <stdint.h>

#define CHECK(condition) \
  do { \
    if (!(condition)) return __LINE__; \
  } while (0)

static void build_powers(
    const int32_t *transitions,
    size_t states,
    size_t classes,
    uint32_t bits,
    int32_t *powers) {
  size_t plane = states * classes;
  for (size_t execution_class = 0U; execution_class < classes; execution_class++) {
    for (size_t state = 0U; state < states; state++) {
      powers[execution_class * states + state] =
          transitions[state * classes + execution_class];
    }
  }
  for (uint32_t bit = 1U; bit < bits; bit++) {
    size_t previous = (size_t) (bit - 1U) * plane;
    size_t current = (size_t) bit * plane;
    for (size_t execution_class = 0U; execution_class < classes; execution_class++) {
      size_t base = execution_class * states;
      for (size_t state = 0U; state < states; state++) {
        int32_t middle = powers[previous + base + state];
        powers[current + base + state] =
            powers[previous + base + (size_t) middle];
      }
    }
  }
}

int main(void) {
  /*
   * Dense DFA for a b* c over classes a,b,c,other.
   * State 2 accepts; state 3 is dead.
   */
  const int32_t transitions[] = {
      1, 3, 3, 3,
      3, 1, 2, 3,
      3, 3, 3, 3,
      3, 3, 3, 3};
  const uint8_t accepting[] = {0, 0, 1, 0};
  const uint8_t live[] = {1, 1, 1, 0};
  enum { STATES = 4, CLASSES = 4, BITS = 8, POWER_CELLS = STATES * CLASSES * BITS };
  int32_t powers[POWER_CELLS];
  build_powers(transitions, STATES, CLASSES, BITS, powers);

  /* Rows: ac, abbbbc, abb, empty, and a run of 100 b symbols followed by c. */
  const int32_t run_classes[] = {0, 2, 0, 1, 2, 0, 1, 0, 1, 2};
  const int32_t run_lengths[] = {1, 1, 1, 4, 1, 1, 2, 1, 100, 1};
  const int32_t offsets[] = {0, 2, 5, 7, 7, 10};
  uint8_t output[5] = {0, 0, 0, 0, 0};

  CHECK(
      m3_derivative_regex_match_runs_batch(
          powers,
          POWER_CELLS,
          BITS,
          STATES,
          CLASSES,
          0U,
          accepting,
          live,
          run_classes,
          run_lengths,
          10U,
          offsets,
          5U,
          output)
      == M3_DERIVATIVE_REGEX_OK);
  CHECK(output[0] == 1U);
  CHECK(output[1] == 1U);
  CHECK(output[2] == 0U);
  CHECK(output[3] == 0U);
  CHECK(output[4] == 1U);

  const int32_t bad_classes[] = {4};
  const int32_t bad_lengths[] = {1};
  const int32_t bad_offsets[] = {0, 1};
  CHECK(
      m3_derivative_regex_match_runs_batch(
          powers,
          POWER_CELLS,
          BITS,
          STATES,
          CLASSES,
          0U,
          accepting,
          live,
          bad_classes,
          bad_lengths,
          1U,
          bad_offsets,
          1U,
          output)
      == M3_DERIVATIVE_REGEX_INVALID);

  CHECK(
      m3_derivative_regex_match_runs_batch(
          powers,
          POWER_CELLS - 1U,
          BITS,
          STATES,
          CLASSES,
          0U,
          accepting,
          live,
          run_classes,
          run_lengths,
          10U,
          offsets,
          5U,
          output)
      == M3_DERIVATIVE_REGEX_INVALID);
  return 0;
}

#undef CHECK
