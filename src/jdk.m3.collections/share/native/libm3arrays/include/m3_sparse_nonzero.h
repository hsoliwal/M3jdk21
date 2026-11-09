/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
#ifndef M3_SPARSE_NONZERO_H
#define M3_SPARSE_NONZERO_H
#include <stddef.h>
#include <stdint.h>

/* Caller-owned primitive lane, read-only; no allocation or retained pointers. */
size_t m3_count_nonzero_i32(const int32_t *values, size_t count);
#endif
