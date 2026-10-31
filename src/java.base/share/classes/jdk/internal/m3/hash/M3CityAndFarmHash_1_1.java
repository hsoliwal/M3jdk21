/*
 * Copyright 2014 Higher Frequency Trading http://www.higherfrequencytrading.com
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

import static java.lang.Long.reverseBytes;
import static java.lang.Long.rotateRight;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static jdk.internal.m3.hash.M3Primitives.NATIVE_LITTLE_ENDIAN;

/**
 * Adapted from the C++ CityHash implementation from Google at
 * https://github.com/google/cityhash/blob/8af9b8c2b889d80c22d6bc26ba0df1afb79a30db/src/city.cc.
 */
class M3CityAndFarmHash_1_1 {
    //FLLDCIM: M3 - LITERAL_CONSTANTS v1.1: this class's magic literals, named by role (user rule: no magic numbers, no floating strings)
    /** Shift of shiftMix. */
    private static final int SHIFT_MIX_SHIFT = 47;
    /** Bits of a byte: the middle or last byte moves up by one. */
    private static final int BYTE_BITS = 8;
    /** z = length + (last byte << 2). */
    private static final int LAST_BYTE_SHIFT = 2;
    /** length + (first four bytes << 3). */
    private static final int FIRST_INT_SHIFT = 3;
    /** First rotation of the 8..16-byte path. */
    private static final int LEN8_ROTATION_1 = 37;
    /** Second rotation of the 8..16-byte path. */
    private static final int LEN8_ROTATION_2 = 25;
    /** Bytes of a 64-bit word. */
    private static final long WORD_BYTES_L = 8L;
    /** Bytes of an int: the 4..7-byte path and its last int. */
    private static final long INT_BYTES_L = 4L;
    /** Bytes of 2 64-bit words. */
    private static final long WORDS_2_BYTES_L = 16L;
    /** Rotation of a + b in the HashLen17to32 mix (17..32 and 33..64 bytes). */
    private static final int LEN17_ROTATION_1 = 43;
    /** Rotation of c in the HashLen17to32 mix (17..32 and 33..64 bytes). */
    private static final int LEN17_ROTATION_2 = 30;
    /** Rotation of b + k2 in the HashLen17to32 mix (17..32 and 33..64 bytes). */
    private static final int LEN17_ROTATION_3 = 18;
    /** Bytes of 3 64-bit words. */
    private static final long WORDS_3_BYTES_L = 24L;
    /** Bytes of 4 64-bit words. */
    private static final long WORDS_4_BYTES_L = 32L;
    /** Multiplier of CityHash's 33..64-byte path. */
    private static final long LEN33_MULTIPLIER = 9L;
    /** Rotation of e + f in CityHash's 33..64-byte path. */
    private static final int LEN33_ROTATION = 42;
    /** Bytes of 8 64-bit words. */
    private static final long WORDS_8_BYTES_L = 64L;
    /** Bytes of 5 64-bit words. */
    private static final long WORDS_5_BYTES_L = 40L;
    /** Bytes of 7 64-bit words. */
    private static final long WORDS_7_BYTES_L = 56L;
    /** Bytes of 6 64-bit words. */
    private static final long WORDS_6_BYTES_L = 48L;
    /** First rotation of WeakHashLen32WithSeeds. */
    private static final int WEAK_ROTATION_1 = 21;
    /** Second rotation of WeakHashLen32WithSeeds. */
    private static final int WEAK_ROTATION_2 = 44;
    /** 64-byte block size minus one: rounds a length down to whole blocks. */
    private static final long BLOCK_MASK_L = 63L;
    /** Rotation of x in the 64-byte loop. */
    private static final int LOOP_ROTATION_X = 37;
    /** Rotation of y in the 64-byte loop. */
    private static final int LOOP_ROTATION_Y = 42;
    /** Rotation of z in the 64-byte loop. */
    private static final int LOOP_ROTATION_Z = 33;
    /** Bytes of a 64-bit word. */
    private static final int WORD_BYTES = 8;
    /** Bytes of 2 64-bit words. */
    private static final int WORDS_2_BYTES = 16;
    /** Bytes of 3 64-bit words. */
    private static final int WORDS_3_BYTES = 24;
    /** Bytes of 4 64-bit words. */
    private static final int WORDS_4_BYTES = 32;
    /** Default seed of FarmHash na and uo. */
    private static final int FARM_SEED = 81;
    /** Bytes of 8 64-bit words. */
    private static final int WORDS_8_BYTES = 64;
    /** Addend of the seed mix: seed * k + 113. */
    private static final int SEED_ADDEND = 113;
    /** log2 of the 64-byte block. */
    private static final int BLOCK_SHIFT = 6;
    /** 64-byte block size minus one: masks an index to its block and backs up from a block's last byte to its first. */
    private static final int BLOCK_MASK = 63;
    /** Bytes of 6 64-bit words. */
    private static final int WORDS_6_BYTES = 48;
    /** Bytes of 5 64-bit words. */
    private static final int WORDS_5_BYTES = 40;
    /** The low byte of z in the multiplier of the last block: mul = k1 + ((z & 0xff) << 1). */
    private static final int MUL_BYTE_MASK = 0xff;
    /** Multiplier 9 of the FarmHash rounds. */
    private static final int ROUND_MULTIPLIER = 9;
    /** mul = k2 + (u & 0x82): two bits of u. */
    private static final int MUL_BIT_MASK = 0x82;
    /** Bytes of 7 64-bit words. */
    private static final int WORDS_7_BYTES = 56;
    /** Rotation of x in FarmHash uo's block loop. */
    private static final int UO_ROTATION_X = 26;
    /** Rotation of y in FarmHash uo's block loop. */
    private static final int UO_ROTATION_Y = 29;
    /** Rotation of v0 in FarmHash uo's block loop. */
    private static final int UO_ROTATION_V0 = 33;
    /** Rotation of v1 in FarmHash uo's block loop. */
    private static final int UO_ROTATION_V1 = 30;
    /** Rotation of z in FarmHash uo's block loop. */
    private static final int UO_ROTATION_Z = 32;
    /** Rotation of w1 in FarmHash uo's block loop. */
    private static final int UO_ROTATION_W1 = 34;
    /** Rotation of v1 before the last block of FarmHash uo. */
    private static final int UO_ROTATION_V1_LAST = 28;
    /** Rotation of v0 before the last block of FarmHash uo. */
    private static final int UO_ROTATION_V0_LAST = 20;
    /** Rotation of the inner uoH of the result. */
    private static final int UO_H_ROTATION_INNER = 30;
    /** Rotation of the outer uoH of the result. */
    private static final int UO_H_ROTATION_OUTER = 31;

