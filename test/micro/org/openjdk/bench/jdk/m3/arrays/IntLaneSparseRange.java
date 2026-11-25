// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package org.openjdk.bench.jdk.m3.arrays;

import java.util.Objects;
import java.util.SplittableRandom;
import java.util.concurrent.TimeUnit;
import jdk.m3.arrays.IntLaneAlgorithms;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Whole-method, setup-excluded primitive range-add maximum microbenchmark.
 *
 * <p>The JMH implementation runs against the exact source-built
 * jdk.m3.collections module. Each trial proves its expected maximum using a
 * separate dense reference computation before measurement starts. This benchmark
 * records performance only; it is not a substitute for source or overflow tests.
 * The baseline and candidate must be compared on matching JDK configurations.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(value = 2, jvmArgsAppend = {"--add-modules=jdk.m3.collections"})
@State(Scope.Thread)
public class IntLaneSparseRange {

    // Covers length admission at 65536, its immediate successor, and huge universe.
    @Param({"65535", "65536", "65537", "2000000"})
    public int universe;

    // 0 for no allocation, small sparse, threshold and dense cases.
    @Param({"0", "16", "256", "9000"})
    public int updates;

    @Param({"RANDOM", "COLLIDING", "TAIL_NEGATIVE", "FULL_RANGE"})
    public String distribution;

    private int[] start;
    private int[] end;
    private long[] delta;

    @Setup(Level.Trial)
    public void prepareAndValidate() {
        if (universe <= 0 || updates < 0) {
            throw new IllegalArgumentException("benchmark geometry must be non-negative");
        }
        start = new int[updates];
        end = new int[updates];
        delta = new long[updates];
        SplittableRandom random = new SplittableRandom(
                ((long) universe << 32) ^ updates * 65537L ^ distribution.hashCode());
        for (int i = 0; i < updates; i++) {
            switch (distribution) {
                case "RANDOM" -> {
                    start[i] = random.nextInt(universe);
                    end[i] = start[i] + random.nextInt(universe - start[i]);
                    delta[i] = random.nextLong(-50, 51);
                }
                case "COLLIDING" -> {
                    start[i] = (i & 3) == 0 ? 0 : universe / 2;
                    end[i] = universe - 1;
                    delta[i] = (i & 1) == 0 ? 3 : -2;
                }
                case "TAIL_NEGATIVE" -> {
                    start[i] = universe / 2 + random.nextInt(universe - universe / 2);
                    end[i] = universe - 1;
                    delta[i] = -1L - random.nextInt(50);
                }
                case "FULL_RANGE" -> {
                    start[i] = 0;
                    end[i] = universe - 1;
                    delta[i] = (i & 1) == 0 ? 5 : -3;
                }
                default -> throw new IllegalArgumentException("unrecognized benchmark shape: " + distribution);
            }
        }
        // Reference array exists only during trial setup, outside the measured method.
        long[] reference = new long[universe];
        for (int i = 0; i < updates; i++) {
            reference[start[i]] = Math.addExact(reference[start[i]], delta[i]);
            if (end[i] + 1 < universe) {
                reference[end[i] + 1] = Math.subtractExact(reference[end[i] + 1], delta[i]);
            }
        }
        long running = 0;
        long maximum = Long.MIN_VALUE;
        for (long change : reference) {
            running = Math.addExact(running, change);
            maximum = Math.max(maximum, running);
        }
        long actual = IntLaneAlgorithms.rangeAddMaximum(universe, start, end, delta);
        long dense = originalDenseRangeAddMaximum();
        if (dense != maximum || actual != maximum) {
            throw new AssertionError("range-add setup mismatch: expected=" + maximum + " actual=" + actual);
        }
    }

    /**
     * Standalone JMH copy of the original dense source atom for matched-VM
     * comparison only. It is not installed into the JDK or reused by the runtime.
     */
    @Benchmark
    public long originalDenseRangeAddMaximum() {
        if (universe < 0) throw new IllegalArgumentException("negative size");
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
        Objects.requireNonNull(delta, "delta");
        if (start.length != end.length || start.length != delta.length) {
            throw new IllegalArgumentException("unaligned range columns");
        }
        if (universe == 0) {
            if (start.length != 0) throw new IndexOutOfBoundsException("update of empty lane");
            return 0L;
        }
        long[] difference = new long[universe];
        for (int i = 0; i < start.length; i++) {
            int left = start[i];
            int right = end[i];
            if (left < 0 || right < left || right >= universe) {
                throw new IndexOutOfBoundsException("invalid inclusive range at operation " + i);
            }
            difference[left] = Math.addExact(difference[left], delta[i]);
            if (right + 1 < universe) {
                difference[right + 1] = Math.subtractExact(difference[right + 1], delta[i]);
            }
        }
        long running = 0L;
        long maximum = Long.MIN_VALUE;
        for (long increment : difference) {
            running = Math.addExact(running, increment);
            if (running > maximum) maximum = running;
        }
        return maximum;
    }

    @Benchmark
    public long wholeRangeAddMaximum() {
        return IntLaneAlgorithms.rangeAddMaximum(universe, start, end, delta);
    }
}
