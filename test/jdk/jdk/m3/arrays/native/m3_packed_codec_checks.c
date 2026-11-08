/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project */
/* SPDX-License-Identifier: Apache-2.0 */
/* Modified 2026-10-29: target-owned M3 arrays codec adaptation from Synexia
 * 6f878f9e5d6c5044eda15efb9e743594e70ce7f7; six packed/full64 operations only. */
#include "m3_packed_codec.h"
#include <stdio.h>
#include <stdlib.h>
static unsigned long m3_checks;
#define M3_CHECK(condition) do { \
    ++m3_checks; \
    if (!(condition)) { \
        fprintf(stderr, "M3_CHECK_FAILED %s:%d: %s\n", __FILE__, __LINE__, #condition); \
        exit(EXIT_FAILURE); \
    } \
} while (0)
#include <limits.h>
#include <stdio.h>
#include <string.h>

#define COUNT(array) (sizeof(array) / sizeof((array)[0]))
#define SENTINEL INT64_C(0x112233445566)

typedef int (*encoder64)(const int64_t *, size_t, uint8_t *, size_t, size_t *);
typedef int (*decoder64)(const uint8_t *, size_t, int64_t *, size_t, size_t *);
static const encoder64 encoders[] = {
    m3_codec_encode_unsigned64, m3_codec_encode_zigzag64
};
static const decoder64 decoders[] = {
    m3_codec_decode_unsigned64, m3_codec_decode_zigzag64
};

static void bytes_are(const uint8_t *bytes, size_t count, uint8_t expected) {
    for (size_t i = 0; i < count; ++i) M3_CHECK(bytes[i] == expected);
}
static void words_are(const int64_t *words, size_t count, int64_t expected) {
    for (size_t i = 0; i < count; ++i) M3_CHECK(words[i] == expected);
}
static void fill_words(int64_t *words, size_t count) {
    for (size_t i = 0; i < count; ++i) words[i] = SENTINEL;
}

/* Literal expected bytes are independent of the production encoder/decoder. */
static void vint_case(size_t mode, const int64_t *values, size_t count,
                      const uint8_t *expected, size_t size) {
    uint8_t bytes[128]; int64_t result[16]; size_t written = 42;
    M3_CHECK(size <= sizeof bytes && count <= COUNT(result));
    memset(bytes, 0xa5, sizeof bytes); fill_words(result, COUNT(result));
    M3_CHECK(encoders[mode](values, count, bytes, size, &written) == M3_CODEC_OK);
    M3_CHECK(written == size && !memcmp(bytes, expected, size));
    bytes_are(bytes + size, sizeof bytes - size, 0xa5);
    M3_CHECK(decoders[mode](expected, size, result, count, &written) == M3_CODEC_OK);
    M3_CHECK(written == count && !memcmp(result, values, count * sizeof(*values)));
    words_are(result + count, COUNT(result) - count, SENTINEL);
    if (size) {
        memset(bytes, 0xa5, sizeof bytes); written = 42;
        M3_CHECK(encoders[mode](values, count, bytes, size - 1, &written) == M3_CODEC_BOUND);
        M3_CHECK(written == 0); bytes_are(bytes, sizeof bytes, 0xa5);
    }
    if (count) {
        fill_words(result, COUNT(result)); written = 42;
        M3_CHECK(decoders[mode](expected, size, result, count - 1, &written) == M3_CODEC_BOUND);
        M3_CHECK(written == 0); words_are(result, COUNT(result), SENTINEL);
    }
}

