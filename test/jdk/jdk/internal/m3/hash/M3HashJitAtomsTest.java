/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
 */

/*
 * @test
 * @summary One JUnit test per atom of the JIT leaf of the zero-allocation-hashing absorption. XXH3_SPLIT v1
 *          (jdk.internal.m3.hash.M3XXH3): the twelve input-length class atoms (XXH3_len_0to16, 9to16, 4to8, 1to3,
 *          17to128, 129to240, 64 and 128 bit), the shared XXH3_hashLong, XXH3_mergeAccs and the two length-class
 *          dispatchers. UNSAFE_TO_BYTE_ARRAY v2: the unchecked read atom (M3ByteArrayAccess i64, u32, i32, u16, i16)
 *          against the bounds-checked getters and a little-endian ByteBuffer. FACADE_ENTRY_CHECK v1: both private
 *          byte[] entries throw IndexOutOfBoundsException for every range outside the array and equal the public
 *          methods inside it. The bounds proof: every algorithm kernel, through a byte[] access whose every read is a
 *          bounds-checked array access, over exact-length arrays: no exception and the facade's result for every
 *          length, one byte less throws. Each private static XXH3 atom is called directly and
 *          compared with the vendored library compiled unchanged (M3HashOracle): every length of its class (hashLong:
 *          241..1100 and strided to 4224, every block edge), fixed and random seeds (the seeded hashLong with the
 *          seed's custom secret), slices at non-zero offsets, through the byte[], ByteBuffer (both orders) and
 *          CharSequence accesses; the 128-bit atoms with and without a result array. XXH3_hashLong is one owner for
 *          two donor computations, so its test proves that the vendored XXH3_hashLong_64b equals the low half of the
 *          vendored XXH3_hashLong_128b for every tested input. XXH3_mergeAccs is compared with a fold written over
 *          Math.unsignedMultiplyHigh.
 * @modules java.base/jdk.internal.m3.hash:open
 *          java.base/sun.nio.ch
 *          jdk.compiler
 *          jdk.unsupported
 * @run junit M3HashJitAtomsTest
 */

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.List;
import java.util.SplittableRandom;
import java.util.stream.IntStream;
import jdk.internal.m3.hash.M3Access;
import jdk.internal.m3.hash.M3LongHashFunction;
import jdk.internal.m3.hash.M3LongTupleHashFunction;
import org.junit.jupiter.api.Test;

