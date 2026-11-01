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

import static java.nio.ByteOrder.LITTLE_ENDIAN;

/**
 * Adapted version of xxHash implementation from https://github.com/Cyan4973/xxHash.
 * This implementation provides endian-independant hash values, but it's slower on big-endian platforms.
 */
class M3XxHash {
    //FLLDCIM: M3 - LITERAL_CONSTANTS v1.1: this class's magic literals, named by role (user rule: no magic numbers, no floating strings)
    /** Bytes of one stripe: four 64-bit lanes. */
    private static final int STRIPE_BYTES = 32;
    /** Rotation of the round function. */
    private static final int ROUND_ROTATION = 31;
    /** Offset of the second lane in a stripe. */
    private static final int LANE_2_OFFSET = 8;
    /** Offset of the third lane in a stripe. */
    private static final int LANE_3_OFFSET = 16;
    /** Offset of the fourth lane in a stripe. */
    private static final int LANE_4_OFFSET = 24;
    /** Rotation of the second lane when the lanes merge. */
    private static final int MERGE_ROTATION_2 = 7;
    /** Rotation of the third lane when the lanes merge. */
    private static final int MERGE_ROTATION_3 = 12;
    /** Rotation of the fourth lane when the lanes merge. */
    private static final int MERGE_ROTATION_4 = 18;
    /** Bytes of a 64-bit word: the step of the 8-byte tail. */
    private static final int WORD_BYTES = 8;
    /** Rotation after a 64-bit tail word. */
    private static final int WORD_ROTATION = 27;
    /** Bytes of an int: the step of the 4-byte tail. */
    private static final int INT_BYTES = 4;
    /** Rotation after a 32-bit tail int. */
    private static final int INT_ROTATION = 23;
    /** Rotation after a tail byte. */
    private static final int BYTE_ROTATION = 11;
    /** First shift of the avalanche. */
    private static final int AVALANCHE_SHIFT_1 = 33;
    /** Second shift of the avalanche. */
    private static final int AVALANCHE_SHIFT_2 = 29;
    /** Third shift of the avalanche. */
    private static final int AVALANCHE_SHIFT_3 = 32;

    // Primes if treated as unsigned
    private static final long P1 = -7046029288634856825L;
    private static final long P2 = -4417276706812531889L;
    private static final long P3 = 1609587929392839161L;
    private static final long P4 = -8796714831421723037L;
    private static final long P5 = 2870177450012600261L;

    static <T> long xxHash64(long seed, T input, M3Access<T> access, long off, long length) {
        long hash;
        long remaining = length;

        if (remaining >= STRIPE_BYTES) {
            long v1 = seed + P1 + P2;
            long v2 = seed + P2;
            long v3 = seed;
            long v4 = seed - P1;

            do {
                v1 += access.i64(input, off) * P2;
                v1 = Long.rotateLeft(v1, ROUND_ROTATION);
                v1 *= P1;

                v2 += access.i64(input, off + LANE_2_OFFSET) * P2;
                v2 = Long.rotateLeft(v2, ROUND_ROTATION);
                v2 *= P1;

                v3 += access.i64(input, off + LANE_3_OFFSET) * P2;
                v3 = Long.rotateLeft(v3, ROUND_ROTATION);
                v3 *= P1;

                v4 += access.i64(input, off + LANE_4_OFFSET) * P2;
                v4 = Long.rotateLeft(v4, ROUND_ROTATION);
                v4 *= P1;

                off += STRIPE_BYTES;
                remaining -= STRIPE_BYTES;
            } while (remaining >= STRIPE_BYTES);

            hash = Long.rotateLeft(v1, 1)
                + Long.rotateLeft(v2, MERGE_ROTATION_2)
                + Long.rotateLeft(v3, MERGE_ROTATION_3)
                + Long.rotateLeft(v4, MERGE_ROTATION_4);

            v1 *= P2;
            v1 = Long.rotateLeft(v1, ROUND_ROTATION);
            v1 *= P1;
            hash ^= v1;
            hash = hash * P1 + P4;

            v2 *= P2;
            v2 = Long.rotateLeft(v2, ROUND_ROTATION);
            v2 *= P1;
            hash ^= v2;
            hash = hash * P1 + P4;

            v3 *= P2;
            v3 = Long.rotateLeft(v3, ROUND_ROTATION);
            v3 *= P1;
            hash ^= v3;
            hash = hash * P1 + P4;

            v4 *= P2;
            v4 = Long.rotateLeft(v4, ROUND_ROTATION);
            v4 *= P1;
            hash ^= v4;
            hash = hash * P1 + P4;
        } else {
            hash = seed + P5;
        }

        hash += length;

        while (remaining >= WORD_BYTES) {
            long k1 = access.i64(input, off);
            k1 *= P2;
            k1 = Long.rotateLeft(k1, ROUND_ROTATION);
            k1 *= P1;
            hash ^= k1;
            hash = Long.rotateLeft(hash, WORD_ROTATION) * P1 + P4;
            off += WORD_BYTES;
            remaining -= WORD_BYTES;
        }

        if (remaining >= INT_BYTES) {
            hash ^= access.u32(input, off) * P1;
            hash = Long.rotateLeft(hash, INT_ROTATION) * P2 + P3;
            off += INT_BYTES;
            remaining -= INT_BYTES;
        }

        while (remaining != 0) {
            hash ^= access.u8(input, off) * P5;
            hash = Long.rotateLeft(hash, BYTE_ROTATION) * P1;
            --remaining;
            ++off;
        }

        return finalize(hash);
    }