    static final long K0 = 0xc3a5c85c97cb3127L;
    private static final long K1 = 0xb492b66fbe98f273L;
    private static final long K2 = 0x9ae16a3b2f90404fL;

    private static long shiftMix(long val) {
        return val ^ (val >>> SHIFT_MIX_SHIFT);
    }

    private static long hashLen16(long u, long v) {
        return hashLen16(u, v, K_MUL);
    }

    private static final long K_MUL = 0x9ddfea08eb382d69L;

    private static long hashLen16(long u, long v, long mul) {
        long a = shiftMix((u ^ v) * mul);
        return shiftMix((v ^ a) * mul) * mul;
    }

    private static long mul(long len) {
        return K2 + (len << 1);
    }

    private static long hash1To3Bytes(int len, int firstByte, int midOrLastByte, int lastByte) {
        int y = firstByte + (midOrLastByte << BYTE_BITS);
        int z = len + (lastByte << LAST_BYTE_SHIFT);
        return shiftMix((((long) y) * K2) ^ (((long) z) * K0)) * K2;
    }

    private static long hash4To7Bytes(long len, long first4Bytes, long last4Bytes) {
        long mul = mul(len);
        return hashLen16(len + (first4Bytes << FIRST_INT_SHIFT), last4Bytes, mul);
    }

    private static long hash8To16Bytes(long len, long first8Bytes, long last8Bytes) {
        long mul = mul(len);
        long a = first8Bytes + K2;
        long c = rotateRight(last8Bytes, LEN8_ROTATION_1) * mul + a;
        long d = (rotateRight(a, LEN8_ROTATION_2) + last8Bytes) * mul;
        return hashLen16(c, d, mul);
    }

    static private <T> long hashLen0To16(M3Access<T> access, T in, long off, long len) {
        if (len >= WORD_BYTES_L) {
            long a = access.i64(in, off);
            long b = access.i64(in, off + len - WORD_BYTES_L);
            return hash8To16Bytes(len, a, b);
        } else if (len >= INT_BYTES_L) {
            long a = access.u32(in, off);
            long b = access.u32(in, off + len - INT_BYTES_L);
            return hash4To7Bytes(len, a, b);
        } else if (len > 0L) {
            int a = access.u8(in, off);
            int b = access.u8(in, off + (len >> 1));
            int c = access.u8(in, off + len - 1L);
            return hash1To3Bytes((int) len, a, b, c);
        }
        return K2;
    }

