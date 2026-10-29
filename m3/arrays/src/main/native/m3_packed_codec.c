/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project */
/* SPDX-License-Identifier: Apache-2.0 */
/* Modified 2026-10-29: target-owned M3 arrays codec adaptation from Synexia
 * 6f878f9e5d6c5044eda15efb9e743594e70ce7f7; six packed/full64 operations only. */
#include "m3_packed_codec.h"
#include <limits.h>

static size_t packed_width(int bits) {
    return bits == 40 || bits == 48 || bits == 56 ? (size_t)(bits / 8) : 0;
}

int m3_codec_encode_packed(const int64_t *src, size_t n, int bits,
                             uint8_t *dst, size_t cap, size_t *written) {
    if (!written) return M3_CODEC_ARGUMENT;
    *written = 0;
    if ((n && !src) || (cap && !dst)) return M3_CODEC_ARGUMENT;
    size_t width = packed_width(bits);
    if (!width) return M3_CODEC_FORMAT;
    if (n > M3_PACKED_CODEC_MAX) return M3_CODEC_BOUND;
    int64_t sign = INT64_C(1) << (bits - 1);
    for (size_t i = 0; i < n; ++i) {
        if (src[i] < -sign || src[i] > sign - 1) return M3_CODEC_FORMAT;
    }
    /* n <= 4096 and width <= 7, so this multiplication cannot overflow size_t. */
    size_t needed = n * width;
    if (needed > cap || needed > M3_PACKED_CODEC_MAX) return M3_CODEC_BOUND;
    size_t at = 0;
    for (size_t i = 0; i < n; ++i) {
        uint64_t value = (uint64_t)src[i];
        for (size_t lane = 0; lane < width; ++lane) {
            dst[at++] = (uint8_t)(value & UINT64_C(255));
            value >>= 8;
        }
    }
    *written = at;
    return M3_CODEC_OK;
}

int m3_codec_decode_packed(const uint8_t *src, size_t n, int bits,
                             int64_t *dst, size_t cap, size_t *written) {
    if (!written) return M3_CODEC_ARGUMENT;
    *written = 0;
    if ((n && !src) || (cap && !dst)) return M3_CODEC_ARGUMENT;
    size_t width = packed_width(bits);
    if (!width) return M3_CODEC_FORMAT;
    if (n > M3_PACKED_CODEC_MAX) return M3_CODEC_BOUND;
    if (n % width) return M3_CODEC_FORMAT;
    size_t count = n / width;
    if (count > cap) return M3_CODEC_BOUND;
    uint64_t sign = UINT64_C(1) << (bits - 1);
    size_t at = 0;
    for (size_t i = 0; i < count; ++i) {
        uint64_t value = 0;
        for (size_t lane = 0; lane < width; ++lane)
            value |= (uint64_t)src[at++] << (8 * lane);
        /* Both casts are representable: widths stop at 56 bits. */
        int64_t decoded = (int64_t)(value & (sign - 1));
        if (value & sign) decoded -= (int64_t)sign;
        dst[i] = decoded;
    }
    *written = count;
    return M3_CODEC_OK;
}

/* Conversion is defined for every signed input, including INT64_MIN. */
static uint64_t encode64_value(int64_t value, int zigzag) {
    uint64_t bits = (uint64_t)value;
    return zigzag ? (bits << 1) ^ (UINT64_C(0) - (uint64_t)(value < 0)) : bits;
}

static int64_t decode64_value(uint64_t value, int zigzag) {
    if (zigzag) {
        int64_t magnitude = (int64_t)(value >> 1);
        return value & UINT64_C(1) ? -magnitude - 1 : magnitude;
    }
    /* Do not cast an out-of-range unsigned value to signed int64_t. */
    return value <= (uint64_t)INT64_MAX ? (int64_t)value
            : -1 - (int64_t)(UINT64_MAX - value);
}

static int encode64(const int64_t *src, size_t n, uint8_t *dst, size_t cap,
                    int zigzag, size_t *written) {
    if (!written) return M3_CODEC_ARGUMENT;
    *written = 0;
    if ((n && !src) || (cap && !dst)) return M3_CODEC_ARGUMENT;
    if (n > M3_PACKED_CODEC_MAX) return M3_CODEC_BOUND;
    size_t needed = 0;
    for (size_t i = 0; i < n; ++i) {
        uint64_t value = encode64_value(src[i], zigzag);
        do { ++needed; value >>= 7; } while (value);
    }
    if (needed > cap || needed > M3_PACKED_CODEC_MAX) return M3_CODEC_BOUND;
    size_t at = 0;
    for (size_t i = 0; i < n; ++i) {
        uint64_t value = encode64_value(src[i], zigzag);
        do {
            uint8_t payload = (uint8_t)(value & UINT64_C(127));
            value >>= 7;
            dst[at++] = value ? (uint8_t)(payload | 128u) : payload;
        } while (value);
    }
    *written = at;
    return M3_CODEC_OK;
}

static int decode64(const uint8_t *src, size_t n, int64_t *dst, size_t cap,
                    int zigzag, size_t *written) {
    if (!written) return M3_CODEC_ARGUMENT;
    *written = 0;
    if ((n && !src) || (cap && !dst)) return M3_CODEC_ARGUMENT;
    if (n > M3_PACKED_CODEC_MAX) return M3_CODEC_BOUND;
    size_t count = 0;
    unsigned group = 0;
    /* Preflight the complete sequence, including any malformed late value. */
    for (size_t i = 0; i < n; ++i) {
        unsigned b = src[i];
        if (group == 9) {
            if (b != 1u) return M3_CODEC_FORMAT;
            ++count;
            group = 0;
        } else if (b & 128u) {
            ++group;
        } else {
            if (group && !(b & 127u)) return M3_CODEC_FORMAT;
            ++count;
            group = 0;
        }
    }
    if (group) return M3_CODEC_FORMAT;
    if (count > cap) return M3_CODEC_BOUND;
    uint64_t value = 0;
    unsigned shift = 0;
    size_t at = 0;
    for (size_t i = 0; i < n; ++i) {
        unsigned b = src[i];
        /* Preflight guarantees shift <= 63 and the tenth payload is exactly one. */
        value |= (uint64_t)(b & 127u) << shift;
        if (b & 128u) shift += 7;
        else {
            dst[at++] = decode64_value(value, zigzag);
            value = 0;
            shift = 0;
        }
    }
    *written = at;
    return M3_CODEC_OK;
}

int m3_codec_encode_unsigned64(const int64_t *src, size_t n,
                                 uint8_t *dst, size_t cap, size_t *written) {
    return encode64(src, n, dst, cap, 0, written);
}
int m3_codec_decode_unsigned64(const uint8_t *src, size_t n,
                                 int64_t *dst, size_t cap, size_t *written) {
    return decode64(src, n, dst, cap, 0, written);
}
int m3_codec_encode_zigzag64(const int64_t *src, size_t n,
                               uint8_t *dst, size_t cap, size_t *written) {
    return encode64(src, n, dst, cap, 1, written);
}
int m3_codec_decode_zigzag64(const uint8_t *src, size_t n,
                               int64_t *dst, size_t cap, size_t *written) {
    return decode64(src, n, dst, cap, 1, written);
}
