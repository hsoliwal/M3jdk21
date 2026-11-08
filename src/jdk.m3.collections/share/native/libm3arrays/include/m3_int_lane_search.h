/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
#ifndef M3_INT_LANE_SEARCH_H
#define M3_INT_LANE_SEARCH_H
#include <stddef.h>
#include <stdint.h>

/* Sorted signed-int lane: first index >= key (upper != 0 means > key).
 * This bounded pure C kernel holds no JNI references or Java addresses.
 */
size_t m3_int_lane_bound(const int32_t *lane, size_t count, int32_t key, int upper);
#endif
