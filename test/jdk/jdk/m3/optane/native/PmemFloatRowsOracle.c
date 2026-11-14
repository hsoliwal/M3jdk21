// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
/* Compile the actual native atom, not a copied candidate implementation. */
#include "../../../../../../src/jdk.m3.io/share/native/libm3optane/m3pmemsdk.c"

static uint32_t seed = UINT32_C(0x91c0ffee);
static uint32_t next_bits(void) {
    uint32_t x = seed;
    x ^= x << 13; x ^= x >> 17; x ^= x << 5;
    seed = x;
    return x;
}

int main(void) {
    unsigned char bytes[4096 * 4 + 8];
    jfloat values[4096];
    uint32_t expected[4096];
    size_t comparisons = 0;
    const uint32_t special[] = {0, UINT32_C(0x80000000), UINT32_C(0x7f800000),
        UINT32_C(0xff800000), UINT32_C(0x7fc12345), UINT32_C(0x7f812345),
        UINT32_C(0xff812345), 1, UINT32_C(0x007fffff), UINT32_C(0x7f7fffff)};
    const size_t lengths[] = {0, 1, 2, 17, 4095, 4096};
    for (size_t alignment = 0; alignment < 8; alignment++) {
        for (size_t fixture = 0; fixture < sizeof(lengths) / sizeof(lengths[0]); fixture++) {
            size_t length = lengths[fixture];
            for (size_t i = 0; i < length; i++) {
                uint32_t bits = i < sizeof(special) / sizeof(special[0]) ? special[i] : next_bits();
                expected[i] = bits;
                for (size_t b = 0; b < 4; b++) bytes[alignment + 4 * i + b] = (unsigned char)(bits >> (b * 8));
            }
            m3_float_rows_decode(bytes + alignment, values, length);
            for (size_t i = 0; i < length; i++) {
                uint32_t actual;
                memcpy(&actual, values + i, sizeof(actual));
                if (actual != expected[i]) {
                    (void)fprintf(stderr, "FP32 LE mismatch alignment=%zu length=%zu index=%zu\n", alignment, length, i);
                    return 1;
                }
                comparisons++;
            }
        }
    }
    m3_float_rows_decode(NULL, NULL, 0);
    (void)printf("PASS exact-source C17 FP32 LE oracle comparisons=%zu alignments=8\n", comparisons);
    return 0;
}
