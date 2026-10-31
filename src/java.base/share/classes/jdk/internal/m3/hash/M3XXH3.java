/*
 * Copyright 2015 Higher Frequency Trading http://www.higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Changed by Synexia (2026-10-31): zero-allocation-hashing 0.16 (Apache License 2.0) absorbed into java.base
 * as jdk.internal.m3.hash; the changes are listed in src/java.base/share/legal/zero-allocation-hashing.md.
 */
package jdk.internal.m3.hash;

import java.nio.ByteBuffer;

import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static jdk.internal.m3.hash.M3Maths.unsignedLongMulXorFold;
import static jdk.internal.m3.hash.M3ByteArrayAccess.*;

/**
 * Adapted version of XXH3 implementation from https://github.com/Cyan4973/xxHash.
 * This implementation provides endian-independant hash values, but it's slower on big-endian platforms.
 */
class M3XXH3 {
    //FLLDCIM: M3 - LITERAL_CONSTANTS v1.1: this class's magic literals, named by role (user rule: no magic numbers, no floating strings)
    /** Shift of the XXH64 avalanche. */
    private static final int XXH64_AVALANCHE_SHIFT_1 = 33;
    /** Shift of the XXH64 avalanche. */
    private static final int XXH64_AVALANCHE_SHIFT_2 = 29;
    /** Shift of the XXH64 avalanche. */
    private static final int XXH64_AVALANCHE_SHIFT_3 = 32;
    /** Shift of the XXH3 avalanche. */
    private static final int AVALANCHE_SHIFT = 37;
    /** Multiplier of the XXH3 avalanche (PRIME_MX1). */
    private static final long PRIME_MX1 = 0x165667919E3779F9L;
    /** The XXH3 avalanche ends with h ^ (h >>> 32). */
    private static final int AVALANCHE_FOLD_SHIFT = 32;
    /** First rotation of rrmxmx. */
    private static final int RRMXMX_ROTATION_1 = 49;
    /** Second rotation of rrmxmx. */
    private static final int RRMXMX_ROTATION_2 = 24;
    /** Multiplier of rrmxmx (PRIME_MX2). */
    private static final long PRIME_MX2 = 0x9FB21C651E98DF25L;
    /** First shift of rrmxmx. */
    private static final int RRMXMX_SHIFT_1 = 35;
    /** Second shift of rrmxmx. */
    private static final int RRMXMX_SHIFT_2 = 28;
    /** Offset of the second 64-bit word of a 16-byte pair. */
    private static final int WORD_1_OFFSET = 8;
    /** Longest input of the 0..16-byte path. */
    private static final int SHORT_INPUT_MAX = 16;
    /** Longest input of the 17..128-byte path. */
    private static final int LEN_17TO128_MAX = 128;
    /** Longest input of the 129..240-byte path. */
    private static final int MIDSIZE_MAX = 240;
    /** Longest input of the 4..8-byte path. */
    private static final int LEN_4TO8_MAX = 8;
    /** Shortest input of the 4..8-byte path. */
    private static final int LEN_4TO8_MIN = 4;
    /** Offset 56 of the default secret. */
    private static final int SECRET_56 = 56;
    /** Offset 64 of the default secret. */
    private static final int SECRET_64 = 64;
    /** Offset 24 of the default secret. */
    private static final int SECRET_24 = 24;
    /** Offset 32 of the default secret. */
    private static final int SECRET_32 = 32;
    /** Offset 40 of the default secret. */
    private static final int SECRET_40 = 40;
    /** Offset 48 of the default secret. */
    private static final int SECRET_48 = 48;
    /** Distance of the last 8 input bytes from the end. */
    private static final int LAST_8 = 8;
    /** The low 32-bit half of a long. */
    private static final long LOW_32_MASK = 0xFFFFFFFFL;
    /** Distance of the last 4 input bytes from the end. */
    private static final int LAST_4 = 4;
    /** Offset 8 of the default secret. */
    private static final int SECRET_8 = 8;
    /** Offset 16 of the default secret. */
    private static final int SECRET_16 = 16;
    /** Distance between the low and the high 32-bit half of a long. */
    private static final int HALF_SHIFT = 32;
    /** Bit position of the third byte of a little-endian word. */
    private static final int BYTE_2_SHIFT = 16;
    /** Bit position of the fourth byte of a little-endian word. */
    private static final int BYTE_3_SHIFT = 24;
    /** Bit position of the second byte of a little-endian word. */
    private static final int BYTE_1_SHIFT = 8;
    /** Offset 4 of the default secret. */
    private static final int SECRET_4 = 4;
    /** Longer inputs of the 17..128-byte path add a second pair of mixes. */
    private static final int LEN_17TO128_PAIR_2 = 32;
    /** Longer inputs of the 17..128-byte path add a third pair of mixes. */
    private static final int LEN_17TO128_PAIR_3 = 64;
    /** Longer inputs of the 17..128-byte path add a fourth pair of mixes. */
    private static final int LEN_17TO128_PAIR_4 = 96;
    /** Input offset 48. */
    private static final int INPUT_48 = 48;
    /** Offset 96 of the default secret. */
    private static final int SECRET_96 = 96;
    /** Distance of the last 64 input bytes from the end. */
    private static final int LAST_64 = 64;
    /** Offset 112 of the default secret. */
    private static final int SECRET_112 = 112;
    /** Input offset 32. */
    private static final int INPUT_32 = 32;
    /** Distance of the last 48 input bytes from the end. */
    private static final int LAST_48 = 48;
    /** Offset 80 of the default secret. */
    private static final int SECRET_80 = 80;
    /** Input offset 16. */
    private static final int INPUT_16 = 16;
    /** Distance of the last 32 input bytes from the end. */
    private static final int LAST_32 = 32;
    /** Distance of the last 16 input bytes from the end. */
    private static final int LAST_16 = 16;
    /** Bytes of one 16-byte mix. */
    private static final int MIX_BYTES = 16;
    /** Rounds of the 129..240-byte path before the secret restarts at MIDSIZE_START_OFFSET. */
    private static final int MIDSIZE_FIRST_ROUNDS = 8;
    /** Secret offset of the rounds after the first ones (129..240 bytes). */
    private static final int MIDSIZE_START_OFFSET = 3;
    /** Smallest secret size. */
    private static final int SECRET_SIZE_MIN = 136;
    /** Distance of the last secret of the 129..240-byte path from SECRET_SIZE_MIN. */
    private static final int MIDSIZE_LAST_OFFSET = 17;
    /** Offset 72 of the default secret. */
    private static final int SECRET_72 = 72;
    /** Offset 88 of the default secret. */
    private static final int SECRET_88 = 88;
    /** (length - 1) << 54 in the 9..16-byte 128-bit path. */
    private static final int LENGTH_MIX_SHIFT = 54;
    /** PRIME64_1 + (length << 2) in the 4..8-byte 128-bit path. */
    private static final int LENGTH_MULT_SHIFT = 2;
    /** m128_lo ^= m128_hi >>> 3. */
    private static final int HIGH_FOLD_SHIFT = 3;
    /** rotl32(swap32(combinedl), 13) gives combinedh. */
    private static final int COMBINED_ROTATION = 13;
    /** Offset 12 of the default secret. */
    private static final int SECRET_12 = 12;
    /** Bytes of one 32-byte mix of the 128-bit hash. */
    private static final int MIX32_BYTES = 32;
    /** Rounds of the 129..240-byte 128-bit path before the secret restarts at MIDSIZE_START_OFFSET. */
    private static final int MIDSIZE_FIRST_ROUNDS_128 = 4;
    /** Offset of the third 64-bit word of a 32-byte mix. */
    private static final int WORD_2_OFFSET = 16;
    /** Offset of the fourth 64-bit word of a 32-byte mix. */
    private static final int WORD_3_OFFSET = 24;
    /** Bytes of one stripe: eight accumulator lanes. */
    private static final int STRIPE_LEN = 64;
    /** Secret bytes consumed per stripe. */
    private static final int SECRET_CONSUME_RATE = 8;
    /** Bytes of one accumulator lane. */
    private static final int LANE_BYTES = 8;
    /** Accumulator lane 2. */
    private static final int LANE_2 = 2;
    /** Accumulator lane 3. */
    private static final int LANE_3 = 3;
    /** Accumulator lane 4. */
    private static final int LANE_4 = 4;
    /** Accumulator lane 5. */
    private static final int LANE_5 = 5;
    /** Accumulator lane 6. */
    private static final int LANE_6 = 6;
    /** Accumulator lane 7. */
    private static final int LANE_7 = 7;
    /** Bytes of the default secret. */
    private static final int SECRET_SIZE = 192;
    /** Shift of the accumulator scramble. */
    private static final int SCRAMBLE_SHIFT = 47;
    /** Distance of the last stripe's secret from the end of the stripes' secret. */
    private static final int SECRET_LASTACC_START = 7;
    /** Secret offset of the accumulator merge. */
    private static final int SECRET_MERGEACCS_START = 11;
    /** Accumulator pair 2 of the merge. */
    private static final int ACC_PAIR_2 = 2;
    /** Accumulator pair 3 of the merge. */
    private static final int ACC_PAIR_3 = 3;

