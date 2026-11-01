/*
 * Changed by Synexia (2026-10-31): zero-allocation-hashing 0.16 (Apache License 2.0) absorbed into java.base
 * as jdk.internal.m3.hash; the changes are listed in src/java.base/share/legal/zero-allocation-hashing.md.
 */
package jdk.internal.m3.hash;

import static java.nio.ByteOrder.LITTLE_ENDIAN;

class M3MetroHash {
    //FLLDCIM: M3 - LITERAL_CONSTANTS v1.1: this class's magic literals, named by role (user rule: no magic numbers, no floating strings)
    /** Bytes of one stripe: four 64-bit lanes. */
    private static final int STRIPE_BYTES = 32;
    /** Rotation of each lane of the 32-byte stripe. */
    private static final int LANE_ROTATION = 29;
    /** Offset of the second 64-bit word of a stripe or block. */
    private static final int LANE_2_OFFSET = 8;
    /** Offset of the third 64-bit word of a stripe. */
    private static final int LANE_3_OFFSET = 16;
    /** Offset of the fourth 64-bit word of a stripe. */
    private static final int LANE_4_OFFSET = 24;
    /** Rotation when the four lanes merge. */
    private static final int MERGE_ROTATION = 37;
    /** Bytes of the 16-byte block: two 64-bit words. */
    private static final int PAIR_BYTES = 16;
    /** Rotation of each lane of the 16-byte block. */
    private static final int PAIR_ROTATION = 29;
    /** Rotation of the cross mix of the 16-byte block. */
    private static final int PAIR_MIX_ROTATION = 21;
    /** Bytes of a 64-bit word: the step of the 8-byte tail. */
    private static final int WORD_BYTES = 8;
    /** Rotation after a 64-bit tail word. */
    private static final int WORD_ROTATION = 55;
    /** Bytes of an int: the step of the 4-byte tail. */
    private static final int INT_BYTES = 4;
    /** Rotation after a 32-bit tail int. */
    private static final int INT_ROTATION = 26;
    /** Bytes of a short: the step of the 2-byte tail. */
    private static final int SHORT_BYTES = 2;
    /** Rotation after a 16-bit tail short. */
    private static final int SHORT_ROTATION = 48;
    /** Rotation after a tail byte. */
    private static final int BYTE_ROTATION = 37;
    /** First rotation of the finalizer. */
    private static final int FINAL_ROTATION_1 = 28;
    /** Second rotation of the finalizer. */
    private static final int FINAL_ROTATION_2 = 29;

    //primes
    private static final long k0 = 0xD6D018F5L;
    private static final long k1 = 0xA2AA033BL;
    private static final long k2 = 0x62992FC1L;
    private static final long k3 = 0x30BC5B29L;

    static <T> long metroHash64(long seed, T input, M3Access<T> access, long off, long length) {
        long remaining = length;

        long h = (seed + k2) * k0;

        if (length >= STRIPE_BYTES) {
            long v0 = h;
            long v1 = h;
            long v2 = h;
            long v3 = h;

            do {
                v0 += access.i64(input, off) * k0;
                v0 = Long.rotateRight(v0, LANE_ROTATION) + v2;
                v1 += access.i64(input, off + LANE_2_OFFSET) * k1;
                v1 = Long.rotateRight(v1, LANE_ROTATION) + v3;
                v2 += access.i64(input, off + LANE_3_OFFSET) * k2;
                v2 = Long.rotateRight(v2, LANE_ROTATION) + v0;
                v3 += access.i64(input, off + LANE_4_OFFSET) * k3;
                v3 = Long.rotateRight(v3, LANE_ROTATION) + v1;

                off += STRIPE_BYTES;
                remaining -= STRIPE_BYTES;
            } while (remaining >= STRIPE_BYTES);

            v2 ^= Long.rotateRight(((v0 + v3) * k0) + v1, MERGE_ROTATION) * k1;
            v3 ^= Long.rotateRight(((v1 + v2) * k1) + v0, MERGE_ROTATION) * k0;
            v0 ^= Long.rotateRight(((v0 + v2) * k0) + v3, MERGE_ROTATION) * k1;
            v1 ^= Long.rotateRight(((v1 + v3) * k1) + v2, MERGE_ROTATION) * k0;

            h += v0 ^ v1;
        }

        if (remaining >= PAIR_BYTES) {
            long v0 = h + (access.i64(input, off) * k2);
            v0 = Long.rotateRight(v0, PAIR_ROTATION) * k3;
            long v1 = h + (access.i64(input, off + LANE_2_OFFSET) * k2);
            v1 = Long.rotateRight(v1, PAIR_ROTATION) * k3;
            v0 ^= Long.rotateRight(v0 * k0, PAIR_MIX_ROTATION) + v1;
            v1 ^= Long.rotateRight(v1 * k3, PAIR_MIX_ROTATION) + v0;
            h += v1;

            off += PAIR_BYTES;
            remaining -= PAIR_BYTES;
        }

        if (remaining >= WORD_BYTES) {
            h += access.i64(input, off) * k3;
            h ^= Long.rotateRight(h, WORD_ROTATION) * k1;

            off += WORD_BYTES;
            remaining -= WORD_BYTES;
        }

        if (remaining >= INT_BYTES) {
            h += access.u32(input, off) * k3;
            h ^= Long.rotateRight(h, INT_ROTATION) * k1;

            off += INT_BYTES;
            remaining -= INT_BYTES;
        }

        if (remaining >= SHORT_BYTES) {
            h += access.u16(input, off) * k3;
            h ^= Long.rotateRight(h, SHORT_ROTATION) * k1;

            off += SHORT_BYTES;
            remaining -= SHORT_BYTES;
        }

        if (remaining >= 1) {
            h += access.u8(input, off) * k3;
            h ^= Long.rotateRight(h, BYTE_ROTATION) * k1;
        }

        return finalize(h);
    }