public class M3HashJitAtomsTest {
    private static final String M3 = "jdk.internal.m3.hash.";
    private static final String LHF = "LongHashFunction";
    private static final String LTHF = "LongTupleHashFunction";
    private static final long CORPUS_SEED = 20261031L;
    private static final long[] FIXED_HASH_SEEDS = {0L, 1L, -1L, Long.MIN_VALUE, Long.MAX_VALUE, 0x9E3779B97F4A7C15L};
    private static final int RANDOM_HASH_SEEDS = 4;
    private static final long[] SEEDS = hashSeeds();
    private static final long[] EDGE_VALUES = {0L, 1L, -1L, Long.MIN_VALUE, Long.MAX_VALUE};
    private static final int PAD = 16;
    private static final int SHORT_SAMPLES = 8;
    private static final int LONG_SAMPLES = 2;
    private static final int LONG_DENSE = 1100;
    private static final int LONG_MAX = 4224;
    private static final int LONG_STRIDE = 7;
    private static final int BLOCK = 1024;
    private static final int EDGE_REACH = 1;
    private static final int SECRET_BYTES = 192;
    private static final int MERGE_SAMPLES = 100_000;
    private static final int MERGE_SPAN = SECRET_BYTES - 64;
    private static final int LANES = 8;
    private static final int LANE_PAIR_BYTES = 16;
    private static final int TUPLE_LONGS = 2;
    private static final int READ_BYTES = 4096;
    private static final int ENTRY_BYTES = 64;
    private static final int ENTRY_STRIDE = 7;
    private static final int PROOF_DENSE = 1100;
    private static final long[] MERGE_OFFSETS = {11L, SECRET_BYTES - 64 - 11};
    private static final Class<?>[] ONE_LONG = {long.class};
    private static final Class<?> XXH3 = type("M3XXH3");
    private static final MethodHandles.Lookup L = lookup(XXH3);
    private static final MethodType SHORT64 =
            MethodType.methodType(long.class, long.class, Object.class, M3Access.class, long.class, long.class);
    private static final MethodType SHORT128 = SHORT64.appendParameterTypes(long[].class);
    private static final MethodType LONG = MethodType.methodType(long.class, byte[].class, Object.class, M3Access.class,
            long.class, long.class, long[].class);
    private static final MethodType DISPATCH64 = MethodType.methodType(long.class, long.class, byte[].class, Object.class,
            M3Access.class, long.class, long.class);
    private static final MethodType DISPATCH128 = DISPATCH64.appendParameterTypes(long[].class);
    private static final MethodType MERGE = MethodType.methodType(long.class, long.class, long.class, long.class,
            long.class, long.class, long.class, long.class, long.class, byte[].class, long.class, long.class);
    private static final MethodHandle HASH_LONG = find("XXH3_hashLong", LONG);
    private static final MethodHandle MERGE_ACCS = find("XXH3_mergeAccs", MERGE);
    private static final MethodHandle INIT_SECRET =
            find("XXH3_initCustomSecret", MethodType.methodType(void.class, byte[].class, long.class));
    private static final byte[] K_SECRET = kSecret();
    @SuppressWarnings("unchecked")
    private static final M3Access<byte[]> BYTE_ACCESS = (M3Access<byte[]>) staticField(type("M3ByteArrayAccess"), "INSTANCE");
    /** A little-endian byte[] access whose every read is a bounds-checked array access: the bounds proof. */
    private static final M3Access<byte[]> CHECKED = checked(ByteOrder.LITTLE_ENDIAN);
    private static final M3Access<ByteBuffer> BUFFER_ACCESS = M3Access.toByteBuffer();
    private static final M3Access<CharSequence> CHAR_ACCESS = M3Access.toCharSequence(ByteOrder.LITTLE_ENDIAN);
    private static final Object O_CHARS = M3HashOracle.call("Access", "toCharSequence", new Class<?>[] {ByteOrder.class},
            ByteOrder.LITTLE_ENDIAN);
    private static final MethodHandle O_BYTES_SLICE =
            M3HashOracle.virtual(LHF, "hashBytes", long.class, byte[].class, int.class, int.class);
    private static final MethodHandle O_ACCESS = M3HashOracle.virtual(LHF, "hash", long.class, Object.class,
            M3HashOracle.type("Access"), long.class, long.class);
    private static final MethodHandle T_BYTES_SLICE =
            M3HashOracle.virtual(LTHF, "hashBytes", long[].class, byte[].class, int.class, int.class);
    private static final MethodHandle T_ACCESS = M3HashOracle.virtual(LTHF, "hash", long[].class, Object.class,
            M3HashOracle.type("Access"), long.class, long.class);

    private static long checks;

    /** One input in every access form: the bytes at [off, off + len) of a padded array, and its char view. */
    private record Input(byte[] padded, int off, int len, StringBuilder chars) {}

    // ---- the length-class atoms, 64 bit ---------------------------------------------------------------------------

    @Test
    void xxh3Len0to16_64b() throws Throwable {
        short64("XXH3_len_0to16_64b", 0, 16);
    }

    @Test
    void xxh3Len9to16_64b() throws Throwable {
        short64("XXH3_len_9to16_64b", 9, 16);
    }

    @Test
    void xxh3Len4to8_64b() throws Throwable {
        short64("XXH3_len_4to8_64b", 4, 8);
    }

    @Test
    void xxh3Len1to3_64b() throws Throwable {
        short64("XXH3_len_1to3_64b", 1, 3);
    }

    @Test
    void xxh3Len17to128_64b() throws Throwable {
        short64("XXH3_len_17to128_64b", 17, 128);
    }

    @Test
    void xxh3Len129to240_64b() throws Throwable {
        short64("XXH3_len_129to240_64b", 129, 240);
    }

