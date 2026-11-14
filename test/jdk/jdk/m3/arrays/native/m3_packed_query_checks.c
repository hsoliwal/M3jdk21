/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors */
/* SPDX-License-Identifier: Apache-2.0 */
#include "m3_packed_codec.h"
#include <limits.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
static unsigned long checks;
#define CHECK(x) do { ++checks; if (!(x)) { fprintf(stderr, "query check failed at %d: %s\n", __LINE__, #x); exit(1); } } while (0)

int main(void) {
    const int widths[] = {40, 48, 56};
    for (size_t w = 0; w < 3; ++w) {
        int bits = widths[w];
        size_t width = (size_t)bits / 8;
        int64_t sign = INT64_C(1) << (bits - 1);
        const int64_t values[] = {-sign, -1, 0, 1, sign - 1};
        uint8_t bytes[35];
        memset(bytes, 0, sizeof bytes);
        /* Literal lane patterns, independent of the production encoder. */
        bytes[width - 1] = 0x80;
        memset(bytes + width, 0xff, width);
        bytes[3 * width] = 1;
        memset(bytes + 4 * width, 0xff, width);
        bytes[5 * width - 1] = 0x7f;
        uint8_t before[35]; memcpy(before, bytes, sizeof bytes);
        const int64_t bounds[] = {INT64_MIN, -sign - 1, -sign, -1, 0, 1, sign - 1, sign, INT64_MAX};
        for (size_t lo = 0; lo < 9; ++lo) for (size_t hi = lo; hi < 9; ++hi) {
            size_t expected = 0, first = SIZE_MAX;
            for (size_t i = 0; i < 5; ++i) if (values[i] >= bounds[lo] && values[i] <= bounds[hi]) {
                if (first == SIZE_MAX) first = i;
                ++expected;
            }
            size_t result = 99;
            CHECK(m3_codec_query_packed(bytes, 5 * width, bits, bounds[lo], bounds[hi], 0, &result) == M3_CODEC_OK);
            CHECK(result == expected);
            CHECK(m3_codec_query_packed(bytes, 5 * width, bits, bounds[lo], bounds[hi], 1, &result) == M3_CODEC_OK);
            CHECK(result == first);
        }
        CHECK(memcmp(bytes, before, sizeof bytes) == 0);
        size_t result = 99;
        CHECK(m3_codec_query_packed(NULL, 0, bits, 0, 0, 0, &result) == M3_CODEC_OK && result == 0);
        CHECK(m3_codec_query_packed(NULL, 0, bits, 0, 0, 1, &result) == M3_CODEC_OK && result == SIZE_MAX);
        CHECK(m3_codec_query_packed(bytes, width - 1, bits, 0, 0, 0, &result) == M3_CODEC_FORMAT && result == 0);
        CHECK(m3_codec_query_packed(bytes, 4097, bits, 0, 0, 0, &result) == M3_CODEC_BOUND && result == 0);
        CHECK(m3_codec_query_packed(NULL, width, bits, 0, 0, 0, &result) == M3_CODEC_ARGUMENT && result == 0);
        CHECK(m3_codec_query_packed(bytes, width, bits, 1, 0, 0, &result) == M3_CODEC_FORMAT && result == 0);
        CHECK(m3_codec_query_packed(bytes, width, bits, 0, 0, 2, &result) == M3_CODEC_ARGUMENT && result == 0);
        CHECK(m3_codec_query_packed(bytes, width, bits, 0, 0, 0, NULL) == M3_CODEC_ARGUMENT);
    }
    size_t result = 99;
    CHECK(m3_codec_query_packed(NULL, 0, 64, 0, 0, 0, &result) == M3_CODEC_FORMAT && result == 0);
    printf("PACKED_QUERY_KERNEL_PASS assertions=%lu\n", checks);
    return 0;
}
