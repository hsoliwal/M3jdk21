/*
 * Changed by Synexia (2026-10-31): zero-allocation-hashing 0.16 (Apache License 2.0) absorbed into java.base
 * as jdk.internal.m3.hash; the changes are listed in src/java.base/share/legal/zero-allocation-hashing.md.
 */
package jdk.internal.m3.hash;

import static java.nio.ByteOrder.LITTLE_ENDIAN;

/**
 * Adapted version of WyHash implementation from https://github.com/wangyi-fudan/wyhash
 * Original @author <a href="mailto:godspeed_china@yeah.net">Wang Yi</a>
 * Adapted @author <a href="mailto:firestrand@gmail.com">Travis Silvers</a>
 * This implementation provides endian-independant hash values, but it's slower on big-endian
 * platforms.
 * The original C based version is also much faster.
 */
class M3WyHash {
    //FLLDCIM: M3 - LITERAL_CONSTANTS v1.1: this class's magic literals, named by role (user rule: no magic numbers, no floating strings)
    /** Bit position of the first byte in the 3-byte read. */
    private static final int BYTE_2_SHIFT = 16;
    /** Bit position of the middle byte in the 3-byte read. */
    private static final int BYTE_1_SHIFT = 8;
    /** Distance between the two 32-bit halves of a long. */
    private static final int HALF_SHIFT = 32;
    /** Bytes of an int: offset of the second half. */
    private static final int INT_BYTES = 4;
    /** Bytes of a 64-bit word. */
    private static final int WORD_BYTES = 8;
    /** Bytes of 2 64-bit words. */
    private static final int WORDS_2_BYTES = 16;
    /** Bytes of 3 64-bit words. */
    private static final int WORDS_3_BYTES = 24;
    /** Bytes of 4 64-bit words. */
    private static final int WORDS_4_BYTES = 32;
    /** Bytes of one round of the long loop: 32 64-bit words. */
    private static final int BLOCK_BYTES = 256;
    /** Bytes of 5 64-bit words. */
    private static final int WORDS_5_BYTES = 40;
    /** Bytes of 6 64-bit words. */
    private static final int WORDS_6_BYTES = 48;
    /** Bytes of 7 64-bit words. */
    private static final int WORDS_7_BYTES = 56;
    /** Bytes of 8 64-bit words. */
    private static final int WORDS_8_BYTES = 64;
    /** Bytes of 9 64-bit words. */
    private static final int WORDS_9_BYTES = 72;
    /** Bytes of 10 64-bit words. */
    private static final int WORDS_10_BYTES = 80;
    /** Bytes of 11 64-bit words. */
    private static final int WORDS_11_BYTES = 88;
    /** Bytes of 12 64-bit words. */
    private static final int WORDS_12_BYTES = 96;
    /** Bytes of 13 64-bit words. */
    private static final int WORDS_13_BYTES = 104;
    /** Bytes of 14 64-bit words. */
    private static final int WORDS_14_BYTES = 112;
    /** Bytes of 15 64-bit words. */
    private static final int WORDS_15_BYTES = 120;
    /** Bytes of 16 64-bit words. */
    private static final int WORDS_16_BYTES = 128;
    /** Bytes of 17 64-bit words. */
    private static final int WORDS_17_BYTES = 136;
    /** Bytes of 18 64-bit words. */
    private static final int WORDS_18_BYTES = 144;
    /** Bytes of 19 64-bit words. */
    private static final int WORDS_19_BYTES = 152;
    /** Bytes of 20 64-bit words. */
    private static final int WORDS_20_BYTES = 160;
    /** Bytes of 21 64-bit words. */
    private static final int WORDS_21_BYTES = 168;
    /** Bytes of 22 64-bit words. */
    private static final int WORDS_22_BYTES = 176;
    /** Bytes of 23 64-bit words. */
    private static final int WORDS_23_BYTES = 184;
    /** Bytes of 24 64-bit words. */
    private static final int WORDS_24_BYTES = 192;
    /** Bytes of 25 64-bit words. */
    private static final int WORDS_25_BYTES = 200;
    /** Bytes of 26 64-bit words. */
    private static final int WORDS_26_BYTES = 208;
    /** Bytes of 27 64-bit words. */
    private static final int WORDS_27_BYTES = 216;
    /** Bytes of 28 64-bit words. */
    private static final int WORDS_28_BYTES = 224;
    /** Bytes of 29 64-bit words. */
    private static final int WORDS_29_BYTES = 232;
    /** Bytes of 30 64-bit words. */
    private static final int WORDS_30_BYTES = 240;
    /** Bytes of 31 64-bit words. */
    private static final int WORDS_31_BYTES = 248;

