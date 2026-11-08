/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
#ifndef M3_LONG_COORDINATE_SEARCH_H
#define M3_LONG_COORDINATE_SEARCH_H
#include <stddef.h>
#include <stdint.h>

/* First offset >= key, or > key if upper; lane is sorted nondecreasing. */
size_t m3_long_coordinate_bound(const int64_t *lane, size_t length, int64_t key, int upper);
#endif
