/* SPDX-License-Identifier: Apache-2.0 */
#ifndef M3_ADAPTIVE_BITPACK_H
#define M3_ADAPTIVE_BITPACK_H
#include <stddef.h>
#include <stdint.h>

#define M3_ADAPTIVE_MAX 4096u
/* 0 success; -1 invalid geometry, capacity, value, padding or signed overflow.
 * Outputs remain untouched on failure. */
int m3_adaptive_encode(const int32_t *src, size_t n, int32_t base, unsigned bits,
                       uint8_t *dst, size_t cap);
int m3_adaptive_decode(const uint8_t *src, size_t nbytes, size_t length,
                       int32_t base, unsigned bits, int32_t *dst, size_t cap);
#endif
