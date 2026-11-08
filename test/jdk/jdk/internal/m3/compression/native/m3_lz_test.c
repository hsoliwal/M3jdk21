// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/6fdee5330b8ab005005785aeff0d1105ba40d12c/synexia-algo/native/compression/tests/m3_lz_test.c blob e3ae86ca5354; recipe m3/compression/recipe/compression-20261106/relocate.py v3
#include <stdint.h>
#include <stddef.h>

size_t m3_lz_prefix(const uint8_t *bytes, size_t left, size_t right, size_t limit);

static size_t scalar(const uint8_t *bytes, size_t a, size_t b, size_t max) {
    size_t matched = 0;
    while (matched < max && bytes[a + matched] == bytes[b + matched]) {
        matched++;
    }
    return matched;
}

int main(void) {
    uint8_t data[2048] = {0};
    uint64_t seed = UINT64_C(0x123456789abcdef0);
    for (size_t i = 0; i < sizeof(data); i++) {
        seed ^= seed << 13;
        seed ^= seed >> 7;
        seed ^= seed << 17;
        data[i] = (uint8_t) seed;
    }
    for (size_t i = 0; i < 1024; i++) {
        size_t a = i;
        size_t b = i % 2 == 0 ? i : (1023 - i);
        size_t limit = (i % 96) < (sizeof(data) - (a > b ? a : b))
                ? i % 96 : sizeof(data) - (a > b ? a : b);
        if (m3_lz_prefix(data, a, b, limit) != scalar(data, a, b, limit)) return 1;
    }
    for (size_t i = 0; i < sizeof(data); i++) {
        data[i] = (uint8_t) 'A';
    }
    if (m3_lz_prefix(data, 0, 7, 1000) != 1000) return 2;
    data[155] = (uint8_t) 'B';
    if (m3_lz_prefix(data, 55, 100, 100) != 55) return 3;
    return 0;
}