    static private <T> long hashLen17To32(M3Access<T> access, T in, long off, long len) {
        long mul = mul(len);
        long a = access.i64(in, off) * K1;
        long b = access.i64(in, off + WORD_BYTES_L);
        long c = access.i64(in, off + len - WORD_BYTES_L) * mul;
        long d = access.i64(in, off + len - WORDS_2_BYTES_L) * K2;
        return hashLen16(rotateRight(a + b, LEN17_ROTATION_1) + rotateRight(c, LEN17_ROTATION_2) + d,
                a + rotateRight(b + K2, LEN17_ROTATION_3) + c, mul);
    }

    static private <T> long cityHashLen33To64(M3Access<T> access, T in, long off, long len) {
        long mul = mul(len);
        long a = access.i64(in, off) * K2;
        long b = access.i64(in, off + WORD_BYTES_L);
        long c = access.i64(in, off + len - WORDS_3_BYTES_L);
        long d = access.i64(in, off + len - WORDS_4_BYTES_L);
        long e = access.i64(in, off + WORDS_2_BYTES_L) * K2;
        long f = access.i64(in, off + WORDS_3_BYTES_L) * LEN33_MULTIPLIER;
        long g = access.i64(in, off + len - WORD_BYTES_L);
        long h = access.i64(in, off + len - WORDS_2_BYTES_L) * mul;
        long u = rotateRight(a + g, LEN17_ROTATION_1) + (rotateRight(b, LEN17_ROTATION_2) + c) * LEN33_MULTIPLIER;
        long v = ((a + g) ^ d) + f + 1L;
        long w = reverseBytes((u + v) * mul) + h;
        long x = rotateRight(e + f, LEN33_ROTATION) + c;
        long y = (reverseBytes((v + w) * mul) + g) * mul;
        long z = e + f + c;
        a = reverseBytes((x + z) * mul + y) + b;
        b = shiftMix((z + a) * mul + d + h) * mul;
        return b + x;
    }

