/* SPDX-License-Identifier: Apache-2.0 */
#include "m3_adaptive_bitpack.h"
#include <assert.h>
#include <limits.h>
#include <stdint.h>
#include <stdio.h>
#include <string.h>

static void check(const int32_t *values, size_t n, int32_t base, unsigned width) {
    uint8_t bytes[M3_ADAPTIVE_MAX];
    int32_t restored[M3_ADAPTIVE_MAX];
    uint8_t reference[M3_ADAPTIVE_MAX];
    memset(bytes, 0xa5, sizeof(bytes));
    memset(reference, 0, sizeof(reference));
    size_t size = (n * width + 7u) / 8u;
    assert(m3_adaptive_encode(values, n, base, width, bytes, sizeof(bytes)) == 0);
    for (size_t i = 0; i < n; ++i) {
        uint32_t delta = (uint32_t)((int64_t)values[i] - base);
        for (unsigned bit = 0; bit < width; ++bit) {
            if ((delta & (UINT32_C(1) << bit)) != 0u) {
                size_t position = i * width + bit;
                reference[position / 8u] |= (uint8_t)(1u << (position & 7u));
            }
        }
    }
    assert(memcmp(bytes, reference, size) == 0);
    assert(m3_adaptive_decode(bytes, size, n, base, width, restored, n) == 0);
    for (size_t i = 0; i < n; ++i) assert(restored[i] == values[i]);
    if (size != 0 && ((n * width) & 7u) != 0u) {
        uint8_t corrupt[M3_ADAPTIVE_MAX];
        memcpy(corrupt, bytes, size);
        corrupt[size - 1u] |= (uint8_t)(1u << ((n * width) & 7u));
        assert(m3_adaptive_decode(corrupt, size, n, base, width, restored, n) != 0);
    }
}

int main(void) {
    int32_t constant[] = {INT32_MIN, INT32_MIN, INT32_MIN};
    check(constant, 3u, INT32_MIN, 0u);
    int32_t extremes[] = {INT32_MIN, INT32_MAX};
    check(extremes, 2u, INT32_MIN, 32u);
    for (unsigned bits = 1; bits <= 32u; ++bits) {
        for (size_t n = 1; n <= 47u; ++n) {
            int32_t values[47];
            int64_t base = INT32_MIN;
            uint64_t mask = bits == 32u ? UINT32_MAX : ((UINT64_C(1) << bits) - 1u);
            for (size_t i = 0; i < n; ++i) {
                uint64_t delta = ((uint64_t)(i * 7927u + bits * 8191u) * 0x9e3779b1u) & mask;
                values[i] = (int32_t)(base + (int64_t)delta);
            }
            check(values, n, (int32_t)base, bits);
        }
    }
    uint8_t untouched[4] = {0xaa, 0xaa, 0xaa, 0xaa};
    int32_t invalid[] = {2};
    assert(m3_adaptive_encode(invalid, 1u, 0, 1u, untouched, sizeof(untouched)) != 0);
    assert(untouched[0] == 0xaa);
    int32_t decoded = -17;
    uint8_t overflow[] = {0xff, 0xff, 0xff, 0xff};
    assert(m3_adaptive_decode(overflow, 4u, 1u, INT32_MAX, 32u, &decoded, 1u) != 0);
    assert(decoded == -17);
    assert(m3_adaptive_decode(overflow, 4u, 2u, 0, 32u, &decoded, 1u) != 0);
    assert(m3_adaptive_encode(constant, 3u, INT32_MIN, 33u, untouched, sizeof(untouched)) != 0);
    printf("M3_ADAPTIVE_BITPACK_NATIVE_PASS\n");
    return 0;
}
