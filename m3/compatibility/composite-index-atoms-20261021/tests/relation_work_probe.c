/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
#include "m3_state_relation.h"
#include <assert.h>
#include <stdio.h>
extern size_t m3_test_expansions;
int main(void) {
    uint64_t a[64], b[64], out[64];
    for (size_t i = 0; i < 64; ++i) { a[i] = UINT64_C(1) << 63; b[i] = UINT64_C(1) << i; }
    assert(m3_state_relation_compose(a, b, out, 64) == 0);
    for (size_t i = 0; i < 64; ++i) assert(out[i] == (UINT64_C(1) << 63));
    printf("native active-row expansions=%zu\n", m3_test_expansions); fflush(stdout);
    assert(m3_test_expansions == 1);
    return 0;
}
