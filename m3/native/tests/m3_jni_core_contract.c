/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
#include "m3_jni_core.h"
#include <stdlib.h>
#include <limits.h>
#include <stdint.h>
#include <stdio.h>

_Static_assert(sizeof(jbyte) == 1, "JNI byte must be 8 bits");
_Static_assert(sizeof(jchar) == 2, "JNI char must be 16 bits");
_Static_assert(sizeof(jint) == 4, "JNI int must be 32 bits");
_Static_assert(sizeof(jlong) == 8, "JNI long must be 64 bits");

#define CHECK(expr) do { if (!(expr)) { \
  fprintf(stderr, "FAIL %s:%d: %s\n", __FILE__, __LINE__, #expr); exit(1); \
} } while (0)

/* Independent per-bit oracle: deliberately not the tested AND-equality formula. */
static int reference_mask(uint64_t bits, uint64_t required, uint64_t excluded, int width) {
    for (int shift = 0; shift < width; ++shift) {
        uint64_t flag = UINT64_C(1) << shift;
        if ((required & flag) != 0 && (bits & flag) == 0) return 0;
        if ((excluded & flag) != 0 && (bits & flag) != 0) return 0;
    }
    return 1;
}

static void check_ranges(void) {
    CHECK(m3_jni_valid_window(0, 0, 0));
    CHECK(m3_jni_valid_window(100, 10, 90));
    CHECK(m3_jni_valid_window(INT_MAX, INT_MAX, 0));
    CHECK(m3_jni_valid_window(INT_MAX, 0, INT_MAX));
    CHECK(!m3_jni_valid_window(100, -1, 1));
    CHECK(!m3_jni_valid_window(100, 1, -1));
    CHECK(!m3_jni_valid_window(100, INT_MAX, INT_MAX));
    CHECK(!m3_jni_valid_window(100, 99, 2));
    CHECK(!m3_jni_valid_window(0, 1, 0));
    CHECK(!m3_jni_valid_window(-1, 0, 0));
}

static void check_unsigned_masks(void) {
    for (int bits = 0; bits < 256; ++bits) {
        for (int required = 0; required < 256; ++required) {
            for (int excluded = 0; excluded < 256; ++excluded) {
                int expected = reference_mask((uint8_t)bits, (uint8_t)required,
                        (uint8_t)excluded, 8);
                CHECK(m3_jni_matches_u8((jbyte)(uint8_t)bits,
                            (jint)required, (jint)excluded) == expected);
            }
        }
    }
    CHECK(!m3_jni_matches_u8((jbyte)255, 256, 0));
    CHECK(!m3_jni_matches_u8((jbyte)255, 0, -1));
}

static void check_full_width_masks(void) {
    const int32_t ints[] = {INT32_MIN, -1, 0, 1, INT32_MAX, 0x55555555};
    const int64_t longs[] = {INT64_MIN, -1, 0, 1, INT64_MAX, INT64_C(0x5555555555555555)};
    for (size_t a = 0; a < sizeof(ints) / sizeof(ints[0]); ++a) {
        for (size_t b = 0; b < sizeof(ints) / sizeof(ints[0]); ++b) {
            for (size_t c = 0; c < sizeof(ints) / sizeof(ints[0]); ++c) {
                int expected = reference_mask((uint32_t)ints[a], (uint32_t)ints[b],
                        (uint32_t)ints[c], 32);
                CHECK(m3_jni_matches_u32(ints[a], ints[b], ints[c]) == expected);
            }
        }
    }
    for (size_t a = 0; a < sizeof(longs) / sizeof(longs[0]); ++a) {
        for (size_t b = 0; b < sizeof(longs) / sizeof(longs[0]); ++b) {
            for (size_t c = 0; c < sizeof(longs) / sizeof(longs[0]); ++c) {
                int expected = reference_mask((uint64_t)longs[a], (uint64_t)longs[b],
                        (uint64_t)longs[c], 64);
                CHECK(m3_jni_matches_u64(longs[a], longs[b], longs[c]) == expected);
            }
        }
    }
}

int main(void) {
    check_ranges();
    check_unsigned_masks();
    check_full_width_masks();
    puts("M3JDK21 JNI common contract PASS: window/mask cases");
    return 0;
}
