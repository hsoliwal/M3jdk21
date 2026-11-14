// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package jdk.internal.m3.collections.packed;

import jdk.m3.arrays.IntWidth;
import jdk.m3.arrays.packed.PackedIntArray;
import jdk.m3.arrays.packed.VInt;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Random;
import java.util.concurrent.CancellationException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Explicit native-suite opt-in for ordinary Java builds. The dedicated verifier
 * always supplies the library path and refuses any skipped native test.
 */
@EnabledIfSystemProperty(named = "lint.native.path", matches = ".+")
class PackedIntNativeTest {
    @BeforeAll
    static void loadExplicitlyQualifiedNativeLibrary() {
        String library = System.getProperty("lint.native.path");
        assertNotNull(library, "set -Dlint.native.path to the library built from packed_int_width.c");
        assertTrue(Path.of(library).isAbsolute(), "native path must be explicit and absolute");
        System.load(library);
    }

    @Test
    void nativeBatchesMatchMathematicalBytesAndJavaForEveryWidthAndSign() {
        Random random = new Random(0x4e41_5449_5645L);
        for (IntWidth width : IntWidth.values()) {
            for (boolean signed : new boolean[] {true, false}) {
                long[] values = new long[4097];
                for (int i = 0; i < values.length; i++) values[i] = PackedIntLineageTest.randomValue(random, width.bits(), signed);
                values[0] = width.minValue(signed);
                values[values.length - 1] = width.maxValue(signed);
                byte[] expected = PackedIntLineageTest.independentBytes(values, width.bytes());
                byte[] encoded = PackedIntNative.encode(values, 0, values.length, width, signed, null);
                assertEquals(values.length * width.bytes(), encoded.length);
                assertArrayEquals(expected, encoded);
                assertArrayEquals(values, PackedIntNative.decode(expected, width, signed, null));
                PackedIntArray javaOwner = PackedIntArray.of(values.length, width, signed);
                javaOwner.copyFrom(values, 0, 0, values.length, null);
                assertArrayEquals(javaOwner.toByteArray(null), encoded);
            }
        }
    }

    @Test
    void nativeOffsetsAndOddTailsHaveNoPaddingOrOverread() {
        int[] counts = {0, 1, 2, 3, 1023, 1024, 1025, 2049};
        for (IntWidth width : IntWidth.values()) {
            for (int count : counts) {
                long[] input = new long[count + 4];
                Arrays.fill(input, Long.MAX_VALUE);
                long[] selected = new long[count];
                for (int i = 0; i < count; i++) selected[i] = input[i + 2] = i % 2 == 0 ? i : -i;
                byte[] bytes = PackedIntNative.encode(input, 2, count, width, true, null);
                assertEquals(count * width.bytes(), bytes.length);
                assertArrayEquals(PackedIntLineageTest.independentBytes(selected, width.bytes()), bytes);
                assertArrayEquals(selected, PackedIntNative.decode(bytes, width, true, null));
                assertEquals(Long.MAX_VALUE, input[0]);
                assertEquals(Long.MAX_VALUE, input[input.length - 1]);
            }
        }
    }

    @Test
    void nativeLiteralFixturesPreserveSignExtensionAndUnsigned64() {
        byte[] negative48 = {0, 0, 0, 0, 0, (byte) 0x80, -1, -1, -1, -1, -1, -1};
        assertArrayEquals(new long[] {-(1L << 47), -1}, PackedIntNative.decode(negative48, IntWidth.INT48, true, null));
        assertArrayEquals(new long[] {1L << 47, (1L << 48) - 1}, PackedIntNative.decode(negative48, IntWidth.INT48, false, null));
        long[] raw64 = {Long.MIN_VALUE, Long.MAX_VALUE, -1, 0};
        byte[] encoded = PackedIntNative.encode(raw64, 0, raw64.length, IntWidth.INT64, false, null);
        assertArrayEquals(PackedIntLineageTest.independentBytes(raw64, 8), encoded);
        assertArrayEquals(raw64, PackedIntNative.decode(encoded, IntWidth.INT64, false, null));
    }

