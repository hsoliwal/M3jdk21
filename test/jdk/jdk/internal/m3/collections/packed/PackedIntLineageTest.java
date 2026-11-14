// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package jdk.internal.m3.collections.packed;

import jdk.m3.arrays.IntWidth;
import jdk.m3.arrays.packed.PackedIntArray;
import jdk.m3.collections.ProgressMonitor;
import jdk.m3.collections.ProgressState;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CancellationException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Independent integer, byte-layout and cancellation oracles for packed storage. */
class PackedIntLineageTest {
    private static final BigInteger BYTE_MASK = BigInteger.valueOf(255);

    @Test
    void widthsAndRangesAgreeWithMathematicalPowersOfTwo() {
        int[] bits = {32, 40, 48, 56, 64};
        IntWidth[] widths = IntWidth.values();
        assertEquals(bits.length, widths.length);
        for (int i = 0; i < bits.length; i++) {
            IntWidth width = widths[i];
            assertEquals(bits[i], width.bits());
            assertEquals(bits[i] / 8, width.bytes());
            BigInteger unsignedMax = BigInteger.ONE.shiftLeft(bits[i]).subtract(BigInteger.ONE);
            BigInteger signedMax = BigInteger.ONE.shiftLeft(bits[i] - 1).subtract(BigInteger.ONE);
            assertEquals(unsignedMax.longValue(), width.mask());
            assertEquals(unsignedMax.longValue(), width.maxValue(false));
            assertEquals(0, width.minValue(false));
            assertEquals(signedMax.longValue(), width.maxValue(true));
            assertEquals(signedMax.negate().subtract(BigInteger.ONE).longValue(), width.minValue(true));
            assertTrue(width.fits(width.minValue(true), true));
            assertTrue(width.fits(width.maxValue(true), true));
            assertEquals(width.maxValue(true), width.require(width.maxValue(true), true));
            if (bits[i] < 64) {
                assertFalse(width.fits(width.maxValue(true) + 1, true));
                assertFalse(width.fits(width.minValue(true) - 1, true));
                assertFalse(width.fits(-1, false));
                assertThrows(ArithmeticException.class, () -> width.require(-1, false));
            } else {
                assertTrue(width.fits(Long.MIN_VALUE, false));
                assertTrue(width.fits(-1, false));
            }
        }
        assertEquals(IntWidth.INT32, IntWidth.smallest(-1, true));
        assertEquals(IntWidth.INT32, IntWidth.smallest(0xffff_ffffL, false));
        assertEquals(IntWidth.INT40, IntWidth.smallest(1L << 32, false));
        assertEquals(IntWidth.INT64, IntWidth.smallest(-1, false));
    }

    @Test
    void signedAndUnsignedBoundariesRoundTripWithoutChangingNeighbors() {
        for (IntWidth width : IntWidth.values()) {
            for (boolean signed : new boolean[] {true, false}) {
                long[] values = signed
                        ? new long[] {0, 1, -1, width.minValue(true), width.maxValue(true), 127, -128}
                        : new long[] {0, 1, 255, width.maxValue(false), 128, 65535, 2};
                PackedIntArray array = PackedIntArray.of(values.length, width, signed);
                assertEquals(values.length, array.length());
                assertSame(width, array.width());
                assertEquals(signed, array.signed());
                for (int i = 0; i < values.length; i++) {
                    array.set(i, values[i]);
                    for (int j = 0; j < values.length; j++) {
                        assertEquals(j <= i ? values[j] : 0, array.get(j), width + " index " + j);
                    }
                }
                assertArrayEquals(independentBytes(values, width.bytes()), array.toByteArray(null));
            }
        }
    }

    @Test
    void fixedLittleEndianFixturesIncludeTheExactFinalEntry() {
        PackedIntArray lint = PackedIntArray.lint(3);
        lint.set(0, 0x1234_5678_9abcL);
        lint.set(1, -1);
        lint.set(2, -(1L << 47));
        byte[] expected = {
                (byte) 0xbc, (byte) 0x9a, 0x78, 0x56, 0x34, 0x12,
                -1, -1, -1, -1, -1, -1,
                0, 0, 0, 0, 0, (byte) 0x80
        };
        assertArrayEquals(expected, lint.toByteArray(null));
        PackedIntArray decoded = PackedIntArray.fromByteArray(expected, IntWidth.INT48, true, null);
        assertEquals(0x1234_5678_9abcL, decoded.get(0));
        assertEquals(-1, decoded.get(1));
        assertEquals(-(1L << 47), decoded.get(2));
        assertEquals(18, decoded.payloadBytes());
    }