    private static long finalize(long hash) {
        hash ^= hash >>> AVALANCHE_SHIFT_1;
        hash *= P2;
        hash ^= hash >>> AVALANCHE_SHIFT_2;
        hash *= P3;
        hash ^= hash >>> AVALANCHE_SHIFT_3;
        return hash;
    }

    static M3LongHashFunction asLongHashFunctionWithoutSeed() {
        return AsLongHashFunction.SEEDLESS_INSTANCE;
    }

    private static class AsLongHashFunction extends M3LongHashFunction {
        //FLLDCIM: M3 - LITERAL_CONSTANTS v1.1: this class's magic literals, named by role (user rule: no magic numbers, no floating strings)
        /** Byte length of a hashed long. */
        private static final int LONG_LENGTH = 8;
        /** Rotation of the round function. */
        private static final int ROUND_ROTATION = 31;
        /** Rotation after a 64-bit tail word. */
        private static final int WORD_ROTATION = 27;
        /** Byte length of a hashed int. */
        private static final int INT_LENGTH = 4;
        /** Rotation after a 32-bit tail int. */
        private static final int INT_ROTATION = 23;
        /** Byte length of a hashed short or char. */
        private static final int SHORT_LENGTH = 2;
        /** Rotation after a tail byte. */
        private static final int BYTE_ROTATION = 11;
        /** Bits of a byte: the second byte of a short. */
        private static final int BYTE_BITS = 8;

        private static final long serialVersionUID = 0L;
        static final AsLongHashFunction SEEDLESS_INSTANCE = new AsLongHashFunction();
        private static final long VOID_HASH = M3XxHash.finalize(P5);

        private Object readResolve() {
            return SEEDLESS_INSTANCE;
        }

        public long seed() {
            return 0L;
        }

        @Override
        public long hashLong(long input) {
            input = M3Primitives.nativeToLittleEndian(input);
            long hash = seed() + P5 + LONG_LENGTH;
            input *= P2;
            input = Long.rotateLeft(input, ROUND_ROTATION);
            input *= P1;
            hash ^= input;
            hash = Long.rotateLeft(hash, WORD_ROTATION) * P1 + P4;
            return M3XxHash.finalize(hash);
        }

        @Override
        public long hashInt(int input) {
            input = M3Primitives.nativeToLittleEndian(input);
            long hash = seed() + P5 + INT_LENGTH;
            hash ^= M3Primitives.unsignedInt(input) * P1;
            hash = Long.rotateLeft(hash, INT_ROTATION) * P2 + P3;
            return M3XxHash.finalize(hash);
        }

        @Override
        public long hashShort(short input) {
            input = M3Primitives.nativeToLittleEndian(input);
            long hash = seed() + P5 + SHORT_LENGTH;
            hash ^= M3Primitives.unsignedByte(input) * P5;
            hash = Long.rotateLeft(hash, BYTE_ROTATION) * P1;
            hash ^= M3Primitives.unsignedByte(input >> BYTE_BITS) * P5;
            hash = Long.rotateLeft(hash, BYTE_ROTATION) * P1;
            return M3XxHash.finalize(hash);
        }

        @Override
        public long hashChar(char input) {
            return hashShort((short) input);
        }

        @Override
        public long hashByte(byte input) {
            long hash = seed() + P5 + 1;
            hash ^= M3Primitives.unsignedByte(input) * P5;
            hash = Long.rotateLeft(hash, BYTE_ROTATION) * P1;
            return M3XxHash.finalize(hash);
        }

        @Override
        public long hashVoid() {
            return VOID_HASH;
        }

        @Override
        public <T> long hash(T input, M3Access<T> access, long off, long len) {
            long seed = seed();
            return M3XxHash.xxHash64(seed, input, access.byteOrder(input, LITTLE_ENDIAN), off, len);
        }
    }

    static M3LongHashFunction asLongHashFunctionWithSeed(long seed) {
        return new AsLongHashFunctionSeeded(seed);
    }

    private static class AsLongHashFunctionSeeded extends AsLongHashFunction {
        private static final long serialVersionUID = 0L;

        private final long seed;
        private final transient long voidHash;

        private AsLongHashFunctionSeeded(long seed) {
            this.seed = seed;
            voidHash = M3XxHash.finalize(seed + P5);
        }

        @Override
        public long seed() {
            return seed;
        }

        @Override
        public long hashVoid() {
            return voidHash;
        }
    }
}
