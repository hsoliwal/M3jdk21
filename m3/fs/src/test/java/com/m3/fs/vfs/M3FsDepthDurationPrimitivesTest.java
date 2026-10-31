/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.fs.vfs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import org.junit.jupiter.api.Test;

/** JDK-only oracle for source-inlined range and duration boundary atoms. */
final class M3FsDepthDurationPrimitivesTest {
    private static final int[] ENDPOINTS = {
            Integer.MIN_VALUE, -1000, -2, -1, 0, 1, 2, 1000, Integer.MAX_VALUE
    };
    private static final int[] DEPTHS = {
            Integer.MIN_VALUE, -1001, -1000, -999, -3, -2, -1,
            0, 1, 2, 3, 999, 1000, 1001, Integer.MAX_VALUE
    };

    private static FileSelectInfo info(final int depth) {
        return new FileSelectInfo() {
            @Override public FileObject getBaseFolder() { return null; }
            @Override public int getDepth() { return depth; }
            @Override public FileObject getFile() { return null; }
        };
    }

    @Test void primitiveSelectorMatchesInclusiveOrderedOrReversedRanges() throws Exception {
        for (final int lower : ENDPOINTS) {
            for (final int upper : ENDPOINTS) {
                final FileDepthSelector selector = new FileDepthSelector(lower, upper);
                final int minimum = Math.min(lower, upper);
                final int maximum = Math.max(lower, upper);
                for (final int depth : DEPTHS) {
                    final FileSelectInfo input = info(depth);
                    assertEquals(depth >= minimum && depth <= maximum,
                            selector.includeFile(input),
                            "include: " + lower + "," + upper + "," + depth);
                    assertEquals(depth < maximum,
                            selector.traverseDescendents(input),
                            "descend: " + lower + "," + upper + "," + depth);
                    assertEquals(depth < maximum,
                            selector.traverseDescendants(input),
                            "correctly spelled delegate: " + lower + "," + upper + "," + depth);
                }
            }
        }
        assertEquals(true, new FileDepthSelector().includeFile(info(0)));
        assertEquals(false, new FileDepthSelector().includeFile(info(1)));
        assertEquals(true, new FileDepthSelector(3).includeFile(info(3)));
        assertThrows(NullPointerException.class, () -> new FileDepthSelector().includeFile(null));
    }

    @Test void exactlyOneSourceDepthReadPerQuery() throws Exception {
        final java.util.concurrent.atomic.AtomicInteger depthReads =
                new java.util.concurrent.atomic.AtomicInteger();
        final FileSelectInfo source = new FileSelectInfo() {
            @Override public FileObject getBaseFolder() { return null; }
            @Override public FileObject getFile() { return null; }
            @Override public int getDepth() { return depthReads.incrementAndGet(); }
        };
        final FileDepthSelector selector = new FileDepthSelector(1, 1);
        assertEquals(true, selector.includeFile(source));
        assertEquals(1, depthReads.get());
        assertEquals(false, selector.traverseDescendents(source));
        assertEquals(2, depthReads.get());
    }

    @Test void durationMillisSaturatesExactlyAtIntBoundaries() {
        final long[] millis = {
                Long.MIN_VALUE, Integer.MIN_VALUE - 1L, Integer.MIN_VALUE,
                Integer.MIN_VALUE + 1L, -1001L, -1L, 0L, 1L, 1001L,
                Integer.MAX_VALUE - 1L, Integer.MAX_VALUE,
                Integer.MAX_VALUE + 1L, Long.MAX_VALUE
        };
        for (final long value : millis) {
            final int expected = value < Integer.MIN_VALUE ? Integer.MIN_VALUE :
                    value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
            assertEquals(expected, FileSystemConfigBuilder.m3MillisInt(Duration.ofMillis(value)));
        }
        assertEquals(0, FileSystemConfigBuilder.m3MillisInt(Duration.ofNanos(1)));
        assertEquals(0, FileSystemConfigBuilder.m3MillisInt(Duration.ofNanos(-1)));
        assertThrows(NullPointerException.class,
                () -> FileSystemConfigBuilder.m3MillisInt(null));
        assertThrows(ArithmeticException.class,
                () -> FileSystemConfigBuilder.m3MillisInt(Duration.ofSeconds(Long.MAX_VALUE)));
        assertThrows(ArithmeticException.class,
                () -> FileSystemConfigBuilder.m3MillisInt(Duration.ofSeconds(Long.MIN_VALUE)));
    }
}