    // Primes
    public static final long _wyp0 = 0xa0761d6478bd642fL;
    public static final long _wyp1 = 0xe7037ed1a0b428dbL;
    public static final long _wyp2 = 0x8ebc6af09c88c6e3L;
    public static final long _wyp3 = 0x589965cc75374cc3L;
    public static final long _wyp4 = 0x1d8e4e27c47d124fL;

    private static long _wymum(final long lhs, final long rhs) {
        return M3Maths.unsignedLongMulXorFold(lhs, rhs);
    }

    private static <T> long _wyr3(final M3Access<T> access, T in, final long index, long k) {
        return ((long) access.u8(in, index) << BYTE_2_SHIFT) |
               ((long) access.u8(in, index + (k >>> 1)) << BYTE_1_SHIFT) |
               ((long) access.u8(in, index + k - 1));
    }

    private static <T> long u64Rorate32(final M3Access<T> access, T in, final long index) {
        return (access.u32(in, index) << HALF_SHIFT) | access.u32(in, index + INT_BYTES);
    }

    /**
     *
     * @param seed seed for the hash
     * @param input the type wrapped by the Access, ex. byte[], ByteBuffer, etc.
     * @param access class wrapping optimized access pattern to the input
     * @param off offset to the input
     * @param length length to read from input
     * @param <T> byte[], ByteBuffer, etc.
     * @return hash result
     */
    static <T> long wyHash64(long seed, T input, M3Access<T> access, long off, long length) {
        if(length <= 0)
            return 0;
        else if(length<INT_BYTES)
            return _wymum(_wymum(_wyr3(access, input,off,length)^seed^_wyp0,
                                 seed^_wyp1)^seed,length^_wyp4);
        else if(length<=WORD_BYTES)
            return _wymum(_wymum(access.u32(input, off) ^ seed ^ _wyp0,
                                 access.u32(input, off + length - INT_BYTES) ^ seed ^ _wyp1)
                          ^ seed, length ^ _wyp4);
        else if(length<=WORDS_2_BYTES)
            return _wymum(_wymum(u64Rorate32(access, input,off)^seed^_wyp0,
                                 u64Rorate32(access, input,off+length-WORD_BYTES)^seed^_wyp1)
                          ^seed,length^_wyp4);
        else if(length<=WORDS_3_BYTES)
            return _wymum(_wymum(u64Rorate32(access, input,off)^seed^_wyp0,
                                 u64Rorate32(access, input,off+WORD_BYTES)^seed^_wyp1)^
                          _wymum(u64Rorate32(access, input,off+length-WORD_BYTES)
                                 ^seed^_wyp2,seed^_wyp3),length^_wyp4);
        else if(length<=WORDS_4_BYTES)
            return _wymum(_wymum(u64Rorate32(access, input,off)^seed^_wyp0,
                                 u64Rorate32(access, input,off+WORD_BYTES)^seed^_wyp1)
                          ^_wymum(u64Rorate32(access, input,off+WORDS_2_BYTES)^seed^_wyp2,
                                  u64Rorate32(access, input,off+length-WORD_BYTES)^seed^_wyp3),length^_wyp4);
        long see1=seed; long i=length, p=off;
        for(;i>BLOCK_BYTES;i-=BLOCK_BYTES,p+=BLOCK_BYTES){
            seed = _wymum(access.i64(input, p) ^ seed ^ _wyp0,
                          access.i64(input, p + WORD_BYTES) ^ seed ^ _wyp1) ^
                   _wymum(access.i64(input, p + WORDS_2_BYTES) ^ seed ^ _wyp2,
                          access.i64(input, p + WORDS_3_BYTES) ^ seed ^ _wyp3);
            see1 = _wymum(access.i64(input, p + WORDS_4_BYTES) ^ see1 ^ _wyp1,
                          access.i64(input, p + WORDS_5_BYTES) ^ see1 ^ _wyp2) ^
                   _wymum(access.i64(input, p + WORDS_6_BYTES) ^ see1 ^ _wyp3,
                          access.i64(input, p + WORDS_7_BYTES) ^ see1 ^ _wyp0);
            seed = _wymum(access.i64(input, p + WORDS_8_BYTES) ^ seed ^ _wyp0,
                          access.i64(input, p + WORDS_9_BYTES) ^ seed ^ _wyp1) ^
                   _wymum(access.i64(input, p + WORDS_10_BYTES) ^ seed ^ _wyp2,
                          access.i64(input, p + WORDS_11_BYTES) ^ seed ^ _wyp3);
            see1 = _wymum(access.i64(input, p + WORDS_12_BYTES) ^ see1 ^ _wyp1,
                          access.i64(input, p + WORDS_13_BYTES) ^ see1 ^ _wyp2) ^
                   _wymum(access.i64(input, p + WORDS_14_BYTES) ^ see1 ^ _wyp3,
                          access.i64(input, p + WORDS_15_BYTES) ^ see1 ^ _wyp0);
            seed = _wymum(access.i64(input, p + WORDS_16_BYTES) ^ seed ^ _wyp0,
                          access.i64(input, p + WORDS_17_BYTES) ^ seed ^ _wyp1) ^
                   _wymum(access.i64(input, p + WORDS_18_BYTES) ^ seed ^ _wyp2,
                          access.i64(input, p + WORDS_19_BYTES) ^ seed ^ _wyp3);
            see1 = _wymum(access.i64(input, p + WORDS_20_BYTES) ^ see1 ^ _wyp1,
                          access.i64(input, p + WORDS_21_BYTES) ^ see1 ^ _wyp2) ^
                   _wymum(access.i64(input, p + WORDS_22_BYTES) ^ see1 ^ _wyp3,
                          access.i64(input, p + WORDS_23_BYTES) ^ see1 ^ _wyp0);
            seed = _wymum(access.i64(input, p + WORDS_24_BYTES) ^ seed ^ _wyp0,
                          access.i64(input, p + WORDS_25_BYTES) ^ seed ^ _wyp1) ^
                   _wymum(access.i64(input, p + WORDS_26_BYTES) ^ seed ^ _wyp2,
                          access.i64(input, p + WORDS_27_BYTES) ^ seed ^ _wyp3);
            see1 = _wymum(access.i64(input, p + WORDS_28_BYTES) ^ see1 ^ _wyp1,
                          access.i64(input, p + WORDS_29_BYTES) ^ see1 ^ _wyp2) ^
                   _wymum(access.i64(input, p + WORDS_30_BYTES) ^ see1 ^ _wyp3,
                          access.i64(input, p + WORDS_31_BYTES) ^ see1 ^ _wyp0);
        }
        for (; i > WORDS_4_BYTES; i -= WORDS_4_BYTES, p += WORDS_4_BYTES) {
            seed = _wymum(access.i64(input, p) ^ seed ^ _wyp0,
                          access.i64(input, p + WORD_BYTES) ^ seed ^ _wyp1);
            see1 = _wymum(access.i64(input, p + WORDS_2_BYTES) ^ see1 ^ _wyp2,
                          access.i64(input, p + WORDS_3_BYTES) ^ see1 ^ _wyp3);
        }
        if (i < INT_BYTES) {
            seed = _wymum(_wyr3(access, input, p, i) ^ seed ^ _wyp0, seed ^ _wyp1);
        } else if (i <= WORD_BYTES) {
            seed = _wymum(access.u32(input, p) ^ seed ^ _wyp0,
                          access.u32(input, p + i - INT_BYTES) ^ seed ^ _wyp1);
        } else if (i <= WORDS_2_BYTES) {
            seed = _wymum(u64Rorate32(access, input, p) ^ seed ^ _wyp0,
                          u64Rorate32(access, input, p + i - WORD_BYTES) ^ seed ^ _wyp1);
        } else if (i <= WORDS_3_BYTES) {
            seed = _wymum(u64Rorate32(access, input, p) ^ seed ^ _wyp0,
                          u64Rorate32(access, input, p + WORD_BYTES) ^ seed ^ _wyp1);
            see1 = _wymum(u64Rorate32(access, input, p + i - WORD_BYTES) ^ see1 ^ _wyp2, see1 ^ _wyp3);
        } else {
            seed = _wymum(u64Rorate32(access, input, p) ^ seed ^ _wyp0,
                          u64Rorate32(access, input, p + WORD_BYTES) ^ seed ^ _wyp1);
            see1 = _wymum(u64Rorate32(access, input, p + WORDS_2_BYTES) ^ see1 ^ _wyp2,
                          u64Rorate32(access, input, p + i - WORD_BYTES) ^ see1 ^ _wyp3);
        }
        return _wymum(seed ^ see1, length ^ _wyp4);
    }

