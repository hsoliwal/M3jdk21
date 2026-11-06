/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0 */
#include "m3_utf8_kmp.h"
#include <stdio.h>
#include <stdint.h>
#include <string.h>

static unsigned long checks;
static uint64_t rng = UINT64_C(20261031);

static unsigned int next_value(void) {
    rng ^= rng << 13;
    rng ^= rng >> 7;
    rng ^= rng << 17;
    return (unsigned int)(rng >> 16);
}

static void check(int value, const char *label) {
    checks++;
    if (!value) {
        fprintf(stderr, "M3_UTF8_KMP_FAIL %s check=%lu\n", label, checks);
        exit(1);
    }
}

static int oracle(const unsigned char *source, size_t length,
                  const unsigned char *needle, size_t size) {
    if (size == 0) return 1;
    if (size > length) return 0;
    for (size_t i = 0; i <= length - size; i++) {
        if (memcmp(source + i, needle, size) == 0) return 1;
    }
    return 0;
}

static void case_one(const unsigned char *source, size_t length,
                     const unsigned char *needle, size_t size) {
    M3Utf8Kmp plan = m3_kmp_prepare(needle, size);
    int expected = oracle(source, length, needle, size);
    check(m3_kmp_contains(&plan, source, length) == expected, "prepared/oracle");
    /* Simulate the exact allocation-failure fallback, without a global hook. */
    free(plan.failure);
    plan.failure = NULL;
    check(m3_kmp_contains(&plan, source, length) == expected, "fallback/oracle");
    m3_kmp_release(&plan);
    m3_kmp_release(&plan);
}

/* Independent plan geometry and known-prefix oracle. No JNI API additions. */
static void compact_failure_lane(void) {
    const unsigned char needle[] = "aabaaac";
    const uint32_t expected[] = {0, 1, 0, 1, 2, 2, 0};
    M3Utf8Kmp plan = m3_kmp_prepare(needle, sizeof(needle) - 1);
    check(sizeof(*plan.failure) == sizeof(uint32_t), "index width");
    check(plan.failure != NULL, "small plan must fit");
    for (size_t i = 0; i < sizeof(expected) / sizeof(expected[0]); i++) {
        check(plan.failure[i] == expected[i], "prefix failure lane");
    }
    m3_kmp_release(&plan);
    M3Utf8Kmp huge = m3_kmp_prepare(needle, SIZE_MAX);
    check(huge.failure == NULL, "overflow-safe fallback");
    check(huge.length == SIZE_MAX, "fallback retains needle length");
    m3_kmp_release(&huge);
}

int main(void) {
    const unsigned char ignored[] = {0};
    const unsigned char binary[] = {0, 255, 128, 'a', 0, 255, 0, 128};
    const unsigned char nul[] = {0, 255, 0};
    case_one(ignored, 0, ignored, 0);
    case_one(ignored, 0, (const unsigned char *)"a", 1);
    case_one(binary, sizeof(binary), nul, sizeof(nul));
    case_one(binary, sizeof(binary), (const unsigned char *)"a", 1);
    case_one(binary, sizeof(binary), (const unsigned char *)"z", 1);
    case_one((const unsigned char *)"ababababac", 10,
             (const unsigned char *)"ababac", 6);
    case_one((const unsigned char *)"aaaaaaaaaaaaaaaaaaaaaaaaab", 26,
             (const unsigned char *)"aaaaaaaab", 9);
    case_one((const unsigned char *)"aaaaa", 5,
             (const unsigned char *)"aaaaaa", 6);
    compact_failure_lane();
    unsigned char repeated[4096];
    unsigned char nearMiss[1024];
    memset(repeated, 'a', sizeof(repeated));
    memset(nearMiss, 'a', sizeof(nearMiss));
    nearMiss[sizeof(nearMiss) - 1] = 'b';
    case_one(repeated, sizeof(repeated), nearMiss, sizeof(nearMiss));
    repeated[sizeof(repeated) - 1] = 'b';
    case_one(repeated, sizeof(repeated), nearMiss, sizeof(nearMiss));
    unsigned char text[256];
    unsigned char pattern[96];
    for (int i = 0; i < 100000; i++) {
        size_t n = next_value() % (sizeof(text) + 1U);
        size_t m = next_value() % (sizeof(pattern) + 1U);
        unsigned int kind = next_value() % 5U;
        for (size_t j = 0; j < n; j++) {
            unsigned int value = next_value();
            text[j] = (unsigned char)(kind == 0 ? 'a' : kind == 1 ? (value % 3U)
                : kind == 2 ? 0 : value);
        }
        for (size_t j = 0; j < m; j++) {
            unsigned int value = next_value();
            pattern[j] = (unsigned char)(kind == 0 ? 'a' : kind == 1 ? (value % 3U)
                : kind == 2 ? 0 : value);
        }
        if (n >= m && m != 0 && (next_value() & 3U) == 0U) {
            size_t start = next_value() % (n - m + 1U);
            memcpy(pattern, text + start, m);
        }
        case_one(text, n, pattern, m);
    }
    printf("M3_UTF8_KMP_PASS checks=%lu cases=100010 prefix_checks=11 index_bits=%zu\n",
           checks, sizeof(uint32_t) * 8U);
    return 0;
}
