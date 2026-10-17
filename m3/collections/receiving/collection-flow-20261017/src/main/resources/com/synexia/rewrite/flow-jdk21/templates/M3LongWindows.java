// Copyright 2026 Hitesh Soliwal and contributors.
// Modified 2026-10-17: target package, progress SPI and JNI symbol adaptation.
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import com.m3.collections.M3ProgressMonitor;
import java.util.Objects;

/** Operation-local rolling state; no payload owner, stream engine, or retained cache. */
final class M3LongWindows {
    enum Kind { SUM, MIN, MAX }
    private M3LongWindows() { }

    static M3LongFlow create(M3LongFlow.Plan upstream, int maximum, int width,
            int step, M3Windows.Tail tail, Kind kind) {
        if (width <= 0 || step <= 0) throw new IllegalArgumentException("positive width/step required");
        Objects.requireNonNull(tail, "tail");
        return new M3LongFlow((sink, monitor) -> {
            M3Flow.check(monitor);
            try (State state = kind == Kind.SUM
                    ? new Sum(width, step, tail, Math.min(width, maximum), sink, monitor)
                    : new Extreme(width, step, tail, Math.min(width, maximum), sink, monitor, kind)) {
                upstream.run(state::accept, monitor);
                state.finish();
            }
            M3Flow.check(monitor);
        }, maximum);
    }

    private abstract static class State implements AutoCloseable {
        final int width, step, maximum;
        final M3Windows.Tail tail;
        final M3LongFlow.Sink sink;
        final M3ProgressMonitor monitor;
        long seen, next;
        boolean running = true;

        State(int width, int step, M3Windows.Tail tail, int maximum,
                M3LongFlow.Sink sink, M3ProgressMonitor monitor) {
            this.width = width;
            this.step = step;
            this.tail = tail;
            this.maximum = maximum;
            this.sink = sink;
            this.monitor = monitor;
        }

        final boolean accept(long value) {
            M3Flow.check(monitor);
            expire(Math.max(0L, seen - width + 1L));
            add(seen, value);
            seen++;
            if (seen == next + width) {
                emit();
                next += step;
            }
            return running;
        }

        final void finish() {
            M3Flow.check(monitor);
            if (tail == M3Windows.Tail.PARTIAL) {
                while (running && next < seen) {
                    expire(next);
                    emit();
                    next += step;
                }
            }
        }

        private void emit() {
            M3Flow.check(monitor);
            running = sink.accept(value());
            M3Flow.check(monitor);
        }

        final int grow(int old, int wanted) {
            if (wanted > maximum) throw new IllegalArgumentException("window state budget");
            return (int) Math.min(maximum, Math.max(wanted, Math.max(8L, 2L * old)));
        }

        abstract void add(long ordinal, long value);
        abstract void expire(long before);
        abstract long value();
        @Override public abstract void close();
    }

    /** Ring retains each primitive once; expired contributions are subtracted modulo 2^64. */
    private static final class Sum extends State {
        long[] values = new long[0];
        int head, count;
        long first, total;

        Sum(int width, int step, M3Windows.Tail tail, int maximum,
                M3LongFlow.Sink sink, M3ProgressMonitor monitor) {
            super(width, step, tail, maximum, sink, monitor);
        }

        @Override void add(long ordinal, long value) {
            if (count == values.length) {
                long[] nextValues = new long[grow(values.length, count + 1)];
                for (int i = 0; i < count; i++) {
                    if ((i & 1023) == 0) M3Flow.check(monitor);
                    nextValues[i] = values[(int) (((long) head + i) % values.length)];
                }
                values = nextValues;
                head = 0;
            }
            if (count == 0) first = ordinal;
            values[(int) (((long) head + count) % values.length)] = value;
            count++;
            total += value;
        }

        @Override void expire(long before) {
            int removed = 0;
            while (count > 0 && first < before) {
                if ((removed++ & 1023) == 0) M3Flow.check(monitor);
                total -= values[head];
                head = (head + 1) % values.length;
                count--;
                first++;
            }
        }

        @Override long value() { return total; }
        @Override public void close() { values = null; }
    }

    /** Each ordinal is pushed once and removed at most once: amortized linear work. */
    private static final class Extreme extends State {
        long[] values = new long[0], ordinals = new long[0];
        int head, count;
        final Kind kind;

        Extreme(int width, int step, M3Windows.Tail tail, int maximum,
                M3LongFlow.Sink sink, M3ProgressMonitor monitor, Kind kind) {
            super(width, step, tail, maximum, sink, monitor);
            this.kind = kind;
        }

        @Override void add(long ordinal, long value) {
            int removed = 0;
            while (count > 0) {
                if ((removed++ & 1023) == 0) M3Flow.check(monitor);
                int last = (int) (((long) head + count - 1) % values.length);
                boolean dominated = kind == Kind.MIN ? values[last] >= value : values[last] <= value;
                if (!dominated) break;
                count--;
            }
            if (count == values.length) {
                int capacity = grow(values.length, count + 1);
                long[] nextValues = new long[capacity], nextOrdinals = new long[capacity];
                for (int i = 0; i < count; i++) {
                    if ((i & 1023) == 0) M3Flow.check(monitor);
                    int at = (int) (((long) head + i) % values.length);
                    nextValues[i] = values[at];
                    nextOrdinals[i] = ordinals[at];
                }
                values = nextValues;
                ordinals = nextOrdinals;
                head = 0;
            }
            int at = (int) (((long) head + count) % values.length);
            values[at] = value;
            ordinals[at] = ordinal;
            count++;
        }

        @Override void expire(long before) {
            int removed = 0;
            while (count > 0 && ordinals[head] < before) {
                if ((removed++ & 1023) == 0) M3Flow.check(monitor);
                head = (head + 1) % values.length;
                count--;
            }
        }

        @Override long value() { return values[head]; }
        @Override public void close() { values = ordinals = null; }
    }
}
