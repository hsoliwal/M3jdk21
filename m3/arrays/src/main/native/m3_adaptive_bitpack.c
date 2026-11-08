/* SPDX-License-Identifier: Apache-2.0 */
#include "include/m3_adaptive_bitpack.h"
#include <limits.h>
#include <string.h>

static int geometry(size_t length, unsigned bits, size_t *bytes) {
    if (length > M3_ADAPTIVE_MAX || bits > 32u) return -1;
    size_t n = (length * (size_t)bits + 7u) / 8u;
    if (n > M3_ADAPTIVE_MAX) return -1;
    *bytes = n;
    return 0;
}

static uint32_t read_delta(const uint8_t *src, size_t index, unsigned width) {
    size_t bit = index * (size_t)width;
    uint32_t result = 0u;
    unsigned out = 0u;
    unsigned remaining = width;
    while (remaining != 0u) {
        unsigned offset = (unsigned)(bit & 7u);
        unsigned take = remaining < (8u - offset) ? remaining : (8u - offset);
        uint32_t mask = (1u << take) - 1u;
        result |= (((uint32_t)src[bit / 8u] >> offset) & mask) << out;
        bit += take;
        out += take;
        remaining -= take;
    }
    return result;
}

static void write_delta(uint8_t *dst, size_t index, unsigned width, uint32_t value) {
    size_t bit = index * (size_t)width;
    unsigned remaining = width;
    while (remaining != 0u) {
        unsigned offset = (unsigned)(bit & 7u);
        unsigned take = remaining < (8u - offset) ? remaining : (8u - offset);
        uint32_t mask = (1u << take) - 1u;
        dst[bit / 8u] |= (uint8_t)((value & mask) << offset);
        value >>= take;
        bit += take;
        remaining -= take;
    }
}

int m3_adaptive_encode(const int32_t *src, size_t n, int32_t base, unsigned bits,
                       uint8_t *dst, size_t cap) {
    size_t bytes;
    if (geometry(n, bits, &bytes) != 0 || bytes > cap ||
        (!src && n != 0u) || (!dst && bytes != 0u)) return -1;
    uint64_t maxdelta = bits == 32u ? UINT32_MAX : ((UINT64_C(1) << bits) - 1u);
    for (size_t i = 0; i < n; ++i) {
        int64_t diff = (int64_t)src[i] - (int64_t)base;
        if (diff < 0 || (uint64_t)diff > maxdelta) return -1;
    }
    if (bytes != 0u) memset(dst, 0, bytes);
    for (size_t i = 0; i < n; ++i)
        write_delta(dst, i, bits, (uint32_t)((int64_t)src[i] - base));
    return 0;
}

int m3_adaptive_decode(const uint8_t *src, size_t nbytes, size_t length,
                       int32_t base, unsigned bits, int32_t *dst, size_t cap) {
    size_t expected;
    if (geometry(length, bits, &expected) != 0 || expected != nbytes ||
        length > cap || (!src && nbytes != 0u) || (!dst && length != 0u)) return -1;
    size_t used = (length * (size_t)bits) & 7u;
    if (used != 0u && ((unsigned)src[nbytes - 1u] >> used) != 0u) return -1;
    for (size_t i = 0; i < length; ++i) {
        int64_t value = (int64_t)base + (int64_t)read_delta(src, i, bits);
        if (value < INT32_MIN || value > INT32_MAX) return -1;
    }
    for (size_t i = 0; i < length; ++i)
        dst[i] = (int32_t)((int64_t)base + (int64_t)read_delta(src, i, bits));
    return 0;
}