static void test_vint_literals(void) {
    const int64_t unsigned_values[] = {
        0, 1, 127, 128, 255, 16383, 16384, INT64_MAX, INT64_MIN, -1
    };
    const uint8_t unsigned_bytes[] = {
        0x00, 0x01, 0x7f, 0x80, 0x01, 0xff, 0x01, 0xff, 0x7f,
        0x80, 0x80, 0x01,
        0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0x7f,
        0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x01,
        0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0x01
    };
    const int64_t signed_values[] = {
        0, -1, 1, -2, 2, -64, 63, -65, 64, INT64_MIN, INT64_MAX
    };
    const uint8_t signed_bytes[] = {
        0x00, 0x01, 0x02, 0x03, 0x04, 0x7f, 0x7e, 0x81, 0x01, 0x80, 0x01,
        0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0x01,
        0xfe, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0x01
    };
    vint_case(0, unsigned_values, COUNT(unsigned_values), unsigned_bytes, sizeof unsigned_bytes);
    vint_case(1, signed_values, COUNT(signed_values), signed_bytes, sizeof signed_bytes);

    /* Each seven-bit boundary, including bit 63, has an independently fixed wire shape. */
    for (unsigned bit = 7; bit <= 63; bit += 7) {
        uint8_t expected[10]; memset(expected, 0x80, sizeof expected);
        size_t width = bit / 7 + 1; expected[width - 1] = 1;
        int64_t raw = bit == 63 ? INT64_MIN : INT64_C(1) << bit;
        vint_case(0, &raw, 1, expected, width);
        int64_t positive = INT64_C(1) << (bit - 1);
        vint_case(1, &positive, 1, expected, width);
        int64_t negative = -positive - 1;
        expected[0] = 0x81;
        vint_case(1, &negative, 1, expected, width);
    }
}

static void reject_vint(size_t mode, const uint8_t *bytes, size_t size, int wanted) {
    int64_t output[3]; fill_words(output, COUNT(output)); size_t written = 42;
    M3_CHECK(decoders[mode](bytes, size, output, COUNT(output), &written) == wanted);
    M3_CHECK(written == 0); words_are(output, COUNT(output), SENTINEL);
}

static void test_vint_malformed(void) {
    for (size_t mode = 0; mode < COUNT(decoders); ++mode) {
        for (unsigned width = 2; width <= 10; ++width) {
            uint8_t bytes[10]; memset(bytes, 0x80, sizeof bytes); bytes[width - 1] = 1;
            for (size_t n = 1; n < width; ++n)
                reject_vint(mode, bytes, n, M3_CODEC_FORMAT);
            bytes[width - 1] = 0;
            reject_vint(mode, bytes, width, M3_CODEC_FORMAT);
            uint8_t late[11] = {1}; memcpy(late + 1, bytes, width);
            reject_vint(mode, late, width + 1, M3_CODEC_FORMAT);
        }
        /* Every possible tenth byte is checked for both extreme nine-byte prefixes. */
        for (unsigned prefix = 0; prefix < 2; ++prefix) {
            uint8_t bytes[10]; memset(bytes, prefix ? 0xff : 0x80, sizeof bytes);
            for (unsigned terminal = 0; terminal <= 255; ++terminal) {
                bytes[9] = (uint8_t)terminal;
                if (terminal != 1) {
                    reject_vint(mode, bytes, sizeof bytes, M3_CODEC_FORMAT);
                    uint8_t late[11] = {0}; memcpy(late + 1, bytes, sizeof bytes);
                    reject_vint(mode, late, sizeof late, M3_CODEC_FORMAT);
                } else {
                    int64_t value = mode == 0 ? (prefix ? -1 : INT64_MIN)
                                             : (prefix ? INT64_MIN : INT64_C(1) << 62);
                    vint_case(mode, &value, 1, bytes, sizeof bytes);
                }
            }
        }
        const uint8_t eleven[] = {
            0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x01
        };
        const uint8_t late_truncation[] = {
            0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0x01, 0x80
        };
        reject_vint(mode, eleven, sizeof eleven, M3_CODEC_FORMAT);
        reject_vint(mode, late_truncation, sizeof late_truncation, M3_CODEC_FORMAT);
    }
}

