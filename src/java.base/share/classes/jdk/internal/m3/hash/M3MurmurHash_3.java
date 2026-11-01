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


import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static jdk.internal.m3.hash.M3Primitives.unsignedInt;
import static jdk.internal.m3.hash.M3Primitives.unsignedShort;

/**
 * Derived from https://github.com/google/guava/blob/fa95e381e665d8ee9639543b99ed38020c8de5ef
 * /guava/src/com/google/common/hash/Murmur3_128HashFunction.java
 */
class M3MurmurHash_3 {
    //FLLDCIM: M3 - LITERAL_CONSTANTS v1.1: this class's magic literals, named by role (user rule: no magic numbers, no floating strings)
    /** Bytes of one 128-bit block. */
    private static final long BLOCK_BYTES = 16L;
    /** Offset of the second 64-bit half of a block. */
    private static final long HALF_BLOCK_OFFSET = 8L;
    /** Rotation of h1 after each block. */
    private static final int H1_ROTATION = 27;
    /** h = h * 5 + c after each block. */
    private static final long MIX_MULTIPLIER = 5L;
    /** Addend of h1 after each block. */
    private static final long H1_ADDEND = 0x52dce729L;
    /** Rotation of h2 after each block. */
    private static final int H2_ROTATION = 31;
    /** Addend of h2 after each block. */
    private static final long H2_ADDEND = 0x38495ab5L;
    /** A tail of 15 bytes. */
    private static final int TAIL_15 = 15;
    /** Offset of byte 14 of the tail. */
    private static final long TAIL_BYTE_14 = 14L;
    /** Bit position of the seventh byte of a little-endian word. */
    private static final int BYTE_6_SHIFT = 48;
    /** A tail of 14 bytes. */
    private static final int TAIL_14 = 14;
    /** Offset of byte 13 of the tail. */
    private static final long TAIL_BYTE_13 = 13L;
    /** Bit position of the sixth byte of a little-endian word. */
    private static final int BYTE_5_SHIFT = 40;
    /** A tail of 13 bytes. */
    private static final int TAIL_13 = 13;
    /** Offset of byte 12 of the tail. */
    private static final long TAIL_BYTE_12 = 12L;
    /** Bit position of the fifth byte of a little-endian word. */
    private static final int BYTE_4_SHIFT = 32;
    /** A tail of 12 bytes. */
    private static final int TAIL_12 = 12;
    /** Offset of byte 11 of the tail. */
    private static final long TAIL_BYTE_11 = 11L;
    /** Bit position of the fourth byte of a little-endian word. */
    private static final int BYTE_3_SHIFT = 24;
    /** A tail of 11 bytes. */
    private static final int TAIL_11 = 11;
    /** Offset of byte 10 of the tail. */
    private static final long TAIL_BYTE_10 = 10L;
    /** Bit position of the third byte of a little-endian word. */
    private static final int BYTE_2_SHIFT = 16;
    /** A tail of 10 bytes. */
    private static final int TAIL_10 = 10;
    /** Offset of byte 9 of the tail. */
    private static final long TAIL_BYTE_9 = 9L;
    /** Bit position of the second byte of a little-endian word. */
    private static final int BYTE_1_SHIFT = 8;
    /** A tail of 9 bytes. */
    private static final int TAIL_9 = 9;
    /** Offset of byte 8 of the tail. */
    private static final long TAIL_BYTE_8 = 8L;
    /** A tail of 8 bytes. */
    private static final int TAIL_8 = 8;
    /** A tail of 7 bytes. */
    private static final int TAIL_7 = 7;
    /** Offset of byte 6 of the tail. */
    private static final long TAIL_BYTE_6 = 6L;
    /** A tail of 6 bytes. */
    private static final int TAIL_6 = 6;
    /** Offset of byte 5 of the tail. */
    private static final long TAIL_BYTE_5 = 5L;
    /** A tail of 5 bytes. */
    private static final int TAIL_5 = 5;
    /** Offset of byte 4 of the tail. */
    private static final long TAIL_BYTE_4 = 4L;
    /** A tail of 4 bytes. */
    private static final int TAIL_4 = 4;
    /** A tail of 3 bytes. */
    private static final int TAIL_3 = 3;
    /** Offset of byte 2 of the tail. */
    private static final long TAIL_BYTE_2 = 2L;
    /** A tail of 2 bytes. */
    private static final int TAIL_2 = 2;
    /** Message: the tail switch covers every remainder. */
    private static final String UNREACHABLE = "Should never get here.";
    /** Shift of the fmix64 finalizer. */
    private static final int FMIX_SHIFT = 33;
    /** First multiplier of the fmix64 finalizer. */
    private static final long FMIX_MULTIPLIER_1 = 0xff51afd7ed558ccdL;
    /** Second multiplier of the fmix64 finalizer. */
    private static final long FMIX_MULTIPLIER_2 = 0xc4ceb9fe1a85ec53L;
    /** Rotation of k1. */
    private static final int K1_ROTATION = 31;
    /** Rotation of k2. */
    private static final int K2_ROTATION = 33;

