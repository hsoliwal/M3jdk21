/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
#ifndef M3_MULTISET_STEP_H
#define M3_MULTISET_STEP_H
#include <stdint.h>

enum { M3_MULTI_UNION = 0, M3_MULTI_INTERSECT = 1,
       M3_MULTI_SUBTRACT = 2, M3_MULTI_SYMDIFF = 3 };

typedef struct {
  int take_left;
  int take_right;
  int emit;
  int32_t value;
} m3_multiset_action;

/* Pure compare/emit atom. Both current values are valid only when has_*=1.
 * Repeated comparisons consume exactly one occurrence from each equal lane.
 * Returns zero only on invalid mode or no available input.
 */
int m3_multiset_step(int mode, int has_left, int32_t left,
    int has_right, int32_t right, m3_multiset_action *result);
#endif