    @Test
    void nativeInputValidationRejectsBadWidthsValuesAndRanges() {
        assertThrows(NullPointerException.class,
                () -> PackedIntNative.encode(null, 0, 0, IntWidth.INT48, true, null));
        assertThrows(NullPointerException.class,
                () -> PackedIntNative.encode(new long[0], 0, 0, null, true, null));
        assertThrows(NullPointerException.class,
                () -> PackedIntNative.decode(null, IntWidth.INT48, true, null));
        assertThrows(IndexOutOfBoundsException.class,
                () -> PackedIntNative.encode(new long[2], Integer.MAX_VALUE, 1, IntWidth.INT48, true, null));
        assertThrows(IndexOutOfBoundsException.class,
                () -> PackedIntNative.encode(new long[2], 0, -1, IntWidth.INT48, true, null));
        assertThrows(IllegalArgumentException.class,
                () -> PackedIntNative.decode(new byte[7], IntWidth.INT48, true, null));
        for (IntWidth width : new IntWidth[] {IntWidth.INT32, IntWidth.INT40, IntWidth.INT48, IntWidth.INT56}) {
            assertThrows(ArithmeticException.class,
                    () -> PackedIntNative.encode(new long[] {width.maxValue(true) + 1}, 0, 1, width, true, null));
            assertThrows(ArithmeticException.class,
                    () -> PackedIntNative.encode(new long[] {-1}, 0, 1, width, false, null));
        }
    }

    @Test
    void cancellationDoesNotMutateNativeInputArrays() {
        long[] values = new long[8193];
        Arrays.fill(values, -19);
        long[] before = values.clone();
        assertThrows(CancellationException.class,
                () -> PackedIntNative.encode(values, 0, values.length, IntWidth.INT48, true,
                        new PackedIntLineageTest.CancelAfter(3)));
        assertArrayEquals(before, values);
        byte[] bytes = PackedIntLineageTest.independentBytes(values, 6);
        byte[] beforeBytes = bytes.clone();
        assertThrows(CancellationException.class,
                () -> PackedIntNative.decode(bytes, IntWidth.INT48, true,
                        new PackedIntLineageTest.CancelAfter(3)));
        assertArrayEquals(beforeBytes, bytes);
    }

    @Test
    void nativeOutputsAreIndependentSnapshots() {
        long[] input = {11, -12};
        byte[] bytes = PackedIntNative.encode(input, 0, input.length, IntWidth.INT48, true, null);
        input[0] = 99;
        long[] decoded = PackedIntNative.decode(bytes, IntWidth.INT48, true, null);
        assertArrayEquals(new long[] {11, -12}, decoded);
        bytes[0] = 23;
        assertEquals(11, decoded[0]);
        decoded[1] = 71;
        assertEquals(-12, PackedIntNative.decode(bytes, IntWidth.INT48, true, null)[1]);
    }

    @Test
    void cancellationAfterACompletedNativeChunkStopsFurtherPublication() {
        long[] values = new long[4097];
        Arrays.fill(values, -17);
        long[] observedWork = {0};
        PackedIntLineageTest.CancelAfter monitor = new PackedIntLineageTest.CancelAfter(Integer.MAX_VALUE) {
            @Override public void worked(long work) {
                observedWork[0] += work;
                setCanceled(true);
            }
        };
        assertThrows(CancellationException.class,
                () -> PackedIntNative.encode(values, 0, values.length, IntWidth.INT48, true, monitor));
        assertTrue(observedWork[0] > 0 && observedWork[0] <= 1024,
                "at least one actual native chunk completed before cancellation");
        for (long value : values) assertEquals(-17, value);
    }