    private static final long C1 = 0x87c37b91114253d5L;
    private static final long C2 = 0x4cf5ad432745937fL;

    @SuppressWarnings("fallthrough")
    private static <T> long hash(long seed, T input, M3Access<T> access, long offset, long length, long[] result) {
        long h1 = seed;
        long h2 = seed;
        long remaining = length;
        while (remaining >= BLOCK_BYTES) {
            long k1 = access.i64(input, offset);
            long k2 = access.i64(input, offset + HALF_BLOCK_OFFSET);
            offset += BLOCK_BYTES;
            remaining -= BLOCK_BYTES;
            h1 ^= mixK1(k1);

            h1 = Long.rotateLeft(h1, H1_ROTATION);
            h1 += h2;
            h1 = h1 * MIX_MULTIPLIER + H1_ADDEND;

            h2 ^= mixK2(k2);

            h2 = Long.rotateLeft(h2, H2_ROTATION);
            h2 += h1;
            h2 = h2 * MIX_MULTIPLIER + H2_ADDEND;
        }

        if (remaining > 0L) {
            long k1 = 0L;
            long k2 = 0L;
            switch ((int) remaining) {
                case TAIL_15:
                    k2 ^= ((long) access.u8(input, offset + TAIL_BYTE_14)) << BYTE_6_SHIFT;// fall through
                case TAIL_14:
                    k2 ^= ((long) access.u8(input, offset + TAIL_BYTE_13)) << BYTE_5_SHIFT;// fall through
                case TAIL_13:
                    k2 ^= ((long) access.u8(input, offset + TAIL_BYTE_12)) << BYTE_4_SHIFT;// fall through
                case TAIL_12:
                    k2 ^= ((long) access.u8(input, offset + TAIL_BYTE_11)) << BYTE_3_SHIFT;// fall through
                case TAIL_11:
                    k2 ^= ((long) access.u8(input, offset + TAIL_BYTE_10)) << BYTE_2_SHIFT;// fall through
                case TAIL_10:
                    k2 ^= ((long) access.u8(input, offset + TAIL_BYTE_9)) << BYTE_1_SHIFT; // fall through
                case TAIL_9:
                    k2 ^= ((long) access.u8(input, offset + TAIL_BYTE_8)); // fall through
                case TAIL_8:
                    k1 ^= access.i64(input, offset);
                    break;
                case TAIL_7:
                    k1 ^= ((long) access.u8(input, offset + TAIL_BYTE_6)) << BYTE_6_SHIFT; // fall through
                case TAIL_6:
                    k1 ^= ((long) access.u8(input, offset + TAIL_BYTE_5)) << BYTE_5_SHIFT; // fall through
                case TAIL_5:
                    k1 ^= ((long) access.u8(input, offset + TAIL_BYTE_4)) << BYTE_4_SHIFT; // fall through
                case TAIL_4:
                    k1 ^= access.u32(input, offset);
                    break;
                case TAIL_3:
                    k1 ^= ((long) access.u8(input, offset + TAIL_BYTE_2)) << BYTE_2_SHIFT; // fall through
                case TAIL_2:
                    k1 ^= ((long) access.u8(input, offset + 1L)) << BYTE_1_SHIFT; // fall through
                case 1:
                    k1 ^= ((long) access.u8(input, offset));
                case 0:
                    break;
                default:
                    throw new AssertionError(UNREACHABLE);
            }
            h1 ^= mixK1(k1);
            h2 ^= mixK2(k2);
        }

        // This version appears to be working slower

//        if (remaining > 0L) {
//            long k1 = 0L;
//            long k2 = 0L;
//            megaSwitch:
//            {
//                fetch0_7:
//                {
//                    fetch8_11:
//                    {
//                        fetch0_3:
//                        {
//                            switch ((int) remaining) {
//                                case 15:
//                                    k2 ^= ((long) access.u8(input, offset + 14L)) << 48;
//                                case 14:
//                                    k2 ^= ((long) Primitives.nativeToLittleEndian(
//                                            access.u16(input, offset + 12L))) << 32;
//                                    break fetch8_11;
//                                case 13:
//                                    k2 ^= ((long) access.u8(input, offset + 12L)) << 32;
//                                case 12:
//                                    break fetch8_11;
//                                case 11:
//                                    k2 ^= ((long) access.u8(input, offset + 10L)) << 16;
//                                case 10:
//                                    k2 ^= (long) Primitives.nativeToLittleEndian(
//                                            access.u16(input, offset + 8L));
//                                    break fetch0_7;
//                                case 9:
//                                    k2 ^= ((long) access.u8(input, offset + 8L));
//                                case 8:
//                                    break fetch0_7;
//                                case 7:
//                                    k1 ^= ((long) access.u8(input, offset + 6L)) << 48;
//                                case 6:
//                                    k1 ^= ((long) Primitives.nativeToLittleEndian(
//                                            access.u16(input, offset + 4L))) << 32;
//                                    break fetch0_3;
//                                case 5:
//                                    k1 ^= ((long) access.u8(input, offset + 4L)) << 32;
//                                case 4:
//                                    break fetch0_3;
//                                case 3:
//                                    k1 ^= ((long) access.u8(input, offset + 2L)) << 16;
//                                case 2:
//                                    k1 ^= (long) Primitives.nativeToLittleEndian(
//                                            access.u16(input, offset));
//                                    break megaSwitch;
//                                case 1:
//                                    k1 ^= ((long) access.u8(input, offset));
//                                    break megaSwitch;
//                                default:
//                                    throw new AssertionError();
//                            }
//                        } // fetch0_3
//                        k1 ^= access.u32(input, offset);
//                        break megaSwitch;
//                    } // fetch8_11
//                    k2 ^= access.u32(input, offset + 8L);
//                } // fetch0_7
//                k1 ^= access.i64(input, offset);
//            } // megaSwitch
//
//            h1 ^= mixK1(k1);
//            h2 ^= mixK2(k2);
//        }

        return finalize(length, h1, h2, result);
    }