    static M3LongHashFunction asLongHashFunctionWithoutSeed() {
        return AsLongHashFunction.SEEDLESS_INSTANCE;
    }

    private static class AsLongHashFunction extends M3LongHashFunction {
        //FLLDCIM: M3 - LITERAL_CONSTANTS v1.1: this class's magic literals, named by role (user rule: no magic numbers, no floating strings)
        /** The low 32-bit half of a long. */
        private static final long LOW_32_MASK = 0xFFFFFFFFL;
        /** Distance between the low and the high 32-bit half of a long. */
        private static final int HALF_SHIFT = 32;
        /** Byte length of a hashed long. */
        private static final int LONG_LENGTH = 8;
        /** Byte length of a hashed int. */
        private static final int INT_LENGTH = 4;
        /** Bit position of the second byte. */
        private static final int BYTE_1_SHIFT = 8;
        /** The low byte. */
        private static final long BYTE_MASK = 0xFFL;
        /** Bit position of the third byte. */
        private static final int BYTE_2_SHIFT = 16;
        /** Byte length of a hashed short or char. */
        private static final int SHORT_LENGTH = 2;

        private static final long serialVersionUID = 0L;
        static final AsLongHashFunction SEEDLESS_INSTANCE = new AsLongHashFunction();

        private Object readResolve() {
            return SEEDLESS_INSTANCE;
        }