    private static final M3Access<byte[]> unsafeLE = M3ByteArrayAccess.INSTANCE.byteOrder(null, LITTLE_ENDIAN);

    /*! Pseudorandom secret taken directly from FARSH. */
    private static final byte[] XXH3_kSecret = {
        (byte)0xb8, (byte)0xfe, (byte)0x6c, (byte)0x39, (byte)0x23, (byte)0xa4, (byte)0x4b, (byte)0xbe, (byte)0x7c, (byte)0x01, (byte)0x81, (byte)0x2c, (byte)0xf7, (byte)0x21, (byte)0xad, (byte)0x1c,
        (byte)0xde, (byte)0xd4, (byte)0x6d, (byte)0xe9, (byte)0x83, (byte)0x90, (byte)0x97, (byte)0xdb, (byte)0x72, (byte)0x40, (byte)0xa4, (byte)0xa4, (byte)0xb7, (byte)0xb3, (byte)0x67, (byte)0x1f,
        (byte)0xcb, (byte)0x79, (byte)0xe6, (byte)0x4e, (byte)0xcc, (byte)0xc0, (byte)0xe5, (byte)0x78, (byte)0x82, (byte)0x5a, (byte)0xd0, (byte)0x7d, (byte)0xcc, (byte)0xff, (byte)0x72, (byte)0x21,
        (byte)0xb8, (byte)0x08, (byte)0x46, (byte)0x74, (byte)0xf7, (byte)0x43, (byte)0x24, (byte)0x8e, (byte)0xe0, (byte)0x35, (byte)0x90, (byte)0xe6, (byte)0x81, (byte)0x3a, (byte)0x26, (byte)0x4c,
        (byte)0x3c, (byte)0x28, (byte)0x52, (byte)0xbb, (byte)0x91, (byte)0xc3, (byte)0x00, (byte)0xcb, (byte)0x88, (byte)0xd0, (byte)0x65, (byte)0x8b, (byte)0x1b, (byte)0x53, (byte)0x2e, (byte)0xa3,
        (byte)0x71, (byte)0x64, (byte)0x48, (byte)0x97, (byte)0xa2, (byte)0x0d, (byte)0xf9, (byte)0x4e, (byte)0x38, (byte)0x19, (byte)0xef, (byte)0x46, (byte)0xa9, (byte)0xde, (byte)0xac, (byte)0xd8,
        (byte)0xa8, (byte)0xfa, (byte)0x76, (byte)0x3f, (byte)0xe3, (byte)0x9c, (byte)0x34, (byte)0x3f, (byte)0xf9, (byte)0xdc, (byte)0xbb, (byte)0xc7, (byte)0xc7, (byte)0x0b, (byte)0x4f, (byte)0x1d,
        (byte)0x8a, (byte)0x51, (byte)0xe0, (byte)0x4b, (byte)0xcd, (byte)0xb4, (byte)0x59, (byte)0x31, (byte)0xc8, (byte)0x9f, (byte)0x7e, (byte)0xc9, (byte)0xd9, (byte)0x78, (byte)0x73, (byte)0x64,
        (byte)0xea, (byte)0xc5, (byte)0xac, (byte)0x83, (byte)0x34, (byte)0xd3, (byte)0xeb, (byte)0xc3, (byte)0xc5, (byte)0x81, (byte)0xa0, (byte)0xff, (byte)0xfa, (byte)0x13, (byte)0x63, (byte)0xeb,
        (byte)0x17, (byte)0x0d, (byte)0xdd, (byte)0x51, (byte)0xb7, (byte)0xf0, (byte)0xda, (byte)0x49, (byte)0xd3, (byte)0x16, (byte)0x55, (byte)0x26, (byte)0x29, (byte)0xd4, (byte)0x68, (byte)0x9e,
        (byte)0x2b, (byte)0x16, (byte)0xbe, (byte)0x58, (byte)0x7d, (byte)0x47, (byte)0xa1, (byte)0xfc, (byte)0x8f, (byte)0xf8, (byte)0xb8, (byte)0xd1, (byte)0x7a, (byte)0xd0, (byte)0x31, (byte)0xce,
        (byte)0x45, (byte)0xcb, (byte)0x3a, (byte)0x8f, (byte)0x95, (byte)0x16, (byte)0x04, (byte)0x28, (byte)0xaf, (byte)0xd7, (byte)0xfb, (byte)0xca, (byte)0xbb, (byte)0x4b, (byte)0x40, (byte)0x7e,
    };

    // Primes
    private static final long XXH_PRIME32_1 = 0x9E3779B1L;   /*!< 0b10011110001101110111100110110001 */
    private static final long XXH_PRIME32_2 = 0x85EBCA77L;   /*!< 0b10000101111010111100101001110111 */
    private static final long XXH_PRIME32_3 = 0xC2B2AE3DL;   /*!< 0b11000010101100101010111000111101 */

    private static final long XXH_PRIME64_1 = 0x9E3779B185EBCA87L;   /*!< 0b1001111000110111011110011011000110000101111010111100101010000111 */
    private static final long XXH_PRIME64_2 = 0xC2B2AE3D27D4EB4FL;   /*!< 0b1100001010110010101011100011110100100111110101001110101101001111 */
    private static final long XXH_PRIME64_3 = 0x165667B19E3779F9L;   /*!< 0b0001011001010110011001111011000110011110001101110111100111111001 */
    private static final long XXH_PRIME64_4 = 0x85EBCA77C2B2AE63L;   /*!< 0b1000010111101011110010100111011111000010101100101010111001100011 */
    private static final long XXH_PRIME64_5 = 0x27D4EB2F165667C5L;   /*!< 0b0010011111010100111010110010111100010110010101100110011111000101 */

    // only support fixed size secret
    private static final long nbStripesPerBlock = (192 - 64) / 8;
    private static final long block_len = 64 * nbStripesPerBlock;

    private static long XXH64_avalanche(long h64) {
        h64 ^= h64 >>> XXH64_AVALANCHE_SHIFT_1;
        h64 *= XXH_PRIME64_2;
        h64 ^= h64 >>> XXH64_AVALANCHE_SHIFT_2;
        h64 *= XXH_PRIME64_3;
        return h64 ^ (h64 >>> XXH64_AVALANCHE_SHIFT_3);
    }
    private static long XXH3_avalanche(long h64) {
        h64 ^= h64 >>> AVALANCHE_SHIFT;
        h64 *= PRIME_MX1;
        return h64 ^ (h64 >>> AVALANCHE_FOLD_SHIFT);
    }
    private static long XXH3_rrmxmx(long h64, final long length) {
        h64 ^= Long.rotateLeft(h64, RRMXMX_ROTATION_1) ^ Long.rotateLeft(h64, RRMXMX_ROTATION_2);
        h64 *= PRIME_MX2;
        h64 ^= (h64 >>> RRMXMX_SHIFT_1) + length;
        h64 *= PRIME_MX2;
        return h64 ^ (h64 >>> RRMXMX_SHIFT_2);
    }

    private static <T> long XXH3_mix16B(final long seed, final T input, final M3Access<T> access, final long offIn, final long offSec) {
        final long input_lo = access.i64(input, offIn);
        final long input_hi = access.i64(input, offIn + WORD_1_OFFSET);
        return unsignedLongMulXorFold(
            input_lo ^ (unsafeLE.i64(XXH3_kSecret, offSec)   + seed),
            input_hi ^ (unsafeLE.i64(XXH3_kSecret, offSec+WORD_1_OFFSET) - seed)
        );
    }

    /*
     * A bit slower than XXH3_mix16B, but handles multiply by zero better.
     */
    private static long XXH128_mix32B_once(final long seed, final long offSec, long acc, final long input0, final long input1, final long input2, final long input3) {
        acc += unsignedLongMulXorFold(
            input0 ^ (unsafeLE.i64(XXH3_kSecret, offSec    ) + seed),
            input1 ^ (unsafeLE.i64(XXH3_kSecret, offSec + WORD_1_OFFSET) - seed));
        return acc ^ (input2 + input3);
    }

    private static long XXH3_mix2Accs(final long acc_lh, final long acc_rh, final byte[] secret, final long offSec) {
        return unsignedLongMulXorFold(
            acc_lh ^ unsafeLE.i64(secret, offSec),
            acc_rh ^ unsafeLE.i64(secret, offSec+WORD_1_OFFSET) );
    }