    static <T> long cityHash64(M3Access<T> access, T in, long off, long len) {
        if (len <= WORDS_4_BYTES_L) {
            if (len <= WORDS_2_BYTES_L) {
                return hashLen0To16(access, in, off, len);
            } else {
                return hashLen17To32(access, in, off, len);
            }
        } else if (len <= WORDS_8_BYTES_L) {
            return cityHashLen33To64(access, in, off, len);
        }

        long x = access.i64(in, off + len - WORDS_5_BYTES_L);
        long y = access.i64(in, off + len - WORDS_2_BYTES_L) + access.i64(in, off + len - WORDS_7_BYTES_L);
        long z = hashLen16(access.i64(in, off + len - WORDS_6_BYTES_L) + len,
                access.i64(in, off + len - WORDS_3_BYTES_L));

        long vFirst, vSecond, wFirst, wSecond;

        // This and following 3 blocks are produced by a single-click inline-function refactoring.
        // IntelliJ IDEA ftw
        // WeakHashLen32WithSeeds
        long a3 = len;
        long b3 = z;
        long w4 = access.i64(in, off + len - WORDS_8_BYTES_L);
        long x4 = access.i64(in, off + len - WORDS_8_BYTES_L + WORD_BYTES_L);
        long y4 = access.i64(in, off + len - WORDS_8_BYTES_L + WORDS_2_BYTES_L);
        long z4 = access.i64(in, off + len - WORDS_8_BYTES_L + WORDS_3_BYTES_L);
        a3 += w4;
        b3 = rotateRight(b3 + a3 + z4, WEAK_ROTATION_1);
        long c3 = a3;
        a3 += x4 + y4;
        b3 += rotateRight(a3, WEAK_ROTATION_2);
        vFirst = a3 + z4;
        vSecond = b3 + c3;

        // WeakHashLen32WithSeeds
        long a2 = y + K1;
        long b2 = x;
        long w3 = access.i64(in, off + len - WORDS_4_BYTES_L);
        long x3 = access.i64(in, off + len - WORDS_4_BYTES_L + WORD_BYTES_L);
        long y3 = access.i64(in, off + len - WORDS_4_BYTES_L + WORDS_2_BYTES_L);
        long z3 = access.i64(in, off + len - WORDS_4_BYTES_L + WORDS_3_BYTES_L);
        a2 += w3;
        b2 = rotateRight(b2 + a2 + z3, WEAK_ROTATION_1);
        long c2 = a2;
        a2 += x3 + y3;
        b2 += rotateRight(a2, WEAK_ROTATION_2);
        wFirst = a2 + z3;
        wSecond = b2 + c2;

        x = x * K1 + access.i64(in, off);

        len = (len - 1L) & (~BLOCK_MASK_L);
        do {
            x = rotateRight(x + y + vFirst + access.i64(in, off + WORD_BYTES_L), LOOP_ROTATION_X) * K1;
            y = rotateRight(y + vSecond + access.i64(in, off + WORDS_6_BYTES_L), LOOP_ROTATION_Y) * K1;
            x ^= wSecond;
            y += vFirst + access.i64(in, off + WORDS_5_BYTES_L);
            z = rotateRight(z + wFirst, LOOP_ROTATION_Z) * K1;

            // WeakHashLen32WithSeeds
            long a1 = vSecond * K1;
            long b1 = x + wFirst;
            long w2 = access.i64(in, off);
            long x2 = access.i64(in, off + WORD_BYTES_L);
            long y2 = access.i64(in, off + WORDS_2_BYTES_L);
            long z2 = access.i64(in, off + WORDS_3_BYTES_L);
            a1 += w2;
            b1 = rotateRight(b1 + a1 + z2, WEAK_ROTATION_1);
            long c1 = a1;
            a1 += x2 + y2;
            b1 += rotateRight(a1, WEAK_ROTATION_2);
            vFirst = a1 + z2;
            vSecond = b1 + c1;

            // WeakHashLen32WithSeeds
            long a = z + wSecond;
            long b = y + access.i64(in, off + WORDS_2_BYTES_L);
            long w1 = access.i64(in, off + WORDS_4_BYTES_L);
            long x1 = access.i64(in, off + WORDS_4_BYTES_L + WORD_BYTES_L);
            long y1 = access.i64(in, off + WORDS_4_BYTES_L + WORDS_2_BYTES_L);
            long z1 = access.i64(in, off + WORDS_4_BYTES_L + WORDS_3_BYTES_L);
            a += w1;
            b = rotateRight(b + a + z1, WEAK_ROTATION_1);
            long c = a;
            a += x1 + y1;
            b += rotateRight(a, WEAK_ROTATION_2);
            wFirst = a + z1;
            wSecond = b + c;

            long tmp = x;
            x = z;
            z = tmp;

            len -= WORDS_8_BYTES_L;
            off += WORDS_8_BYTES_L;
        } while (len != 0);
        return hashLen16(hashLen16(vFirst, wFirst) + shiftMix(y) * K1 + z,
                hashLen16(vSecond, wSecond) + x);
    }

    private static class AsLongHashFunction extends M3LongHashFunction {
        //FLLDCIM: M3 - LITERAL_CONSTANTS v1.1: this class's magic literals, named by role (user rule: no magic numbers, no floating strings)
        /** Byte length of a hashed long. */
        private static final long LONG_LENGTH = 8L;
        /** Byte length of a hashed int. */
        private static final long INT_LENGTH = 4L;
        /** Byte length of a hashed char. */
        private static final int CHAR_LENGTH = 2;

        private static final long serialVersionUID = 0L;
        private static final AsLongHashFunction SEEDLESS_INSTANCE = new AsLongHashFunction();

        private Object readResolve() {
            return SEEDLESS_INSTANCE;
        }

        @Override
        public long hashLong(long input) {
            input = M3Primitives.nativeToLittleEndian(input);
            long hash = hash8To16Bytes(LONG_LENGTH, input, input);
            return finalize(hash);
        }

        @Override
        public long hashInt(int input) {
            input = M3Primitives.nativeToLittleEndian(input);
            long unsignedInt = M3Primitives.unsignedInt(input);
            long hash = hash4To7Bytes(INT_LENGTH, unsignedInt, unsignedInt);
            return finalize(hash);
        }

        @Override
        public long hashShort(short input) {
            return hashChar((char) input);
        }

        private static final int FIRST_SHORT_BYTE_SHIFT = NATIVE_LITTLE_ENDIAN ? 0 : 8;
        // JIT could probably optimize & -1 to no-op
        private static final int FIRST_SHORT_BYTE_MASK = NATIVE_LITTLE_ENDIAN ? 0xFF : -1;
        private static final int SECOND_SHORT_BYTE_SHIFT = 8 - FIRST_SHORT_BYTE_SHIFT;
        private static final int SECOND_SHORT_BYTE_MASK = NATIVE_LITTLE_ENDIAN ? -1 : 0xFF;

