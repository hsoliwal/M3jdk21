/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project */
/* SPDX-License-Identifier: Apache-2.0 */
/* Modified 2026-10-29: target-owned M3 arrays codec adaptation from Synexia
 * 6f878f9e5d6c5044eda15efb9e743594e70ce7f7; six packed/full64 operations only. */
#ifndef M3_PACKED_CODEC_H
#define M3_PACKED_CODEC_H

#include <stddef.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

#define M3_PACKED_CODEC_MAX 4096u

enum m3_codec_status { M3_CODEC_OK, M3_CODEC_ARGUMENT, M3_CODEC_BOUND, M3_CODEC_FORMAT };

/* Packed widths are exactly 40, 48 or 56, signed two's-complement, little-endian,
 * with no padding. The fixed-width decoder rejects a partial final element.
 * Unsigned64 uses int64_t bit carriers: negative carriers represent [2^63, 2^64).
 * ZigZag64 maps signed int64_t values before/after canonical base-128 encoding.
 * Every input array count and encoded byte count is at most M3_PACKED_CODEC_MAX.
 * Base-128 is minimal: no terminal zero after continuation, no truncation, and
 * byte ten must be exactly 0x01. All buffers are disjoint and caller-owned.
 * A failure sets *written to zero and leaves the complete destination untouched.
 * cap/written measure bytes for encoders and int64_t elements for decoders. */
int m3_codec_encode_packed(const int64_t *src, size_t n, int bits,
                             uint8_t *dst, size_t cap, size_t *written);
int m3_codec_decode_packed(const uint8_t *src, size_t n, int bits,
                             int64_t *dst, size_t cap, size_t *written);
int m3_codec_encode_unsigned64(const int64_t *src, size_t n,
                                 uint8_t *dst, size_t cap, size_t *written);
int m3_codec_decode_unsigned64(const uint8_t *src, size_t n,
                                 int64_t *dst, size_t cap, size_t *written);
int m3_codec_encode_zigzag64(const int64_t *src, size_t n,
                               uint8_t *dst, size_t cap, size_t *written);
int m3_codec_decode_zigzag64(const uint8_t *src, size_t n,
                               int64_t *dst, size_t cap, size_t *written);

#ifdef __cplusplus
}
#endif

#endif