    private static long finalize(long h) {
        h ^= Long.rotateRight(h, FINAL_ROTATION_1);
        h *= k0;
        h ^= Long.rotateRight(h, FINAL_ROTATION_2);
        return h;
    }

    private static class AsLongHashFunction extends M3LongHashFunction {
        //FLLDCIM: M3 - LITERAL_CONSTANTS v1.1: this class's magic literals, named by role (user rule: no magic numbers, no floating strings)
        /** Rotation after a 64-bit tail word. */
        private static final int WORD_ROTATION = 55;
        /** Rotation after a 32-bit tail int. */
        private static final int INT_ROTATION = 26;
        /** Rotation after a 16-bit tail short. */
        private static final int SHORT_ROTATION = 48;
        /** Rotation after a tail byte. */
        private static final int BYTE_ROTATION = 37;

        private static final long serialVersionUID = 0L;
        private static final AsLongHashFunction SEEDLESS_INSTANCE = new AsLongHashFunction();
        private static final long VOID_HASH = M3MetroHash.finalize(k2 * k0);

        private Object readResolve() {
            return SEEDLESS_INSTANCE;
        }

        protected long seed() {
            return 0L;
        }

        @Override
        public long hashLong(long input) {
            input = M3Primitives.nativeToLittleEndian(input);
            long h = (seed() + k2) * k0;
            h += input * k3;
            h ^= Long.rotateRight(h, WORD_ROTATION) * k1;

            return M3MetroHash.finalize(h);
        }

        @Override
        public long hashInt(int input) {
            input = M3Primitives.nativeToLittleEndian(input);
            long h = (seed() + k2) * k0;
            h += M3Primitives.unsignedInt(input) * k3;
            h ^= Long.rotateRight(h, INT_ROTATION) * k1;

            return M3MetroHash.finalize(h);
        }

        @Override
        public long hashShort(short input) {
            input = M3Primitives.nativeToLittleEndian(input);
            long h = (seed() + k2) * k0;
            h += M3Primitives.unsignedShort(input) * k3;
            h ^= Long.rotateRight(h, SHORT_ROTATION) * k1;

            return M3MetroHash.finalize(h);
        }

        @Override
        public long hashChar(char input) {
            return hashShort((short) input);
        }

        @Override
        public long hashByte(byte input) {
            long h = (seed() + k2) * k0;
            h += M3Primitives.unsignedByte(input) * k3;
            h ^= Long.rotateRight(h, BYTE_ROTATION) * k1;

            return M3MetroHash.finalize(h);
        }

        @Override
        public long hashVoid() {
            return VOID_HASH;
        }

        @Override
        public <T> long hash(T input, M3Access<T> access, long off, long len) {
            long seed = seed();
            return M3MetroHash.metroHash64(seed, input, access.byteOrder(input, LITTLE_ENDIAN), off, len);
        }
    }

    static M3LongHashFunction asLongHashFunctionWithoutSeed() {
        return AsLongHashFunction.SEEDLESS_INSTANCE;
    }

    static M3LongHashFunction asLongHashFunctionWithSeed(long seed) {
        return new AsLongHashFunctionSeeded(seed);
    }

    private static class AsLongHashFunctionSeeded extends AsLongHashFunction {
        private static final long serialVersionUID = 0L;

        private final long seed;
        private final transient long voidHash;

        AsLongHashFunctionSeeded(long seed) {
            this.seed = seed;
            voidHash = M3MetroHash.finalize((seed + k2) * k0);
        }

        @Override
        public long hashVoid() {
            return voidHash;
        }

        @Override
        protected long seed() {
            return seed;
        }
    }
}
