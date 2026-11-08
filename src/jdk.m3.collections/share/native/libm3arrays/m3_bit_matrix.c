/* Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 * Original M3 primitive matrix JNI batch. No retained JNI elements or pointers. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/d4985c2a46d713724b3872c85633655e53d76662/synexia-mat-collections/native/mat_bit_matrix.c blob e6b5cb6ea27f; recipe m3/arrays/recipe/bitmatrix-20261106/relocate.py v3
#include <jni.h>
#include <stdint.h>
#include <limits.h>
#include <stddef.h>

static unsigned trailing_zeroes(uint64_t value) {
#if defined(__GNUC__) || defined(__clang__)
    return (unsigned) __builtin_ctzll(value);
#else
    unsigned count = 0U;
    while ((value & UINT64_C(1)) == 0U) { value >>= 1; count++; }
    return count;
#endif
}

/* Six-stage 64-by-64 broadword transpose, no retained heap or JNI state. */
static void transpose_tile64(uint64_t tile[64]) {
    uint64_t mask = UINT64_C(0xffffffff00000000);
    for (unsigned shift = 32U; shift != 0U; shift >>= 1U) {
        for (unsigned at = 0; at < 64U; at = (at + shift + 1U) & ~shift) {
            uint64_t swap = (tile[at] ^ (tile[at + shift] << shift)) & mask;
            tile[at] ^= swap;
            tile[at + shift] ^= swap >> shift;
        }
        if (shift > 1U) mask ^= mask >> (shift >> 1U);
    }
}

static unsigned popcount64(uint64_t value) {
#if defined(__GNUC__) || defined(__clang__)
    return (unsigned) __builtin_popcountll(value);
#else
    unsigned count = 0;
    while (value != 0U) { value &= value - 1U; ++count; }
    return count;
#endif
}

JNIEXPORT jint JNICALL
Java_jdk_m3_arrays_BitMatrix_nativeAbi0(JNIEnv *env, jclass owner) {
    (void) env; (void) owner;
    return 1;
}

JNIEXPORT jlongArray JNICALL
Java_jdk_m3_arrays_BitMatrix_transposeNative0(
        JNIEnv *env, jclass owner, jlongArray input, jint rows,
        jint columns, jint words_per_row, jint target_words_per_row) {
    (void) owner;
    if (input == NULL || rows < 0 || columns < 0 || words_per_row < 0
            || target_words_per_row < 0) return NULL;
    const jint expected_source_width = (columns >> 6) + ((columns & 63) != 0);
    const jint expected_target_width = (rows >> 6) + ((rows & 63) != 0);
    const int64_t in_length = (int64_t) rows * words_per_row;
    const int64_t out_length = (int64_t) columns * target_words_per_row;
    if (words_per_row != expected_source_width
            || target_words_per_row != expected_target_width
            || in_length > INT_MAX || out_length > INT_MAX
            || (*env)->GetArrayLength(env, input) != (jsize) in_length) return NULL;

    jlongArray output = (*env)->NewLongArray(env, (jsize) out_length);
    if (output == NULL || out_length == 0 || in_length == 0) return output;
    jlong *source = (*env)->GetLongArrayElements(env, input, NULL);
    if (source == NULL) return NULL;
    jlong *target = (*env)->GetLongArrayElements(env, output, NULL);
    if (target == NULL) {
        (*env)->ReleaseLongArrayElements(env, input, source, JNI_ABORT);
        return NULL;
    }

    jint full_rows = 0;
    jint full_words = 0;
    if (rows >= 64 && columns >= 64) {
        unsigned sample = 0U;
        for (unsigned local = 0U; local < 64U; ++local) {
            sample += popcount64((uint64_t) source[(size_t) local
                    * (size_t) words_per_row]);
        }
        /* Bounded deterministic first-tile eligibility probe.
         * False negatives retain the original scalar algorithm exactly. */
        if (sample >= 256U) {
            full_rows = rows & ~63;
            full_words = columns >> 6;
        }
    }
    uint64_t tile[64];
    for (jint row_base = 0; row_base < full_rows; row_base += 64) {
        size_t target_word = (size_t) ((unsigned) row_base >> 6);
        for (jint word = 0; word < words_per_row; ++word) {
            if (word < full_words) {
                unsigned population = 0U;
                for (unsigned local = 0U; local < 64U; ++local) {
                    uint64_t value = (uint64_t) source[(size_t) (row_base + (jint) local)
                            * (size_t) words_per_row + (size_t) word];
                    tile[local] = value;
                    population += popcount64(value);
                }
                if (population >= 256U) {
                    transpose_tile64(tile);
                    size_t base_column = (size_t) word << 6;
                    for (size_t local = 0; local < 64U; ++local) {
                        target[(base_column + local) * (size_t) target_words_per_row
                                + target_word] = (jlong) tile[local];
                    }
                    continue;
                }
            }
            for (unsigned local = 0; local < 64U; ++local) {
                uint64_t value = (uint64_t) source[(size_t) (row_base + (jint) local)
                        * (size_t) words_per_row + (size_t) word];
                while (value != UINT64_C(0)) {
                    size_t column = ((size_t) word << 6) + trailing_zeroes(value);
                    if (column < (size_t) columns) {
                        size_t index = column * (size_t) target_words_per_row + target_word;
                        target[index] = (jlong) (((uint64_t) target[index])
                                | (UINT64_C(1) << local));
                    }
                    value &= value - UINT64_C(1);
                }
            }
        }
    }
    for (jint row = full_rows; row < rows; ++row) {
        const size_t source_base = (size_t) row * (size_t) words_per_row;
        const uint64_t row_mask = UINT64_C(1) << (row & 63);
        const size_t target_word = (size_t) ((unsigned) row >> 6);
        for (jint word = 0; word < words_per_row; ++word) {
            uint64_t value = (uint64_t) source[source_base + (size_t) word];
            while (value != UINT64_C(0)) {
                size_t column = ((size_t) word << 6) + trailing_zeroes(value);
                if (column < (size_t) columns) {
                    size_t index = column * (size_t) target_words_per_row + target_word;
                    target[index] = (jlong) (((uint64_t) target[index]) | row_mask);
                }
                value &= value - UINT64_C(1);
            }
        }
    }
    (*env)->ReleaseLongArrayElements(env, output, target, 0);
    (*env)->ReleaseLongArrayElements(env, input, source, JNI_ABORT);
    return output;
}