    private static long finalize(long length, long h1, long h2, long[] result) {
        h1 ^= length;
        h2 ^= length;

        h1 += h2;
        h2 += h1;

        h1 = fmix64(h1);
        h2 = fmix64(h2);


        if (null != result) {
            h1 += h2;
            result[0] = h1;
            result[1] = h1 + h2;
            return h1;
        } else {
            return h1 + h2;
        }
    }

    private static long fmix64(long k) {
        k ^= k >>> FMIX_SHIFT;
        k *= FMIX_MULTIPLIER_1;
        k ^= k >>> FMIX_SHIFT;
        k *= FMIX_MULTIPLIER_2;
        k ^= k >>> FMIX_SHIFT;
        return k;
    }

    private static long mixK1(long k1) {
        k1 *= C1;
        k1 = Long.rotateLeft(k1, K1_ROTATION);
        k1 *= C2;
        return k1;
    }

    private static long mixK2(long k2) {
        k2 *= C2;
        k2 = Long.rotateLeft(k2, K2_ROTATION);
        k2 *= C1;
        return k2;
    }

    private static class AsLongTupleHashFunction extends M3DualHashFunction {
        //FLLDCIM: M3 - LITERAL_CONSTANTS v1.1: this class's magic literals, named by role (user rule: no magic numbers, no floating strings)
        /** Bits of the 128-bit result. */
        private static final int RESULT_BITS = 128;
        /** Longs of the 128-bit result. */
        private static final int RESULT_LONGS = 2;
        /** Byte length of a hashed long. */
        private static final long LONG_LENGTH = 8L;
        /** Byte length of a hashed int. */
        private static final long INT_LENGTH = 4L;
        /** Byte length of a hashed short or char. */
        private static final long SHORT_LENGTH = 2L;

