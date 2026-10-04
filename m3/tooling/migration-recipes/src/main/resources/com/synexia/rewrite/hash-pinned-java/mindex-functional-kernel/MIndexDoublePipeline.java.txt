/*
 * Copyright (c) 2026, Contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 */

package jdk.internal.mindex.function;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleConsumer;
import java.util.function.DoublePredicate;
import java.util.function.DoubleUnaryOperator;

/**
 * Immutable fused double map/filter/slice pipeline.
 *
 * <p>Builder-time fusion collapses adjacent MIndex maps and predicates. Execution with a reusable
 * Workspace creates no per-element objects. Ordinary JDK functional interfaces remain valid
 * fallback stages.</p>
 */
public final class MIndexDoublePipeline {
    private static final byte MAP = 1;
    private static final byte FILTER = 2;
    private static final byte SKIP = 3;
    private static final byte LIMIT = 4;
    

    private final byte[] kinds;
    private final Object[] functions;
    private final long[] operands;
    private final int[] stateSlots;
    private final int stateCount;

    private MIndexDoublePipeline(byte[] kinds, Object[] functions, long[] operands,
            int[] stateSlots, int stateCount) {
        this.kinds = kinds;
        this.functions = functions;
        this.operands = operands;
        this.stateSlots = stateSlots;
        this.stateCount = stateCount;
    }

    public static Builder builder() { return new Builder(); }
    public int stageCount() { return kinds.length; }
    public Workspace workspace() { return new Workspace(stateCount); }

    public int transform(double[] input, double[] output, Workspace workspace) {
        Objects.requireNonNull(input);
        Objects.requireNonNull(output);
        if (output.length < input.length) {
            throw new IllegalArgumentException("output must have worst-case input capacity");
        }
        Workspace state = prepared(workspace);
        int written = 0;
        for (double inputValue : input) {
            if (applyOne(inputValue, state)) output[written++] = state.current;
            if (state.stopped) break;
        }
        return written;
    }

    public int count(double[] input, Workspace workspace) {
        Objects.requireNonNull(input);
        Workspace state = prepared(workspace);
        int count = 0;
        for (double inputValue : input) {
            if (applyOne(inputValue, state)) count++;
            if (state.stopped) break;
        }
        return count;
    }

    public double sum(double[] input, Workspace workspace) {
        Objects.requireNonNull(input);
        Workspace state = prepared(workspace);
        double sum = 0.0d;
        double compensation = 0.0d;
        double simple = 0.0d;
        for (double inputValue : input) {
            if (applyOne(inputValue, state)) {
                double value = state.current;
                simple += value;
                double adjusted = value - compensation;
                double next = sum + adjusted;
                compensation = (next - sum) - adjusted;
                sum = next;
            }
            if (state.stopped) break;
        }
        double corrected = sum - compensation;
        return Double.isNaN(corrected) && Double.isInfinite(simple) ? simple : corrected;
    }

    public double reduce(double[] input, double identity,
            DoubleBinaryOperator reducer, Workspace workspace) {
        Objects.requireNonNull(input);
        Objects.requireNonNull(reducer);
        Workspace state = prepared(workspace);
        double result = identity;
        for (double inputValue : input) {
            if (applyOne(inputValue, state)) result = reducer.applyAsDouble(result, state.current);
            if (state.stopped) break;
        }
        return result;
    }

    public boolean anyMatch(double[] input, DoublePredicate predicate, Workspace workspace) {
        Objects.requireNonNull(input);
        Objects.requireNonNull(predicate);
        Workspace state = prepared(workspace);
        for (double inputValue : input) {
            if (applyOne(inputValue, state) && predicate.test(state.current)) return true;
            if (state.stopped) break;
        }
        return false;
    }

    public boolean allMatch(double[] input, DoublePredicate predicate, Workspace workspace) {
        Objects.requireNonNull(input);
        Objects.requireNonNull(predicate);
        Workspace state = prepared(workspace);
        for (double inputValue : input) {
            if (applyOne(inputValue, state) && !predicate.test(state.current)) return false;
            if (state.stopped) break;
        }
        return true;
    }

    public void forEach(double[] input, DoubleConsumer consumer, Workspace workspace) {
        Objects.requireNonNull(input);
        Objects.requireNonNull(consumer);
        Workspace state = prepared(workspace);
        for (double inputValue : input) {
            if (applyOne(inputValue, state)) consumer.accept(state.current);
            if (state.stopped) break;
        }
    }

    public boolean findFirst(double[] input, double[] output, Workspace workspace) {
        Objects.requireNonNull(input);
        Objects.requireNonNull(output);
        if (output.length == 0) throw new IllegalArgumentException("output length is zero");
        Workspace state = prepared(workspace);
        for (double inputValue : input) {
            if (applyOne(inputValue, state)) {
                output[0] = state.current;
                return true;
            }
            if (state.stopped) break;
        }
        return false;
    }

