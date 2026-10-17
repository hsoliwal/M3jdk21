// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
#ifndef M3_PRECOMPUTE_DERIVATIVE_REGEX_H
#define M3_PRECOMPUTE_DERIVATIVE_REGEX_H

#include <stddef.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

enum {
  M3_DERIVATIVE_REGEX_OK = 0,
  M3_DERIVATIVE_REGEX_INVALID = 1,
  M3_DERIVATIVE_REGEX_OVERFLOW = 2
};

/*
 * Execute a frozen derivative DFA over run-length encoded execution classes.
 *
 * powers layout:
 *   [power_bit][execution_class][state] -> next_state
 * where power_bit 0 consumes one unit, bit 1 consumes two, and so on.
 *
 * offsets has row_count + 1 entries and partitions run_classes/run_lengths.
 * No input pointer is retained after return.
 */
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
    uint8_t *output);

#ifdef __cplusplus
}
#endif

#endif