        private static final long serialVersionUID = 0L;
        private static final AsLongTupleHashFunction SEEDLESS_INSTANCE = new AsLongTupleHashFunction();
        private static final M3LongHashFunction SEEDLESS_INSTANCE_LONG = SEEDLESS_INSTANCE.asLongHashFunction();

        private Object readResolve() {
            return SEEDLESS_INSTANCE;
        }

        @Override
        public int bitsLength() {
            return RESULT_BITS;
        }
        @Override
        public long[] newResultArray() {
            return new long[RESULT_LONGS]; // override for a little performance
        }

        long seed() {
            return 0L;
        }

        protected long hashNativeLong(long nativeLong, long len, long[] result) {
            long h1 = mixK1(nativeLong);
            long h2 = 0L;
            return M3MurmurHash_3.finalize(len, h1, h2, result);
        }

        @Override
        public long dualHashLong(long input, long[] result) {
            return hashNativeLong(M3Primitives.nativeToLittleEndian(input), LONG_LENGTH, result);
        }

        @Override
        public long dualHashInt(int input, long[] result) {
            return hashNativeLong(unsignedInt(M3Primitives.nativeToLittleEndian(input)), INT_LENGTH, result);
        }

        @Override
        public long dualHashShort(short input, long[] result) {
            return hashNativeLong(unsignedShort(M3Primitives.nativeToLittleEndian(input)), SHORT_LENGTH, result);
        }

        @Override
        public long dualHashChar(char input, long[] result) {
            return hashNativeLong(unsignedShort(M3Primitives.nativeToLittleEndian(input)), SHORT_LENGTH, result);
        }

        @Override
        public long dualHashByte(byte input, long[] result) {
            return hashNativeLong(M3Primitives.unsignedByte((int) input), 1L, result);
        }

        @Override
        public long dualHashVoid(long[] result) {
            if (null != result) {
                result[0] = 0;
                result[1] = 0;
            }
            return 0;
        }

        @Override
        public <T> long dualHash(T input, M3Access<T> access, long off, long len, long[] result) {
            long seed = seed();
            return M3MurmurHash_3.hash(seed, input, access.byteOrder(input, LITTLE_ENDIAN), off, len, result);
        }
    }

    static M3LongTupleHashFunction asLongTupleHashFunctionWithoutSeed() {
        return AsLongTupleHashFunction.SEEDLESS_INSTANCE;
    }
    static M3LongHashFunction asLongHashFunctionWithoutSeed() {
        return AsLongTupleHashFunction.SEEDLESS_INSTANCE_LONG;
    }

    private static class AsLongTupleHashFunctionSeeded extends AsLongTupleHashFunction {
        private static final long serialVersionUID = 0L;

        private final long seed;
        private final transient long[] voidHash = newResultArray();

        private AsLongTupleHashFunctionSeeded(long seed) {
            this.seed = seed;
            M3MurmurHash_3.finalize(0L, seed, seed, voidHash);
        }

        @Override
        long seed() {
            return seed;
        }

        @Override
        protected long hashNativeLong(long nativeLong, long len, long[] result) {
            long seed = this.seed;
            long h1 = seed ^ mixK1(nativeLong);
            long h2 = seed;
            return M3MurmurHash_3.finalize(len, h1, h2, result);
        }

        @Override
        public long dualHashVoid(long[] result) {
            if (null != result) {
                result[0] = voidHash[0];
                result[1] = voidHash[1];
            }
            return voidHash[0];
        }
    }

    static M3LongTupleHashFunction asLongTupleHashFunctionWithSeed(long seed) {
        return new AsLongTupleHashFunctionSeeded(seed);
    }
    static M3LongHashFunction asLongHashFunctionWithSeed(long seed) {
        return new AsLongTupleHashFunctionSeeded(seed).asLongHashFunction();
    }
}