    @Test
    void randomWritesAgreeWithBigIntegerAndLongArrayOracles() {
        Random random = new Random(0x4c49_4e54_2026L);
        for (IntWidth width : IntWidth.values()) {
            for (boolean signed : new boolean[] {true, false}) {
                long[] oracle = new long[257];
                PackedIntArray array = PackedIntArray.of(oracle.length, width, signed);
                for (int trial = 0; trial < 4096; trial++) {
                    int index = random.nextInt(oracle.length);
                    long value = randomValue(random, width.bits(), signed);
                    array.set(index, value);
                    oracle[index] = value;
                    assertEquals(value, array.get(index));
                    if (index > 0) assertEquals(oracle[index - 1], array.get(index - 1));
                    if (index + 1 < oracle.length) assertEquals(oracle[index + 1], array.get(index + 1));
                }
                assertArrayEquals(independentBytes(oracle, width.bytes()), array.toByteArray(null));
                long[] decoded = new long[oracle.length];
                array.copyTo(0, decoded, 0, decoded.length, null);
                assertArrayEquals(oracle, decoded);
            }
        }
    }

    @Test
    void invalidStoresAndBatchesLeaveAllExistingValuesIntact() {
        for (IntWidth width : new IntWidth[] {IntWidth.INT32, IntWidth.INT40, IntWidth.INT48, IntWidth.INT56}) {
            PackedIntArray array = PackedIntArray.of(4, width, true);
            array.copyFrom(new long[] {7, -3, 19, 11}, 0, 0, 4, null);
            byte[] before = array.toByteArray(null);
            assertThrows(IllegalArgumentException.class, () -> array.set(1, width.maxValue(true) + 1));
            assertThrows(IllegalArgumentException.class, () -> array.set(1, width.minValue(true) - 1));
            assertThrows(IllegalArgumentException.class, () -> array.copyFrom(
                    new long[] {1, 2, width.maxValue(true) + 1}, 0, 1, 3, null));
            assertThrows(IllegalArgumentException.class, () -> array.fill(0, 4, width.maxValue(true) + 1, null));
            assertArrayEquals(before, array.toByteArray(null));
            PackedIntArray unsigned = PackedIntArray.of(2, width, false);
            assertThrows(IllegalArgumentException.class, () -> unsigned.set(0, -1));
            assertEquals(0, unsigned.get(0));
        }
    }

    @Test
    void bulkOffsetsEmptyRangesAndFillsPreserveOutsideValues() {
        PackedIntArray array = PackedIntArray.lint(7);
        array.fill(0, 7, 99, null);
        long[] source = {777, 11, -12, 13, 888};
        array.copyFrom(source, 1, 2, 3, null);
        long[] out = new long[9];
        Arrays.fill(out, -999);
        array.copyTo(0, out, 1, 7, null);
        assertArrayEquals(new long[] {-999, 99, 99, 11, -12, 13, 99, 99, -999}, out);
        array.copyFrom(source, source.length, array.length(), 0, null);
        array.copyTo(array.length(), out, out.length, 0, null);
        array.fill(7, 7, 0, null);
        array.fill(3, 5, -77, null);
        assertEquals(11, array.get(2));
        assertEquals(-77, array.get(3));
        assertEquals(-77, array.get(4));
        assertEquals(99, array.get(5));
    }

    @Test
    void byteSnapshotsAndImportedPayloadsDoNotAliasOwners() {
        PackedIntArray array = PackedIntArray.lint(2);
        array.set(0, 23);
        byte[] exported = array.toByteArray(null);
        exported[0] = 99;
        assertEquals(23, array.get(0));
        PackedIntArray imported = PackedIntArray.fromByteArray(exported, IntWidth.INT48, true, null);
        exported[0] = 42;
        assertEquals(99, imported.get(0));
        imported.set(1, 71);
        assertEquals(0, array.get(1));
        assertEquals(0, exported[6]);
    }

    @Test
    void cancellationDuringCopyFromDoesNotPublishPartialBytes() {
        PackedIntArray array = PackedIntArray.lint(4097);
        array.fill(0, array.length(), 7, null);
        byte[] before = array.toByteArray(null);
        long[] source = new long[array.length()];
        Arrays.fill(source, -11);
        assertThrows(CancellationException.class,
                () -> array.copyFrom(source, 0, 0, source.length, new CancelAfter(3)));
        assertArrayEquals(before, array.toByteArray(null));
    }

    @Test
    void cancellationDuringCopyToPreservesCallerDestination() {
        PackedIntArray array = PackedIntArray.lint(4097);
        array.fill(0, array.length(), -7, null);
        long[] destination = new long[array.length() + 2];
        Arrays.fill(destination, 55);
        long[] before = destination.clone();
        assertThrows(CancellationException.class,
                () -> array.copyTo(0, destination, 1, array.length(), new CancelAfter(3)));
        assertArrayEquals(before, destination);
    }

    @Test
    void cancellationDuringFillAndAtSingleStorePreservesOwner() {
        PackedIntArray array = PackedIntArray.lint(4097);
        array.set(2, -19);
        byte[] before = array.toByteArray(null);
        assertThrows(CancellationException.class,
                () -> array.fill(0, array.length(), 25, new CancelAfter(3)));
        assertArrayEquals(before, array.toByteArray(null));
        assertThrows(CancellationException.class, () -> array.set(2, 7, new CancelAfter(1)));
        assertEquals(-19, array.get(2));
        assertThrows(CancellationException.class, () -> array.get(2, new CancelAfter(1)));
    }