    @Test
    void privateNativeEntrypointsDefendBoundsValuesAndDestinationBuffers() throws ReflectiveOperationException {
        Method encode = PackedIntNative.class.getDeclaredMethod("encodeChunk0", long[].class,
                int.class, int.class, byte[].class, int.class, int.class, boolean.class);
        Method decode = PackedIntNative.class.getDeclaredMethod("decodeChunk0", byte[].class,
                int.class, int.class, long[].class, int.class, int.class, boolean.class);
        encode.setAccessible(true);
        decode.setAccessible(true);
        byte[] bytes = new byte[16];
        Arrays.fill(bytes, (byte) 0x5a);
        byte[] originalBytes = bytes.clone();
        assertNativeFailure(encode, ArithmeticException.class,
                new long[] {1, (long) Integer.MAX_VALUE + 1}, 0, 2, bytes, 0, 32, true);
        assertNativeFailure(encode, ArithmeticException.class,
                new long[] {-1}, 0, 1, bytes, 0, 48, false);
        assertNativeFailure(encode, IllegalArgumentException.class,
                new long[] {1}, 0, 1, bytes, 0, 33, true);
        assertNativeFailure(encode, IllegalArgumentException.class,
                new long[1025], 0, 1025, bytes, 0, 32, true);
        assertNativeFailure(encode, IndexOutOfBoundsException.class,
                new long[] {1}, Integer.MAX_VALUE, 1, bytes, 0, 32, true);
        assertNativeFailure(encode, IndexOutOfBoundsException.class,
                new long[] {1}, 0, 1, bytes, 15, 32, true);
        assertNativeFailure(encode, NullPointerException.class,
                null, 0, 1, bytes, 0, 32, true);
        assertArrayEquals(originalBytes, bytes);
        long[] longs = new long[4];
        Arrays.fill(longs, 0x1234_5678L);
        long[] originalLongs = longs.clone();
        assertNativeFailure(decode, IllegalArgumentException.class, bytes, 0, 1, longs, 0, 33, true);
        assertNativeFailure(decode, IllegalArgumentException.class, bytes, 0, 1025, longs, 0, 32, true);
        assertNativeFailure(decode, IndexOutOfBoundsException.class, bytes, 15, 1, longs, 0, 32, true);
        assertNativeFailure(decode, IndexOutOfBoundsException.class, bytes, 0, 1, longs, 4, 32, true);
        assertNativeFailure(decode, NullPointerException.class, null, 0, 1, longs, 0, 32, true);
        assertArrayEquals(originalLongs, longs);
    }

    @Test
    void nativeVIntMatchesIndependentWireAcrossPowerBoundariesAndRandomValues() {
        long[] values = new long[4480];
        Random random = new Random(0x5649_4e54_4e41_5449L);
        for (int i = 0; i < values.length; i++) values[i] = random.nextLong();
        int cursor = 0;
        for (int bit = 0; bit < Long.SIZE; bit++) {
            long power = 1L << bit;
            for (long value : new long[] {power - 1, power, power + 1, -power - 1, -power, -power + 1}) {
                values[cursor++] = value;
            }
        }
        for (boolean signed : new boolean[] {false, true}) {
            byte[] reference = independentVIntBytes(values, signed);
            assertArrayEquals(reference, PackedIntNative.encodeVInt(values, 0, values.length, signed, null));
            assertArrayEquals(VInt.encode(values, 0, values.length, signed, null), reference);
            assertArrayEquals(values, PackedIntNative.decodeVInt(reference, signed, null));
        }
    }