    // ---- the length-class atoms, 128 bit --------------------------------------------------------------------------

    @Test
    void xxh3Len0to16_128b() throws Throwable {
        short128("XXH3_len_0to16_128b", 0, 16);
    }

    @Test
    void xxh3Len9to16_128b() throws Throwable {
        short128("XXH3_len_9to16_128b", 9, 16);
    }

    @Test
    void xxh3Len4to8_128b() throws Throwable {
        short128("XXH3_len_4to8_128b", 4, 8);
    }

    @Test
    void xxh3Len1to3_128b() throws Throwable {
        short128("XXH3_len_1to3_128b", 1, 3);
    }

    @Test
    void xxh3Len17to128_128b() throws Throwable {
        short128("XXH3_len_17to128_128b", 17, 128);
    }

    @Test
    void xxh3Len129to240_128b() throws Throwable {
        short128("XXH3_len_129to240_128b", 129, 240);
    }

    // ---- the shared hashLong atom and its merge -------------------------------------------------------------------

    @Test
    void xxh3HashLong() throws Throwable {
        SplittableRandom r = new SplittableRandom(CORPUS_SEED);
        for (long seed : SEEDS) {
            byte[] secret = secret(seed);
            Object o64 = M3HashOracle.call(LHF, "xx3", ONE_LONG, seed);
            Object o128 = M3HashOracle.call(LTHF, "xx128", ONE_LONG, seed);
            for (int len : longLengths()) {
                for (int k = 0; k < LONG_SAMPLES; k++) {
                    Input in = input(r, len, k);
                    long want64 = (long) O_BYTES_SLICE.invoke(o64, in.padded(), in.off(), len);
                    long[] want128 = (long[]) T_BYTES_SLICE.invoke(o128, in.padded(), in.off(), len);
                    // one owner, two donor computations: XXH3_hashLong_64b == low half of XXH3_hashLong_128b
                    eq(want64, want128[0], "vendored xx3 vs xx128 low", len);
                    for (int form = 0; form < Forms.COUNT; form++) {
                        long[] result = new long[TUPLE_LONGS];
                        eq(want64, Forms.hashLong(form, secret, in, null), "XXH3_hashLong " + Forms.NAMES[form], len);
                        eq(want128[0], Forms.hashLong(form, secret, in, result), "XXH3_hashLong low " + Forms.NAMES[form], len);
                        eqArray(want128, result, "XXH3_hashLong result " + Forms.NAMES[form], len);
                    }
                }
            }
        }
        for (int len : new int[] {241, BLOCK, BLOCK + 1, LONG_MAX}) {
            outOfRange(len, (b, l) -> {
                long ignored = (long) HASH_LONG.invokeExact(K_SECRET, (Object) b, (M3Access<?>) CHECKED, 0L, (long) l,
                        (long[]) null);
            });
        }
        report("XXH3_hashLong");
    }

    @Test
    void xxh3MergeAccs() throws Throwable {
        SplittableRandom r = new SplittableRandom(CORPUS_SEED);
        long[] acc = new long[LANES];
        for (int i = 0; i < MERGE_SAMPLES; i++) {
            for (int lane = 0; lane < LANES; lane++) {
                acc[lane] = i < EDGE_VALUES.length ? EDGE_VALUES[(i + lane) % EDGE_VALUES.length] : r.nextLong();
            }
            byte[] secret = i % 2 == 0 ? K_SECRET : secret(r.nextLong());
            long offSec = i < MERGE_OFFSETS.length * 2 ? MERGE_OFFSETS[i % MERGE_OFFSETS.length] : r.nextInt(MERGE_SPAN + 1);
            long start = r.nextLong();
            long want = start;
            ByteBuffer le = ByteBuffer.wrap(secret).order(ByteOrder.LITTLE_ENDIAN);
            for (int pair = 0; pair < LANES / 2; pair++) {
                long x = acc[2 * pair] ^ le.getLong((int) offSec + LANE_PAIR_BYTES * pair);
                long y = acc[2 * pair + 1] ^ le.getLong((int) offSec + LANE_PAIR_BYTES * pair + Long.BYTES);
                want += (x * y) ^ Math.unsignedMultiplyHigh(x, y);
            }
            long got = (long) MERGE_ACCS.invokeExact(acc[0], acc[1], acc[2], acc[3], acc[4], acc[5], acc[6], acc[7],
                    secret, offSec, start);
            eq(want, got, "XXH3_mergeAccs", i);
        }
        report("XXH3_mergeAccs");
    }