        public long seed() {
            return 0L;
        }

        @Override
        public long hashLong(long input) {
            input = M3Primitives.nativeToLittleEndian(input);
            long hi = input & LOW_32_MASK;
            long lo = (input >>> HALF_SHIFT) & LOW_32_MASK;
            return _wymum(_wymum(hi ^ seed() ^ _wyp0,
                          lo ^ seed() ^ _wyp1)
                   ^ seed(), LONG_LENGTH ^ _wyp4);
        }

        @Override
        public long hashInt(int input) {
            input = M3Primitives.nativeToLittleEndian(input);
            long longInput = (input & LOW_32_MASK);
            return _wymum(_wymum(longInput ^ seed() ^ _wyp0,
                                 longInput ^ seed() ^ _wyp1)
                          ^ seed(), INT_LENGTH ^ _wyp4);
        }

        @Override
        public long hashShort(short input) {
            input = M3Primitives.nativeToLittleEndian(input);
            long hi = (input >>> BYTE_1_SHIFT) & BYTE_MASK;
            long wyr3 = hi | hi << BYTE_1_SHIFT | (input & BYTE_MASK) << BYTE_2_SHIFT;
            return _wymum(_wymum(wyr3 ^ seed() ^ _wyp0,
                                 seed() ^ _wyp1) ^ seed(), SHORT_LENGTH ^ _wyp4);
        }

        @Override
        public long hashChar(final char input) {
            return hashShort((short)input);
        }

        @Override
        public long hashByte(final byte input) {
            long hi = input & BYTE_MASK;
            long wyr3 = hi | hi << BYTE_1_SHIFT | hi << BYTE_2_SHIFT;
            return _wymum(_wymum(wyr3 ^ seed() ^ _wyp0,
                                 seed() ^ _wyp1) ^ seed(), 1 ^ _wyp4);
        }

        @Override
        public long hashVoid() {
            return 0;
        }

        @Override
        public <T> long hash(final T input, final M3Access<T> access,
                             final long off, final long len) {
            long seed = seed();
            return M3WyHash.wyHash64(seed, input, access.byteOrder(input, LITTLE_ENDIAN), off, len);
        }
    }

    static M3LongHashFunction asLongHashFunctionWithSeed(long seed) {
        return new AsLongHashFunctionSeeded(seed);
    }

    private static class AsLongHashFunctionSeeded extends AsLongHashFunction {
        private static final long serialVersionUID = 0L;

        private final long seed;

        private AsLongHashFunctionSeeded(long seed) {
            this.seed = seed;
        }

        @Override
        public long seed() {
            return seed;
        }
    }
}