    @Test
    void nativeVIntLiteralFixturesOffsetsAndWindowCrossingsAreExact() {
        byte[] literal = {0, 1, 2, 3, 127, (byte) 0x80, 1, (byte) 0xff, 127};
        long[] unsigned = {0, 1, 2, 3, 127, 128, 16383};
        long[] signed = {0, -1, 1, -2, -64, 64, -8192};
        assertArrayEquals(unsigned, PackedIntNative.decodeVInt(literal, false, null));
        assertArrayEquals(signed, PackedIntNative.decodeVInt(literal, true, null));
        assertArrayEquals(literal, PackedIntNative.encodeVInt(unsigned, 0, unsigned.length, false, null));
        assertArrayEquals(literal, PackedIntNative.encodeVInt(signed, 0, signed.length, true, null));
        byte[] ten = HexFormat.of().parseHex("ffffffffffffffffff01");
        assertArrayEquals(new long[] {-1}, PackedIntNative.decodeVInt(ten, false, null));
        assertArrayEquals(new long[] {Long.MIN_VALUE}, PackedIntNative.decodeVInt(ten, true, null));
        assertArrayEquals(ten, PackedIntNative.encodeVInt(new long[] {-1}, 0, 1, false, null));
        assertArrayEquals(ten, PackedIntNative.encodeVInt(new long[] {Long.MIN_VALUE}, 0, 1, true, null));

        for (boolean mode : new boolean[] {false, true}) {
            for (int count : new int[] {0, 1, 2, 1023, 1024, 1025, 2049}) {
                long[] input = new long[count + 4];
                Arrays.fill(input, 733);
                long[] selected = new long[count];
                for (int i = 0; i < count; i++) {
                    selected[i] = input[i + 2] = switch (i % 4) {
                        case 0 -> Long.MIN_VALUE;
                        case 1 -> -1;
                        case 2 -> i;
                        default -> (long) i << 42;
                    };
                }
                byte[] expected = independentVIntBytes(selected, mode);
                byte[] actual = PackedIntNative.encodeVInt(input, 2, count, mode, null);
                assertArrayEquals(expected, actual);
                byte[] framed = new byte[actual.length + 6];
                Arrays.fill(framed, (byte) 0x80);
                System.arraycopy(actual, 0, framed, 3, actual.length);
                assertArrayEquals(selected,
                        PackedIntNative.decodeVInt(framed, 3, 3 + actual.length, mode, null));
                assertThrows(IllegalArgumentException.class,
                        () -> PackedIntNative.decodeVInt(framed, mode, null));
                assertEquals(733, input[0]);
                assertEquals(733, input[input.length - 1]);
            }
        }
    }

    @Test
    void nativeVIntRejectsAllBadTenthBytesAndMalformedTailsBeforeOutputWork() {
        byte[] value = new byte[10];
        Arrays.fill(value, (byte) 0xff);
        for (int last = 0; last < 256; last++) {
            value[9] = (byte) last;
            for (boolean signed : new boolean[] {false, true}) {
                if (last == 1) {
                    assertArrayEquals(new long[] {signed ? Long.MIN_VALUE : -1},
                            PackedIntNative.decodeVInt(value, signed, null));
                } else {
                    assertThrows(IllegalArgumentException.class,
                            () -> PackedIntNative.decodeVInt(value, signed, null));
                }
            }
        }
        for (byte[] malformed : new byte[][] {
                {(byte) 0x80}, {(byte) 0x80, 0}, {1, (byte) 0x80},
                {(byte) 0x81, (byte) 0x80, 0}, new byte[2052]}) {
            if (malformed.length == 2052) malformed[malformed.length - 1] = (byte) 0x80;
            byte[] before = malformed.clone();
            NativeWorkMonitor progress = new NativeWorkMonitor();
            assertThrows(IllegalArgumentException.class,
                    () -> PackedIntNative.decodeVInt(malformed, false, progress));
            assertEquals(0, progress.work);
            assertArrayEquals(before, malformed);
        }
    }

