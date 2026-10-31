// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.runtime.capability;

import java.util.Objects;
import java.util.concurrent.CancellationException;

/**
 * Source-backed sliding byte-window statistics. Retains 256 primitive frequencies,
 * not per-window objects, byte payloads, decoded text, or a second string interner.
 * All offsets are relative to the borrowed immutable SourceArtifact.Span.
 */
public final class ByteSlidingWindows {
    public static final int MAX_WINDOWS = 1_000_000;

    @FunctionalInterface
    public interface Sink {
        void accept(int fromInclusive, int toExclusive, int distinctBytes, int modalUnsignedByte);
    }

    private ByteSlidingWindows() { }

    /**
     * Emit fixed-width windows starting at 0, step, 2*step ... while fully inside the span.
     * Validation includes the entire output count before the first callback: an inadequate
     * maxWindows limit fails closed without partially publishing results.
     * Modal ties select the numerically smallest unsigned byte. A zero-width window is invalid.
     * A short span emits no windows. Neither the source nor a callback-owned object is retained.
     */
    public static int scan(SourceArtifact.Span span, int width, int step,
                           int maxWindows, Sink sink) {
        SourceArtifact.Span source = Objects.requireNonNull(span, "span");
        Objects.requireNonNull(sink, "sink");
        if (width <= 0 || step <= 0 || maxWindows < 0 || maxWindows > MAX_WINDOWS) {
            throw new IllegalArgumentException("sliding-window width, step or output budget");
        }
        final int size = source.length();
        final int total = width > size ? 0 : 1 + (size - width) / step;
        if (total > maxWindows) throw new IllegalArgumentException("sliding-window output budget");
        if (total == 0) return 0;

        int[] counts = new int[256];
        int distinct = 0;
        for (int i = 0; i < width; i++) {
            checkpoint(i);
            int key = Byte.toUnsignedInt(source.byteAt(i));
            if (counts[key]++ == 0) distinct++;
        }
        int previousStart = 0;
        for (int emitted = 0; emitted < total; emitted++) {
            int start = emitted * step;
            if (emitted != 0) {
                int previousEnd = previousStart + width;
                int newEnd = start + width;
                int removedEnd = Math.min(start, previousEnd);
                for (int i = previousStart; i < removedEnd; i++) {
                    checkpoint(i);
                    int value = Byte.toUnsignedInt(source.byteAt(i));
                    if (--counts[value] == 0) distinct--;
                }
                int addedStart = Math.max(previousEnd, start);
                for (int i = addedStart; i < newEnd; i++) {
                    checkpoint(i);
                    int value = Byte.toUnsignedInt(source.byteAt(i));
                    if (counts[value]++ == 0) distinct++;
                }
            }
            int mode = -1;
            int highest = 0;
            for (int b = 0; b < counts.length; b++) {
                if (counts[b] > highest) {
                    highest = counts[b];
                    mode = b;
                }
            }
            checkpoint(start);
            sink.accept(start, start + width, distinct, mode);
            previousStart = start;
        }
        return total;
    }

    private static void checkpoint(int index) {
        if ((index & 4095) == 0 && Thread.currentThread().isInterrupted()) {
            throw new CancellationException("sliding-window scan interrupted");
        }
    }
}
