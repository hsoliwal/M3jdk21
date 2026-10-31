/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
 */

/*
 * @test
 * @summary One JUnit test per atom of the zero-allocation-hashing absorption (jdk.internal.m3.hash.M3*). The algorithm
 *          atoms (xxHash, XXH3 64/128, wyhash, MurmurHash3 64/128, CityHash/FarmHash na/uo, MetroHash) are
 *          differential against the vendored library compiled unchanged (M3HashOracle) for every length 0..1100,
 *          random inputs, fixed and random seeds, through every kept input form: byte[] and slices, heap, read-only
 *          and direct ByteBuffers in both orders, Latin-1 and UTF-16 Strings, StringBuilder, CharSequence access in
 *          both orders, and the primitives. The facade atoms are checked against the vendored API minus the drop
 *          ledger and for identical exceptions; the access and kernel atoms against the vendored atom they replace
 *          or a JDK reference.
 * @modules java.base/jdk.internal.m3.hash:open
 *          java.base/sun.nio.ch
 *          jdk.compiler
 *          jdk.unsupported
 * @run junit M3HashAtomsTest
 */

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.TreeSet;
import java.util.function.LongFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jdk.internal.m3.hash.M3Access;
import jdk.internal.m3.hash.M3LongHashFunction;
import jdk.internal.m3.hash.M3LongTupleHashFunction;
import org.junit.jupiter.api.Test;

public class M3HashAtomsTest {
    private static final String M3 = "jdk.internal.m3.hash.";
    private static final String M3_PREFIX = "M3";
    private static final String LHF = "LongHashFunction";
    private static final String LTHF = "LongTupleHashFunction";
    private static final String ACCESS = "Access";
    private static final int MAX_LENGTH = 1100;
    private static final int SLICE_PAD = 16;
    private static final long CORPUS_SEED = 20261031L;
    private static final long[] FIXED_HASH_SEEDS = {0L, 1L, -1L, Long.MIN_VALUE, Long.MAX_VALUE, 0x9E3779B97F4A7C15L};
    private static final int RANDOM_HASH_SEEDS = 4;
    private static final long[] SEEDS = hashSeeds();
    private static final long[] EDGE_VALUES = {0L, 1L, -1L, Long.MIN_VALUE, Long.MAX_VALUE};
    private static final int PRIMITIVE_SAMPLES = 256;
    private static final int ACCESS_WINDOW = 64;
    private static final int CHAR_WINDOW = 48;
    private static final int MATH_SAMPLES = 100_000;
    private static final int UTF16_BOUND = Character.MAX_VALUE + 1;
    private static final int TUPLE_LONGS = 2;
    private static final int TUPLE_BITS = 128;
    private static final ByteOrder[] ORDERS = {ByteOrder.BIG_ENDIAN, ByteOrder.LITTLE_ENDIAN};
    private static final Class<?>[] NO_PARAMS = {};
    private static final Class<?>[] ONE_LONG = {long.class};
    private static final Class<?>[] TWO_LONGS = {long.class, long.class};

    private static final Class<?> O_ACCESS_TYPE = M3HashOracle.type(ACCESS);
    private static final MethodHandle O_BYTES = M3HashOracle.virtual(LHF, "hashBytes", long.class, byte[].class);
    private static final MethodHandle O_BYTES_SLICE =
            M3HashOracle.virtual(LHF, "hashBytes", long.class, byte[].class, int.class, int.class);
    private static final MethodHandle O_BUFFER = M3HashOracle.virtual(LHF, "hashBytes", long.class, ByteBuffer.class);
    private static final MethodHandle O_BUFFER_SLICE =
            M3HashOracle.virtual(LHF, "hashBytes", long.class, ByteBuffer.class, int.class, int.class);
    private static final MethodHandle O_STRING = M3HashOracle.virtual(LHF, "hashChars", long.class, String.class);
    private static final MethodHandle O_STRING_SLICE =
            M3HashOracle.virtual(LHF, "hashChars", long.class, String.class, int.class, int.class);
    private static final MethodHandle O_BUILDER = M3HashOracle.virtual(LHF, "hashChars", long.class, StringBuilder.class);
    private static final MethodHandle O_BUILDER_SLICE =
            M3HashOracle.virtual(LHF, "hashChars", long.class, StringBuilder.class, int.class, int.class);
    private static final MethodHandle O_ACCESS =
            M3HashOracle.virtual(LHF, "hash", long.class, Object.class, O_ACCESS_TYPE, long.class, long.class);
    private static final MethodHandle O_LONG = M3HashOracle.virtual(LHF, "hashLong", long.class, long.class);
    private static final MethodHandle O_INT = M3HashOracle.virtual(LHF, "hashInt", long.class, int.class);
    private static final MethodHandle O_SHORT = M3HashOracle.virtual(LHF, "hashShort", long.class, short.class);
    private static final MethodHandle O_CHAR = M3HashOracle.virtual(LHF, "hashChar", long.class, char.class);
    private static final MethodHandle O_BYTE = M3HashOracle.virtual(LHF, "hashByte", long.class, byte.class);
    private static final MethodHandle O_VOID = M3HashOracle.virtual(LHF, "hashVoid", long.class);