    @Test
    void nativeVIntRejectsBadBoundsAndReturnsIndependentExactOutputs() {
        assertThrows(NullPointerException.class,
                () -> PackedIntNative.encodeVInt(null, 0, 0, false, null));
        assertThrows(NullPointerException.class,
                () -> PackedIntNative.decodeVInt(null, false, null));
        assertThrows(NullPointerException.class,
                () -> PackedIntNative.decodeVInt(null, 0, 0, false, null));
        assertThrows(IndexOutOfBoundsException.class,
                () -> PackedIntNative.encodeVInt(new long[2], Integer.MAX_VALUE, 1, false, null));
        assertThrows(IndexOutOfBoundsException.class,
                () -> PackedIntNative.encodeVInt(new long[2], 0, -1, false, null));
        for (int[] range : new int[][] {{-1, 0}, {1, 0}, {0, 3}, {Integer.MAX_VALUE, Integer.MAX_VALUE}}) {
            assertThrows(IndexOutOfBoundsException.class,
                    () -> PackedIntNative.decodeVInt(new byte[2], range[0], range[1], false, null));
        }
        long[] values = {11, -12};
        byte[] wire = PackedIntNative.encodeVInt(values, 0, 2, true, null);
        assertEquals(2, wire.length);
        values[0] = 71;
        long[] decoded = PackedIntNative.decodeVInt(wire, true, null);
        assertArrayEquals(new long[] {11, -12}, decoded);
        wire[0] = 0;
        assertEquals(11, decoded[0]);
        decoded[1] = 99;
        assertEquals(-12, PackedIntNative.decodeVInt(wire, true, null)[1]);
        assertEquals(0, PackedIntNative.encodeVInt(new long[0], 0, 0, true, null).length);
        assertEquals(0, PackedIntNative.decodeVInt(new byte[0], true, null).length);
        assertEquals(0, PackedIntNative.decodeVInt(new byte[] {(byte) 0x80}, 1, 1, true, null).length);
    }

    @Test
    void nativeVIntCancellationCoversEveryCheckpointInBothPasses() {
        long[] values = new long[2053];
        Arrays.fill(values, -17);
        long[] beforeValues = values.clone();
        byte[] payload = independentVIntBytes(values, true);
        byte[] beforePayload = payload.clone();
        for (boolean encode : new boolean[] {true, false}) {
            boolean reachedSuccess = false;
            int refused = 0;
            for (int checkpoint = 1; checkpoint <= 32; checkpoint++) {
                PackedIntLineageTest.CancelAfter monitor = new PackedIntLineageTest.CancelAfter(checkpoint);
                try {
                    if (encode) {
                        assertArrayEquals(payload,
                                PackedIntNative.encodeVInt(values, 0, values.length, true, monitor));
                    } else {
                        assertArrayEquals(values, PackedIntNative.decodeVInt(payload, true, monitor));
                    }
                    reachedSuccess = true;
                    break;
                } catch (CancellationException expected) {
                    refused++;
                }
                assertArrayEquals(beforeValues, values);
                assertArrayEquals(beforePayload, payload);
            }
            assertTrue(reachedSuccess && refused >= 8, "preflight and output checkpoints were exercised");
        }
    }

    @Test
    void nativeVIntProgressIsBoundedAndCallbackCancellationStopsAfterARealChunk() {
        long[] values = new long[2053];
        Arrays.fill(values, Long.MIN_VALUE);
        byte[] wire = independentVIntBytes(values, true);
        for (boolean encode : new boolean[] {true, false}) {
            NativeWorkMonitor success = new NativeWorkMonitor();
            if (encode) {
                assertArrayEquals(wire, PackedIntNative.encodeVInt(values, 0, values.length, true, success));
            } else {
                assertArrayEquals(values, PackedIntNative.decodeVInt(wire, true, success));
            }
            assertEquals(values.length, success.work);
            assertEquals(3, success.workCalls);
            NativeWorkMonitor cancel = new NativeWorkMonitor();
            cancel.cancelAtWork = 1;
            assertThrows(CancellationException.class, () -> {
                if (encode) PackedIntNative.encodeVInt(values, 0, values.length, true, cancel);
                else PackedIntNative.decodeVInt(wire, true, cancel);
            });
            assertEquals(1024, cancel.work, "one actual native output chunk completed");
            NativeWorkMonitor failure = new NativeWorkMonitor();
            failure.workFailure = new IllegalStateException("native VInt callback fixture");
            assertSame(failure.workFailure, assertThrows(IllegalStateException.class, () -> {
                if (encode) PackedIntNative.encodeVInt(values, 0, values.length, true, failure);
                else PackedIntNative.decodeVInt(wire, true, failure);
            }));
        }
    }