        @Override
        public long hashChar(char input) {
            int unsignedInput = (int) input;
            int firstByte = (unsignedInput >> FIRST_SHORT_BYTE_SHIFT) & FIRST_SHORT_BYTE_MASK;
            int secondByte = (unsignedInput >> SECOND_SHORT_BYTE_SHIFT) & SECOND_SHORT_BYTE_MASK;
            long hash = hash1To3Bytes(CHAR_LENGTH, firstByte, secondByte, secondByte);
            return finalize(hash);
        }

        @Override
        public long hashByte(byte input) {
            int unsignedByte = M3Primitives.unsignedByte(input);
            long hash = hash1To3Bytes(1, unsignedByte, unsignedByte, unsignedByte);
            return finalize(hash);
        }

        @Override
        public long hashVoid() {
            return K2;
        }

        @Override
        public <T> long hash(T input, M3Access<T> access, long off, long len) {
            long hash;
            hash = M3CityAndFarmHash_1_1.cityHash64(access.byteOrder(input, LITTLE_ENDIAN), input, off, len);
            return finalize(hash);
        }

        long finalize(long hash) {
            return hash;
        }
    }

    static M3LongHashFunction asLongHashFunctionWithoutSeed() {
        return AsLongHashFunction.SEEDLESS_INSTANCE;
    }

    private static class AsLongHashFunctionSeeded extends AsLongHashFunction {
        private static final long serialVersionUID = 0L;

        final long seed0, seed1;
        private final transient long voidHash;

        private AsLongHashFunctionSeeded(long seed0, long seed1) {
            this.seed0 = seed0;
            this.seed1 = seed1;
            voidHash = finalize(K2);
        }

        @Override
        public long hashVoid() {
            return voidHash;
        }

        @Override
        protected long finalize(long hash) {
            return hashLen16(hash - seed0, seed1);
        }
    }

    static M3LongHashFunction asLongHashFunctionWithSeed(long seed) {
        return new AsLongHashFunctionSeeded(K2, seed);
    }

    static M3LongHashFunction asLongHashFunctionWithTwoSeeds(long seed0, long seed1) {
        return new AsLongHashFunctionSeeded(seed0, seed1);
    }


    // FarmHash

    private static <T> long naHashLen33To64(M3Access<T> access, T in, long off, long len) {
        long mul = mul(len);
        long a = access.i64(in, off) * K2;
        long b = access.i64(in, off + WORD_BYTES_L);
        long c = access.i64(in, off + len - WORD_BYTES) * mul;
        long d = access.i64(in, off + len - WORDS_2_BYTES) * K2;
        long y = rotateRight(a + b, LEN17_ROTATION_1) + rotateRight(c, LEN17_ROTATION_2) + d;
        long z = hashLen16(y, a + rotateRight(b + K2, LEN17_ROTATION_3) + c, mul);
        long e = access.i64(in, off + WORDS_2_BYTES) * mul;
        long f = access.i64(in, off + WORDS_3_BYTES);
        long g = (y + access.i64(in, off + len - WORDS_4_BYTES)) * mul;
        long h = (z + access.i64(in, off + len - WORDS_3_BYTES)) * mul;
        return hashLen16(rotateRight(e + f, LEN17_ROTATION_1) + rotateRight(g, LEN17_ROTATION_2) + h,
                e + rotateRight(f + a, LEN17_ROTATION_3) + g, mul);
    }