    //FLLDCIM: HPCPOP - XXH3_SPLIT v1: length-class dispatcher; each class is its own atom with its own C2 inlining budget
    private static <T> long XXH3_64bits_internal(final long seed, final byte[] secret, final T input, final M3Access<T> access, final long off, final long length) {
        if (length <= SHORT_INPUT_MAX) {
            return XXH3_len_0to16_64b(seed, input, access, off, length);
        }
        if (length <= LEN_17TO128_MAX) {
            return XXH3_len_17to128_64b(seed, input, access, off, length);
        }
        if (length <= MIDSIZE_MAX) {
            return XXH3_len_129to240_64b(seed, input, access, off, length);
        }

        return XXH3_hashLong(secret, input, access, off, length, null);
    }

    //FLLDCIM: HPCPOP - XXH3_SPLIT v1: atom for input lengths 0..16 (xxHash XXH3_len_0to16_64b)
    private static <T> long XXH3_len_0to16_64b(final long seed, final T input, final M3Access<T> access, final long off, final long length) {
        // XXH3_len_0to16_64b
        if (length > LEN_4TO8_MAX) {
            return XXH3_len_9to16_64b(seed, input, access, off, length);
        }
        if (length >= LEN_4TO8_MIN) {
            return XXH3_len_4to8_64b(seed, input, access, off, length);
        }
        if (length != 0) {
            return XXH3_len_1to3_64b(seed, input, access, off, length);
        }
        return XXH64_avalanche(seed ^ unsafeLE.i64(XXH3_kSecret, SECRET_56+BYTE_BASE) ^ unsafeLE.i64(XXH3_kSecret, SECRET_64+BYTE_BASE));
    }

    //FLLDCIM: HPCPOP - XXH3_SPLIT v1: atom for input lengths 9..16 (xxHash XXH3_len_9to16_64b)
    private static <T> long XXH3_len_9to16_64b(final long seed, final T input, final M3Access<T> access, final long off, final long length) {
        // XXH3_len_9to16_64b
        final long bitflip1 = (unsafeLE.i64(XXH3_kSecret, SECRET_24+BYTE_BASE) ^ unsafeLE.i64(XXH3_kSecret, SECRET_32+BYTE_BASE)) + seed;
        final long bitflip2 = (unsafeLE.i64(XXH3_kSecret, SECRET_40+BYTE_BASE) ^ unsafeLE.i64(XXH3_kSecret, SECRET_48+BYTE_BASE)) - seed;
        final long input_lo = access.i64(input, off) ^ bitflip1;
        final long input_hi = access.i64(input, off + length - LAST_8) ^ bitflip2;
        final long acc = length + Long.reverseBytes(input_lo) + input_hi + unsignedLongMulXorFold(input_lo, input_hi);
        return XXH3_avalanche(acc);
    }

    //FLLDCIM: HPCPOP - XXH3_SPLIT v1: atom for input lengths 4..8 (xxHash XXH3_len_4to8_64b)
    private static <T> long XXH3_len_4to8_64b(final long seed, final T input, final M3Access<T> access, final long off, final long length) {
        // XXH3_len_4to8_64b
        long s = seed ^ Long.reverseBytes(seed & LOW_32_MASK);
        final long input1 = (long)access.i32(input, off); // high int will be shifted
        final long input2 = access.u32(input, off + length - LAST_4);
        final long bitflip = (unsafeLE.i64(XXH3_kSecret, SECRET_8+BYTE_BASE) ^ unsafeLE.i64(XXH3_kSecret, SECRET_16+BYTE_BASE)) - s;
        final long keyed = (input2 + (input1 << HALF_SHIFT)) ^ bitflip;
        return XXH3_rrmxmx(keyed, length);
    }

    //FLLDCIM: HPCPOP - XXH3_SPLIT v1: atom for input lengths 1..3 (xxHash XXH3_len_1to3_64b)
    private static <T> long XXH3_len_1to3_64b(final long seed, final T input, final M3Access<T> access, final long off, final long length) {
        // XXH3_len_1to3_64b
        final int c1 = access.u8(input, off + 0);
        final int c2 = access.i8(input, off + (length >> 1)); // high 3 bytes will be shifted
        final int c3 = access.u8(input, off + length - 1);
        final long combined = M3Primitives.unsignedInt((c1 << BYTE_2_SHIFT) | (c2  << BYTE_3_SHIFT) | c3 | ((int)length << BYTE_1_SHIFT));
        final long bitflip = M3Primitives.unsignedInt(unsafeLE.i32(XXH3_kSecret, BYTE_BASE) ^ unsafeLE.i32(XXH3_kSecret, SECRET_4+BYTE_BASE)) + seed;
        return XXH64_avalanche(combined ^ bitflip);
    }

    //FLLDCIM: HPCPOP - XXH3_SPLIT v1: atom for input lengths 17..128 (xxHash XXH3_len_17to128_64b)
    private static <T> long XXH3_len_17to128_64b(final long seed, final T input, final M3Access<T> access, final long off, final long length) {
        // XXH3_len_17to128_64b
        long acc = length * XXH_PRIME64_1;

        if (length > LEN_17TO128_PAIR_2) {
            if (length > LEN_17TO128_PAIR_3) {
                if (length > LEN_17TO128_PAIR_4) {
                    acc += XXH3_mix16B(seed, input, access, off + INPUT_48, BYTE_BASE + SECRET_96);
                    acc += XXH3_mix16B(seed, input, access, off + length - LAST_64, BYTE_BASE + SECRET_112);
                }
                acc += XXH3_mix16B(seed, input, access, off + INPUT_32, BYTE_BASE + SECRET_64);
                acc += XXH3_mix16B(seed, input, access, off + length - LAST_48, BYTE_BASE + SECRET_80);
            }
            acc += XXH3_mix16B(seed, input, access, off + INPUT_16, BYTE_BASE + SECRET_32);
            acc += XXH3_mix16B(seed, input, access, off + length - LAST_32, BYTE_BASE + SECRET_48);
        }
        acc += XXH3_mix16B(seed, input, access, off, BYTE_BASE);
        acc += XXH3_mix16B(seed, input, access, off + length - LAST_16, BYTE_BASE + SECRET_16);

        return XXH3_avalanche(acc);
    }

    //FLLDCIM: HPCPOP - XXH3_SPLIT v1: atom for input lengths 129..240 (xxHash XXH3_len_129to240_64b)
    private static <T> long XXH3_len_129to240_64b(final long seed, final T input, final M3Access<T> access, final long off, final long length) {
        // XXH3_len_129to240_64b
        long acc = length * XXH_PRIME64_1;
        final int nbRounds = (int)length / MIX_BYTES;
        int i = 0;
        for (; i < MIDSIZE_FIRST_ROUNDS; ++i) {
            acc += XXH3_mix16B(seed, input, access, off + MIX_BYTES*i, BYTE_BASE + MIX_BYTES*i);
        }
        acc = XXH3_avalanche(acc);

        for (; i < nbRounds; ++i) {
            acc += XXH3_mix16B(seed, input, access, off + MIX_BYTES*i, BYTE_BASE + MIX_BYTES*(i-MIDSIZE_FIRST_ROUNDS) + MIDSIZE_START_OFFSET);
        }

        /* last bytes */
        acc += XXH3_mix16B(seed, input, access, off + length - LAST_16, BYTE_BASE + SECRET_SIZE_MIN - MIDSIZE_LAST_OFFSET);
        return XXH3_avalanche(acc);
    }

    //FLLDCIM: HPCPOP - XXH3_SPLIT v1: length-class dispatcher; each class is its own atom with its own C2 inlining budget
    private static <T> long XXH3_128bits_internal(final long seed, final byte[] secret, final T input, final M3Access<T> access, final long off, final long length, final long[] result) {
        if (length <= SHORT_INPUT_MAX) {
            return XXH3_len_0to16_128b(seed, input, access, off, length, result);
        }
        if (length <= LEN_17TO128_MAX) {
            return XXH3_len_17to128_128b(seed, input, access, off, length, result);
        }

        if (length <= MIDSIZE_MAX) {
            return XXH3_len_129to240_128b(seed, input, access, off, length, result);
        }

        return XXH3_hashLong(secret, input, access, off, length, result);
    }

    //FLLDCIM: HPCPOP - XXH3_SPLIT v1: atom for input lengths 0..16 (xxHash XXH3_len_0to16_128b)
    private static <T> long XXH3_len_0to16_128b(final long seed, final T input, final M3Access<T> access, final long off, final long length, final long[] result) {
        // XXH3_len_0to16_128b
        if (length > LEN_4TO8_MAX) {
            return XXH3_len_9to16_128b(seed, input, access, off, length, result);
        }
        if (length >= LEN_4TO8_MIN) {
            return XXH3_len_4to8_128b(seed, input, access, off, length, result);
        }
        if (length != 0) {
            return XXH3_len_1to3_128b(seed, input, access, off, length, result);
        }
        final long low = XXH64_avalanche(seed ^ unsafeLE.i64(XXH3_kSecret, BYTE_BASE+SECRET_64) ^ unsafeLE.i64(XXH3_kSecret, BYTE_BASE+SECRET_72));
        if (null != result) {
            result[0] = low;
            result[1] = XXH64_avalanche(seed ^ unsafeLE.i64(XXH3_kSecret, BYTE_BASE+SECRET_80) ^ unsafeLE.i64(XXH3_kSecret, BYTE_BASE+SECRET_88));
        }
        return low;
    }

