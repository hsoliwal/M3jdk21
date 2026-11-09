/* SPDX-License-Identifier: Apache-2.0 */
#include "m3_int_codebook.h"

static unsigned required_bits(size_t count) {
    unsigned bits = 0u;
    size_t max_id = count - 1u;
    while (max_id != 0u) { bits++; max_id >>= 1u; }
    return bits;
}

static uint32_t read_index(const uint8_t *data, size_t at, unsigned bits) {
    uint32_t id = 0u;
    unsigned output_shift = 0u;
    for (unsigned remaining = bits; remaining != 0u;) {
        unsigned shift = (unsigned)(at & 7u);
        unsigned take = remaining < (8u - shift) ? remaining : 8u - shift;
        uint32_t mask = (UINT32_C(1) << take) - 1u;
        id |= (((uint32_t)data[at >> 3u] >> shift) & mask) << output_shift;
        at += take;
        output_shift += take;
        remaining -= take;
    }
    return id;
}

int m3_int_codebook_decode(const uint8_t *ids, size_t id_bytes,
                           const int32_t *dictionary, size_t dictionary_length,
                           size_t length, unsigned bits,
                           int32_t *dst, size_t dst_capacity) {
    if (dictionary_length == 0u || dictionary_length > M3_INT_CODEBOOK_MAX
            || length > M3_INT_CODEBOOK_MAX || id_bytes > M3_INT_CODEBOOK_MAX
            || length > dst_capacity || bits > 12u || bits != required_bits(dictionary_length)
            || (length != 0u && !dst) || !dictionary) return -1;
    size_t expected = (length * (size_t)bits + 7u) >> 3u;
    if (expected != id_bytes || (id_bytes != 0u && !ids)) return -1;
    unsigned tail = (unsigned)((length * (size_t)bits) & 7u);
    if (tail != 0u && ((unsigned)ids[id_bytes - 1u] >> tail) != 0u) return -1;
    for (size_t i = 0; i < length; ++i) {
        if ((size_t)read_index(ids, i * (size_t)bits, bits) >= dictionary_length)
            return -1;
    }
    for (size_t i = 0; i < length; ++i)
        dst[i] = dictionary[read_index(ids, i * (size_t)bits, bits)];
    return 0;
}