    static <T> long naHash64(M3Access<T> access, T in, long off, long len) {
        final long seed = FARM_SEED;
        if (len <= WORDS_4_BYTES) {
            if (len <= WORDS_2_BYTES) {
                return hashLen0To16(access, in, off, len);
            } else {
                return hashLen17To32(access, in, off, len);
            }
        } else if (len <= WORDS_8_BYTES) {
            return naHashLen33To64(access, in, off, len);
        }

        // For strings over 64 bytes we loop.  Internal state consists of
        // 56 bytes: v, w, x, y, and z.
        long x = seed;
        // == seed * k1 + 113; This overflows uint64 and is a compile error,
        // so we expand the constant by hand
        @SuppressWarnings("NumericOverflow")
        long y = seed * K1 + SEED_ADDEND;
        long z = shiftMix(y * K2 + SEED_ADDEND) * K2;
        long v1 = 0, v2 = 0;
        long w1 = 0, w2 = 0;
        x = x * K2 + access.i64(in, off);

        // Set end so that after the loop we have 1 to 64 bytes left to process.
        long end = off + ((len - 1) >> BLOCK_SHIFT) * WORDS_8_BYTES;
        long last64 = end + ((len - 1) & BLOCK_MASK) - BLOCK_MASK;

        do {
            x = rotateRight(x + y + v1 + access.i64(in, off + WORD_BYTES), LOOP_ROTATION_X) * K1;
            y = rotateRight(y + v2 + access.i64(in, off + WORDS_6_BYTES), LOOP_ROTATION_Y) * K1;
            x ^= w2;
            y += v1 + access.i64(in, off + WORDS_5_BYTES);
            z = rotateRight(z + w1, LOOP_ROTATION_Z) * K1;
            long a = v2 * K1;
            long b = x + w1;
            long z1 = access.i64(in, off + WORDS_3_BYTES);
            a += access.i64(in, off);
            b = rotateRight(b + a + z1, WEAK_ROTATION_1);
            long c = a;
            a += access.i64(in, off + WORD_BYTES);
            a += access.i64(in, off + WORDS_2_BYTES);
            b += rotateRight(a, WEAK_ROTATION_2);
            v1 = a + z1;
            v2 = b + c;
            long a1 = z + w2;
            long b1 = y + access.i64(in, off + WORDS_2_BYTES);
            long z2 = access.i64(in, off + WORDS_4_BYTES + WORDS_3_BYTES);
            a1 += access.i64(in, off + WORDS_4_BYTES);
            b1 = rotateRight(b1 + a1 + z2, WEAK_ROTATION_1);
            long c1 = a1;
            a1 += access.i64(in, off + WORDS_4_BYTES + WORD_BYTES);
            a1 += access.i64(in, off + WORDS_4_BYTES + WORDS_2_BYTES);
            b1 += rotateRight(a1, WEAK_ROTATION_2);
            w1 = a1 + z2;
            w2 = b1 + c1;
            long t = z;
            z = x;
            x = t;
        } while ((off += WORDS_8_BYTES) != end);

        off = last64;

        long mul = K1 + ((z & MUL_BYTE_MASK) << 1);

        // Make s point to the last 64 bytes of input.
        w1 += (len - 1) & BLOCK_MASK;
        v1 += w1;
        w1 += v1;
        x = rotateRight(x + y + v1 + access.i64(in, off + WORD_BYTES), LOOP_ROTATION_X) * mul;
        y = rotateRight(y + v2 + access.i64(in, off + WORDS_6_BYTES), LOOP_ROTATION_Y) * mul;
        x ^= w2 * ROUND_MULTIPLIER;
        y += v1 * ROUND_MULTIPLIER + access.i64(in, off + WORDS_5_BYTES);
        z = rotateRight(z + w1, LOOP_ROTATION_Z) * mul;
        long a = v2 * mul;
        long b = x + w1;
        long z1 = access.i64(in, off + WORDS_3_BYTES);
        a += access.i64(in, off);
        b = rotateRight(b + a + z1, WEAK_ROTATION_1);
        long c = a;
        a += access.i64(in, off + WORD_BYTES);
        a += access.i64(in, off + WORDS_2_BYTES);
        b += rotateRight(a, WEAK_ROTATION_2);
        v1 = a + z1;
        v2 = b + c;
        long a1 = z + w2;
        long b1 = y + access.i64(in, off + WORDS_2_BYTES);
        long z2 = access.i64(in, off + WORDS_4_BYTES + WORDS_3_BYTES);
        a1 += access.i64(in, off + WORDS_4_BYTES);
        b1 = rotateRight(b1 + a1 + z2, WEAK_ROTATION_1);
        long c1 = a1;
        a1 += access.i64(in, off + WORDS_4_BYTES + WORD_BYTES);
        a1 += access.i64(in, off + WORDS_4_BYTES + WORDS_2_BYTES);
        b1 += rotateRight(a1, WEAK_ROTATION_2);
        w1 = a1 + z2;
        w2 = b1 + c1;
        long t = z;
        z = x;
        x = t;
        return hashLen16(hashLen16(v1, w1, mul) + shiftMix(y) * K0 + z,
                hashLen16(v2, w2, mul) + x,
                mul);
    }

    private static <T> long naHash64WithSeeds(M3Access<T> access, T in, long off, long len, long seed0, long seed1) {
        return hashLen16(naHash64(access, in, off, len) - seed0, seed1);
    }

    private static long uoH(long x, long y, long mul, int r) {
        long a = (x ^ y) * mul;
        a = shiftMix(a);
        long b = (y ^ a) * mul;
        return rotateRight(b, r) * mul;
    }