    //FLLDCIM: HPCPOP - XXH3_SPLIT v1: atom for input lengths 9..16 (xxHash XXH3_len_9to16_128b)
    private static <T> long XXH3_len_9to16_128b(final long seed, final T input, final M3Access<T> access, final long off, final long length, final long[] result) {
        // XXH3_len_9to16_128b
        final long bitflipl = (unsafeLE.i64(XXH3_kSecret, SECRET_32+BYTE_BASE) ^ unsafeLE.i64(XXH3_kSecret, SECRET_40+BYTE_BASE)) - seed;
        final long bitfliph = (unsafeLE.i64(XXH3_kSecret, SECRET_48+BYTE_BASE) ^ unsafeLE.i64(XXH3_kSecret, SECRET_56+BYTE_BASE)) + seed;
        long input_hi = access.i64(input, off + length - LAST_8);
        final long input_lo = access.i64(input, off) ^ input_hi ^ bitflipl;
        long m128_lo = input_lo * XXH_PRIME64_1;
        long m128_hi = M3Maths.unsignedLongMulHigh(input_lo, XXH_PRIME64_1);
        m128_lo += (length - 1) << LENGTH_MIX_SHIFT;
        input_hi ^= bitfliph;
        m128_hi += input_hi + M3Primitives.unsignedInt((int)input_hi) * (XXH_PRIME32_2 - 1);
        m128_lo ^= Long.reverseBytes(m128_hi);

        final long low = XXH3_avalanche(m128_lo * XXH_PRIME64_2);
        if (null != result) {
            result[0] = low;
            result[1] = XXH3_avalanche(M3Maths.unsignedLongMulHigh(m128_lo, XXH_PRIME64_2) + m128_hi * XXH_PRIME64_2);
        }
        return low;
    }

    //FLLDCIM: HPCPOP - XXH3_SPLIT v1: atom for input lengths 4..8 (xxHash XXH3_len_4to8_128b)
    private static <T> long XXH3_len_4to8_128b(final long seed, final T input, final M3Access<T> access, final long off, final long length, final long[] result) {
        // XXH3_len_4to8_128b
        long s = seed ^ Long.reverseBytes(seed & LOW_32_MASK);
        final long input_lo = access.u32(input, off);
        final long input_hi = (long)access.i32(input, off + length - LAST_4); // high int will be shifted

        final long bitflip = (unsafeLE.i64(XXH3_kSecret, SECRET_16+BYTE_BASE) ^ unsafeLE.i64(XXH3_kSecret, SECRET_24+BYTE_BASE)) + s;
        final long keyed = (input_lo + (input_hi << HALF_SHIFT)) ^ bitflip;
        final long pl = XXH_PRIME64_1 + (length << LENGTH_MULT_SHIFT); /* Shift len to the left to ensure it is even, this avoids even multiplies. */
        long m128_lo = keyed * pl;
        long m128_hi = M3Maths.unsignedLongMulHigh(keyed, pl);
        m128_hi += (m128_lo << 1);
        m128_lo ^= (m128_hi >>> HIGH_FOLD_SHIFT);

        m128_lo ^= m128_lo >>> RRMXMX_SHIFT_1;
        m128_lo *= PRIME_MX2;
        m128_lo ^= m128_lo >>> RRMXMX_SHIFT_2;

        if (null != result) {
            result[0] = m128_lo;
            result[1] = XXH3_avalanche(m128_hi);
        }
        return m128_lo;
    }

    //FLLDCIM: HPCPOP - XXH3_SPLIT v1: atom for input lengths 1..3 (xxHash XXH3_len_1to3_128b)
    private static <T> long XXH3_len_1to3_128b(final long seed, final T input, final M3Access<T> access, final long off, final long length, final long[] result) {
        // XXH3_len_1to3_128b
        final int c1 = access.u8(input, off + 0);
        final int c2 = access.i8(input, off + (length >> 1)); // high 3 bytes will be shifted
        final int c3 = access.u8(input, off + length - 1);
        final int combinedl = (c1 << BYTE_2_SHIFT) | (c2  << BYTE_3_SHIFT) | c3 | ((int)length << BYTE_1_SHIFT);
        final int combinedh = Integer.rotateLeft(Integer.reverseBytes(combinedl), COMBINED_ROTATION);
        final long bitflipl = M3Primitives.unsignedInt(unsafeLE.i32(XXH3_kSecret, BYTE_BASE) ^ unsafeLE.i32(XXH3_kSecret, BYTE_BASE+SECRET_4)) + seed;
        final long bitfliph = M3Primitives.unsignedInt(unsafeLE.i32(XXH3_kSecret, BYTE_BASE+SECRET_8) ^ unsafeLE.i32(XXH3_kSecret, BYTE_BASE+SECRET_12)) - seed;

        final long low = XXH64_avalanche(M3Primitives.unsignedInt(combinedl) ^ bitflipl);
        if (null != result) {
            result[0] = low;
            result[1] = XXH64_avalanche(M3Primitives.unsignedInt(combinedh) ^ bitfliph);
        }
        return low;
    }

    //FLLDCIM: HPCPOP - XXH3_SPLIT v1: atom for input lengths 17..128 (xxHash XXH3_len_17to128_128b)
    private static <T> long XXH3_len_17to128_128b(final long seed, final T input, final M3Access<T> access, final long off, final long length, final long[] result) {
        // XXH3_len_17to128_128b
        long acc0 = length * XXH_PRIME64_1;
        long acc1 = 0;
        if (length > LEN_17TO128_PAIR_2) {
            if (length > LEN_17TO128_PAIR_3) {
                if (length > LEN_17TO128_PAIR_4) {
                    final long input0 = access.i64(input, off + INPUT_48);
                    final long input1 = access.i64(input, off + INPUT_48 + WORD_1_OFFSET);
                    final long input2 = access.i64(input, off + length - LAST_64);
                    final long input3 = access.i64(input, off + length - LAST_64 + WORD_1_OFFSET);
                    acc0 = XXH128_mix32B_once(seed, BYTE_BASE + SECRET_96,      acc0, input0, input1, input2, input3);
                    acc1 = XXH128_mix32B_once(seed, BYTE_BASE + SECRET_96 + MIX_BYTES, acc1, input2, input3, input0, input1);
                }
                final long input0 = access.i64(input, off + INPUT_32);
                final long input1 = access.i64(input, off + INPUT_32 + WORD_1_OFFSET);
                final long input2 = access.i64(input, off + length - LAST_48);
                final long input3 = access.i64(input, off + length - LAST_48 + WORD_1_OFFSET);
                acc0 = XXH128_mix32B_once(seed, BYTE_BASE + SECRET_64,      acc0, input0, input1, input2, input3);
                acc1 = XXH128_mix32B_once(seed, BYTE_BASE + SECRET_64 + MIX_BYTES, acc1, input2, input3, input0, input1);
            }
            final long input0 = access.i64(input, off + INPUT_16);
            final long input1 = access.i64(input, off + INPUT_16 + WORD_1_OFFSET);
            final long input2 = access.i64(input, off + length - LAST_32);
            final long input3 = access.i64(input, off + length - LAST_32 + WORD_1_OFFSET);
            acc0 = XXH128_mix32B_once(seed, BYTE_BASE + SECRET_32,      acc0, input0, input1, input2, input3);
            acc1 = XXH128_mix32B_once(seed, BYTE_BASE + SECRET_32 + MIX_BYTES, acc1, input2, input3, input0, input1);
        }
        final long input0 = access.i64(input, off + 0);
        final long input1 = access.i64(input, off + 0 + WORD_1_OFFSET);
        final long input2 = access.i64(input, off + length - LAST_16);
        final long input3 = access.i64(input, off + length - LAST_16 + WORD_1_OFFSET);
        acc0 = XXH128_mix32B_once(seed, BYTE_BASE,      acc0, input0, input1, input2, input3);
        acc1 = XXH128_mix32B_once(seed, BYTE_BASE + MIX_BYTES, acc1, input2, input3, input0, input1);

        final long low = XXH3_avalanche(acc0 + acc1);
        if (null != result) {
            result[0] = low;
            result[1] = -XXH3_avalanche(acc0*XXH_PRIME64_1 + acc1*XXH_PRIME64_4 + (length - seed)*XXH_PRIME64_2);
        }
        return low;
    }