    // ---- the dispatchers ------------------------------------------------------------------------------------------

    @Test
    void xxh3Dispatch64() throws Throwable {
        MethodHandle m = find("XXH3_64bits_internal", DISPATCH64);
        SplittableRandom r = new SplittableRandom(CORPUS_SEED);
        for (long seed : SEEDS) {
            byte[] secret = secret(seed);
            Object oracle = M3HashOracle.call(LHF, "xx3", ONE_LONG, seed);
            for (int len : allLengths()) {
                Input in = input(r, len, len);
                long want = (long) O_BYTES_SLICE.invoke(oracle, in.padded(), in.off(), len);
                eq(want, (long) m.invokeExact(seed, secret, (Object) in.padded(), (M3Access<?>) BYTE_ACCESS, (long) in.off(),
                        (long) len), "XXH3_64bits_internal", len);
            }
        }
        report("XXH3_64bits_internal");
    }

    @Test
    void xxh3Dispatch128() throws Throwable {
        MethodHandle m = find("XXH3_128bits_internal", DISPATCH128);
        SplittableRandom r = new SplittableRandom(CORPUS_SEED);
        for (long seed : SEEDS) {
            byte[] secret = secret(seed);
            Object oracle = M3HashOracle.call(LTHF, "xx128", ONE_LONG, seed);
            for (int len : allLengths()) {
                Input in = input(r, len, len);
                long[] want = (long[]) T_BYTES_SLICE.invoke(oracle, in.padded(), in.off(), len);
                long[] result = new long[TUPLE_LONGS];
                eq(want[0], (long) m.invokeExact(seed, secret, (Object) in.padded(), (M3Access<?>) BYTE_ACCESS,
                        (long) in.off(), (long) len, result), "XXH3_128bits_internal", len);
                eqArray(want, result, "XXH3_128bits_internal result", len);
                eq(want[0], (long) m.invokeExact(seed, secret, (Object) in.padded(), (M3Access<?>) BYTE_ACCESS,
                        (long) in.off(), (long) len, (long[]) null), "XXH3_128bits_internal no result", len);
            }
        }
        report("XXH3_128bits_internal");
    }

    // ---- UNSAFE_TO_BYTE_ARRAY v2: the read atom ---------------------------------------------------------------------