    static <T> long uoHash64WithSeeds(M3Access<T> access, T in, long off, long len, long seed0, long seed1) {
        if (len <= WORDS_8_BYTES) {
            return naHash64WithSeeds(access, in, off, len, seed0, seed1);
        }

        long x = seed0;
        long y = seed1 * K2 + SEED_ADDEND;
        long z = shiftMix(y * K2) * K2;
        long v0 = seed0;
        long v1 = seed1;
        long w0 = 0;
        long w1 = 0;
        long u = x - z;
        x *= K2;
        long mul = K2 + (u & MUL_BIT_MASK);

        long end = off + ((len - 1) >> BLOCK_SHIFT) * WORDS_8_BYTES;
        long last64 = end + ((len - 1) & BLOCK_MASK) - BLOCK_MASK;

        do {
            long a0 = access.i64(in, off);
            long a1 = access.i64(in, off + WORD_BYTES);
            long a2 = access.i64(in, off + WORDS_2_BYTES);
            long a3 = access.i64(in, off + WORDS_3_BYTES);
            long a4 = access.i64(in, off + WORDS_4_BYTES);
            long a5 = access.i64(in, off + WORDS_5_BYTES);
            long a6 = access.i64(in, off + WORDS_6_BYTES);
            long a7 = access.i64(in, off + WORDS_7_BYTES);
            x += a0 + a1;
            y += a2;
            z += a3;
            v0 += a4;
            v1 += a5 + a1;
            w0 += a6;
            w1 += a7;

            x = rotateRight(x, UO_ROTATION_X);
            x *= ROUND_MULTIPLIER;
            y = rotateRight(y, UO_ROTATION_Y);
            z *= mul;
            v0 = rotateRight(v0, UO_ROTATION_V0);
            v1 = rotateRight(v1, UO_ROTATION_V1);
            w0 ^= x;
            w0 *= ROUND_MULTIPLIER;
            z = rotateRight(z, UO_ROTATION_Z);
            z += w1;
            w1 += z;
            z *= ROUND_MULTIPLIER;

            long t = u;
            u = y;
            y = t;

            z += a0 + a6;
            v0 += a2;
            v1 += a3;
            w0 += a4;
            w1 += a5 + a6;
            x += a1;
            y += a7;

            y += v0;
            v0 += x - y;
            v1 += w0;
            w0 += v1;
            w1 += x - y;
            x += w1;
            w1 = rotateRight(w1, UO_ROTATION_W1);

            t = u;
            u = z;
            z = t;

        } while ((off += WORDS_8_BYTES) != end);

        off = last64;

        u *= ROUND_MULTIPLIER;
        v1 = rotateRight(v1, UO_ROTATION_V1_LAST);
        v0 = rotateRight(v0, UO_ROTATION_V0_LAST);
        w0 += (len - 1) & BLOCK_MASK;
        u += y;
        y += u;
        x = rotateRight(y - x + v0 + access.i64(in, off + WORD_BYTES), LOOP_ROTATION_X) * mul;
        y = rotateRight(y ^ v1 ^ access.i64(in, off + WORDS_6_BYTES), LOOP_ROTATION_Y) * mul;
        x ^= w1 * ROUND_MULTIPLIER;
        y += v0 + access.i64(in, off + WORDS_5_BYTES);
        z = rotateRight(z + w0, LOOP_ROTATION_Z) * mul;

        long a = v1 * mul;
        long b = x + w0;
        long z1 = access.i64(in, off + WORDS_3_BYTES);
        a += access.i64(in, off);
        b = rotateRight(b + a + z1, WEAK_ROTATION_1);
        long c = a;
        a += access.i64(in, off + WORD_BYTES);
        a += access.i64(in, off + WORDS_2_BYTES);
        b += rotateRight(a, WEAK_ROTATION_2);
        v0 =  a + z1;
        v1 = b + c;

        long a1 = z + w1;
        long b1 = y + access.i64(in, off + WORDS_2_BYTES);
        long z2 = access.i64(in, off + WORDS_4_BYTES + WORDS_3_BYTES);
        a1 += access.i64(in, off + WORDS_4_BYTES);
        b1 = rotateRight(b1 + a1 + z2, WEAK_ROTATION_1);
        long c1 = a1;
        a1 += access.i64(in, off + WORDS_4_BYTES + WORD_BYTES);
        a1 += access.i64(in, off + WORDS_4_BYTES + WORDS_2_BYTES);
        b1 += rotateRight(a1, WEAK_ROTATION_2);
        w0 = a1 + z2;
        w1 = b1 + c1;
        return uoH(hashLen16(v0 + x, w0 ^ y, mul) + z - u,
                uoH(v1 + y, w1 + z, K2, UO_H_ROTATION_INNER) ^ x,
                K2,
                UO_H_ROTATION_OUTER);
    }