    //FLLDCIM: HPCPOP - XXH3_SPLIT v1: atom for input lengths 129..240 (xxHash XXH3_len_129to240_128b)
    private static <T> long XXH3_len_129to240_128b(final long seed, final T input, final M3Access<T> access, final long off, final long length, final long[] result) {
        // XXH3_len_129to240_128b
        final int nbRounds = (int)length / MIX32_BYTES;
        long acc0 = length * XXH_PRIME64_1;
        long acc1 = 0;
        int i = 0;
        for (; i < MIDSIZE_FIRST_ROUNDS_128; ++i) {
            final long input0 = access.i64(input, off + MIX32_BYTES*i);
            final long input1 = access.i64(input, off + MIX32_BYTES*i + WORD_1_OFFSET);
            final long input2 = access.i64(input, off + MIX32_BYTES*i + WORD_2_OFFSET);
            final long input3 = access.i64(input, off + MIX32_BYTES*i + WORD_3_OFFSET);
            acc0 = XXH128_mix32B_once(seed, BYTE_BASE + MIX32_BYTES*i,      acc0, input0, input1, input2, input3);
            acc1 = XXH128_mix32B_once(seed, BYTE_BASE + MIX32_BYTES*i + MIX_BYTES, acc1, input2, input3, input0, input1);
        }
        acc0 = XXH3_avalanche(acc0);
        acc1 = XXH3_avalanche(acc1);

        for (; i < nbRounds; ++i) {
            final long input0 = access.i64(input, off + MIX32_BYTES*i);
            final long input1 = access.i64(input, off + MIX32_BYTES*i + WORD_1_OFFSET);
            final long input2 = access.i64(input, off + MIX32_BYTES*i + WORD_2_OFFSET);
            final long input3 = access.i64(input, off + MIX32_BYTES*i + WORD_3_OFFSET);
            acc0 = XXH128_mix32B_once(seed, BYTE_BASE + MIDSIZE_START_OFFSET + MIX32_BYTES*(i-MIDSIZE_FIRST_ROUNDS_128),      acc0, input0, input1, input2, input3);
            acc1 = XXH128_mix32B_once(seed, BYTE_BASE + MIDSIZE_START_OFFSET + MIX32_BYTES*(i-MIDSIZE_FIRST_ROUNDS_128) + MIX_BYTES, acc1, input2, input3, input0, input1);
        }

        /* last bytes */
        final long input0 = access.i64(input, off + length - LAST_16);
        final long input1 = access.i64(input, off + length - LAST_16 + WORD_1_OFFSET);
        final long input2 = access.i64(input, off + length - LAST_32);
        final long input3 = access.i64(input, off + length - LAST_32 + WORD_1_OFFSET);
        acc0 = XXH128_mix32B_once(-seed, BYTE_BASE + SECRET_SIZE_MIN - MIDSIZE_LAST_OFFSET - MIX_BYTES, acc0, input0, input1, input2, input3);
        acc1 = XXH128_mix32B_once(-seed, BYTE_BASE + SECRET_SIZE_MIN - MIDSIZE_LAST_OFFSET     , acc1, input2, input3, input0, input1);

        final long low = XXH3_avalanche(acc0 + acc1);
        if (null != result) {
            result[0] = low;
            result[1] = -XXH3_avalanche(acc0*XXH_PRIME64_1 + acc1*XXH_PRIME64_4 + (length - seed)*XXH_PRIME64_2);
        }
        return low;
    }