static void test_packed_literals(void) {
    const int widths[] = {40, 48, 56};
    const uint8_t expected[3][42] = {
        {0, 0, 0, 0, 0x80,
         0xff, 0xff, 0xff, 0xff, 0xff,
         0, 0, 0, 0, 0,
         1, 0, 0, 0, 0,
         0xff, 0xff, 0xff, 0xff, 0x7f,
         7, 6, 5, 4, 3},
        {0, 0, 0, 0, 0, 0x80,
         0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
         0, 0, 0, 0, 0, 0,
         1, 0, 0, 0, 0, 0,
         0xff, 0xff, 0xff, 0xff, 0xff, 0x7f,
         7, 6, 5, 4, 3, 2},
        {0, 0, 0, 0, 0, 0, 0x80,
         0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
         0, 0, 0, 0, 0, 0, 0,
         1, 0, 0, 0, 0, 0, 0,
         0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0x7f,
         7, 6, 5, 4, 3, 2, 1}
    };
    for (size_t choice = 0; choice < COUNT(widths); ++choice) {
        int bits = widths[choice]; size_t width = (size_t)(bits / 8);
        int64_t minimum = -(INT64_C(1) << (bits - 1));
        int64_t pattern = INT64_C(0x01020304050607) & ((INT64_C(1) << bits) - 1);
        const int64_t values[] = {minimum, -1, 0, 1, -minimum - 1, pattern};
        uint8_t output[43]; int64_t decoded[7]; size_t written = 42;
        size_t size = COUNT(values) * width;
        memset(output, 0xa5, sizeof output); fill_words(decoded, COUNT(decoded));
        M3_CHECK(m3_codec_encode_packed(values, COUNT(values), bits,
                                         output, size, &written) == M3_CODEC_OK);
        M3_CHECK(written == size && !memcmp(output, expected[choice], size));
        bytes_are(output + size, sizeof output - size, 0xa5);
        M3_CHECK(m3_codec_decode_packed(expected[choice], size, bits,
                                         decoded, COUNT(values), &written) == M3_CODEC_OK);
        M3_CHECK(written == COUNT(values) && !memcmp(decoded, values, sizeof values));
        M3_CHECK(decoded[COUNT(values)] == SENTINEL);

        memset(output, 0xa5, sizeof output); written = 42;
        M3_CHECK(m3_codec_encode_packed(values, COUNT(values), bits,
                                         output, size - 1, &written) == M3_CODEC_BOUND);
        M3_CHECK(written == 0); bytes_are(output, sizeof output, 0xa5);
        fill_words(decoded, COUNT(decoded)); written = 42;
        M3_CHECK(m3_codec_decode_packed(expected[choice], size, bits,
                                         decoded, COUNT(values) - 1, &written) == M3_CODEC_BOUND);
        M3_CHECK(written == 0); words_are(decoded, COUNT(decoded), SENTINEL);
        M3_CHECK(m3_codec_decode_packed(expected[choice], size - 1, bits,
                                         decoded, COUNT(decoded), &written) == M3_CODEC_FORMAT);
        M3_CHECK(written == 0); words_are(decoded, COUNT(decoded), SENTINEL);

        const int64_t invalid[] = {minimum - 1, -minimum, INT64_MIN, INT64_MAX};
        for (size_t i = 0; i < COUNT(invalid); ++i) {
            int64_t late[] = {0, invalid[i]}; written = 42;
            M3_CHECK(m3_codec_encode_packed(late, COUNT(late), bits,
                                             output, sizeof output, &written) == M3_CODEC_FORMAT);
            M3_CHECK(written == 0); bytes_are(output, sizeof output, 0xa5);
        }
        M3_CHECK(m3_codec_encode_packed(NULL, 0, bits, NULL, 0, &written) == M3_CODEC_OK);
        M3_CHECK(written == 0);
        M3_CHECK(m3_codec_decode_packed(NULL, 0, bits, NULL, 0, &written) == M3_CODEC_OK);
        M3_CHECK(written == 0);
    }
    const int invalid_bits[] = {-1, 0, 8, 32, 39, 41, 47, 49, 55, 57, 64, INT_MAX};
    for (size_t i = 0; i < COUNT(invalid_bits); ++i) {
        size_t written = 42;
        M3_CHECK(m3_codec_encode_packed(NULL, 0, invalid_bits[i], NULL, 0,
                                         &written) == M3_CODEC_FORMAT);
        M3_CHECK(written == 0);
        written = 42;
        M3_CHECK(m3_codec_decode_packed(NULL, 0, invalid_bits[i], NULL, 0,
                                         &written) == M3_CODEC_FORMAT);
        M3_CHECK(written == 0);
    }
}