    @Test
    void byteArrayReadAtom() {
        byte[] b = new byte[READ_BYTES + Long.BYTES];
        new SplittableRandom(CORPUS_SEED).nextBytes(b);
        ByteBuffer le = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i + Long.BYTES <= b.length; i++) {
            eq(le.getLong(i), BYTE_ACCESS.i64(b, i), "i64 vs little-endian ByteBuffer", i);
            eq(BYTE_ACCESS.getLong(b, i), BYTE_ACCESS.i64(b, i), "i64 vs getLong", i);
            eq(Integer.toUnsignedLong(le.getInt(i)), BYTE_ACCESS.u32(b, i), "u32 vs little-endian ByteBuffer", i);
            eq(BYTE_ACCESS.getUnsignedInt(b, i), BYTE_ACCESS.u32(b, i), "u32 vs getUnsignedInt", i);
            eq(le.getInt(i), BYTE_ACCESS.i32(b, i), "i32 vs little-endian ByteBuffer", i);
            eq(BYTE_ACCESS.getInt(b, i), BYTE_ACCESS.i32(b, i), "i32 vs getInt", i);
            eq(Short.toUnsignedInt(le.getShort(i)), BYTE_ACCESS.u16(b, i), "u16 vs little-endian ByteBuffer", i);
            eq(BYTE_ACCESS.getUnsignedShort(b, i), BYTE_ACCESS.u16(b, i), "u16 vs getUnsignedShort", i);
            eq(le.getShort(i), BYTE_ACCESS.i16(b, i), "i16 vs little-endian ByteBuffer", i);
            eq(BYTE_ACCESS.getShort(b, i), BYTE_ACCESS.i16(b, i), "i16 vs getShort", i);
            eq(BYTE_ACCESS.getUnsignedByte(b, i), BYTE_ACCESS.u8(b, i), "u8 vs getUnsignedByte", i);
            eq(BYTE_ACCESS.getByte(b, i), BYTE_ACCESS.i8(b, i), "i8 vs getByte", i);
        }
        // the getters keep their checks: the exception ByteArrayLittleEndian throws
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> BYTE_ACCESS.getLong(b, b.length - Long.BYTES + 1));
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> BYTE_ACCESS.getInt(b, -1));
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> BYTE_ACCESS.getShort(b, b.length - 1));
        checks += 3;
        report("M3ByteArrayAccess read atom");
    }

    // ---- FACADE_ENTRY_CHECK v1 ----------------------------------------------------------------------------------------

    @Test
    void facadeEntryCheck() throws Throwable {
        MethodHandle single = lookup(M3LongHashFunction.class).findVirtual(M3LongHashFunction.class, "unsafeHash",
                MethodType.methodType(long.class, byte[].class, long.class, long.class));
        MethodHandle tuple = lookup(M3LongTupleHashFunction.class).findStatic(M3LongTupleHashFunction.class, "unsafeHash",
                MethodType.methodType(void.class, M3LongTupleHashFunction.class, byte[].class, long.class, long.class,
                        long[].class));
        byte[] b = new byte[ENTRY_BYTES];
        new SplittableRandom(CORPUS_SEED).nextBytes(b);
        long[][] bad = {{-1, 1}, {0, -1}, {0, ENTRY_BYTES + 1}, {ENTRY_BYTES, 1}, {1, Long.MAX_VALUE},
                {Long.MAX_VALUE, 1}, {Long.MIN_VALUE, 0}, {ENTRY_BYTES + 1, 0}};
        for (M3LongHashFunction f : List.of(M3LongHashFunction.xx3(), M3LongHashFunction.xx(), M3LongHashFunction.wy_3(),
                M3LongHashFunction.murmur_3(), M3LongHashFunction.city_1_1(), M3LongHashFunction.metro())) {
            for (long[] x : bad) {
                assertThrows(IndexOutOfBoundsException.class, () -> {
                    long ignored = (long) single.invokeExact(f, b, x[0], x[1]);
                });
                checks++;
            }
            for (int off = 0; off <= ENTRY_BYTES; off++) {
                for (int len = 0; off + len <= ENTRY_BYTES; len += ENTRY_STRIDE) {
                    eq(f.hashBytes(b, off, len), (long) single.invokeExact(f, b, (long) off, (long) len), "unsafeHash", len);
                }
            }
        }
        for (M3LongTupleHashFunction f : List.of(M3LongTupleHashFunction.xx128(), M3LongTupleHashFunction.murmur_3())) {
            for (long[] x : bad) {
                // a block, so that invokeExact is an expression statement of type (...)void
                assertThrows(IndexOutOfBoundsException.class, () -> {
                    tuple.invokeExact(f, b, x[0], x[1], new long[TUPLE_LONGS]);
                });
                checks++;
            }
            for (int off = 0; off <= ENTRY_BYTES; off++) {
                for (int len = 0; off + len <= ENTRY_BYTES; len += ENTRY_STRIDE) {
                    long[] result = new long[TUPLE_LONGS];
                    tuple.invokeExact(f, b, (long) off, (long) len, result);
                    eqArray(f.hashBytes(b, off, len), result, "tuple unsafeHash", len);
                }
            }
        }
        report("FACADE_ENTRY_CHECK");
    }

    // ---- the bounds proof: every kernel reads only inside [off, off + len) -------------------------------------------

    @Test
    void boundsProof() {
        SplittableRandom r = new SplittableRandom(CORPUS_SEED);
        List<M3LongHashFunction> single = List.of(M3LongHashFunction.xx(), M3LongHashFunction.xx(1L),
                M3LongHashFunction.xx3(), M3LongHashFunction.xx3(1L), M3LongHashFunction.xx128low(),
                M3LongHashFunction.xx128low(1L), M3LongHashFunction.wy_3(), M3LongHashFunction.wy_3(1L),
                M3LongHashFunction.murmur_3(), M3LongHashFunction.murmur_3(1L), M3LongHashFunction.city_1_1(),
                M3LongHashFunction.city_1_1(1L, 2L), M3LongHashFunction.farmNa(), M3LongHashFunction.farmNa(1L, 2L),
                M3LongHashFunction.farmUo(), M3LongHashFunction.farmUo(1L, 2L), M3LongHashFunction.metro(),
                M3LongHashFunction.metro(1L));
        List<M3LongTupleHashFunction> tuples = List.of(M3LongTupleHashFunction.xx128(),
                M3LongTupleHashFunction.xx128(1L), M3LongTupleHashFunction.murmur_3(), M3LongTupleHashFunction.murmur_3(1L));
        int[] lengths = IntStream.concat(IntStream.rangeClosed(0, PROOF_DENSE),
                IntStream.iterate(PROOF_DENSE + 1, n -> n <= LONG_MAX, n -> n + LONG_STRIDE)).toArray();
        for (int len : lengths) {
            byte[] exact = new byte[len];
            r.nextBytes(exact);
            byte[] shorter = Arrays.copyOf(exact, Math.max(0, len - EDGE_REACH));
            for (M3LongHashFunction f : single) {
                eq(f.hashBytes(exact), f.hash(exact, CHECKED, 0L, len), "checked kernel", len);
                if (len > 0) {
                    assertThrows(IndexOutOfBoundsException.class, () -> f.hash(shorter, CHECKED, 0L, len));
                    checks++;
                }
            }
            for (M3LongTupleHashFunction f : tuples) {
                eqArray(f.hashBytes(exact), f.hash(exact, CHECKED, 0L, len), "checked tuple kernel", len);
                if (len > 0) {
                    assertThrows(IndexOutOfBoundsException.class, () -> f.hash(shorter, CHECKED, 0L, len));
                    checks++;
                }
            }
        }
        report("bounds proof");
    }

    // ---- drivers --------------------------------------------------------------------------------------------------

    private static void short64(String atom, int lo, int hi) throws Throwable {
        MethodHandle m = find(atom, SHORT64);
        SplittableRandom r = new SplittableRandom(CORPUS_SEED);
        for (long seed : SEEDS) {
            Object oracle = M3HashOracle.call(LHF, "xx3", ONE_LONG, seed);
            for (int len = lo; len <= hi; len++) {
                for (int k = 0; k < SHORT_SAMPLES; k++) {
                    Input in = input(r, len, k);
                    long want = (long) O_BYTES_SLICE.invoke(oracle, in.padded(), in.off(), len);
                    long wantChars = (long) O_ACCESS.invoke(oracle, (Object) in.chars(), O_CHARS, (long) in.off(),
                            (long) len);
                    for (int form = 0; form < Forms.COUNT; form++) {
                        eq(form == Forms.CHARS ? wantChars : want, Forms.short64(m, form, seed, in),
                                atom + " " + Forms.NAMES[form], len);
                    }
                }
            }
        }
        for (int len = Math.max(lo, 1); len <= hi; len++) {
            outOfRange(len, (b, l) -> {
                long ignored = (long) m.invokeExact(0L, (Object) b, (M3Access<?>) CHECKED, 0L, (long) l);
            });
        }
        report(atom);
    }

    private static void short128(String atom, int lo, int hi) throws Throwable {
        MethodHandle m = find(atom, SHORT128);
        SplittableRandom r = new SplittableRandom(CORPUS_SEED);
        for (long seed : SEEDS) {
            Object oracle = M3HashOracle.call(LTHF, "xx128", ONE_LONG, seed);
            for (int len = lo; len <= hi; len++) {
                for (int k = 0; k < SHORT_SAMPLES; k++) {
                    Input in = input(r, len, k);
                    long[] want = (long[]) T_BYTES_SLICE.invoke(oracle, in.padded(), in.off(), len);
                    long[] wantChars = (long[]) T_ACCESS.invoke(oracle, (Object) in.chars(), O_CHARS, (long) in.off(),
                            (long) len);
                    for (int form = 0; form < Forms.COUNT; form++) {
                        long[] w = form == Forms.CHARS ? wantChars : want;
                        long[] result = new long[TUPLE_LONGS];
                        eq(w[0], Forms.short128(m, form, seed, in, result), atom + " " + Forms.NAMES[form], len);
                        eqArray(w, result, atom + " result " + Forms.NAMES[form], len);
                        eq(w[0], Forms.short128(m, form, seed, in, null), atom + " no result " + Forms.NAMES[form], len);
                    }
                }
            }
        }
        for (int len = Math.max(lo, 1); len <= hi; len++) {
            outOfRange(len, (b, l) -> {
                long ignored = (long) m.invokeExact(0L, (Object) b, (M3Access<?>) CHECKED, 0L, (long) l, (long[]) null);
            });
        }
        report(atom);
    }

    /** The access forms every atom is called through: byte[], ByteBuffer little- and big-endian, CharSequence. */
    private static final class Forms {
        static final int BYTE_ARRAY = 0;
        static final int BUFFER_LE = 1;
        static final int BUFFER_BE = 2;
        static final int CHARS = 3;
        static final int COUNT = 4;
        static final String[] NAMES = {"byte[]", "ByteBuffer LE", "ByteBuffer BE", "CharSequence"};

        private Forms() {}

        static Object input(int form, Input in) {
            return switch (form) {
                case BYTE_ARRAY -> in.padded();
                case BUFFER_LE -> ByteBuffer.wrap(in.padded()).order(ByteOrder.LITTLE_ENDIAN);
                case BUFFER_BE -> ByteBuffer.wrap(in.padded()).order(ByteOrder.BIG_ENDIAN);
                default -> in.chars();
            };
        }

        /** The little-endian view of the form's access, as the dispatchers pass it to the atoms. */
        static M3Access<?> access(int form, Object input) {
            return switch (form) {
                case BYTE_ARRAY -> BYTE_ACCESS;
                case BUFFER_LE, BUFFER_BE -> BUFFER_ACCESS.byteOrder((ByteBuffer) input, ByteOrder.LITTLE_ENDIAN);
                default -> CHAR_ACCESS.byteOrder((CharSequence) input, ByteOrder.LITTLE_ENDIAN);
            };
        }

        static long short64(MethodHandle m, int form, long seed, Input in) throws Throwable {
            Object x = input(form, in);
            return (long) m.invokeExact(seed, x, access(form, x), (long) in.off(), (long) in.len());
        }

        static long short128(MethodHandle m, int form, long seed, Input in, long[] result) throws Throwable {
            Object x = input(form, in);
            return (long) m.invokeExact(seed, x, access(form, x), (long) in.off(), (long) in.len(), result);
        }

        static long hashLong(int form, byte[] secret, Input in, long[] result) throws Throwable {
            Object x = input(form, in);
            return (long) HASH_LONG.invokeExact(secret, x, access(form, x), (long) in.off(), (long) in.len(), result);
        }
    }

    // ---- helpers --------------------------------------------------------------------------------------------------

    interface Read {
        void run(byte[] input, int len) throws Throwable;
    }

    /** Through the checked access: the exact array reads cleanly, one byte less throws IndexOutOfBoundsException. */
    private static void outOfRange(int len, Read call) throws Throwable {
        byte[] exact = new byte[len];
        call.run(exact, len);
        assertThrows(IndexOutOfBoundsException.class, () -> call.run(new byte[len - EDGE_REACH], len));
        checks += 2;
    }

    /**
     * A random input of len bytes at a non-zero offset of a padded array; its char view holds the same byte
     * sequence (little-endian chars) at the same byte offset, so every form hashes the same bytes.
     */
    private static Input input(SplittableRandom r, int len, int k) {
        int off = 1 + k % (PAD - 1);
        byte[] padded = new byte[len + PAD + off];
        r.nextBytes(padded);
        char[] c = new char[(padded.length + 1) / 2];
        for (int i = 0; i < c.length; i++) {
            int lo = padded[2 * i] & 0xFF;
            int hi = 2 * i + 1 < padded.length ? padded[2 * i + 1] & 0xFF : 0;
            c[i] = (char) (lo | hi << Byte.SIZE);
        }
        return new Input(padded, off, len, new StringBuilder().append(c));
    }

    private static int[] longLengths() {
        int[] dense = IntStream.rangeClosed(241, LONG_DENSE).toArray();
        int[] strided = IntStream.iterate(LONG_DENSE + 1, n -> n <= LONG_MAX, n -> n + LONG_STRIDE).toArray();
        int[] edges = IntStream.rangeClosed(1, LONG_MAX / BLOCK)
                .flatMap(b -> IntStream.of(b * BLOCK - 1, b * BLOCK, b * BLOCK + 1)).toArray();
        return IntStream.concat(IntStream.concat(Arrays.stream(dense),
                Arrays.stream(strided)), Arrays.stream(edges)).distinct().sorted().toArray();
    }

    private static int[] allLengths() {
        return IntStream.concat(IntStream.rangeClosed(0, 240),
                Arrays.stream(longLengths())).toArray();
    }

    private static long[] hashSeeds() {
        SplittableRandom r = new SplittableRandom(CORPUS_SEED);
        long[] s = Arrays.copyOf(FIXED_HASH_SEEDS, FIXED_HASH_SEEDS.length + RANDOM_HASH_SEEDS);
        for (int i = FIXED_HASH_SEEDS.length; i < s.length; i++) {
            s[i] = r.nextLong();
        }
        return s;
    }

    /** The secret XXH3_hashLong reads for a seed: the default secret for seed 0, else the seed's custom secret. */
    private static byte[] secret(long seed) throws Throwable {
        if (seed == 0L) {
            return K_SECRET;
        }
        byte[] s = new byte[SECRET_BYTES];
        INIT_SECRET.invokeExact(s, seed);
        return s;
    }

    private static M3Access<byte[]> checked(ByteOrder order) {
        return new M3Access<>() {
            @Override
            public int getByte(byte[] input, long offset) {
                if (offset != (int) offset) {
                    throw new IndexOutOfBoundsException("offset " + offset);
                }
                return input[(int) offset];
            }

            @Override
            public ByteOrder byteOrder(byte[] input) {
                return order;
            }

            @Override
            protected M3Access<byte[]> reverseAccess() {
                return checked(order == ByteOrder.LITTLE_ENDIAN ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
            }
        };
    }

    private static Class<?> type(String simple) {
        try {
            return Class.forName(M3 + simple);
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static MethodHandles.Lookup lookup(Class<?> c) {
        try {
            return MethodHandles.privateLookupIn(c, MethodHandles.lookup());
        } catch (IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static MethodHandle find(String name, MethodType type) {
        try {
            return L.findStatic(XXH3, name, type);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("XXH3 atom missing: " + name + type, e);
        }
    }

    private static Object staticField(Class<?> c, String name) {
        try {
            return lookup(c).findStaticVarHandle(c, name, c).get();
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static byte[] kSecret() {
        try {
            return ((byte[]) L.findStaticVarHandle(XXH3, "XXH3_kSecret", byte[].class).get()).clone();
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static void eq(long want, long got, String what, int at) {
        checks++;
        if (want != got) {
            fail(what + " at " + at + ": vendored " + want + ", absorbed " + got);
        }
    }

    private static void eqArray(long[] want, long[] got, String what, int at) {
        checks++;
        assertArrayEquals(want, got, () -> what + " at " + at);
    }

    private static void report(String what) {
        System.out.println(what + ": " + checks + " comparisons so far");
    }
}