    private Workspace prepared(Workspace workspace) {
        Workspace state = Objects.requireNonNull(workspace);
        state.prepare(stateCount);
        return state;
    }

    private boolean applyOne(double input, Workspace state) {
        if (state.stopped) return false;
        double value = input;
        boolean stopAfterCurrent = false;
        for (int stage = 0; stage < kinds.length; stage++) {
            switch (kinds[stage]) {
                case MAP -> value = ((DoubleUnaryOperator) functions[stage]).applyAsDouble(value);
                case FILTER -> {
                    if (!((DoublePredicate) functions[stage]).test(value)) return false;
                }
                case SKIP -> {
                    int slot = stateSlots[stage];
                    if (state.counters[slot] < operands[stage]) {
                        state.counters[slot]++;
                        return false;
                    }
                }
                case LIMIT -> {
                    int slot = stateSlots[stage];
                    if (state.counters[slot] >= operands[stage]) {
                        state.stopped = true;
                        return false;
                    }
                    if (++state.counters[slot] == operands[stage]) stopAfterCurrent = true;
                }
                default -> throw new AssertionError(kinds[stage]);
            }
        }
        state.current = value;
        if (stopAfterCurrent) state.stopped = true;
        return true;
    }

    public static final class Workspace {
        private long[] counters;
        private boolean stopped;
        private double current;

        private Workspace(int stateCount) { counters = new long[stateCount]; }

        private void prepare(int stateCount) {
            if (counters.length < stateCount) counters = new long[stateCount];
            Arrays.fill(counters, 0, stateCount, 0L);
            stopped = false;
        }
    }

    public static final class Builder {
        private byte[] kinds = new byte[8];
        private Object[] functions = new Object[8];
        private long[] operands = new long[8];
        private int[] stateSlots = new int[8];
        private int size;
        private int stateCount;
        private boolean frozen;

        public Builder map(DoubleUnaryOperator operator) {
            ensureMutable();
            Objects.requireNonNull(operator);
            if (size > 0 && kinds[size - 1] == MAP
                    && functions[size - 1] instanceof MIndexDoubleUnaryOperator left
                    && operator instanceof MIndexDoubleUnaryOperator right) {
                functions[size - 1] = left.andThen(right);
                return this;
            }
            append(MAP, operator, 0L, -1);
            return this;
        }

        public Builder filter(DoublePredicate predicate) {
            ensureMutable();
            Objects.requireNonNull(predicate);
            if (size > 0 && kinds[size - 1] == FILTER
                    && functions[size - 1] instanceof MIndexDoublePredicate left
                    && predicate instanceof MIndexDoublePredicate right) {
                functions[size - 1] = left.and(right);
                return this;
            }
            append(FILTER, predicate, 0L, -1);
            return this;
        }

        public Builder skip(long count) {
            ensureMutable();
            if (count < 0L) throw new IllegalArgumentException("negative skip");
            if (count == 0L) return this;
            if (size > 0 && kinds[size - 1] == SKIP) {
                operands[size - 1] = saturatingAdd(operands[size - 1], count);
                return this;
            }
            append(SKIP, null, count, stateCount++);
            return this;
        }

        public Builder limit(long count) {
            ensureMutable();
            if (count < 0L) throw new IllegalArgumentException("negative limit");
            if (size > 0 && kinds[size - 1] == LIMIT) {
                operands[size - 1] = Math.min(operands[size - 1], count);
                return this;
            }
            append(LIMIT, null, count, stateCount++);
            return this;
        }

        public MIndexDoublePipeline freeze() {
            ensureMutable();
            frozen = true;
            return new MIndexDoublePipeline(
                    Arrays.copyOf(kinds, size),
                    Arrays.copyOf(functions, size),
                    Arrays.copyOf(operands, size),
                    Arrays.copyOf(stateSlots, size),
                    stateCount);
        }

        private void append(byte kind, Object function, long operand, int stateSlot) {
            if (size == kinds.length) {
                int capacity = size << 1;
                kinds = Arrays.copyOf(kinds, capacity);
                functions = Arrays.copyOf(functions, capacity);
                operands = Arrays.copyOf(operands, capacity);
                stateSlots = Arrays.copyOf(stateSlots, capacity);
            }
            kinds[size] = kind;
            functions[size] = function;
            operands[size] = operand;
            stateSlots[size] = stateSlot;
            size++;
        }

        private void ensureMutable() {
            if (frozen) throw new IllegalStateException("builder already frozen");
        }

        private static long saturatingAdd(long left, long right) {
            return left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
        }
    }
}
