/* SPDX-License-Identifier: Apache-2.0 */
#include "m3_int_codebook.h"
#include <assert.h>
#include <stdint.h>
#include <stdio.h>
#include <string.h>
#include <limits.h>

static void write_index(uint8_t *data, size_t position, unsigned bits, size_t value) {
    size_t bit = position * (size_t)bits;
    for (unsigned p = 0; p < bits; p++) {
        if ((value & ((size_t)1 << p)) != 0u)
            data[(bit + p) >> 3u] |= (uint8_t)(1u << ((bit + p) & 7u));
    }
}

int main(void) {
    int32_t dictionary[M3_INT_CODEBOOK_MAX];
    int32_t out[M3_INT_CODEBOOK_MAX];
    uint8_t input[M3_INT_CODEBOOK_MAX];
    size_t cases = 0u;
    dictionary[0] = INT32_MIN;
    for (size_t n = 1u; n <= 100u; ++n) {
        for (size_t k = 1u; k <= 90u; ++k) {
            unsigned bits = 0u;
            while (((size_t)1u << bits) < k) bits++;
            memset(input, 0, sizeof(input));
            for (size_t j = 0u; j < k; j++) dictionary[j] = (int32_t)((int64_t)j * 1031 - 20000);
            for (size_t i = 0u; i < n; i++) write_index(input, i, bits, (i * 7u + n) % k);
            size_t bytes = (n * (size_t)bits + 7u) >> 3u;
            assert(m3_int_codebook_decode(input, bytes, dictionary, k, n, bits, out, n) == 0);
            for (size_t i = 0u; i < n; i++) assert(out[i] == dictionary[(i * 7u + n) % k]);
            cases++;
        }
    }
    int32_t one[] = {7};
    int32_t previous[] = {42};
    uint8_t invalid_id[] = {3};
    assert(m3_int_codebook_decode(invalid_id, 1u, one, 1u, 1u, 2u, previous, 1u) != 0);
    assert(previous[0] == 42);
    int32_t two[] = {INT32_MIN, INT32_MAX};
    uint8_t tail[] = {0x81};
    assert(m3_int_codebook_decode(tail, 1u, two, 2u, 1u, 1u, previous, 1u) != 0);
    assert(previous[0] == 42);
    uint8_t wrong_id[] = {3};
    int32_t three[] = {1, 2, 3};
    assert(m3_int_codebook_decode(wrong_id, 1u, three, 3u, 1u, 2u, previous, 1u) != 0);
    assert(previous[0] == 42);
    assert(m3_int_codebook_decode(NULL, 0u, one, 1u, 3u, 0u, out, 3u) == 0);
    for (size_t i = 0u; i < 3u; i++) assert(out[i] == 7);
    printf("M3_INT_CODEBOOK_NATIVE_PASS cases=%zu\n", cases);
    return 0;
}