    private static final MethodHandle T_BYTES = M3HashOracle.virtual(LTHF, "hashBytes", long[].class, byte[].class);
    private static final MethodHandle T_BYTES_SLICE =
            M3HashOracle.virtual(LTHF, "hashBytes", long[].class, byte[].class, int.class, int.class);
    private static final MethodHandle T_BYTES_INTO =
            M3HashOracle.virtual(LTHF, "hashBytes", void.class, byte[].class, long[].class);
    private static final MethodHandle T_BUFFER_SLICE =
            M3HashOracle.virtual(LTHF, "hashBytes", long[].class, ByteBuffer.class, int.class, int.class);
    private static final MethodHandle T_STRING = M3HashOracle.virtual(LTHF, "hashChars", long[].class, String.class);
    private static final MethodHandle T_CHARS = M3HashOracle.virtual(LTHF, "hashChars", long[].class, CharSequence.class);
    private static final MethodHandle T_ACCESS =
            M3HashOracle.virtual(LTHF, "hash", long[].class, Object.class, O_ACCESS_TYPE, long.class, long.class);
    private static final MethodHandle T_LONG = M3HashOracle.virtual(LTHF, "hashLong", long[].class, long.class);
    private static final MethodHandle T_INT = M3HashOracle.virtual(LTHF, "hashInt", long[].class, int.class);
    private static final MethodHandle T_SHORT = M3HashOracle.virtual(LTHF, "hashShort", long[].class, short.class);
    private static final MethodHandle T_CHAR = M3HashOracle.virtual(LTHF, "hashChar", long[].class, char.class);
    private static final MethodHandle T_BYTE = M3HashOracle.virtual(LTHF, "hashByte", long[].class, byte.class);
    private static final MethodHandle T_VOID = M3HashOracle.virtual(LTHF, "hashVoid", long[].class);
    private static final MethodHandle T_BITS = M3HashOracle.virtual(LTHF, "bitsLength", int.class);

    private static final MethodHandle A_LONG = M3HashOracle.virtual(ACCESS, "getLong", long.class, Object.class, long.class);
    private static final MethodHandle A_UINT =
            M3HashOracle.virtual(ACCESS, "getUnsignedInt", long.class, Object.class, long.class);
    private static final MethodHandle A_INT = M3HashOracle.virtual(ACCESS, "getInt", int.class, Object.class, long.class);
    private static final MethodHandle A_USHORT =
            M3HashOracle.virtual(ACCESS, "getUnsignedShort", int.class, Object.class, long.class);
    private static final MethodHandle A_SHORT = M3HashOracle.virtual(ACCESS, "getShort", int.class, Object.class, long.class);
    private static final MethodHandle A_UBYTE =
            M3HashOracle.virtual(ACCESS, "getUnsignedByte", int.class, Object.class, long.class);
    private static final MethodHandle A_BYTE = M3HashOracle.virtual(ACCESS, "getByte", int.class, Object.class, long.class);
    private static final MethodHandle A_ORDER =
            M3HashOracle.virtual(ACCESS, "byteOrder", ByteOrder.class, Object.class);
    private static final MethodHandle A_IN_ORDER =
            M3HashOracle.virtual(ACCESS, "byteOrder", O_ACCESS_TYPE, Object.class, ByteOrder.class);

    /** The public members the absorption does not keep (FACADE_DROP_RAW_MEMORY v1, ACCESS_DROP_UNSAFE v1). */
    private static final Set<String> DROPPED_LONG = Set.of("long hashBoolean(boolean)", "long hashBooleans(boolean[])",
            "long hashBooleans(boolean[],int,int)", "long hashMemory(long,long)", "long hashChars(char[])",
            "long hashChars(char[],int,int)", "long hashShorts(short[])", "long hashShorts(short[],int,int)",
            "long hashInts(int[])", "long hashInts(int[],int,int)", "long hashLongs(long[])",
            "long hashLongs(long[],int,int)");
    private static final Set<String> DROPPED_TUPLE = droppedTuple();
    private static final Set<String> DROPPED_ACCESS = Set.of("static M3Access unsafe()");

    private static long checks;

    /** One hash function on both sides: absorbed and vendored. */
    private record Fn(String name, M3LongHashFunction m3, Object oracle) {}

    private record TupleFn(String name, M3LongTupleHashFunction m3, Object oracle) {}

    private interface TwoSeeds {
        M3LongHashFunction of(long seed0, long seed1);
    }

    // ---- the algorithm atoms -------------------------------------------------------------------------------------

    @Test
    void xxHash() throws Throwable {
        differential(functions("xx", M3LongHashFunction.xx(), M3LongHashFunction::xx, null));
    }

    @Test
    void xxh3() throws Throwable {
        List<Fn> fns = functions("xx3", M3LongHashFunction.xx3(), M3LongHashFunction::xx3, null);
        fns.addAll(functions("xx128low", M3LongHashFunction.xx128low(), M3LongHashFunction::xx128low, null));
        differential(fns);
        tupleDifferential(tupleFunctions("xx128", M3LongTupleHashFunction.xx128(), M3LongTupleHashFunction::xx128));
    }

    @Test
    void wyHash() throws Throwable {
        differential(functions("wy_3", M3LongHashFunction.wy_3(), M3LongHashFunction::wy_3, null));
    }

    @Test
    void murmurHash3() throws Throwable {
        differential(functions("murmur_3", M3LongHashFunction.murmur_3(), M3LongHashFunction::murmur_3, null));
        tupleDifferential(tupleFunctions("murmur_3", M3LongTupleHashFunction.murmur_3(),
                M3LongTupleHashFunction::murmur_3));
    }

    @Test
    void cityAndFarmHash() throws Throwable {
        List<Fn> fns = functions("city_1_1", M3LongHashFunction.city_1_1(), M3LongHashFunction::city_1_1,
                M3LongHashFunction::city_1_1);
        fns.addAll(functions("farmNa", M3LongHashFunction.farmNa(), M3LongHashFunction::farmNa,
                M3LongHashFunction::farmNa));
        fns.addAll(functions("farmUo", M3LongHashFunction.farmUo(), M3LongHashFunction::farmUo,
                M3LongHashFunction::farmUo));
        differential(fns);
    }

    @Test
    void metroHash() throws Throwable {
        differential(functions("metro", M3LongHashFunction.metro(), M3LongHashFunction::metro, null));
    }

    // ---- the facade atoms ----------------------------------------------------------------------------------------