    private static class Na extends M3CityAndFarmHash_1_1.AsLongHashFunction {
        private static final long serialVersionUID = 0L;
        private static final Na SEEDLESS_NA = new Na();

        private Object readResolve() {
            return SEEDLESS_NA;
        }

        @Override
        public <T> long hash(T input, M3Access<T> access, long off, long len) {
            long hash;
            hash = M3CityAndFarmHash_1_1.naHash64(access.byteOrder(input, LITTLE_ENDIAN), input, off, len);
            return finalize(hash);
        }
    }

    static M3LongHashFunction naWithoutSeeds() {
        return Na.SEEDLESS_NA;
    }

    private static class NaSeeded extends Na {
        private static final long serialVersionUID = 0L;

        private final long seed0, seed1;
        private final transient long voidHash;

        private NaSeeded(long seed0, long seed1) {
            this.seed0 = seed0;
            this.seed1 = seed1;
            voidHash = finalize(K2);
        }

        @Override
        public long hashVoid() {
            return voidHash;
        }

        @Override
        protected long finalize(long hash) {
            return hashLen16(hash - seed0, seed1);
        }
    }

    static M3LongHashFunction naWithSeed(long seed) {
        return new NaSeeded(K2, seed);
    }

    static M3LongHashFunction naWithSeeds(long seed0, long seed1) {
        return new NaSeeded(seed0, seed1);
    }

    private static final class Uo extends AsLongHashFunction {
        //FLLDCIM: M3 - LITERAL_CONSTANTS v1.1: this class's magic literals, named by role (user rule: no magic numbers, no floating strings)
        /** Bytes of 8 64-bit words. */
        private static final int WORDS_8_BYTES = 64;
        /** Default seed of FarmHash na and uo. */
        private static final int FARM_SEED = 81;

        private static final long serialVersionUID = 0L;
        private static final Uo SEEDLESS_UO = new Uo();

        private Object readResolve() {
            return SEEDLESS_UO;
        }

        @Override
        public <T> long hash(T input, M3Access<T> access, long off, long len) {
            if (len <= WORDS_8_BYTES) {
                return M3CityAndFarmHash_1_1.naHash64(access.byteOrder(input, LITTLE_ENDIAN), input, off, len);
            }
            return M3CityAndFarmHash_1_1.uoHash64WithSeeds(access.byteOrder(input, LITTLE_ENDIAN), input, off, len, FARM_SEED, 0);
        }
    }

    static M3LongHashFunction uoWithoutSeeds() {
        return Uo.SEEDLESS_UO;
    }

    private final static class UoWithOneSeed extends AsLongHashFunctionSeeded {
        //FLLDCIM: M3 - LITERAL_CONSTANTS v1.1: this class's magic literals, named by role (user rule: no magic numbers, no floating strings)
        /** Bytes of 8 64-bit words. */
        private static final int WORDS_8_BYTES = 64;

        private static final long serialVersionUID = 0L;

        private UoWithOneSeed(long seed) {
            super(K2, seed);
        }

        @Override
        public <T> long hash(T input, M3Access<T> access, long off, long len) {
            if (len <= WORDS_8_BYTES) {
                return finalize(M3CityAndFarmHash_1_1.naHash64(access.byteOrder(input, LITTLE_ENDIAN), input, off, len));
            }
            return M3CityAndFarmHash_1_1.uoHash64WithSeeds(access.byteOrder(input, LITTLE_ENDIAN), input, off, len, 0, seed1);
        }
    }

    static M3LongHashFunction uoWithSeed(long seed) {
        return new UoWithOneSeed(seed);
    }

    private static class UoSeeded extends AsLongHashFunctionSeeded {
        private static final long serialVersionUID = 0L;

        private UoSeeded(long seed0, long seed1) {
            super(seed0, seed1);
        }

        @Override
        public <T> long hash(T input, M3Access<T> access, long off, long len) {
            return M3CityAndFarmHash_1_1.uoHash64WithSeeds(
                    access.byteOrder(input, LITTLE_ENDIAN), input, off, len, seed0, seed1);
        }
    }

    static M3LongHashFunction uoWithSeeds(long seed0, long seed1) {
        return new UoSeeded(seed0, seed1);
    }
}
