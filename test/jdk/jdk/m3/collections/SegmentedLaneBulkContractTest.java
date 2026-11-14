/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Random;
import org.junit.jupiter.api.Test;

final class SegmentedLaneBulkContractTest {
    @Test void longCopiesMatchArraycopyAcrossBothOverlapDirections() {
        Random random = new Random(0x5e6);
        long[] expected = new long[17000];
        LongLane28 lane = new LongLane28();
        for (int pass = 0; pass < 1500; pass++) {
            int from = random.nextInt(expected.length + 1);
            int length = random.nextInt(expected.length - from + 1);
            int to = random.nextInt(expected.length - length + 1);
            if ((pass & 3) == 0) {
                long value = pass % 8 == 0 ? 0 : random.nextLong();
                Arrays.fill(expected, from, from + length, value);
                lane.fill(from, from + length, value);
            } else {
                System.arraycopy(expected, from, expected, to, length);
                lane.copyFrom(lane, from, to, length);
            }
            long[] actual = new long[expected.length];
            lane.copyTo(0, actual, 0, actual.length);
            assertArrayEquals(expected, actual, "pass " + pass);
        }
    }

    @Test void intCopiesAndImportsMatchFlatArrays() {
        Random random = new Random(91);
        int[] expected = new int[12000];
        IntLane28 lane = new IntLane28();
        for (int pass = 0; pass < 700; pass++) {
            int length = random.nextInt(6000);
            int from = random.nextInt(expected.length - length + 1);
            int to = random.nextInt(expected.length - length + 1);
            if (pass % 3 == 0) {
                int[] input = random.ints(length).toArray();
                System.arraycopy(input, 0, expected, to, length);
                lane.copyFrom(input, 0, to, length);
            } else if (pass % 3 == 1) {
                Arrays.fill(expected, from, from + length, 0);
                lane.fill(from, from + length, 0);
            } else {
                System.arraycopy(expected, from, expected, to, length);
                lane.copyFrom(lane, from, to, length);
            }
            int[] actual = new int[expected.length];
            lane.copyTo(0, actual, 0, actual.length);
            assertArrayEquals(expected, actual);
        }
    }

    @Test void sparseZeroPropagationAndExtremeEndpointsDoNotGrowHoles() {
        int end = Address28.MAX_SLOTS;
        LongLane28 source = new LongLane28();
        LongLane28 target = new LongLane28();
        source.fill(0, end, 0);
        target.copyFrom(source, 0, 0, end);
        assertEquals(0, source.allocatedBlockCount());
        assertEquals(0, target.allocatedBlockCount());
        source.copyFrom(new long[] {Long.MIN_VALUE, 0, Long.MAX_VALUE}, 0, end - 3, 3);
        target.copyFrom(source, end - 3, (1 << 20) - 1, 3);
        long[] actual = new long[3];
        target.copyTo((1 << 20) - 1, actual, 0, 3);
        assertArrayEquals(new long[] {Long.MIN_VALUE, 0, Long.MAX_VALUE}, actual);
        assertEquals(2, target.allocatedBlockCount());
        target.copyFrom(source, 0, (1 << 20) - 1, 3);
        target.copyTo((1 << 20) - 1, actual, 0, 3);
        assertArrayEquals(new long[3], actual);
        target.copyFrom(source, end, end, 0);
        target.copyTo(end, new long[0], 0, 0);
        target.fill(end, end, 1);
    }