    //FLLDCIM: HPCPOP - XXH3_SPLIT v1: atom for input lengths above 240, shared: XXH3_hashLong_64b is the low half of XXH3_hashLong_128b
    private static <T> long XXH3_hashLong(final byte[] secret, final T input, final M3Access<T> access, final long off, final long length, final long[] result) {
        // XXH3_hashLong_128b_internal
        long acc_0 = XXH_PRIME32_3;
        long acc_1 = XXH_PRIME64_1;
        long acc_2 = XXH_PRIME64_2;
        long acc_3 = XXH_PRIME64_3;
        long acc_4 = XXH_PRIME64_4;
        long acc_5 = XXH_PRIME32_2;
        long acc_6 = XXH_PRIME64_5;
        long acc_7 = XXH_PRIME32_1;

        // XXH3_hashLong_internal_loop
        final long nb_blocks = (length - 1) / block_len;
        for (long n = 0; n < nb_blocks; n++) {
            // XXH3_accumulate
            final long offBlock = off + n * block_len;
            for (long s = 0; s < nbStripesPerBlock; s++ ) {
                // XXH3_accumulate_512
                final long offStripe = offBlock + s * STRIPE_LEN;
                final long offSec = s * SECRET_CONSUME_RATE;
                {
                    final long data_val_0 = access.i64(input, offStripe + LANE_BYTES*0);
                    final long data_val_1 = access.i64(input, offStripe + LANE_BYTES*1);
                    final long data_key_0 = data_val_0 ^ unsafeLE.i64(secret, BYTE_BASE + offSec + LANE_BYTES*0);
                    final long data_key_1 = data_val_1 ^ unsafeLE.i64(secret, BYTE_BASE + offSec + LANE_BYTES*1);
                    /* swap adjacent lanes */
                    acc_0 += data_val_1 + (LOW_32_MASK & data_key_0) * (data_key_0 >>> HALF_SHIFT);
                    acc_1 += data_val_0 + (LOW_32_MASK & data_key_1) * (data_key_1 >>> HALF_SHIFT);
                }
                {
                    final long data_val_0 = access.i64(input, offStripe + LANE_BYTES*LANE_2);
                    final long data_val_1 = access.i64(input, offStripe + LANE_BYTES*LANE_3);
                    final long data_key_0 = data_val_0 ^ unsafeLE.i64(secret, BYTE_BASE + offSec + LANE_BYTES*LANE_2);
                    final long data_key_1 = data_val_1 ^ unsafeLE.i64(secret, BYTE_BASE + offSec + LANE_BYTES*LANE_3);
                    /* swap adjacent lanes */
                    acc_2 += data_val_1 + (LOW_32_MASK & data_key_0) * (data_key_0 >>> HALF_SHIFT);
                    acc_3 += data_val_0 + (LOW_32_MASK & data_key_1) * (data_key_1 >>> HALF_SHIFT);
                }
                {
                    final long data_val_0 = access.i64(input, offStripe + LANE_BYTES*LANE_4);
                    final long data_val_1 = access.i64(input, offStripe + LANE_BYTES*LANE_5);
                    final long data_key_0 = data_val_0 ^ unsafeLE.i64(secret, BYTE_BASE + offSec + LANE_BYTES*LANE_4);
                    final long data_key_1 = data_val_1 ^ unsafeLE.i64(secret, BYTE_BASE + offSec + LANE_BYTES*LANE_5);
                    /* swap adjacent lanes */
                    acc_4 += data_val_1 + (LOW_32_MASK & data_key_0) * (data_key_0 >>> HALF_SHIFT);
                    acc_5 += data_val_0 + (LOW_32_MASK & data_key_1) * (data_key_1 >>> HALF_SHIFT);
                }
                {
                    final long data_val_0 = access.i64(input, offStripe + LANE_BYTES*LANE_6);
                    final long data_val_1 = access.i64(input, offStripe + LANE_BYTES*LANE_7);
                    final long data_key_0 = data_val_0 ^ unsafeLE.i64(secret, BYTE_BASE + offSec + LANE_BYTES*LANE_6);
                    final long data_key_1 = data_val_1 ^ unsafeLE.i64(secret, BYTE_BASE + offSec + LANE_BYTES*LANE_7);
                    /* swap adjacent lanes */
                    acc_6 += data_val_1 + (LOW_32_MASK & data_key_0) * (data_key_0 >>> HALF_SHIFT);
                    acc_7 += data_val_0 + (LOW_32_MASK & data_key_1) * (data_key_1 >>> HALF_SHIFT);
                }
            }

            // XXH3_scrambleAcc_scalar
            final long offSec = BYTE_BASE + SECRET_SIZE - STRIPE_LEN;
            acc_0 = (acc_0 ^ (acc_0 >>> SCRAMBLE_SHIFT) ^ unsafeLE.i64(secret, offSec + LANE_BYTES*0)) * XXH_PRIME32_1;
            acc_1 = (acc_1 ^ (acc_1 >>> SCRAMBLE_SHIFT) ^ unsafeLE.i64(secret, offSec + LANE_BYTES*1)) * XXH_PRIME32_1;
            acc_2 = (acc_2 ^ (acc_2 >>> SCRAMBLE_SHIFT) ^ unsafeLE.i64(secret, offSec + LANE_BYTES*LANE_2)) * XXH_PRIME32_1;
            acc_3 = (acc_3 ^ (acc_3 >>> SCRAMBLE_SHIFT) ^ unsafeLE.i64(secret, offSec + LANE_BYTES*LANE_3)) * XXH_PRIME32_1;
            acc_4 = (acc_4 ^ (acc_4 >>> SCRAMBLE_SHIFT) ^ unsafeLE.i64(secret, offSec + LANE_BYTES*LANE_4)) * XXH_PRIME32_1;
            acc_5 = (acc_5 ^ (acc_5 >>> SCRAMBLE_SHIFT) ^ unsafeLE.i64(secret, offSec + LANE_BYTES*LANE_5)) * XXH_PRIME32_1;
            acc_6 = (acc_6 ^ (acc_6 >>> SCRAMBLE_SHIFT) ^ unsafeLE.i64(secret, offSec + LANE_BYTES*LANE_6)) * XXH_PRIME32_1;
            acc_7 = (acc_7 ^ (acc_7 >>> SCRAMBLE_SHIFT) ^ unsafeLE.i64(secret, offSec + LANE_BYTES*LANE_7)) * XXH_PRIME32_1;
        }

        /* last partial block */
        final long nbStripes = ((length - 1) - (block_len * nb_blocks)) / STRIPE_LEN;
        final long offBlock = off + block_len * nb_blocks;
        for (long s = 0; s < nbStripes; s++) {
            // XXH3_accumulate_512
            final long offStripe = offBlock + s * STRIPE_LEN;
            final long offSec = s * SECRET_CONSUME_RATE;
            {
                final long data_val_0 = access.i64(input, offStripe + LANE_BYTES*0);
                final long data_val_1 = access.i64(input, offStripe + LANE_BYTES*1);
                final long data_key_0 = data_val_0 ^ unsafeLE.i64(secret, BYTE_BASE + offSec + LANE_BYTES*0);
                final long data_key_1 = data_val_1 ^ unsafeLE.i64(secret, BYTE_BASE + offSec + LANE_BYTES*1);
                /* swap adjacent lanes */
                acc_0 += data_val_1 + (LOW_32_MASK & data_key_0) * (data_key_0 >>> HALF_SHIFT);
                acc_1 += data_val_0 + (LOW_32_MASK & data_key_1) * (data_key_1 >>> HALF_SHIFT);
            }
            {
                final long data_val_0 = access.i64(input, offStripe + LANE_BYTES*LANE_2);
                final long data_val_1 = access.i64(input, offStripe + LANE_BYTES*LANE_3);
                final long data_key_0 = data_val_0 ^ unsafeLE.i64(secret, BYTE_BASE + offSec + LANE_BYTES*LANE_2);
                final long data_key_1 = data_val_1 ^ unsafeLE.i64(secret, BYTE_BASE + offSec + LANE_BYTES*LANE_3);
                /* swap adjacent lanes */
                acc_2 += data_val_1 + (LOW_32_MASK & data_key_0) * (data_key_0 >>> HALF_SHIFT);
                acc_3 += data_val_0 + (LOW_32_MASK & data_key_1) * (data_key_1 >>> HALF_SHIFT);
            }
            {
                final long data_val_0 = access.i64(input, offStripe + LANE_BYTES*LANE_4);
                final long data_val_1 = access.i64(input, offStripe + LANE_BYTES*LANE_5);
                final long data_key_0 = data_val_0 ^ unsafeLE.i64(secret, BYTE_BASE + offSec + LANE_BYTES*LANE_4);
                final long data_key_1 = data_val_1 ^ unsafeLE.i64(secret, BYTE_BASE + offSec + LANE_BYTES*LANE_5);
                /* swap adjacent lanes */
                acc_4 += data_val_1 + (LOW_32_MASK & data_key_0) * (data_key_0 >>> HALF_SHIFT);
                acc_5 += data_val_0 + (LOW_32_MASK & data_key_1) * (data_key_1 >>> HALF_SHIFT);
            }
            {
                final long data_val_0 = access.i64(input, offStripe + LANE_BYTES*LANE_6);
                final long data_val_1 = access.i64(input, offStripe + LANE_BYTES*LANE_7);
                final long data_key_0 = data_val_0 ^ unsafeLE.i64(secret, BYTE_BASE + offSec + LANE_BYTES*LANE_6);
                final long data_key_1 = data_val_1 ^ unsafeLE.i64(secret, BYTE_BASE + offSec + LANE_BYTES*LANE_7);
                /* swap adjacent lanes */
                acc_6 += data_val_1 + (LOW_32_MASK & data_key_0) * (data_key_0 >>> HALF_SHIFT);
                acc_7 += data_val_0 + (LOW_32_MASK & data_key_1) * (data_key_1 >>> HALF_SHIFT);
            }
        }

        /* last stripe */
        // XXH3_accumulate_512
        final long offStripe = off + length - STRIPE_LEN;
        final long offSec = SECRET_SIZE - STRIPE_LEN - SECRET_LASTACC_START;
        {
            final long data_val_0 = access.i64(input, offStripe + LANE_BYTES*0);
            final long data_val_1 = access.i64(input, offStripe + LANE_BYTES*1);
            final long data_key_0 = data_val_0 ^ unsafeLE.i64(secret, BYTE_BASE + offSec + LANE_BYTES*0);
            final long data_key_1 = data_val_1 ^ unsafeLE.i64(secret, BYTE_BASE + offSec + LANE_BYTES*1);
            /* swap adjacent lanes */
            acc_0 += data_val_1 + (LOW_32_MASK & data_key_0) * (data_key_0 >>> HALF_SHIFT);
            acc_1 += data_val_0 + (LOW_32_MASK & data_key_1) * (data_key_1 >>> HALF_SHIFT);
        }
        {
            final long data_val_0 = access.i64(input, offStripe + LANE_BYTES*LANE_2);
            final long data_val_1 = access.i64(input, offStripe + LANE_BYTES*LANE_3);
            final long data_key_0 = data_val_0 ^ unsafeLE.i64(secret, BYTE_BASE + offSec + LANE_BYTES*LANE_2);
            final long data_key_1 = data_val_1 ^ unsafeLE.i64(secret, BYTE_BASE + offSec + LANE_BYTES*LANE_3);
            /* swap adjacent lanes */
            acc_2 += data_val_1 + (LOW_32_MASK & data_key_0) * (data_key_0 >>> HALF_SHIFT);
            acc_3 += data_val_0 + (LOW_32_MASK & data_key_1) * (data_key_1 >>> HALF_SHIFT);
        }
        {
            final long data_val_0 = access.i64(input, offStripe + LANE_BYTES*LANE_4);
            final long data_val_1 = access.i64(input, offStripe + LANE_BYTES*LANE_5);
            final long data_key_0 = data_val_0 ^ unsafeLE.i64(secret, BYTE_BASE + offSec + LANE_BYTES*LANE_4);
            final long data_key_1 = data_val_1 ^ unsafeLE.i64(secret, BYTE_BASE + offSec + LANE_BYTES*LANE_5);
            /* swap adjacent lanes */
            acc_4 += data_val_1 + (LOW_32_MASK & data_key_0) * (data_key_0 >>> HALF_SHIFT);
            acc_5 += data_val_0 + (LOW_32_MASK & data_key_1) * (data_key_1 >>> HALF_SHIFT);
        }
        {
            final long data_val_0 = access.i64(input, offStripe + LANE_BYTES*LANE_6);
            final long data_val_1 = access.i64(input, offStripe + LANE_BYTES*LANE_7);
            final long data_key_0 = data_val_0 ^ unsafeLE.i64(secret, BYTE_BASE + offSec + LANE_BYTES*LANE_6);
            final long data_key_1 = data_val_1 ^ unsafeLE.i64(secret, BYTE_BASE + offSec + LANE_BYTES*LANE_7);
            /* swap adjacent lanes */
            acc_6 += data_val_1 + (LOW_32_MASK & data_key_0) * (data_key_0 >>> HALF_SHIFT);
            acc_7 += data_val_0 + (LOW_32_MASK & data_key_1) * (data_key_1 >>> HALF_SHIFT);
        }

        // XXH3_mergeAccs
        final long low = XXH3_avalanche(XXH3_mergeAccs(acc_0, acc_1, acc_2, acc_3, acc_4, acc_5, acc_6, acc_7,
                secret, BYTE_BASE + SECRET_MERGEACCS_START, length * XXH_PRIME64_1));
        if (null != result) {
            result[0] = low;
            result[1] = XXH3_avalanche(XXH3_mergeAccs(acc_0, acc_1, acc_2, acc_3, acc_4, acc_5, acc_6, acc_7,
                    secret, BYTE_BASE + SECRET_SIZE - STRIPE_LEN - SECRET_MERGEACCS_START, ~(length * XXH_PRIME64_2)));
        }
        return low;
    }

    //FLLDCIM: HPCPOP - XXH3_SPLIT v1: XXH3_mergeAccs, the four lane-pair folds of the hashLong accumulators
    private static long XXH3_mergeAccs(final long acc_0, final long acc_1, final long acc_2, final long acc_3, final long acc_4, final long acc_5, final long acc_6, final long acc_7, final byte[] secret, final long offSec, final long start) {
        return start
                + XXH3_mix2Accs(acc_0, acc_1, secret, offSec)
                + XXH3_mix2Accs(acc_2, acc_3, secret, offSec + MIX_BYTES)
                + XXH3_mix2Accs(acc_4, acc_5, secret, offSec + MIX_BYTES * ACC_PAIR_2)
                + XXH3_mix2Accs(acc_6, acc_7, secret, offSec + MIX_BYTES * ACC_PAIR_3);
    }