    @Test
    void progressCallbacksCannotMutateTheSameOwner() {
        PackedIntArray array = PackedIntArray.lint(2049);
        ProgressMonitor callback = new CancelAfter(Integer.MAX_VALUE) {
            @Override public void worked(long work) {
                assertEquals(0, array.get(0));
                array.set(0, 999);
            }
        };
        assertThrows(IllegalStateException.class, () -> array.fill(0, array.length(), 12, callback));
        assertEquals(0, array.get(0));
        assertEquals(0, array.get(array.length() - 1));
        array.set(0, 17);
        assertEquals(17, array.get(0), "callback failure must release the mutation guard");
    }

    @Test
    void malformedGeometryAndRangesFailBeforeAllocationOrMutation() {
        assertThrows(IllegalArgumentException.class, () -> PackedIntArray.of(-1, IntWidth.INT48, true));
        assertThrows(IllegalArgumentException.class,
                () -> PackedIntArray.of(Integer.MAX_VALUE, IntWidth.INT64, true));
        assertThrows(NullPointerException.class, () -> PackedIntArray.of(1, null, true));
        assertThrows(IllegalArgumentException.class,
                () -> PackedIntArray.fromByteArray(new byte[7], IntWidth.INT48, true, null));
        assertThrows(NullPointerException.class,
                () -> PackedIntArray.fromByteArray(null, IntWidth.INT48, true, null));
        PackedIntArray array = PackedIntArray.lint(3);
        assertThrows(IndexOutOfBoundsException.class, () -> array.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> array.set(3, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> array.fill(2, 1, 0, null));
        assertThrows(IndexOutOfBoundsException.class,
                () -> array.copyFrom(new long[2], Integer.MAX_VALUE, 0, 1, null));
        assertThrows(IndexOutOfBoundsException.class,
                () -> array.copyTo(0, new long[3], 2, 2, null));
        assertEquals(0, PackedIntArray.of(0, IntWidth.INT56, false).payloadBytes());
    }

    @Test
    void tenMillionValuesUseTheExactUserSpecifiedBackingPayload() throws ReflectiveOperationException {
        for (IntWidth width : IntWidth.values()) assertTenMillionBackingBytes(width);
    }

    private static void assertTenMillionBackingBytes(IntWidth width) throws ReflectiveOperationException {
        int count = 10_000_000;
        PackedIntArray array = PackedIntArray.of(count, width, true);
        long expectedBytes = count * (long) (width.bits() / 8);
        long allocatedPayload = 0;
        int arrayFields = 0;
        for (Field field : PackedIntArray.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) || !field.getType().isArray()) continue;
            assertEquals(byte[].class, field.getType(), "the numeric owner must not retain wider or boxed arrays");
            field.setAccessible(true);
            allocatedPayload += ((byte[]) field.get(array)).length;
            arrayFields++;
        }
        assertEquals(1, arrayFields, "one parent-owned packed payload");
        assertEquals(expectedBytes, allocatedPayload);
        assertEquals(expectedBytes, array.payloadBytes());
        array.set(0, -1);
        array.set(count / 2, width.maxValue(true));
        array.set(count - 1, width.minValue(true));
        assertEquals(-1, array.get(0));
        assertEquals(width.maxValue(true), array.get(count / 2));
        assertEquals(width.minValue(true), array.get(count - 1));
    }

    static long randomValue(Random random, int bits, boolean signed) {
        BigInteger value = new BigInteger(bits, random);
        if (signed && value.testBit(bits - 1)) value = value.subtract(BigInteger.ONE.shiftLeft(bits));
        return value.longValue();
    }

    static byte[] independentBytes(long[] values, int widthBytes) {
        byte[] bytes = new byte[Math.multiplyExact(values.length, widthBytes)];
        for (int index = 0; index < values.length; index++) {
            BigInteger value = BigInteger.valueOf(values[index]);
            for (int part = 0; part < widthBytes; part++) {
                bytes[index * widthBytes + part] = value.and(BYTE_MASK).byteValue();
                value = value.shiftRight(8);
            }
        }
        return bytes;
    }

    /** Counts actual cancellation checkpoints; no timing, threads or fake runtime owners. */
    static class CancelAfter implements ProgressMonitor {
        private final int cancelAt;
        private int checks;
        private boolean canceled;
        CancelAfter(int cancelAt) { this.cancelAt = cancelAt; }
        @Override public void checkCanceled() {
            if (++checks >= cancelAt || canceled) throw new CancellationException("test checkpoint " + checks);
        }
        @Override public void beginTask(String name, long total) { }
        @Override public void done() { }
        @Override public void worked(long work) { }
        @Override public void setProgress(long work, long total) { }
        @Override public void setTaskName(String name) { }
        @Override public void subTask(String name) { }
        @Override public boolean isCanceled() { return canceled || checks >= cancelAt; }
        @Override public void setCanceled(boolean value) { canceled = value; }
        @Override public ProgressState state() { return null; }
    }
}