    @Test
    void privateVIntPreflightReportsExactCountsAndBoundedPositions() throws ReflectiveOperationException {
        Method size = PackedIntNative.class.getDeclaredMethod("sizeVIntChunk0",
                long[].class, int.class, int.class, boolean.class);
        Method scan = PackedIntNative.class.getDeclaredMethod("scanVIntChunk0",
                byte[].class, int.class, int.class);
        size.setAccessible(true);
        scan.setAccessible(true);
        long[] values = new long[1024];
        Arrays.fill(values, -1);
        assertEquals(10240, size.invoke(null, values, 0, values.length, false));
        assertEquals(1024, size.invoke(null, values, 0, values.length, true));
        assertEquals(0, size.invoke(null, values, values.length, 0, true));
        byte[] framed = new byte[1032];
        Arrays.fill(framed, (byte) 1);
        long first = (long) scan.invoke(null, framed, 3, 1030);
        assertEquals(1027, first >>> 32);
        assertEquals(1024, (int) first);
        long tail = (long) scan.invoke(null, framed, 1027, 1030);
        assertEquals(1030, tail >>> 32);
        assertEquals(3, (int) tail);
        long empty = (long) scan.invoke(null, framed, 1030, 1030);
        assertEquals(1030, empty >>> 32);
        assertEquals(0, (int) empty);
        assertNativeFailure(size, NullPointerException.class, null, 0, 0, false);
        assertNativeFailure(size, IllegalArgumentException.class, values, 0, -1, true);
        assertNativeFailure(size, IllegalArgumentException.class, values, 0, 1025, true);
        assertNativeFailure(size, IndexOutOfBoundsException.class, values, Integer.MAX_VALUE, 1, true);
        assertNativeFailure(scan, NullPointerException.class, null, 0, 0);
        assertNativeFailure(scan, IndexOutOfBoundsException.class, framed, -1, 0);
        assertNativeFailure(scan, IndexOutOfBoundsException.class, framed, Integer.MIN_VALUE, Integer.MAX_VALUE);
        assertNativeFailure(scan, IndexOutOfBoundsException.class, framed, 0, Integer.MAX_VALUE);
        assertNativeFailure(scan, IndexOutOfBoundsException.class, framed, 2, 1);
        assertNativeFailure(scan, IllegalArgumentException.class, new byte[] {(byte) 0x80, 0}, 0, 2);
    }

