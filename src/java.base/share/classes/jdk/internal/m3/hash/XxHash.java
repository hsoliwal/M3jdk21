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
 * Changed by Synexia (2026-11-06): the stripe round, lane merge and tail of xxHash64 are package-private atoms
 * that M3XxHash64Streaming composes too (recipe m3/qualification/hash-xxhash64-streaming-20261106).
 */
package jdk.internal.m3.hash;

import static java.nio.ByteOrder.LITTLE_ENDIAN;

/**
 * Adapted version of xxHash implementation from https://github.com/Cyan4973/xxHash.
 * This implementation provides endian-independant hash values, but it's slower on big-endian platforms.
 */
class XxHash {
    //FLLDCIM: M3 - LITERAL_CONSTANTS v1.1: this class's magic literals, named by role (user rule: no magic numbers, no floating strings)
    //FLLDCIM: HPCPOP - XXH64_KERNEL_ATOMS v1: package-private for M3XxHash64Streaming (one xxHash64 kernel in java.base; recipe m3/qualification/hash-xxhash64-streaming-20261106)
    /** Bytes of one stripe: four 64-bit lanes. */
    static final int STRIPE_BYTES = 32;
    /** Rotation of the round function. */
    private static final int ROUND_ROTATION = 31;
    //FLLDCIM: HPCPOP - XXH64_KERNEL_ATOMS v1: package-private for M3XxHash64Streaming (one xxHash64 kernel in java.base; recipe m3/qualification/hash-xxhash64-streaming-20261106)
    /** Offset of the second lane in a stripe. */
    static final int LANE_2_OFFSET = 8;
    //FLLDCIM: HPCPOP - XXH64_KERNEL_ATOMS v1: package-private for M3XxHash64Streaming (one xxHash64 kernel in java.base; recipe m3/qualification/hash-xxhash64-streaming-20261106)
    /** Offset of the third lane in a stripe. */
    static final int LANE_3_OFFSET = 16;
    //FLLDCIM: HPCPOP - XXH64_KERNEL_ATOMS v1: package-private for M3XxHash64Streaming (one xxHash64 kernel in java.base; recipe m3/qualification/hash-xxhash64-streaming-20261106)
    /** Offset of the fourth lane in a stripe. */
    static final int LANE_4_OFFSET = 24;
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

    //FLLDCIM: HPCPOP - XXH64_KERNEL_ATOMS v1: the one-shot composes the stripe round, the lane merge and the tail atoms below; M3XxHash64Streaming composes the same atoms over its scratch stripe, so java.base carries one xxHash64 kernel (recipe m3/qualification/hash-xxhash64-streaming-20261106)
    static <T> long xxHash64(long seed, T input, Access<T> access, long off, long length) {
        long hash;
        long remaining = length;

        if (remaining >= STRIPE_BYTES) {
            long v1 = lane1(seed);
            long v2 = lane2(seed);
            long v3 = lane3(seed);
            long v4 = lane4(seed);

            do {
                v1 = round(v1, access.i64(input, off));
                v2 = round(v2, access.i64(input, off + LANE_2_OFFSET));
                v3 = round(v3, access.i64(input, off + LANE_3_OFFSET));
                v4 = round(v4, access.i64(input, off + LANE_4_OFFSET));
                off += STRIPE_BYTES;
                remaining -= STRIPE_BYTES;
            } while (remaining >= STRIPE_BYTES);

            hash = mergeLanes(v1, v2, v3, v4);
        } else {
            hash = shortStart(seed);
        }

        hash += length;
        return tail(hash, input, access, off, remaining);
    }

    //FLLDCIM: HPCPOP - XXH64_KERNEL_ATOMS v1: the four lane seeds of a stripe state (xxHash XXH64_reset)
    static long lane1(long seed) {
        return seed + P1 + P2;
    }

    static long lane2(long seed) {
        return seed + P2;
    }

    static long lane3(long seed) {
        return seed;
    }

    static long lane4(long seed) {
        return seed - P1;
    }

    //FLLDCIM: HPCPOP - XXH64_KERNEL_ATOMS v1: the start value of an input shorter than one stripe
    static long shortStart(long seed) {
        return seed + P5;
    }

    //FLLDCIM: HPCPOP - XXH64_KERNEL_ATOMS v1: the stripe round (xxHash XXH64_round)
    static long round(long acc, long input) {
        acc += input * P2;
        acc = Long.rotateLeft(acc, ROUND_ROTATION);
        acc *= P1;
        return acc;
    }

