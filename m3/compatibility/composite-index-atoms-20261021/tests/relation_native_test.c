/* Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0 */
#include "m3_state_relation.h"
#include <assert.h>
#include <stdio.h>
#include <string.h>

static uint64_t state = UINT64_C(0x4d334e4154495645);
static uint64_t random_word(void) {
    state ^= state << 13; state ^= state >> 7; state ^= state << 17; return state;
}
static void oracle(const uint64_t *a, const uint64_t *b, uint64_t *out, size_t n) {
    for (size_t i = 0; i < n; ++i) {
        out[i] = 0;
        for (size_t j = 0; j < n; ++j) if ((a[i] & (UINT64_C(1) << j)) != 0) out[i] |= b[j];
    }
}
int main(void) {
    size_t checks = 0;
    for (size_t n = 1; n <= 64; ++n) {
        const uint64_t mask = n == 64 ? UINT64_MAX : (UINT64_C(1) << n) - 1;
        for (size_t trial = 0; trial < 250; ++trial) {
            uint64_t a[64], b[64], expected[64], out[64], alias[192];
            for (size_t i = 0; i < n; ++i) {
                a[i] = (trial % 5 == 0 ? UINT64_C(1) << (n - 1) : random_word()) & mask;
                b[i] = random_word() & mask;
            }
            oracle(a, b, expected, n);
            assert(m3_state_relation_compose(a, b, out, n) == 0);
            assert(memcmp(out, expected, n * sizeof(uint64_t)) == 0); checks += 2;
            memcpy(alias, a, n * sizeof(uint64_t));
            assert(m3_state_relation_compose(alias, b, alias + 1, n) == 0);
            assert(memcmp(alias + 1, expected, n * sizeof(uint64_t)) == 0); checks += 2;
            memcpy(alias + 1, b, n * sizeof(uint64_t));
            assert(m3_state_relation_compose(a, alias + 1, alias, n) == 0);
            assert(memcmp(alias, expected, n * sizeof(uint64_t)) == 0); checks += 2;
            // Inputs may overlap one another as well; output overlaps both.
            for (size_t i = 0; i < 2 * n + 3; ++i) alias[i] = random_word() & mask;
            oracle(alias, alias + 2, expected, n);
            assert(m3_state_relation_compose(alias, alias + 2, alias + 1, n) == 0);
            assert(memcmp(alias + 1, expected, n * sizeof(uint64_t)) == 0); checks += 2;
            memcpy(alias, a, n * sizeof(uint64_t));
            assert(m3_state_relation_repeat(alias, alias + 1, n, 0) == 0);
            for (size_t i = 0; i < n; ++i) { assert(alias[i + 1] == (UINT64_C(1) << i)); ++checks; }
            if (n < 64) {
                a[n - 1] |= UINT64_C(1) << n;
                memset(out, 0x5a, sizeof(out));
                uint64_t before[64]; memcpy(before, out, sizeof(out));
                assert(m3_state_relation_compose(a, b, out, n) == -1);
                assert(memcmp(before, out, sizeof(out)) == 0);
                assert(m3_state_relation_repeat(a, out, n, 0) == -1);
                assert(memcmp(before, out, sizeof(out)) == 0); checks += 4;
            }
        }
    }
    uint64_t a[64] = {0}, out[64] = {0};
    assert(m3_state_relation_compose(NULL, a, out, 1) == -1);
    assert(m3_state_relation_compose(a, NULL, out, 1) == -1);
    assert(m3_state_relation_compose(a, a, NULL, 1) == -1);
    assert(m3_state_relation_compose(a, a, out, 0) == -1);
    assert(m3_state_relation_compose(a, a, out, 65) == -1); checks += 5;
    printf("PASS native alias/domain assertions=%zu\n", checks);
    return 0;
}
