// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
#include "m3_derivative_regex.h"

#include <limits.h>

static uint32_t apply_power(
    const int32_t *powers,
    size_t plane,
    size_t state_count,
    uint32_t bit,
    uint32_t execution_class,
    uint32_t state) {
  size_t index =
      (size_t) bit * plane
      + (size_t) execution_class * state_count
      + (size_t) state;
  return (uint32_t) powers[index];
}

static uint32_t advance_run(
    const int32_t *powers,
    size_t plane,
    size_t state_count,
    uint32_t power_bits,
    const uint8_t *live,
    uint32_t execution_class,
    uint32_t state,
    uint32_t length) {
  if (length == UINT32_C(0)) return state;

  uint32_t highest_bit = power_bits - UINT32_C(1);
  uint32_t chunk = UINT32_C(1) << highest_bit;
  uint32_t chunks = length / chunk;
  for (uint32_t count = 0; count < chunks; count++) {
    state =
        apply_power(
            powers,
            plane,
            state_count,
            highest_bit,
            execution_class,
            state);
    if (live[state] == UINT8_C(0)) return state;
  }

  uint32_t tail = length % chunk;
  uint32_t bit = UINT32_C(0);
  while (tail != UINT32_C(0)) {
    if ((tail & UINT32_C(1)) != UINT32_C(0)) {
      state =
          apply_power(
              powers,
              plane,
              state_count,
              bit,
              execution_class,
              state);
    }
    tail >>= UINT32_C(1);
    bit++;
  }
  return state;
}

int m3_derivative_regex_match_runs_batch(
    const int32_t *powers,
    size_t power_cells,
    uint32_t power_bits,
    size_t state_count,
    size_t class_count,
    uint32_t start_state,
    const uint8_t *accepting,
    const uint8_t *live,
    const int32_t *run_classes,
    const int32_t *run_lengths,
    size_t run_count,
    const int32_t *offsets,
    size_t row_count,
    uint8_t *output) {
  if (powers == NULL
      || accepting == NULL
      || live == NULL
      || offsets == NULL
      || output == NULL
      || state_count == 0U
      || class_count == 0U
      || power_bits == UINT32_C(0)
      || power_bits > UINT32_C(31)
      || (size_t) start_state >= state_count
      || state_count > (size_t) INT32_MAX
      || class_count > (size_t) INT32_MAX
      || run_count > (size_t) INT32_MAX
      || row_count > (size_t) INT32_MAX
      || (run_count != 0U && (run_classes == NULL || run_lengths == NULL))) {
    return M3_DERIVATIVE_REGEX_INVALID;
  }

  if (class_count > SIZE_MAX / state_count) {
    return M3_DERIVATIVE_REGEX_OVERFLOW;
  }
  size_t plane = class_count * state_count;
  if ((size_t) power_bits > SIZE_MAX / plane) {
    return M3_DERIVATIVE_REGEX_OVERFLOW;
  }
  if (power_cells != (size_t) power_bits * plane) {
    return M3_DERIVATIVE_REGEX_INVALID;
  }

  for (size_t cell = 0U; cell < power_cells; cell++) {
    int32_t target = powers[cell];
    if (target < 0 || (size_t) target >= state_count) {
      return M3_DERIVATIVE_REGEX_INVALID;
    }
  }
  for (size_t state = 0U; state < state_count; state++) {
    if (accepting[state] > UINT8_C(1) || live[state] > UINT8_C(1)) {
      return M3_DERIVATIVE_REGEX_INVALID;
    }
  }

  if (offsets[0] != 0) return M3_DERIVATIVE_REGEX_INVALID;
  for (size_t row = 0U; row < row_count; row++) {
    int32_t from = offsets[row];
    int32_t to = offsets[row + 1U];
    if (from < 0
        || to < from
        || (size_t) to > run_count) {
      return M3_DERIVATIVE_REGEX_INVALID;
    }
  }
  if ((size_t) offsets[row_count] != run_count) {
    return M3_DERIVATIVE_REGEX_INVALID;
  }

  for (size_t row = 0U; row < row_count; row++) {
    uint32_t state = start_state;
    size_t from = (size_t) offsets[row];
    size_t to = (size_t) offsets[row + 1U];
    for (size_t run = from; run < to; run++) {
      int32_t execution_class = run_classes[run];
      int32_t length = run_lengths[run];
      if (execution_class < 0
          || (size_t) execution_class >= class_count
          || length < 0) {
        return M3_DERIVATIVE_REGEX_INVALID;
      }
      state =
          advance_run(
              powers,
              plane,
              state_count,
              power_bits,
              live,
              (uint32_t) execution_class,
              state,
              (uint32_t) length);
      if ((size_t) state >= state_count) {
        return M3_DERIVATIVE_REGEX_INVALID;
      }
      if (live[state] == UINT8_C(0)) break;
    }
    output[row] =
        accepting[state] == UINT8_C(0) ? UINT8_C(0) : UINT8_C(1);
  }

  return M3_DERIVATIVE_REGEX_OK;
}