    private static void XXH3_initCustomSecret(final byte[] customSecret, final long seed64) {
        final int nbRounds = SECRET_SIZE / MIX_BYTES;
        final ByteBuffer bb = ByteBuffer.wrap(customSecret).order(LITTLE_ENDIAN);
        for (int i=0; i < nbRounds; i++) {
            final long lo = unsafeLE.i64(XXH3_kSecret, BYTE_BASE + MIX_BYTES*i)     + seed64;
            final long hi = unsafeLE.i64(XXH3_kSecret, BYTE_BASE + MIX_BYTES*i + WORD_1_OFFSET) - seed64;
            bb.putLong(MIX_BYTES * i + 0, lo);
            bb.putLong(MIX_BYTES * i + WORD_1_OFFSET, hi);
        }
    }

    static M3LongHashFunction asLongHashFunctionWithoutSeed() {
        return AsLongHashFunction.SEEDLESS_INSTANCE;
    }

    private static class AsLongHashFunction extends M3LongHashFunction {
        //FLLDCIM: M3 - LITERAL_CONSTANTS v1.1: this class's magic literals, named by role (user rule: no magic numbers, no floating strings)
        /** The low 32-bit half of a long. */
        private static final long LOW_32_MASK = 0xFFFFFFFFL;
        /** Offset 8 of the default secret. */
        private static final int SECRET_8 = 8;
        /** Offset 16 of the default secret. */
        private static final int SECRET_16 = 16;
        /** Distance between the low and the high 32-bit half of a long. */
        private static final int HALF_SHIFT = 32;
        /** Byte length of a hashed long. */
        private static final int LONG_LENGTH = 8;
        /** Byte length of a hashed int. */
        private static final int INT_LENGTH = 4;
        /** Bit position of the second byte of a little-endian word. */
        private static final int BYTE_1_SHIFT = 8;
        /** Bit position of the third byte of a little-endian word. */
        private static final int BYTE_2_SHIFT = 16;
        /** Bit position of the fourth byte of a little-endian word. */
        private static final int BYTE_3_SHIFT = 24;
        /** Byte length of a hashed short or char. */
        private static final int SHORT_LENGTH = 2;
        /** Offset 4 of the default secret. */
        private static final int SECRET_4 = 4;
        /** Offset 56 of the default secret. */
        private static final int SECRET_56 = 56;
        /** Offset 64 of the default secret. */
        private static final int SECRET_64 = 64;

        private static final long serialVersionUID = 0L;
        private static final AsLongHashFunction SEEDLESS_INSTANCE = new AsLongHashFunction();

        public long seed() {
            return 0L;
        }

        @Override
        public long hashLong(long input) {
            input = M3Primitives.nativeToLittleEndian(input);
            final long s = seed() ^ Long.reverseBytes(seed() & LOW_32_MASK);
            final long bitflip = (unsafeLE.i64(M3XXH3.XXH3_kSecret, SECRET_8+BYTE_BASE) ^ unsafeLE.i64(M3XXH3.XXH3_kSecret, SECRET_16+BYTE_BASE)) - s;
            final long keyed = Long.rotateLeft(input, HALF_SHIFT) ^ bitflip;
            return XXH3_rrmxmx(keyed, LONG_LENGTH);
        }

        @Override
        public long hashInt(int input) {
            input = M3Primitives.nativeToLittleEndian(input);
            long s = seed() ^ Long.reverseBytes(seed() & LOW_32_MASK);
            final long bitflip = (unsafeLE.i64(M3XXH3.XXH3_kSecret, SECRET_8+BYTE_BASE) ^ unsafeLE.i64(M3XXH3.XXH3_kSecret, SECRET_16+BYTE_BASE)) - s;
            final long keyed = (M3Primitives.unsignedInt(input) + (((long)input) << HALF_SHIFT)) ^ bitflip;
            return XXH3_rrmxmx(keyed, INT_LENGTH);
        }

        @Override
        public long hashShort(short input) {
            input = M3Primitives.nativeToLittleEndian(input);
            final int c1 = M3Primitives.unsignedByte((byte)input);
            final int c2 = M3Primitives.unsignedShort(input) >>> BYTE_1_SHIFT;
            final int c3 = c2;
            final long combined = M3Primitives.unsignedInt((c1 << BYTE_2_SHIFT) | (c2 << BYTE_3_SHIFT) | c3 | (SHORT_LENGTH << BYTE_1_SHIFT));
            final long bitflip = (unsafeLE.u32(M3XXH3.XXH3_kSecret, BYTE_BASE) ^ unsafeLE.u32(M3XXH3.XXH3_kSecret, SECRET_4+BYTE_BASE)) + seed();
            return XXH64_avalanche(combined ^ bitflip);
        }

        @Override
        public long hashChar(char input) {
            return hashShort((short) input);
        }

        @Override
        public long hashByte(byte input) {
            final int c1 = M3Primitives.unsignedByte(input);
            final int c2 = c1;
            final int c3 = c1;
            final long combined = M3Primitives.unsignedInt((c1 << BYTE_2_SHIFT) | (c2 << BYTE_3_SHIFT) | c3 | (1 << BYTE_1_SHIFT));
            final long bitflip = (unsafeLE.u32(M3XXH3.XXH3_kSecret, BYTE_BASE) ^ unsafeLE.u32(M3XXH3.XXH3_kSecret, SECRET_4+BYTE_BASE)) + seed();
            return XXH64_avalanche(combined ^ bitflip);
        }

        @Override
        public long hashVoid() {
            return XXH64_avalanche(seed() ^ unsafeLE.i64(M3XXH3.XXH3_kSecret, SECRET_56+BYTE_BASE) ^ unsafeLE.i64(M3XXH3.XXH3_kSecret, SECRET_64+BYTE_BASE));
        }

        @Override
        public <T> long hash(final T input, final M3Access<T> access, final long off, final long len) {
            return M3XXH3.XXH3_64bits_internal(0, M3XXH3.XXH3_kSecret, input, access.byteOrder(input, LITTLE_ENDIAN), off, len);
        }
    }

    static M3LongHashFunction asLongHashFunctionWithSeed(final long seed) {
        return 0 == seed ? AsLongHashFunction.SEEDLESS_INSTANCE : new AsLongHashFunctionSeeded(seed);
    }

    private static class AsLongHashFunctionSeeded extends AsLongHashFunction {
        private static final long serialVersionUID = 0L;

        private final long seed;
        private final byte[] secret = new byte[192];

        private AsLongHashFunctionSeeded(final long seed) {
            this.seed = seed;
            XXH3_initCustomSecret(this.secret, seed);
        }

        @Override
        public long seed() {
            return seed;
        }

        @Override
        public <T> long hash(final T input, final M3Access<T> access, final long off, final long len) {
            return M3XXH3.XXH3_64bits_internal(this.seed, this.secret, input, access.byteOrder(input, LITTLE_ENDIAN), off, len);
        }
    }

    static M3LongTupleHashFunction asLongTupleHashFunctionWithoutSeed() {
        return AsLongTupleHashFunction.SEEDLESS_INSTANCE;
    }
    static M3LongHashFunction asLongTupleLowHashFunctionWithoutSeed() {
        return AsLongTupleHashFunction.SEEDLESS_INSTANCE.asLongHashFunction();
    }

    private static class AsLongTupleHashFunction extends M3DualHashFunction {
        //FLLDCIM: M3 - LITERAL_CONSTANTS v1.1: this class's magic literals, named by role (user rule: no magic numbers, no floating strings)
        /** Bits of the 128-bit result. */
        private static final int RESULT_BITS = 128;
        /** Longs of the 128-bit result. */
        private static final int RESULT_LONGS = 2;
        /** The low 32-bit half of a long. */
        private static final long LOW_32_MASK = 0xFFFFFFFFL;
        /** Offset 16 of the default secret. */
        private static final int SECRET_16 = 16;
        /** Offset 24 of the default secret. */
        private static final int SECRET_24 = 24;
        /** Byte length of a hashed long. */
        private static final int LONG_LENGTH = 8;
        /** PRIME64_1 + (length << 2) in the 4..8-byte 128-bit path. */
        private static final int LENGTH_MULT_SHIFT = 2;
        /** m128_lo ^= m128_hi >>> 3. */
        private static final int HIGH_FOLD_SHIFT = 3;
        /** First shift of rrmxmx. */
        private static final int RRMXMX_SHIFT_1 = 35;
        /** Multiplier of rrmxmx (PRIME_MX2). */
        private static final long PRIME_MX2 = 0x9FB21C651E98DF25L;
        /** Second shift of rrmxmx. */
        private static final int RRMXMX_SHIFT_2 = 28;
        /** Distance between the low and the high 32-bit half of a long. */
        private static final int HALF_SHIFT = 32;
        /** Byte length of a hashed int. */
        private static final int INT_LENGTH = 4;
        /** Bit position of the second byte of a little-endian word. */
        private static final int BYTE_1_SHIFT = 8;
        /** Bit position of the third byte of a little-endian word. */
        private static final int BYTE_2_SHIFT = 16;
        /** Bit position of the fourth byte of a little-endian word. */
        private static final int BYTE_3_SHIFT = 24;
        /** Byte length of a hashed short or char. */
        private static final int SHORT_LENGTH = 2;
        /** rotl32(swap32(combinedl), 13) gives combinedh. */
        private static final int COMBINED_ROTATION = 13;
        /** Offset 4 of the default secret. */
        private static final int SECRET_4 = 4;
        /** Offset 8 of the default secret. */
        private static final int SECRET_8 = 8;
        /** Offset 12 of the default secret. */
        private static final int SECRET_12 = 12;
        /** Offset 64 of the default secret. */
        private static final int SECRET_64 = 64;
        /** Offset 72 of the default secret. */
        private static final int SECRET_72 = 72;
        /** Offset 80 of the default secret. */
        private static final int SECRET_80 = 80;
        /** Offset 88 of the default secret. */
        private static final int SECRET_88 = 88;