    //FLLDCIM: HPCPOP - XXH64_KERNEL_ATOMS v1: one lane folded into the merged hash (xxHash XXH64_mergeRound)
    static long mergeRound(long hash, long lane) {
        hash ^= round(0L, lane);
        return hash * P1 + P4;
    }

    //FLLDCIM: HPCPOP - XXH64_KERNEL_ATOMS v1: the four lanes merged after the last full stripe (xxHash XXH64_mergeAccs)
    static long mergeLanes(long v1, long v2, long v3, long v4) {
        long hash = Long.rotateLeft(v1, 1)
            + Long.rotateLeft(v2, MERGE_ROTATION_2)
            + Long.rotateLeft(v3, MERGE_ROTATION_3)
            + Long.rotateLeft(v4, MERGE_ROTATION_4);
        hash = mergeRound(hash, v1);
        hash = mergeRound(hash, v2);
        hash = mergeRound(hash, v3);
        return mergeRound(hash, v4);
    }

    //FLLDCIM: HPCPOP - XXH64_KERNEL_ATOMS v1: the tail once the length is folded in: 8-byte words, one 4-byte int, the bytes, the avalanche (xxHash XXH64_finalize)
    static <T> long tail(long hash, T input, Access<T> access, long off, long remaining) {
        while (remaining >= WORD_BYTES) {
            hash ^= round(0L, access.i64(input, off));
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
        return tailBytes(hash, input, access, off, remaining);
    }

    private static <T> long tailBytes(long hash, T input, Access<T> access, long off, long remaining) {
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

    static LongHashFunction asLongHashFunctionWithoutSeed() {
        return AsLongHashFunction.SEEDLESS_INSTANCE;
    }

    private static class AsLongHashFunction extends LongHashFunction {
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
        private static final long VOID_HASH = XxHash.finalize(P5);

        private Object readResolve() {
            return SEEDLESS_INSTANCE;
        }

        public long seed() {
            return 0L;
        }

        @Override
        public long hashLong(long input) {
            input = Primitives.nativeToLittleEndian(input);
            long hash = seed() + P5 + LONG_LENGTH;
            input *= P2;
            input = Long.rotateLeft(input, ROUND_ROTATION);
            input *= P1;
            hash ^= input;
            hash = Long.rotateLeft(hash, WORD_ROTATION) * P1 + P4;
            return XxHash.finalize(hash);
        }

        @Override
        public long hashInt(int input) {
            input = Primitives.nativeToLittleEndian(input);
            long hash = seed() + P5 + INT_LENGTH;
            hash ^= Primitives.unsignedInt(input) * P1;
            hash = Long.rotateLeft(hash, INT_ROTATION) * P2 + P3;
            return XxHash.finalize(hash);
        }

        @Override
        public long hashShort(short input) {
            input = Primitives.nativeToLittleEndian(input);
            long hash = seed() + P5 + SHORT_LENGTH;
            hash ^= Primitives.unsignedByte(input) * P5;
            hash = Long.rotateLeft(hash, BYTE_ROTATION) * P1;
            hash ^= Primitives.unsignedByte(input >> BYTE_BITS) * P5;
            hash = Long.rotateLeft(hash, BYTE_ROTATION) * P1;
            return XxHash.finalize(hash);
        }

        @Override
        public long hashChar(char input) {
            return hashShort((short) input);
        }

        @Override
        public long hashByte(byte input) {
            long hash = seed() + P5 + 1;
            hash ^= Primitives.unsignedByte(input) * P5;
            hash = Long.rotateLeft(hash, BYTE_ROTATION) * P1;
            return XxHash.finalize(hash);
        }

        @Override
        public long hashVoid() {
            return VOID_HASH;
        }

        @Override
        public <T> long hash(T input, Access<T> access, long off, long len) {
            long seed = seed();
            return XxHash.xxHash64(seed, input, access.byteOrder(input, LITTLE_ENDIAN), off, len);
        }
    }

    static LongHashFunction asLongHashFunctionWithSeed(long seed) {
        return new AsLongHashFunctionSeeded(seed);
    }

    private static class AsLongHashFunctionSeeded extends AsLongHashFunction {
        private static final long serialVersionUID = 0L;

        private final long seed;
        private final transient long voidHash;

        private AsLongHashFunctionSeeded(long seed) {
            this.seed = seed;
            voidHash = XxHash.finalize(seed + P5);
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