    @Test
    void privateVIntCodecRefusalsPreserveEveryDestinationSentinel() throws ReflectiveOperationException {
        Method encode = PackedIntNative.class.getDeclaredMethod("encodeVIntChunk0", long[].class,
                int.class, int.class, byte[].class, int.class, int.class, boolean.class);
        Method decode = PackedIntNative.class.getDeclaredMethod("decodeVIntChunk0", byte[].class,
                int.class, int.class, int.class, long[].class, int.class, boolean.class);
        encode.setAccessible(true);
        decode.setAccessible(true);
        byte[] bytes = new byte[16];
        Arrays.fill(bytes, (byte) 0x5a);
        byte[] beforeBytes = bytes.clone();
        assertNativeFailure(encode, NullPointerException.class, null, 0, 0, bytes, 0, 16, true);
        assertNativeFailure(encode, NullPointerException.class, new long[0], 0, 0, null, 0, 0, true);
        assertNativeFailure(encode, IllegalArgumentException.class, new long[1025], 0, 1025, bytes, 0, 16, true);
        assertNativeFailure(encode, IllegalArgumentException.class, new long[0], 0, -1, bytes, 0, 16, true);
        assertNativeFailure(encode, IndexOutOfBoundsException.class, new long[] {1}, -1, 1, bytes, 0, 16, true);
        assertNativeFailure(encode, IndexOutOfBoundsException.class, new long[] {1}, 0, 1, bytes, 3, 2, true);
        assertNativeFailure(encode, IndexOutOfBoundsException.class,
                new long[] {1}, 0, 1, bytes, Integer.MIN_VALUE, Integer.MAX_VALUE, true);
        assertNativeFailure(encode, IndexOutOfBoundsException.class,
                new long[] {1, -1}, 0, 2, bytes, 3, 5, false);
        assertArrayEquals(beforeBytes, bytes, "late encoded capacity failure must not publish the first value");

        long[] longs = {11, 23, 37, 41};
        long[] beforeLongs = longs.clone();
        assertNativeFailure(decode, NullPointerException.class, null, 0, 0, 0, longs, 0, true);
        assertNativeFailure(decode, NullPointerException.class, new byte[0], 0, 0, 0, null, 0, true);
        assertNativeFailure(decode, IllegalArgumentException.class, new byte[1025], 0, 1025, 1025, longs, 0, true);
        assertNativeFailure(decode, IllegalArgumentException.class, new byte[0], 0, 0, -1, longs, 0, true);
        assertNativeFailure(decode, IndexOutOfBoundsException.class, new byte[] {1}, -1, 1, 1, longs, 0, true);
        assertNativeFailure(decode, IndexOutOfBoundsException.class, new byte[] {1}, 0, 2, 1, longs, 0, true);
        assertNativeFailure(decode, IndexOutOfBoundsException.class, new byte[] {1}, 0, 1, 1, longs, 4, true);
        assertNativeFailure(decode, IndexOutOfBoundsException.class,
                new byte[] {1}, Integer.MIN_VALUE, Integer.MAX_VALUE, 1, longs, 0, true);
        assertNativeFailure(decode, IllegalArgumentException.class, new byte[] {1}, 0, 1, 2, longs, 0, true);
        assertNativeFailure(decode, IllegalArgumentException.class,
                new byte[] {1, (byte) 0x80, 0}, 0, 3, 2, longs, 1, true);
        assertArrayEquals(beforeLongs, longs, "a malformed second value must not publish the first value");

        assertEquals(3, encode.invoke(null, new long[] {-1}, 0, 1, bytes, 2, 3, true));
        for (int i = 0; i < bytes.length; i++) assertEquals(i == 2 ? 1 : (byte) 0x5a, bytes[i]);
        assertEquals(3, decode.invoke(null, bytes, 2, 3, 1, longs, 1, true));
        assertArrayEquals(new long[] {11, -1, 37, 41}, longs);
        assertEquals(16, encode.invoke(null, new long[0], 0, 0, bytes, 16, 16, true));
        assertEquals(3, decode.invoke(null, bytes, 3, 3, 0, longs, longs.length, true));
    }

    private static byte[] independentVIntBytes(long[] values, boolean signed) {
        BigInteger base = BigInteger.valueOf(128);
        BigInteger two64 = BigInteger.ONE.shiftLeft(64);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        for (long value : values) {
            BigInteger number = BigInteger.valueOf(value);
            if (signed) {
                number = number.signum() < 0
                        ? number.negate().multiply(BigInteger.TWO).subtract(BigInteger.ONE)
                        : number.multiply(BigInteger.TWO);
            } else if (number.signum() < 0) {
                number = number.add(two64);
            }
            do {
                BigInteger[] qr = number.divideAndRemainder(base);
                bytes.write(qr[1].intValueExact() + (qr[0].signum() == 0 ? 0 : 128));
                number = qr[0];
            } while (number.signum() != 0);
        }
        return bytes.toByteArray();
    }

    private static final class NativeWorkMonitor extends PackedIntLineageTest.CancelAfter {
        long work;
        int workCalls;
        long cancelAtWork = Long.MAX_VALUE;
        RuntimeException workFailure;
        NativeWorkMonitor() { super(Integer.MAX_VALUE); }
        @Override public void worked(long completed) {
            assertTrue(completed > 0 && completed <= 1024);
            work += completed;
            workCalls++;
            if (workFailure != null) throw workFailure;
            if (work >= cancelAtWork) setCanceled(true);
        }
        @Override public void beginTask(String name, long total) { fail("caller owns lifecycle"); }
        @Override public void done() { fail("caller owns lifecycle"); }
    }

    private static void assertNativeFailure(Method method, Class<? extends Throwable> expected,
            Object... arguments) {
        InvocationTargetException thrown = assertThrows(InvocationTargetException.class,
                () -> method.invoke(null, arguments));
        assertInstanceOf(expected, thrown.getCause());
    }
}
