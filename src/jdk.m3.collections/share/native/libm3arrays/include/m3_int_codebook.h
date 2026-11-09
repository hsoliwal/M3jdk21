/* SPDX-License-Identifier: Apache-2.0 */
#ifndef M3_INT_CODEBOOK_H
#define M3_INT_CODEBOOK_H
#include <stddef.h>
#include <stdint.h>
#define M3_INT_CODEBOOK_MAX 4096u
/* Exact LSB-first IDs, up to 4096 entries/bytes/dictionary entries.
 * Validates every ID and padding byte before writing a single result.
 * Returns 0 on success or -1 on invalid arguments; failure leaves dst intact. */
int m3_int_codebook_decode(const uint8_t *ids, size_t id_bytes,
                           const int32_t *dictionary, size_t dictionary_length,
                           size_t length, unsigned bits,
                           int32_t *dst, size_t dst_capacity);
#endif
