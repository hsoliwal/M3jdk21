/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.arrays.packed;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CancellationException;
import java.util.function.LongConsumer;
import jdk.m3.arrays.IntWidth;
import jdk.m3.collections.DefaultProgressMonitor;
import jdk.m3.collections.ProgressMonitor;
import jdk.m3.collections.ProgressState;
import org.junit.jupiter.api.Test;

/** PACKED_UNION U9: the Synexia common-collections PackedIntArray API on the M3 owner. */
class PackedIntArrayUnionTest {

    /** Counts worked units; optionally runs an action on each worked callback. */
    private static final class Probe implements ProgressMonitor {
        private final DefaultProgressMonitor d = new DefaultProgressMonitor();
        long worked;
        LongConsumer onWorked = w -> { };

        @Override public void beginTask(String taskName, long totalWork) { d.beginTask(taskName, totalWork); }
        @Override public void done() { d.done(); }
        @Override public void worked(long work) { worked += work; onWorked.accept(work); }
        @Override public void setProgress(long w, long totalWork) { d.setProgress(w, totalWork); }
        @Override public void setTaskName(String taskName) { d.setTaskName(taskName); }
        @Override public void subTask(String subTaskName) { d.subTask(subTaskName); }
        @Override public boolean isCanceled() { return d.isCanceled(); }
        @Override public void setCanceled(boolean canceled) { d.setCanceled(canceled); }
        @Override public ProgressState state() { return d.state(); }
    }

    @Test
    void factoriesKeepTypedClassesAndCoverEveryWidth() {
        assertInstanceOf(Int40Array.class, PackedIntArray.of(3, IntWidth.INT40, true));
        assertInstanceOf(Int48Array.class, PackedIntArray.of(3, IntWidth.INT48, true));
        assertInstanceOf(Int56Array.class, PackedIntArray.of(3, IntWidth.INT56, true));
        assertInstanceOf(Int48Array.class, PackedIntArray.lint(2));
        for (IntWidth width : IntWidth.values()) {
            for (boolean signed : new boolean[] {true, false}) {
                PackedIntArray array = PackedIntArray.of(5, width, signed);
                assertEquals(width, array.width());
                assertEquals(signed, array.signed());
                assertEquals(5L * width.bytes(), array.payloadBytes());
                array.set(4, width.maxValue(signed));
                assertEquals(width.maxValue(signed), array.get(4));
                if (width != IntWidth.INT64) {
                    long over = signed ? width.maxValue(true) + 1 : width.mask() + 1;
                    assertThrows(IllegalArgumentException.class, () -> array.set(0, over));
                }
            }
        }
        assertThrows(IllegalArgumentException.class, () -> PackedIntArray.of(-1, IntWidth.INT32, true));
    }

    @Test
    void unsignedAndSignedDecodingIsLittleEndian() {
        PackedIntArray u32 = PackedIntArray.of(1, IntWidth.INT32, false);
        u32.set(0, 0xF1020304L);
        assertEquals(0xF1020304L, u32.get(0));
        assertArrayEquals(new byte[] {4, 3, 2, (byte) 0xF1}, u32.toByteArray(null));
        assertThrows(IllegalArgumentException.class, () -> u32.set(0, -1));
        PackedIntArray s32 = PackedIntArray.fromByteArray(u32.toByteArray(), IntWidth.INT32, true, null);
        assertEquals((int) 0xF1020304L, s32.get(0));
        PackedIntArray u64 = PackedIntArray.of(2, IntWidth.INT64, false);
        u64.set(0, -1L);
        assertEquals(-1L, u64.get(0));
        assertEquals(Long.MIN_VALUE, u64.minValue());
        assertEquals(Long.MAX_VALUE, u64.maxValue());
        assertThrows(IllegalArgumentException.class,
                () -> PackedIntArray.fromByteArray(new byte[7], IntWidth.INT32, true, null));
        assertThrows(IllegalArgumentException.class,
                () -> u32.copyTo(0, PackedIntArray.of(1, IntWidth.INT32, true), 0, 1));
    }

    @Test
    void monitoredBulkOperationsReportAndStageCommits() {
        int n = 2500;
        long[] values = new long[n];
        for (int i = 0; i < n; i++) values[i] = i - 1000;
        PackedIntArray array = PackedIntArray.of(n, IntWidth.INT32, true);
        Probe probe = new Probe();
        array.copyFrom(values, 0, 0, n, probe);
        assertEquals(n, probe.worked);
        long[] back = new long[n];
        array.copyTo(0, back, 0, n, null);
        assertArrayEquals(values, back);
        array.fill(10, 20, 7, null);
        assertEquals(7, array.get(15, null));
        array.set(3, -3, null);
        assertEquals(-3, array.get(3));

        long[] invalid = values.clone();
        invalid[n - 1] = 1L << 40;
        assertThrows(IllegalArgumentException.class, () -> array.copyFrom(invalid, 0, 0, n, null));
        assertEquals(-1000, array.get(0), "an invalid later value leaves the destination unchanged");

        Probe canceling = new Probe();
        canceling.onWorked = w -> canceling.setCanceled(true);
        long[] destination = new long[n];
        assertThrows(CancellationException.class, () -> array.copyTo(0, destination, 0, n, canceling));
        assertTrue(java.util.Arrays.stream(destination).allMatch(v -> v == 0), "cancellation leaves the destination unchanged");
    }

    @Test
    void mutationFromMonitorCallbackIsRejected() {
        PackedIntArray array = PackedIntArray.of(2048, IntWidth.INT40, true);
        Probe probe = new Probe();
        probe.onWorked = w -> array.set(0, 1);
        assertThrows(IllegalStateException.class, () -> array.fill(0, 2048, 5, probe));
        assertEquals(0, array.get(0));
        assertFalse(array.get(1) == 5, "the staged fill was not committed");
        array.set(0, 9);
        assertEquals(9, array.get(0), "the callback guard is released after the operation");
    }
}