    @Test void invalidRangesAreRejectedBeforeWriting() {
        LongLane28 lane = new LongLane28();
        lane.set(0, 19);
        long[] output = {7, 8};
        assertThrows(IndexOutOfBoundsException.class, () -> lane.copyTo(0, output, 1, 2));
        assertArrayEquals(new long[] {7, 8}, output);
        assertThrows(IndexOutOfBoundsException.class, () -> lane.copyFrom(new long[] {1}, 0, 0, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> lane.copyFrom(lane, 0, Integer.MAX_VALUE, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> lane.fill(-1, 1, 3));
        assertThrows(NullPointerException.class, () -> lane.copyFrom((long[]) null, 0, 0, 0));
        assertEquals(19, lane.get(0));
    }

    @Test void bitmapRankSelectAndTraversalMatchBitSet() {
        int limit = (1 << 20) + 129;
        BitSet oracle = new BitSet();
        BitLane28 lane = new BitLane28();
        Random random = new Random(102);
        int[] edges = {0, 1, 62, 63, 64, 65, 4095, 4096, (1 << 20) - 1, 1 << 20};
        for (int bit : edges) { lane.set(bit); oracle.set(bit); }
        for (int i = 0; i < 3000; i++) {
            int bit = random.nextInt(limit);
            boolean value = random.nextBoolean();
            lane.set(bit, value); oracle.set(bit, value);
        }
        assertEquals(oracle.cardinality(), lane.cardinality());
        int ordinal = 0;
        for (int bit = oracle.nextSetBit(0); bit >= 0; bit = oracle.nextSetBit(bit + 1)) {
            assertEquals(bit, lane.select(ordinal++));
        }
        for (int i = 0; i < 500; i++) {
            int from = random.nextInt(limit + 1);
            int to = from + random.nextInt(limit - from + 1);
            assertEquals(oracle.nextSetBit(from), lane.nextSetBit(from));
            assertEquals(oracle.previousSetBit(from - 1), lane.previousSetBit(from - 1));
            assertEquals(oracle.get(from, to).cardinality(), lane.cardinality(from, to));
            assertEquals(oracle.get(0, to).cardinality(), lane.rank(to));
        }
        assertEquals(-1, lane.select(-1));
        assertEquals(-1, lane.select(ordinal));
        assertEquals(-1, lane.nextSetBit(Address28.MAX_SLOTS));
        lane.set(Address28.MAX_SLOT);
        assertEquals(Address28.MAX_SLOT, lane.previousSetBit(Address28.MAX_SLOT));
        assertEquals(1, lane.cardinality(Address28.MAX_SLOT, Address28.MAX_SLOTS));
        assertThrows(IndexOutOfBoundsException.class, () -> lane.rank(-1));
    }

    @Test void sparseRowsRetainAscendingSlotAndGenerationSemantics() {
        LongLaneStore28 rows = new LongLaneStore28(1);
        long[] handles = new long[9000];
        for (int i = 0; i < handles.length; i++) handles[i] = rows.allocate1(i);
        for (int i = 0; i < handles.length; i++) if (i % 4095 != 0) rows.release(handles[i]);
        long reused = rows.allocate1(123);
        assertThrows(IllegalStateException.class, () -> rows.get(handles[8999], 0));
        assertTrue(rows.isLive(reused));
        long[] output = new long[rows.size() + 2];
        assertEquals(rows.size(), rows.copyLiveLane(0, output, 1));
        assertArrayEquals(new long[] {0, 0, 4095, 8190, 123, 0}, output);
        int[] count = {0};
        rows.forEachLive(handle -> assertEquals(output[++count[0]], rows.get(handle, 0)));
        assertEquals(rows.size(), count[0]);
    }

    @Test void requiredNativeLongPagesAreZeroedAtEveryBoundary() {
        int[] slots = {0, 4095, 4096, (1 << 20) - 1, 1 << 20, Address28.MAX_SLOT};
        for (int slot : slots) {
            LongLane28 lane = new LongLane28();
            lane.set(slot, Long.MIN_VALUE);
            assertTrue(jdk.m3.arrays.ArrayNative.isLoaded());
            assertEquals(1, lane.allocatedRegionCount());
            assertEquals(1, lane.allocatedBlockCount());
            assertEquals(32768, lane.payloadBytes());
            long[] page = new long[Address28.VALUES_PER_BLOCK];
            int first = slot & ~Address28.OFFSET_MASK;
            lane.copyTo(first, page, 0, page.length);
            for (int i = 0; i < page.length; i++)
                assertEquals(first + i == slot ? Long.MIN_VALUE : 0, page[i]);
            lane.set(first, Long.MAX_VALUE);
            lane.clear(slot);
            assertEquals(1, lane.allocatedBlockCount());
            assertEquals(1, lane.allocatedRegionCount());
        }
    }

    @Test void requiredNativeLongImportsAndExportsStayDetached() {
        LongLane28 lane = new LongLane28();
        long[] source = {Long.MIN_VALUE, 0, Long.MAX_VALUE};
        lane.copyFrom(source, 0, 4095, source.length);
        source[0] = 71;
        long[] output = new long[3];
        lane.copyTo(4095, output, 0, output.length);
        assertArrayEquals(new long[] {Long.MIN_VALUE, 0, Long.MAX_VALUE}, output);
        output[2] = 19;
        assertEquals(Long.MAX_VALUE, lane.get(4097));
        assertEquals(2, lane.allocatedBlockCount());
        lane.copyFrom(lane, 4095, 4096, 3);
        lane.copyTo(4096, output, 0, output.length);
        assertArrayEquals(new long[] {Long.MIN_VALUE, 0, Long.MAX_VALUE}, output);
        lane.copyFrom(lane, 4096, 4095, 3);
        lane.copyTo(4095, output, 0, output.length);
        assertArrayEquals(new long[] {Long.MIN_VALUE, 0, Long.MAX_VALUE}, output);
    }

    @Test void requiredNativeLongArithmeticFailurePreservesThePage() {
        LongLane28 lane = new LongLane28();
        lane.set(0, Long.MAX_VALUE);
        assertThrows(ArithmeticException.class, () -> lane.addAndGet(0, 1));
        assertEquals(Long.MAX_VALUE, lane.get(0));
        assertEquals(Long.MAX_VALUE, lane.getAndSet(0, Long.MIN_VALUE));
        assertThrows(ArithmeticException.class, () -> lane.addAndGet(0, -1));
        assertEquals(Long.MIN_VALUE, lane.get(0));
        assertEquals(1, lane.allocatedBlockCount());
    }

    @Test void requiredNativeLongColdZerosAndMissingProviderInFreshVm() throws Exception {
        runLongLaneChild("missing");
    }

    @Test void requiredNativeLongSilentNullNeverPublishesAPageInFreshVm() throws Exception {
        runLongLaneChild("silent-null");
    }

    private static void runLongLaneChild(String mode) throws Exception {
        java.nio.file.Path log = java.nio.file.Files.createTempFile("m3-long-page-", ".log");
        var args = new java.util.ArrayList<String>();
        args.add(java.nio.file.Path.of(System.getProperty("java.home"), "bin", "java").toString());
        args.addAll(java.util.List.of("-Xms16m", "-Xmx64m", "-Xcheck:jni", "-ea",
                "-XX:-UsePerfData", "-XX:-HeapDumpOnOutOfMemoryError",
                "-XX:-CreateCoredumpOnCrash", "-XX:ErrorFile=NUL",
                "-Djava.io.tmpdir=" + System.getProperty("java.io.tmpdir")));
        if (mode.equals("silent-null")) {
            String provider = System.getProperty("m3.arrays.native.path");
            assertNotNull(provider, "silent-null JNI case requires the configured actual provider");
            var fixture = java.nio.file.Path.of(provider).getParent().resolve(
                    System.mapLibraryName("m3_array_allocation_null_fixture"));
            args.add("-Dm3.arrays.native.path=" + fixture);
        }
        args.add("-cp");
        String classpath = System.getProperty("surefire.test.class.path");
        if (classpath == null || classpath.isBlank()) {
            var entries = new java.util.LinkedHashSet<>(java.util.Arrays.asList(
                    System.getProperty("java.class.path").split(
                    java.util.regex.Pattern.quote(java.io.File.pathSeparator), -1)));
            for (Class<?> type : new Class<?>[] {LongLane28.class,
                    SegmentedLaneBulkContractTest.class, jdk.m3.arrays.ArrayNative.class}) {
                var source = type.getProtectionDomain().getCodeSource();
                if (source != null && source.getLocation() != null
                        && source.getLocation().getProtocol().equals("file"))
                    entries.add(java.nio.file.Path.of(source.getLocation().toURI()).toString());
            }
            classpath = String.join(java.io.File.pathSeparator, entries);
        }
        args.add(classpath);
        args.add(SegmentedLaneBulkContractTest.class.getName());
        args.add(mode);
        Process process = new ProcessBuilder(args).redirectErrorStream(true)
                .redirectOutput(log.toFile()).start();
        try {
            assertTrue(process.waitFor(30, java.util.concurrent.TimeUnit.SECONDS), "long-page child timed out");
            String output = java.nio.file.Files.readString(log);
            assertEquals(0, process.exitValue(), output);
            assertEquals("PASS long-lane " + mode, output.strip());
        } finally {
            if (process.isAlive()) process.destroyForcibly();
            java.nio.file.Files.deleteIfExists(log);
        }
    }

    /** Fresh-process failure discriminator; reuses the supplied JNI null fixture. */
    public static void main(String[] args) {
        assertEquals(1, args.length);
        assertTrue(args[0].equals("missing") || args[0].equals("silent-null"));
        assertTrue(!jdk.m3.arrays.ArrayNative.isLoaded());
        boolean missing = args[0].equals("missing");
        if (missing) System.clearProperty("m3.arrays.native.path");
        LongLane28 empty = new LongLane28();
        assertEquals(0, empty.get(0));
        empty.set(0, 0);
        empty.clear(Address28.MAX_SLOT);
        assertEquals(0, empty.getAndSet(4096, 0));
        assertEquals(0, empty.addAndGet(1 << 20, 0));
        empty.fill(0, Address28.MAX_SLOTS, 0);
        empty.copyFrom(new long[4100], 0, 4094, 4100);
        empty.copyFrom(new LongLane28(), 0, 0, Address28.MAX_SLOTS);
        empty.fill(Address28.MAX_SLOTS, Address28.MAX_SLOTS, 1);
        empty.copyFrom(new long[0], 0, Address28.MAX_SLOTS, 0);
        empty.copyTo(Address28.MAX_SLOTS, new long[0], 0, 0);
        assertThrows(IndexOutOfBoundsException.class, () -> empty.set(-1, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> empty.set(Address28.MAX_SLOTS, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> empty.fill(-1, 1, 1));
        assertThrows(NullPointerException.class, () -> empty.copyFrom((long[]) null, 0, 0, 0));
        assertEquals(0, empty.allocatedRegionCount());
        assertEquals(0, empty.allocatedBlockCount());
        assertEquals(0, empty.payloadBytes());
        assertTrue(!jdk.m3.arrays.ArrayNative.isLoaded());
        Class<? extends Throwable> failure = missing ? UnsatisfiedLinkError.class : OutOfMemoryError.class;
        for (int slot : new int[] {0, 4095, 4096, (1 << 20) - 1, 1 << 20, Address28.MAX_SLOT}) {
            for (int operation = 0; operation < 5; operation++) {
                LongLane28 lane = new LongLane28();
                int selected = operation;
                org.junit.jupiter.api.function.Executable write = () -> {
                    switch (selected) {
                        case 0 -> lane.set(slot, 1);
                        case 1 -> lane.getAndSet(slot, 1);
                        case 2 -> lane.addAndGet(slot, 1);
                        case 3 -> lane.fill(slot, slot + 1, 1);
                        case 4 -> lane.copyFrom(new long[] {1}, 0, slot, 1);
                        default -> throw new AssertionError(selected);
                    }
                };
                assertThrows(failure, write);
                assertThrows(failure, write);
                assertEquals(0, lane.get(slot));
                assertTrue(!lane.isBlockAllocated(slot));
                assertEquals(1, lane.allocatedRegionCount(), "retain existing directory-first publication");
                assertEquals(0, lane.allocatedBlockCount());
                assertEquals(0, lane.payloadBytes());
            }
        }
        assertEquals(!missing, jdk.m3.arrays.ArrayNative.isLoaded());
        System.out.println("PASS long-lane " + args[0]);
    }

}
