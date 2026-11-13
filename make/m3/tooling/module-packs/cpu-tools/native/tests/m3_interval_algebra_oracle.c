/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project */
/* SPDX-License-Identifier: Apache-2.0 */
/* Native kernel oracle: independent finite occupancy/membership reference; never shipped. */
#include <stdint.h>
#include <limits.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "../m3_interval_algebra.c"

static unsigned long long checks;
static uint32_t state = UINT32_C(0x7d3e41ab);
#define REQUIRE(expr, reason) do { checks++; if (!(expr)) { \
    fprintf(stderr,"FAIL %s at check=%llu\n", reason, checks); exit(4); } } while (0)

static uint32_t next_number(void) {
    state ^= state << 13;
    state ^= state >> 17;
    state ^= state << 5;
    return state;
}

/* Encode a set of 7 integer positions as normalized signed half-open intervals. */
static jsize from_mask(uint32_t mask, jint *out) {
    jsize used = 0;
    for (int i = 0; i < 7;) {
        if ((mask & (UINT32_C(1) << (unsigned)i)) == 0) { i++; continue; }
        out[used++] = (jint)(i - 3);
        while (i < 7 && (mask & (UINT32_C(1) << (unsigned)i)) != 0) i++;
        out[used++] = (jint)(i - 3);
    }
    return used;
}

static uint32_t to_mask(const jint *pairs, jsize length) {
    uint32_t mask = 0;
    for (jsize index = 0; index < length; index += 2) {
        REQUIRE(pairs[index] < pairs[index + 1], "nonempty native endpoint");
        if (index != 0) REQUIRE(pairs[index - 1] < pairs[index], "normalized gap");
        for (int i = 0; i < 7; i++) {
            if (pairs[index] <= i - 3 && i - 3 < pairs[index + 1])
                mask |= UINT32_C(1) << (unsigned)i;
        }
    }
    return mask;
}

static void compare(jint *a, jsize na, jint *b, jsize nb,
                    uint32_t expected, const char *scenario) {
    jint result[16] = {0};
    jint swapped[16] = {0};
    const jsize size = m3_intersect_half_open(a, na, b, nb, NULL);
    const jsize reverse = m3_intersect_half_open(b, nb, a, na, NULL);
    REQUIRE(size >= 0 && size <= 14, "output size bound");
    REQUIRE(size == reverse, "cardinality symmetry");
    REQUIRE(m3_intersect_half_open(a, na, b, nb, result) == size, "count/write parity");
    REQUIRE(m3_intersect_half_open(b, nb, a, na, swapped) == size, "reverse count/write");
    REQUIRE(memcmp(result, swapped, (size_t)size * sizeof(jint)) == 0, "endpoint symmetry");
    REQUIRE(to_mask(result, size) == expected, scenario);
}

static void exhaustive(void) {
    for (uint32_t left = 0; left < 128; left++) {
        jint a[16];
        const jsize na = from_mask(left, a);
        for (uint32_t right = 0; right < 128; right++) {
            jint b[16];
            const jsize nb = from_mask(right, b);
            compare(a, na, b, nb, left & right, "independent truth intersection");
        }
    }
}

static void random_runs(void) {
    /* All intervals are monotone with >1 gaps; containment and empty results. */
    for (int trial = 0; trial < 20000; trial++) {
        jint a[48], b[48];
        jsize na = 0, nb = 0;
        jint cursor = -1000;
        int countA = (int)(next_number() % 12);
        for (int k = 0; k < countA; k++) {
            cursor += (jint)(next_number() % 10) + 1;
            a[na++] = cursor;
            cursor += (jint)(next_number() % 10) + 1;
            a[na++] = cursor;
        }
        cursor = -1000;
        int countB = (int)(next_number() % 12);
        for (int k = 0; k < countB; k++) {
            cursor += (jint)(next_number() % 10) + 1;
            b[nb++] = cursor;
            cursor += (jint)(next_number() % 10) + 1;
            b[nb++] = cursor;
        }
        jint out[48];
        jsize sz = m3_intersect_half_open(a, na, b, nb, out);
        REQUIRE(sz >= 0 && sz <= 48, "random bound");
        REQUIRE(m3_intersect_half_open(a, na, b, nb, NULL) == sz, "random count/emit");
        for (jsize i=0; i<sz; i+=2) {
            REQUIRE(out[i] < out[i+1], "random nonempty");
            if (i>0) REQUIRE(out[i-1] < out[i], "random gaps");
            int fromA = 0, fromB = 0;
            for (jsize k=0; k<na; k+=2) if (a[k]<=out[i] && out[i+1]<=a[k+1]) fromA=1;
            for (jsize k=0; k<nb; k+=2) if (b[k]<=out[i] && out[i+1]<=b[k+1]) fromB=1;
            REQUIRE(fromA && fromB, "random contained by both inputs");
        }
    }
    jint a[] = {INT_MIN, 0, 1, INT_MAX};
    jint b[] = {-10, 12};
    jint expected[] = {-10, 0, 1, 12};
    jint out[4] = {0};
    REQUIRE(m3_intersect_half_open(a, 4, b, 2, out) == 4, "extreme size");
    REQUIRE(memcmp(expected,out,sizeof(out)) == 0, "extreme values");
}

int main(void) {
    exhaustive();
    random_runs();
    printf("M3_INTERVAL_ALGEBRA_KERNEL_PASS checks=%llu\n", checks);
    return 0;
}