/* Separate exact-size objects expose any speculative eight-byte final load to ASan. */
static void test_packed_exact_sources(void) {
    const uint8_t bytes40[5] = {0, 0, 0, 0, 0x80};
    const uint8_t bytes48[6] = {0, 0, 0, 0, 0, 0x80};
    const uint8_t bytes56[7] = {0, 0, 0, 0, 0, 0, 0x80};
    const uint8_t *sources[] = {bytes40, bytes48, bytes56};
    const int widths[] = {40, 48, 56};
    for (size_t i = 0; i < COUNT(widths); ++i) {
        int64_t result[2] = {SENTINEL, SENTINEL}; size_t written = 42;
        M3_CHECK(m3_codec_decode_packed(sources[i], (size_t)(widths[i] / 8), widths[i],
                                         result, 1, &written) == M3_CODEC_OK);
        M3_CHECK(written == 1 && result[0] == -(INT64_C(1) << (widths[i] - 1)));
        M3_CHECK(result[1] == SENTINEL);
    }
}

static void test_limits_and_pointers(void) {
    int64_t values[M3_PACKED_CODEC_MAX + 1u] = {0};
    int64_t decoded[M3_PACKED_CODEC_MAX];
    uint8_t bytes[M3_PACKED_CODEC_MAX + 1u];
    size_t written = 42;
    for (size_t mode = 0; mode < COUNT(encoders); ++mode) {
        values[M3_PACKED_CODEC_MAX - 1u] = 0;
        memset(bytes, 0xa5, sizeof bytes);
        M3_CHECK(encoders[mode](values, M3_PACKED_CODEC_MAX, bytes,
                               M3_PACKED_CODEC_MAX, &written) == M3_CODEC_OK);
        M3_CHECK(written == M3_PACKED_CODEC_MAX);
        bytes_are(bytes, M3_PACKED_CODEC_MAX, 0);
        M3_CHECK(bytes[M3_PACKED_CODEC_MAX] == 0xa5);
        M3_CHECK(decoders[mode](bytes, M3_PACKED_CODEC_MAX, decoded,
                               COUNT(decoded), &written) == M3_CODEC_OK);
        M3_CHECK(written == M3_PACKED_CODEC_MAX);
        words_are(decoded, COUNT(decoded), 0);

        values[M3_PACKED_CODEC_MAX - 1u] = mode == 0 ? 128 : 64;
        memset(bytes, 0xa5, sizeof bytes); written = 42;
        M3_CHECK(encoders[mode](values, M3_PACKED_CODEC_MAX, bytes,
                               sizeof bytes, &written) == M3_CODEC_BOUND);
        M3_CHECK(written == 0); bytes_are(bytes, sizeof bytes, 0xa5);
        M3_CHECK(encoders[mode](values, COUNT(values), bytes,
                               sizeof bytes, &written) == M3_CODEC_BOUND);
        M3_CHECK(written == 0); bytes_are(bytes, sizeof bytes, 0xa5);
        fill_words(decoded, COUNT(decoded));
        M3_CHECK(decoders[mode](bytes, sizeof bytes, decoded,
                               COUNT(decoded), &written) == M3_CODEC_BOUND);
        M3_CHECK(written == 0); words_are(decoded, COUNT(decoded), SENTINEL);

        M3_CHECK(encoders[mode](NULL, 0, NULL, 0, &written) == M3_CODEC_OK && written == 0);
        M3_CHECK(decoders[mode](NULL, 0, NULL, 0, &written) == M3_CODEC_OK && written == 0);
        M3_CHECK(encoders[mode](NULL, 1, bytes, sizeof bytes, &written) == M3_CODEC_ARGUMENT);
        M3_CHECK(written == 0); bytes_are(bytes, sizeof bytes, 0xa5);
        M3_CHECK(decoders[mode](NULL, 1, decoded, COUNT(decoded), &written) == M3_CODEC_ARGUMENT);
        M3_CHECK(written == 0); words_are(decoded, COUNT(decoded), SENTINEL);
        M3_CHECK(encoders[mode](values, 1, NULL, 1, &written) == M3_CODEC_ARGUMENT);
        M3_CHECK(written == 0);
        M3_CHECK(decoders[mode](bytes, 1, NULL, 1, &written) == M3_CODEC_ARGUMENT);
        M3_CHECK(written == 0);
        M3_CHECK(encoders[mode](values, 1, bytes, sizeof bytes, NULL) == M3_CODEC_ARGUMENT);
        bytes_are(bytes, sizeof bytes, 0xa5);
        M3_CHECK(decoders[mode](bytes, 1, decoded, COUNT(decoded), NULL) == M3_CODEC_ARGUMENT);
        words_are(decoded, COUNT(decoded), SENTINEL);
    }
    memset(values, 0, sizeof values);
    const int widths[] = {40, 48, 56};
    for (size_t i = 0; i < COUNT(widths); ++i) {
        int bits = widths[i]; size_t width = (size_t)(bits / 8);
        size_t count = M3_PACKED_CODEC_MAX / width;
        memset(bytes, 0xa5, sizeof bytes);
        M3_CHECK(m3_codec_encode_packed(values, count, bits,
                                         bytes, count * width, &written) == M3_CODEC_OK);
        M3_CHECK(written == count * width); bytes_are(bytes, written, 0);
        bytes_are(bytes + written, sizeof bytes - written, 0xa5);
        fill_words(decoded, COUNT(decoded));
        M3_CHECK(m3_codec_decode_packed(bytes, count * width, bits,
                                         decoded, count, &written) == M3_CODEC_OK);
        M3_CHECK(written == count); words_are(decoded, count, 0);
        words_are(decoded + count, COUNT(decoded) - count, SENTINEL);
        memset(bytes, 0xa5, sizeof bytes); written = 42;
        M3_CHECK(m3_codec_encode_packed(values, count + 1, bits,
                                         bytes, sizeof bytes, &written) == M3_CODEC_BOUND);
        M3_CHECK(written == 0); bytes_are(bytes, sizeof bytes, 0xa5);
        M3_CHECK(m3_codec_encode_packed(values, COUNT(values), bits,
                                         bytes, sizeof bytes, &written) == M3_CODEC_BOUND);
        M3_CHECK(written == 0); bytes_are(bytes, sizeof bytes, 0xa5);
        fill_words(decoded, COUNT(decoded));
        M3_CHECK(m3_codec_decode_packed(bytes, sizeof bytes, bits,
                                         decoded, COUNT(decoded), &written) == M3_CODEC_BOUND);
        M3_CHECK(written == 0); words_are(decoded, COUNT(decoded), SENTINEL);
        M3_CHECK(m3_codec_encode_packed(NULL, 1, bits,
                                         bytes, sizeof bytes, &written) == M3_CODEC_ARGUMENT);
        M3_CHECK(written == 0); bytes_are(bytes, sizeof bytes, 0xa5);
        M3_CHECK(m3_codec_decode_packed(NULL, width, bits,
                                         decoded, COUNT(decoded), &written) == M3_CODEC_ARGUMENT);
        M3_CHECK(written == 0); words_are(decoded, COUNT(decoded), SENTINEL);
        M3_CHECK(m3_codec_encode_packed(values, 1, bits, NULL, width,
                                         &written) == M3_CODEC_ARGUMENT && written == 0);
        M3_CHECK(m3_codec_decode_packed(bytes, width, bits, NULL, 1,
                                         &written) == M3_CODEC_ARGUMENT && written == 0);
        M3_CHECK(m3_codec_encode_packed(values, 1, bits, bytes, sizeof bytes,
                                         NULL) == M3_CODEC_ARGUMENT);
        bytes_are(bytes, sizeof bytes, 0xa5);
        M3_CHECK(m3_codec_decode_packed(bytes, width, bits, decoded, COUNT(decoded),
                                         NULL) == M3_CODEC_ARGUMENT);
        words_are(decoded, COUNT(decoded), SENTINEL);
    }
}

int main(void) {
    test_vint_literals();
    test_vint_malformed();
    test_packed_literals();
    test_packed_exact_sources();
    test_limits_and_pointers();
    printf("M3_PACKED_CODEC_KERNEL_PASS assertions=%lu packed_widths=3 "
           "full64_modes=2 tenth_bytes=256 failure_output=untouched\n", m3_checks);
    return 0;
}