        private static final long serialVersionUID = 0L;
        private static final AsLongTupleHashFunction SEEDLESS_INSTANCE = new AsLongTupleHashFunction();

        public long seed() {
            return 0L;
        }

        @Override
        public int bitsLength() {
            return RESULT_BITS;
        }

        @Override
        public long[] newResultArray() {
            return new long[RESULT_LONGS]; // override for a little performance
        }

        @Override
        public long dualHashLong(long input, final long[] result) {
            input = M3Primitives.nativeToLittleEndian(input);
            long s = seed() ^ Long.reverseBytes(seed() & LOW_32_MASK);
            final long bitflip = (unsafeLE.i64(XXH3_kSecret, SECRET_16+BYTE_BASE) ^ unsafeLE.i64(XXH3_kSecret, SECRET_24+BYTE_BASE)) + s;
            final long keyed = input ^ bitflip;
            final long pl = XXH_PRIME64_1 + (LONG_LENGTH << LENGTH_MULT_SHIFT); /* Shift len to the left to ensure it is even, this avoids even multiplies. */
            long m128_lo = keyed * pl;
            long m128_hi = M3Maths.unsignedLongMulHigh(keyed, pl);
            m128_hi += (m128_lo << 1);
            m128_lo ^= (m128_hi >>> HIGH_FOLD_SHIFT);

            m128_lo ^= m128_lo >>> RRMXMX_SHIFT_1;
            m128_lo *= PRIME_MX2;
            m128_lo ^= m128_lo >>> RRMXMX_SHIFT_2;

            if (null != result) {
                result[0] = m128_lo;
                result[1] = XXH3_avalanche(m128_hi);
            }
            return m128_lo;
        }

        @Override
        public long dualHashInt(final int input, final long[] result) {
            final long inputU = M3Primitives.unsignedInt(M3Primitives.nativeToLittleEndian(input));
            long s = seed() ^ Long.reverseBytes(seed() & LOW_32_MASK);
            final long bitflip = (unsafeLE.i64(XXH3_kSecret, SECRET_16+BYTE_BASE) ^ unsafeLE.i64(XXH3_kSecret, SECRET_24+BYTE_BASE)) + s;
            final long keyed = (inputU + (inputU << HALF_SHIFT)) ^ bitflip;
            final long pl = XXH_PRIME64_1 + (INT_LENGTH << LENGTH_MULT_SHIFT); /* Shift len to the left to ensure it is even, this avoids even multiplies. */
            long m128_lo = keyed * pl;
            long m128_hi = M3Maths.unsignedLongMulHigh(keyed, pl);

            m128_hi += (m128_lo << 1);
            m128_lo ^= (m128_hi >>> HIGH_FOLD_SHIFT);

            m128_lo ^= m128_lo >>> RRMXMX_SHIFT_1;
            m128_lo *= PRIME_MX2;
            m128_lo ^= m128_lo >>> RRMXMX_SHIFT_2;

            if (null != result) {
                result[0] = m128_lo;
                result[1] = XXH3_avalanche(m128_hi);
            }
            return m128_lo;
        }

        @Override
        public long dualHashShort(short input, final long[] result) {
            input = M3Primitives.nativeToLittleEndian(input);
            final int c1 = M3Primitives.unsignedByte((byte)input);
            final int c2 = M3Primitives.unsignedShort(input) >>> BYTE_1_SHIFT;
            final int c3 = c2;
            final int combinedl = (c1 << BYTE_2_SHIFT) | (c2  << BYTE_3_SHIFT) | c3 | (SHORT_LENGTH << BYTE_1_SHIFT);
            final int combinedh = Integer.rotateLeft(Integer.reverseBytes(combinedl), COMBINED_ROTATION);
            final long bitflipl = M3Primitives.unsignedInt(unsafeLE.i32(XXH3_kSecret, BYTE_BASE) ^ unsafeLE.i32(XXH3_kSecret, BYTE_BASE+SECRET_4)) + seed();
            final long bitfliph = M3Primitives.unsignedInt(unsafeLE.i32(XXH3_kSecret, BYTE_BASE+SECRET_8) ^ unsafeLE.i32(XXH3_kSecret, BYTE_BASE+SECRET_12)) - seed();

            final long low = XXH64_avalanche(M3Primitives.unsignedInt(combinedl) ^ bitflipl);
            if (null != result) {
                result[0] = low;
                result[1] = XXH64_avalanche(M3Primitives.unsignedInt(combinedh) ^ bitfliph);
            }
            return low;
        }

        @Override
        public long dualHashChar(char input, final long[] result) {
            return dualHashShort((short) input, result);
        }

        @Override
        public long dualHashByte(byte input, final long[] result) {
            final int c1 = M3Primitives.unsignedByte(input);
            //final int c2 = c1;
            final int c2 = input;
            final int c3 = c1;
            final int combinedl = (c1 << BYTE_2_SHIFT) | (c2  << BYTE_3_SHIFT) | c3 | (1 << BYTE_1_SHIFT);
            final int combinedh = Integer.rotateLeft(Integer.reverseBytes(combinedl), COMBINED_ROTATION);
            final long bitflipl = M3Primitives.unsignedInt(unsafeLE.i32(XXH3_kSecret, BYTE_BASE) ^ unsafeLE.i32(XXH3_kSecret, BYTE_BASE+SECRET_4)) + seed();
            final long bitfliph = M3Primitives.unsignedInt(unsafeLE.i32(XXH3_kSecret, BYTE_BASE+SECRET_8) ^ unsafeLE.i32(XXH3_kSecret, BYTE_BASE+SECRET_12)) - seed();

            final long low = XXH64_avalanche(M3Primitives.unsignedInt(combinedl) ^ bitflipl);
            if (null != result) {
                result[0] = low;
                result[1] = XXH64_avalanche(M3Primitives.unsignedInt(combinedh) ^ bitfliph);
            }
            return low;
        }

        @Override
        public long dualHashVoid(final long[] result) {
            final long low = XXH64_avalanche(seed() ^ unsafeLE.i64(XXH3_kSecret, BYTE_BASE+SECRET_64) ^ unsafeLE.i64(XXH3_kSecret, BYTE_BASE+SECRET_72));
            if (null != result) {
                result[0] = low;
                result[1] = XXH64_avalanche(seed() ^ unsafeLE.i64(XXH3_kSecret, BYTE_BASE+SECRET_80) ^ unsafeLE.i64(XXH3_kSecret, BYTE_BASE+SECRET_88));
            }
            return low;
        }

        @Override
        public <T> long dualHash(final T input, final M3Access<T> access, final long off, final long len, final long[] result) {
            return M3XXH3.XXH3_128bits_internal(0, M3XXH3.XXH3_kSecret, input, access.byteOrder(input, LITTLE_ENDIAN), off, len, result);
        }
    }

    static M3LongTupleHashFunction asLongTupleHashFunctionWithSeed(final long seed) {
        return 0 == seed ? AsLongTupleHashFunction.SEEDLESS_INSTANCE : new AsLongTupleHashFunctionSeeded(seed);
    }
    static M3LongHashFunction asLongTupleLowHashFunctionWithSeed(long seed) {
        return new AsLongTupleHashFunctionSeeded(seed).asLongHashFunction();
    }

    private static class AsLongTupleHashFunctionSeeded extends AsLongTupleHashFunction {
        private static final long serialVersionUID = 0L;

        private final long seed;
        private final byte[] secret = new byte[192];

        private AsLongTupleHashFunctionSeeded(final long seed) {
            this.seed = seed;
            XXH3_initCustomSecret(this.secret, seed);
        }

        @Override
        public long seed() {
            return seed;
        }

        @Override
        public <T> long dualHash(final T input, final M3Access<T> access, final long off, final long len, final long[] result) {
            return M3XXH3.XXH3_128bits_internal(seed, secret, input, access.byteOrder(input, LITTLE_ENDIAN), off, len, result);
        }
    }
}