    @Test
    void longHashFunction() throws Throwable {
        assertEquals(surface(M3HashOracle.type(LHF), DROPPED_LONG), surface(M3LongHashFunction.class, Set.of()));
        Fn f = functions("xx3", M3LongHashFunction.xx3(), M3LongHashFunction::xx3, null).get(0);
        byte[] b = new byte[ACCESS_WINDOW];
        String s = "m3".repeat(ACCESS_WINDOW);
        StringBuilder sb = new StringBuilder(s);
        ByteBuffer direct = ByteBuffer.allocateDirect(ACCESS_WINDOW);
        int[][] bad = {{-1, 1}, {0, -1}, {0, ACCESS_WINDOW + 1}, {ACCESS_WINDOW, 1}, {1, Integer.MAX_VALUE}};
        for (int[] x : bad) {
            sameThrow(() -> O_BYTES_SLICE.invoke(f.oracle(), b, x[0], x[1]), () -> f.m3().hashBytes(b, x[0], x[1]));
            sameThrow(() -> O_BUFFER_SLICE.invoke(f.oracle(), direct, x[0], x[1]),
                    () -> f.m3().hashBytes(direct, x[0], x[1]));
            sameThrow(() -> O_STRING_SLICE.invoke(f.oracle(), s, x[0], x[1]), () -> f.m3().hashChars(s, x[0], x[1]));
            sameThrow(() -> O_BUILDER_SLICE.invoke(f.oracle(), sb, x[0], x[1]),
                    () -> f.m3().hashChars(sb, x[0], x[1]));
        }
        sameThrow(() -> O_BYTES.invoke(f.oracle(), (byte[]) null), () -> f.m3().hashBytes((byte[]) null));
        sameThrow(() -> O_STRING.invoke(f.oracle(), (String) null), () -> f.m3().hashChars((String) null));
        ByteBuffer moved = ByteBuffer.wrap(b).position(1).limit(ACCESS_WINDOW - 1);
        f.m3().hashBytes(moved);
        assertEquals(1, moved.position());
        assertEquals(ACCESS_WINDOW - 1, moved.limit());
        assertEquals(ByteOrder.BIG_ENDIAN, moved.order());
        report("longHashFunction");
    }

    @Test
    void longTupleHashFunction() throws Throwable {
        assertEquals(surface(M3HashOracle.type(LTHF), DROPPED_TUPLE), surface(M3LongTupleHashFunction.class, Set.of()));
        for (TupleFn f : tupleFunctions("xx128", M3LongTupleHashFunction.xx128(), M3LongTupleHashFunction::xx128)) {
            assertEquals((int) T_BITS.invoke(f.oracle()), f.m3().bitsLength());
            assertEquals(TUPLE_BITS, f.m3().bitsLength());
            assertEquals(TUPLE_LONGS, f.m3().newResultArray().length);
            byte[] b = new byte[ACCESS_WINDOW];
            long[] into = f.m3().newResultArray();
            f.m3().hashBytes(b, into);
            eqArray((long[]) T_BYTES.invoke(f.oracle(), b), into, f.name(), ACCESS_WINDOW, "hashBytes(byte[], long[])");
            sameThrow(() -> T_BYTES_INTO.invoke(f.oracle(), b, new long[1]), () -> f.m3().hashBytes(b, new long[1]));
            sameThrow(() -> T_BYTES_INTO.invoke(f.oracle(), b, (long[]) null), () -> f.m3().hashBytes(b, (long[]) null));
        }
        report("longTupleHashFunction");
    }

    @Test
    void dualHashFunction() throws Throwable {
        Class<?> dual = Class.forName(M3 + "M3DualHashFunction");
        SplittableRandom r = new SplittableRandom(CORPUS_SEED);
        for (long seed : SEEDS) {
            M3LongTupleHashFunction xx128 = M3LongTupleHashFunction.xx128(seed);
            M3LongTupleHashFunction murmur = M3LongTupleHashFunction.murmur_3(seed);
            assertTrue(dual.isInstance(xx128) && dual.isInstance(murmur), "tuple functions are M3DualHashFunction");
            for (int len = 0; len <= MAX_LENGTH; len += SLICE_PAD) {
                byte[] b = bytes(r, len);
                eq(xx128.hashBytes(b)[0], M3LongHashFunction.xx128low(seed).hashBytes(b), "xx128low", len, "low half");
                eq(murmur.hashBytes(b)[0], M3LongHashFunction.murmur_3(seed).hashBytes(b), "murmur_3", len, "low half");
            }
        }
        report("dualHashFunction");
    }

    // ---- the access atoms ----------------------------------------------------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    void byteArrayAccess() throws Throwable {
        Field instance = Class.forName(M3 + "M3ByteArrayAccess").getDeclaredField("INSTANCE");
        instance.setAccessible(true);
        M3Access<byte[]> a = (M3Access<byte[]>) instance.get(null);
        Field base = M3HashOracle.type("UnsafeAccess").getDeclaredField("BYTE_BASE");
        base.setAccessible(true);
        long oracleBase = base.getLong(null);
        Object unsafe = M3HashOracle.call(ACCESS, "unsafe", NO_PARAMS);
        byte[] b = bytes(new SplittableRandom(CORPUS_SEED), ACCESS_WINDOW + Long.BYTES);
        for (ByteOrder order : ORDERS) {
            M3Access<byte[]> m = a.byteOrder(b, order);
            Object o = A_IN_ORDER.invoke(unsafe, (Object) b, order);
            ByteBuffer ref = ByteBuffer.wrap(b).order(order);
            assertEquals(order, m.byteOrder(b));
            for (int i = 0; i < ACCESS_WINDOW; i++) {
                accessRow(m, b, i, o, oracleBase + i, "byte[] " + order);
                eq(ref.getLong(i), m.getLong(b, i), "byte[] vs ByteBuffer", i, order + " getLong");
                eq(ref.getInt(i), m.getInt(b, i), "byte[] vs ByteBuffer", i, order + " getInt");
                eq(ref.getShort(i), m.getShort(b, i), "byte[] vs ByteBuffer", i, order + " getShort");
            }
        }
        assertSame(a, a.byteOrder(b, ByteOrder.LITTLE_ENDIAN));
        assertThrows(IndexOutOfBoundsException.class, () -> a.getLong(b, b.length - Long.BYTES + 1));
        report("byteArrayAccess");
    }

    @Test
    void byteBufferAccess() throws Throwable {
        M3Access<ByteBuffer> a = M3Access.toByteBuffer();
        Object oracle = M3HashOracle.call(ACCESS, "toByteBuffer", NO_PARAMS);
        byte[] b = bytes(new SplittableRandom(CORPUS_SEED), ACCESS_WINDOW + Long.BYTES);
        ByteBuffer direct = ByteBuffer.allocateDirect(b.length).put(0, b);
        for (ByteBuffer buffer : List.of(ByteBuffer.wrap(b), ByteBuffer.wrap(b).asReadOnlyBuffer(), direct)) {
            for (ByteOrder order : ORDERS) {
                ByteBuffer in = buffer.duplicate().order(order);
                eqObject(A_ORDER.invoke(oracle, (Object) in), a.byteOrder(in), "ByteBuffer byteOrder");
                for (ByteOrder view : ORDERS) {
                    M3Access<ByteBuffer> m = a.byteOrder(in, view);
                    Object o = A_IN_ORDER.invoke(oracle, (Object) in, view);
                    for (int i = 0; i < ACCESS_WINDOW; i++) {
                        accessRow(m, in, i, o, i, "ByteBuffer " + order + " as " + view);
                    }
                }
            }
        }
        report("byteBufferAccess");
    }

    @Test
    void charSequenceAccess() throws Throwable {
        String s = utf16(new SplittableRandom(CORPUS_SEED), CHAR_WINDOW);
        M3Access<CharSequence> nat = M3Access.toNativeCharSequence();
        Object oracleNative = M3HashOracle.call(ACCESS, "toNativeCharSequence", NO_PARAMS);
        eqObject(A_ORDER.invoke(oracleNative, (Object) s), nat.byteOrder(s), "native CharSequence byteOrder");
        for (ByteOrder order : ORDERS) {
            M3Access<CharSequence> a = M3Access.toCharSequence(order);
            Object oracle = M3HashOracle.call(ACCESS, "toCharSequence", new Class<?>[] {ByteOrder.class}, order);
            for (CharSequence in : List.of(s, new StringBuilder(s))) {
                for (ByteOrder view : ORDERS) {
                    M3Access<CharSequence> m = a.byteOrder(in, view);
                    Object o = A_IN_ORDER.invoke(oracle, (Object) in, view);
                    for (int i = 0; i + Long.BYTES <= 2 * CHAR_WINDOW; i++) {
                        accessRow(m, in, i, o, i, "CharSequence " + order + " as " + view);
                    }
                }
            }
        }
        report("charSequenceAccess");
    }

    @Test
    void accessTemplate() {
        assertEquals(surface(M3HashOracle.type(ACCESS), DROPPED_ACCESS), surface(M3Access.class, Set.of()));
        byte[] b = bytes(new SplittableRandom(CORPUS_SEED), ACCESS_WINDOW + Long.BYTES);
        for (ByteOrder order : ORDERS) {
            M3Access<byte[]> t = bytewise(order);
            ByteBuffer ref = ByteBuffer.wrap(b).order(order);
            for (int i = 0; i < ACCESS_WINDOW; i++) {
                eq(ref.getLong(i), t.getLong(b, i), "template", i, order + " getLong");
                eq(ref.getLong(i), t.i64(b, i), "template", i, order + " i64");
                eq(Integer.toUnsignedLong(ref.getInt(i)), t.getUnsignedInt(b, i), "template", i, order + " getUnsignedInt");
                eq(Integer.toUnsignedLong(ref.getInt(i)), t.u32(b, i), "template", i, order + " u32");
                eq(ref.getInt(i), t.getInt(b, i), "template", i, order + " getInt");
                eq(ref.getInt(i), t.i32(b, i), "template", i, order + " i32");
                eq(Short.toUnsignedInt(ref.getShort(i)), t.getUnsignedShort(b, i), "template", i, order + " getUnsignedShort");
                eq(Short.toUnsignedInt(ref.getShort(i)), t.u16(b, i), "template", i, order + " u16");
                eq(ref.getShort(i), t.getShort(b, i), "template", i, order + " getShort");
                eq(ref.getShort(i), t.i16(b, i), "template", i, order + " i16");
                eq(Byte.toUnsignedInt(b[i]), t.getUnsignedByte(b, i), "template", i, order + " getUnsignedByte");
                eq(Byte.toUnsignedInt(b[i]), t.u8(b, i), "template", i, order + " u8");
                eq(b[i], t.i8(b, i), "template", i, order + " i8");
            }
        }
        report("accessTemplate");
    }

    // ---- the kernel atoms ----------------------------------------------------------------------------------------

    @Test
    void maths() throws Throwable {
        Class<?> maths = Class.forName(M3 + "M3Maths");
        MethodHandles.Lookup l = MethodHandles.privateLookupIn(maths, MethodHandles.lookup());
        MethodType binary = MethodType.methodType(long.class, long.class, long.class);
        MethodHandle fold = l.findStatic(maths, "unsignedLongMulXorFold", binary);
        MethodHandle high = l.findStatic(maths, "unsignedLongMulHigh", binary);
        List<Object> impls = new ArrayList<>();
        for (String type : List.of("M3Maths", "M3MathsJDK9")) {
            Class<?> c = Class.forName(M3 + type);
            impls.add(MethodHandles.privateLookupIn(c, MethodHandles.lookup())
                    .findConstructor(c, MethodType.methodType(void.class)).invoke());
        }
        MethodHandle foldImp = l.findVirtual(maths, "unsignedLongMulXorFoldImp", binary);
        MethodHandle highImp = l.findVirtual(maths, "unsignedLongMulHighImp", binary);
        SplittableRandom r = new SplittableRandom(CORPUS_SEED);
        for (int i = 0; i < MATH_SAMPLES + EDGE_VALUES.length * EDGE_VALUES.length; i++) {
            long x = i < MATH_SAMPLES ? r.nextLong() : EDGE_VALUES[(i - MATH_SAMPLES) / EDGE_VALUES.length];
            long y = i < MATH_SAMPLES ? r.nextLong() : EDGE_VALUES[(i - MATH_SAMPLES) % EDGE_VALUES.length];
            long refHigh = Math.unsignedMultiplyHigh(x, y);
            long refFold = (x * y) ^ refHigh;
            eq(refHigh, (long) high.invokeExact(x, y), "M3Maths", i, "unsignedLongMulHigh");
            eq(refFold, (long) fold.invokeExact(x, y), "M3Maths", i, "unsignedLongMulXorFold");
            for (Object impl : impls) {
                eq(refHigh, (long) highImp.invoke(impl, x, y), impl.getClass().getSimpleName(), i, "unsignedLongMulHighImp");
                eq(refFold, (long) foldImp.invoke(impl, x, y), impl.getClass().getSimpleName(), i, "unsignedLongMulXorFoldImp");
            }
        }
        report("maths");
    }

    @Test
    void primitives() throws Throwable {
        Class<?> p = Class.forName(M3 + "M3Primitives");
        MethodHandles.Lookup l = MethodHandles.privateLookupIn(p, MethodHandles.lookup());
        boolean little = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;
        assertEquals(little, (boolean) l.findStaticGetter(p, "NATIVE_LITTLE_ENDIAN", boolean.class).invokeExact());
        MethodHandle uInt = l.findStatic(p, "unsignedInt", MethodType.methodType(long.class, int.class));
        MethodHandle uShort = l.findStatic(p, "unsignedShort", MethodType.methodType(int.class, int.class));
        MethodHandle uByte = l.findStatic(p, "unsignedByte", MethodType.methodType(int.class, int.class));
        MethodHandle leLong = l.findStatic(p, "nativeToLittleEndian", MethodType.methodType(long.class, long.class));
        MethodHandle leInt = l.findStatic(p, "nativeToLittleEndian", MethodType.methodType(int.class, int.class));
        MethodHandle leShort = l.findStatic(p, "nativeToLittleEndian", MethodType.methodType(short.class, short.class));
        MethodHandle leChar = l.findStatic(p, "nativeToLittleEndian", MethodType.methodType(char.class, char.class));
        MethodHandle beLong = l.findStatic(p, "nativeToBigEndian", MethodType.methodType(long.class, long.class));
        MethodHandle beInt = l.findStatic(p, "nativeToBigEndian", MethodType.methodType(int.class, int.class));
        MethodHandle beShort = l.findStatic(p, "nativeToBigEndian", MethodType.methodType(short.class, short.class));
        MethodHandle beChar = l.findStatic(p, "nativeToBigEndian", MethodType.methodType(char.class, char.class));
        SplittableRandom r = new SplittableRandom(CORPUS_SEED);
        for (int i = 0; i < PRIMITIVE_SAMPLES; i++) {
            long v = i < EDGE_VALUES.length ? EDGE_VALUES[i] : r.nextLong();
            eq(Integer.toUnsignedLong((int) v), (long) uInt.invokeExact((int) v), "M3Primitives", i, "unsignedInt");
            eq(Short.toUnsignedInt((short) v), (int) uShort.invokeExact((int) (short) v), "M3Primitives", i, "unsignedShort");
            eq(Byte.toUnsignedInt((byte) v), (int) uByte.invokeExact((int) (byte) v), "M3Primitives", i, "unsignedByte");
            eq(little ? v : Long.reverseBytes(v), (long) leLong.invokeExact(v), "M3Primitives", i, "nativeToLittleEndian(long)");
            eq(little ? Long.reverseBytes(v) : v, (long) beLong.invokeExact(v), "M3Primitives", i, "nativeToBigEndian(long)");
            int iv = (int) v;
            eq(little ? iv : Integer.reverseBytes(iv), (int) leInt.invokeExact(iv), "M3Primitives", i, "nativeToLittleEndian(int)");
            eq(little ? Integer.reverseBytes(iv) : iv, (int) beInt.invokeExact(iv), "M3Primitives", i, "nativeToBigEndian(int)");
            short sv = (short) v;
            eq(little ? sv : Short.reverseBytes(sv), (short) leShort.invokeExact(sv), "M3Primitives", i, "nativeToLittleEndian(short)");
            eq(little ? Short.reverseBytes(sv) : sv, (short) beShort.invokeExact(sv), "M3Primitives", i, "nativeToBigEndian(short)");
            char cv = (char) v;
            eq(little ? cv : Character.reverseBytes(cv), (char) leChar.invokeExact(cv), "M3Primitives", i, "nativeToLittleEndian(char)");
            eq(little ? Character.reverseBytes(cv) : cv, (char) beChar.invokeExact(cv), "M3Primitives", i, "nativeToBigEndian(char)");
        }
        report("primitives");
    }

    @Test
    void util() throws Throwable {
        Class<?> u = Class.forName(M3 + "M3Util");
        MethodType check = MethodType.methodType(void.class, int.class, int.class, int.class);
        MethodHandle m3 = MethodHandles.privateLookupIn(u, MethodHandles.lookup()).findStatic(u, "checkArrayOffs", check);
        Method oracle = M3HashOracle.type("Util").getDeclaredMethod("checkArrayOffs", int.class, int.class, int.class);
        oracle.setAccessible(true);
        int[] values = {Integer.MIN_VALUE, -1, 0, 1, Long.BYTES, ACCESS_WINDOW, Integer.MAX_VALUE};
        for (int length : values) {
            for (int off : values) {
                for (int len : values) {
                    if (length < 0) {
                        continue;
                    }
                    // a block, so that invokeExact is an expression statement of type (int,int,int)void
                    sameThrow(() -> oracle.invoke(null, length, off, len), () -> {
                        m3.invokeExact(length, off, len);
                    });
                }
            }
        }
        report("util");
    }

    // ---- differential drivers ------------------------------------------------------------------------------------

    private static void differential(List<Fn> fns) throws Throwable {
        SplittableRandom r = new SplittableRandom(CORPUS_SEED);
        ByteBuffer direct = ByteBuffer.allocateDirect(MAX_LENGTH + SLICE_PAD);
        M3Access<CharSequence> be = M3Access.toCharSequence(ByteOrder.BIG_ENDIAN);
        M3Access<CharSequence> le = M3Access.toCharSequence(ByteOrder.LITTLE_ENDIAN);
        Class<?>[] byOrder = {ByteOrder.class};
        Object oracleBe = M3HashOracle.call(ACCESS, "toCharSequence", byOrder, ByteOrder.BIG_ENDIAN);
        Object oracleLe = M3HashOracle.call(ACCESS, "toCharSequence", byOrder, ByteOrder.LITTLE_ENDIAN);
        for (int len = 0; len <= MAX_LENGTH; len++) {
            byte[] padded = bytes(r, len + SLICE_PAD);
            int off = 1 + len % (SLICE_PAD - 1);
            byte[] exact = Arrays.copyOfRange(padded, off, off + len);
            ByteOrder order = ORDERS[len & 1];
            ByteBuffer positioned = ByteBuffer.wrap(padded).order(order).position(off).limit(off + len);
            ByteBuffer readOnly = ByteBuffer.wrap(padded).asReadOnlyBuffer().order(order);
            ByteBuffer d = direct.duplicate().order(order).put(0, padded);
            String latin1 = new String(exact, StandardCharsets.ISO_8859_1);
            String utf16 = utf16(r, len);
            StringBuilder sb = new StringBuilder(utf16);
            int cOff = len / 3;
            int cLen = len - cOff;
            long bytesOfChars = 2L * len;
            for (Fn f : fns) {
                Object o = f.oracle();
                M3LongHashFunction m = f.m3();
                String n = f.name();
                eq((long) O_BYTES.invoke(o, exact), m.hashBytes(exact), n, len, "hashBytes(byte[])");
                eq((long) O_BYTES_SLICE.invoke(o, padded, off, len), m.hashBytes(padded, off, len), n, len, "hashBytes(byte[], off, len)");
                eq((long) O_BUFFER.invoke(o, positioned), m.hashBytes(positioned), n, len, "hashBytes(heap ByteBuffer " + order + ")");
                eq((long) O_BUFFER_SLICE.invoke(o, readOnly, off, len), m.hashBytes(readOnly, off, len), n, len, "hashBytes(read-only ByteBuffer " + order + ", off, len)");
                eq((long) O_BUFFER_SLICE.invoke(o, d, off, len), m.hashBytes(d, off, len), n, len, "hashBytes(direct ByteBuffer " + order + ", off, len)");
                eq((long) O_STRING.invoke(o, latin1), m.hashChars(latin1), n, len, "hashChars(Latin-1 String)");
                eq((long) O_STRING.invoke(o, utf16), m.hashChars(utf16), n, len, "hashChars(UTF-16 String)");
                eq((long) O_STRING_SLICE.invoke(o, utf16, cOff, cLen), m.hashChars(utf16, cOff, cLen), n, len, "hashChars(String, off, len)");
                eq((long) O_BUILDER.invoke(o, sb), m.hashChars(sb), n, len, "hashChars(StringBuilder)");
                eq((long) O_BUILDER_SLICE.invoke(o, sb, cOff, cLen), m.hashChars(sb, cOff, cLen), n, len, "hashChars(StringBuilder, off, len)");
                eq((long) O_ACCESS.invoke(o, (Object) sb, oracleBe, 0L, bytesOfChars), m.hash(sb, be, 0L, bytesOfChars), n, len, "hash(CharSequence, BIG_ENDIAN access)");
                eq((long) O_ACCESS.invoke(o, (Object) sb, oracleLe, 0L, bytesOfChars), m.hash(sb, le, 0L, bytesOfChars), n, len, "hash(CharSequence, LITTLE_ENDIAN access)");
            }
        }
        for (Fn f : fns) {
            Object o = f.oracle();
            M3LongHashFunction m = f.m3();
            eq((long) O_VOID.invoke(o), m.hashVoid(), f.name(), 0, "hashVoid()");
            for (int i = 0; i < PRIMITIVE_SAMPLES; i++) {
                long v = i < EDGE_VALUES.length ? EDGE_VALUES[i] : r.nextLong();
                eq((long) O_LONG.invoke(o, v), m.hashLong(v), f.name(), i, "hashLong");
                eq((long) O_INT.invoke(o, (int) v), m.hashInt((int) v), f.name(), i, "hashInt");
                eq((long) O_SHORT.invoke(o, (short) v), m.hashShort((short) v), f.name(), i, "hashShort");
                eq((long) O_CHAR.invoke(o, (char) v), m.hashChar((char) v), f.name(), i, "hashChar");
                eq((long) O_BYTE.invoke(o, (byte) v), m.hashByte((byte) v), f.name(), i, "hashByte");
            }
        }
        report(fns.get(0).name() + " x " + fns.size() + " functions");
    }

    private static void tupleDifferential(List<TupleFn> fns) throws Throwable {
        SplittableRandom r = new SplittableRandom(CORPUS_SEED);
        ByteBuffer direct = ByteBuffer.allocateDirect(MAX_LENGTH + SLICE_PAD);
        M3Access<CharSequence> be = M3Access.toCharSequence(ByteOrder.BIG_ENDIAN);
        Object oracleBe = M3HashOracle.call(ACCESS, "toCharSequence", new Class<?>[] {ByteOrder.class}, ByteOrder.BIG_ENDIAN);
        for (int len = 0; len <= MAX_LENGTH; len++) {
            byte[] padded = bytes(r, len + SLICE_PAD);
            int off = 1 + len % (SLICE_PAD - 1);
            byte[] exact = Arrays.copyOfRange(padded, off, off + len);
            ByteOrder order = ORDERS[len & 1];
            ByteBuffer readOnly = ByteBuffer.wrap(padded).asReadOnlyBuffer().order(order);
            ByteBuffer d = direct.duplicate().order(order).put(0, padded);
            String latin1 = new String(exact, StandardCharsets.ISO_8859_1);
            StringBuilder sb = new StringBuilder(utf16(r, len));
            for (TupleFn f : fns) {
                Object o = f.oracle();
                M3LongTupleHashFunction m = f.m3();
                String n = f.name();
                eqArray((long[]) T_BYTES.invoke(o, exact), m.hashBytes(exact), n, len, "hashBytes(byte[])");
                eqArray((long[]) T_BYTES_SLICE.invoke(o, padded, off, len), m.hashBytes(padded, off, len), n, len, "hashBytes(byte[], off, len)");
                eqArray((long[]) T_BUFFER_SLICE.invoke(o, readOnly, off, len), m.hashBytes(readOnly, off, len), n, len, "hashBytes(read-only ByteBuffer, off, len)");
                eqArray((long[]) T_BUFFER_SLICE.invoke(o, d, off, len), m.hashBytes(d, off, len), n, len, "hashBytes(direct ByteBuffer, off, len)");
                eqArray((long[]) T_STRING.invoke(o, latin1), m.hashChars(latin1), n, len, "hashChars(Latin-1 String)");
                eqArray((long[]) T_CHARS.invoke(o, (CharSequence) sb), m.hashChars(sb), n, len, "hashChars(StringBuilder)");
                eqArray((long[]) T_ACCESS.invoke(o, (Object) sb, oracleBe, 0L, 2L * len), m.hash(sb, be, 0L, 2L * len), n, len, "hash(CharSequence, BIG_ENDIAN access)");
            }
        }
        for (TupleFn f : fns) {
            Object o = f.oracle();
            M3LongTupleHashFunction m = f.m3();
            eqArray((long[]) T_VOID.invoke(o), m.hashVoid(), f.name(), 0, "hashVoid()");
            for (int i = 0; i < PRIMITIVE_SAMPLES; i++) {
                long v = i < EDGE_VALUES.length ? EDGE_VALUES[i] : r.nextLong();
                eqArray((long[]) T_LONG.invoke(o, v), m.hashLong(v), f.name(), i, "hashLong");
                eqArray((long[]) T_INT.invoke(o, (int) v), m.hashInt((int) v), f.name(), i, "hashInt");
                eqArray((long[]) T_SHORT.invoke(o, (short) v), m.hashShort((short) v), f.name(), i, "hashShort");
                eqArray((long[]) T_CHAR.invoke(o, (char) v), m.hashChar((char) v), f.name(), i, "hashChar");
                eqArray((long[]) T_BYTE.invoke(o, (byte) v), m.hashByte((byte) v), f.name(), i, "hashByte");
            }
        }
        report(fns.get(0).name() + " tuple x " + fns.size() + " functions");
    }

    // ---- helpers -------------------------------------------------------------------------------------------------

    private static long[] hashSeeds() {
        SplittableRandom r = new SplittableRandom(CORPUS_SEED);
        long[] s = Arrays.copyOf(FIXED_HASH_SEEDS, FIXED_HASH_SEEDS.length + RANDOM_HASH_SEEDS);
        for (int i = FIXED_HASH_SEEDS.length; i < s.length; i++) {
            s[i] = r.nextLong();
        }
        return s;
    }

    private static List<Fn> functions(String factory, M3LongHashFunction seedless, LongFunction<M3LongHashFunction> seeded,
                                      TwoSeeds twoSeeds) {
        List<Fn> fns = new ArrayList<>();
        fns.add(new Fn(factory + "()", seedless, M3HashOracle.call(LHF, factory, NO_PARAMS)));
        for (long s : SEEDS) {
            fns.add(new Fn(factory + "(" + s + ")", seeded.apply(s), M3HashOracle.call(LHF, factory, ONE_LONG, s)));
            if (twoSeeds != null) {
                fns.add(new Fn(factory + "(" + s + ", " + ~s + ")", twoSeeds.of(s, ~s),
                        M3HashOracle.call(LHF, factory, TWO_LONGS, s, ~s)));
            }
        }
        return fns;
    }

    private static List<TupleFn> tupleFunctions(String factory, M3LongTupleHashFunction seedless,
                                                LongFunction<M3LongTupleHashFunction> seeded) {
        List<TupleFn> fns = new ArrayList<>();
        fns.add(new TupleFn(factory + "()", seedless, M3HashOracle.call(LTHF, factory, NO_PARAMS)));
        for (long s : SEEDS) {
            fns.add(new TupleFn(factory + "(" + s + ")", seeded.apply(s), M3HashOracle.call(LTHF, factory, ONE_LONG, s)));
        }
        return fns;
    }

    private static Set<String> droppedTuple() {
        Set<String> s = new TreeSet<>(Set.of("void hashBoolean(boolean,long[])", "long[] hashBoolean(boolean)",
                "void hashMemory(long,long,long[])", "long[] hashMemory(long,long)"));
        for (String m : List.of("hashBooleans(boolean[]", "hashChars(char[]", "hashShorts(short[]", "hashInts(int[]",
                "hashLongs(long[]")) {
            s.addAll(List.of("void " + m + ",long[])", "long[] " + m + ")", "void " + m + ",int,int,long[])",
                    "long[] " + m + ",int,int)"));
        }
        return s;
    }

    /** Public and protected methods as "[static ]return name(params)", donor types named as their M3 types. */
    private static Set<String> surface(Class<?> c, Set<String> dropped) {
        Set<String> s = Stream.of(c.getDeclaredMethods())
                .filter(m -> !m.isSynthetic() && (m.getModifiers() & (Modifier.PUBLIC | Modifier.PROTECTED)) != 0)
                .map(m -> (Modifier.isStatic(m.getModifiers()) ? "static " : "") + name(m.getReturnType()) + " "
                        + m.getName() + Stream.of(m.getParameterTypes()).map(M3HashAtomsTest::name)
                        .collect(Collectors.joining(",", "(", ")")))
                .collect(Collectors.toCollection(TreeSet::new));
        assertTrue(s.containsAll(dropped), () -> c + " lacks dropped members " + dropped);
        s.removeAll(dropped);
        return s;
    }

    private static String name(Class<?> t) {
        String simple = t.getSimpleName();
        boolean donor = t.getName().startsWith(M3HashOracle.PKG);
        return donor && !simple.startsWith(M3_PREFIX) ? M3_PREFIX + simple : simple;
    }

    private static M3Access<byte[]> bytewise(ByteOrder order) {
        return new M3Access<>() {
            @Override
            public int getByte(byte[] input, long offset) {
                return input[(int) offset];
            }

            @Override
            public ByteOrder byteOrder(byte[] input) {
                return order;
            }

            @Override
            protected M3Access<byte[]> reverseAccess() {
                return bytewise(order == ByteOrder.BIG_ENDIAN ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
            }
        };
    }

    private static <T> void accessRow(M3Access<T> m, T in, int i, Object oracle, long oracleOffset, String what)
            throws Throwable {
        eq((long) A_LONG.invoke(oracle, (Object) in, oracleOffset), m.getLong(in, i), what, i, "getLong");
        eq((long) A_UINT.invoke(oracle, (Object) in, oracleOffset), m.getUnsignedInt(in, i), what, i, "getUnsignedInt");
        eq((int) A_INT.invoke(oracle, (Object) in, oracleOffset), m.getInt(in, i), what, i, "getInt");
        eq((int) A_USHORT.invoke(oracle, (Object) in, oracleOffset), m.getUnsignedShort(in, i), what, i, "getUnsignedShort");
        eq((int) A_SHORT.invoke(oracle, (Object) in, oracleOffset), m.getShort(in, i), what, i, "getShort");
        eq((int) A_UBYTE.invoke(oracle, (Object) in, oracleOffset), m.getUnsignedByte(in, i), what, i, "getUnsignedByte");
        eq((int) A_BYTE.invoke(oracle, (Object) in, oracleOffset), m.getByte(in, i), what, i, "getByte");
        eq(m.getLong(in, i), m.i64(in, i), what, i, "i64");
        eq(m.getUnsignedInt(in, i), m.u32(in, i), what, i, "u32");
        eq(m.getInt(in, i), m.i32(in, i), what, i, "i32");
        eq(m.getUnsignedShort(in, i), m.u16(in, i), what, i, "u16");
        eq(m.getShort(in, i), m.i16(in, i), what, i, "i16");
        eq(m.getUnsignedByte(in, i), m.u8(in, i), what, i, "u8");
        eq(m.getByte(in, i), m.i8(in, i), what, i, "i8");
        eqObject(A_ORDER.invoke(oracle, (Object) in), m.byteOrder(in), what + " byteOrder");
    }

    private static byte[] bytes(SplittableRandom r, int n) {
        byte[] b = new byte[n];
        r.nextBytes(b);
        return b;
    }

    private static String utf16(SplittableRandom r, int n) {
        char[] c = new char[n];
        for (int i = 0; i < n; i++) {
            c[i] = (char) r.nextInt(UTF16_BOUND);
        }
        return new String(c);
    }

    private static void eq(long oracle, long m3, String fn, int at, String form) {
        checks++;
        if (oracle != m3) {
            fail(fn + " " + form + " at " + at + ": vendored " + oracle + ", absorbed " + m3);
        }
    }

    private static void eqArray(long[] oracle, long[] m3, String fn, int at, String form) {
        checks++;
        if (!Arrays.equals(oracle, m3)) {
            fail(fn + " " + form + " at " + at + ": vendored " + Arrays.toString(oracle) + ", absorbed " + Arrays.toString(m3));
        }
    }

    private static void eqObject(Object oracle, Object m3, String what) {
        checks++;
        assertEquals(oracle, m3, what);
    }

    interface Call {
        void run() throws Throwable;
    }

    /** Both sides throw the same exception class, or both return normally. */
    private static void sameThrow(Call oracle, Call m3) {
        checks++;
        assertEquals(outcome(oracle), outcome(m3));
    }

    private static String outcome(Call c) {
        try {
            c.run();
            return "returned";
        } catch (java.lang.reflect.InvocationTargetException e) {
            return e.getCause().getClass().getName();
        } catch (Throwable e) {
            return e.getClass().getName();
        }
    }

    private static void report(String what) {
        System.out.println(what + ": " + checks + " comparisons so far");
    }
}
