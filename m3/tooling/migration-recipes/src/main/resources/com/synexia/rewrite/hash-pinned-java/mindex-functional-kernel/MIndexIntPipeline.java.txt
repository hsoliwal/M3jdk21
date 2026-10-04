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
import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;
import java.util.function.IntUnaryOperator;

/**
 * Immutable fused int map/filter/slice pipeline.
 *
 * <p>Builder-time fusion collapses adjacent MIndex maps and predicates. Execution with a reusable
 * Workspace creates no per-element objects. Ordinary JDK functional interfaces remain valid
 * fallback stages.</p>
 */
public final class MIndexIntPipeline {
    private static final byte MAP = 1;
    private static final byte FILTER = 2;
    private static final byte SKIP = 3;
    private static final byte LIMIT = 4;
    private static final long PRESENT = Long.MIN_VALUE;

    private final byte[] kinds;
    private final Object[] functions;
    private final long[] operands;
    private final int[] stateSlots;
    private final int stateCount;

    private MIndexIntPipeline(byte[] kinds, Object[] functions, long[] operands,
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

    public int transform(int[] input, int[] output, Workspace workspace) {
        Objects.requireNonNull(input);
        Objects.requireNonNull(output);
        if (output.length < input.length) {
            throw new IllegalArgumentException("output must have worst-case input capacity");
        }
        Workspace state = prepared(workspace);
        int written = 0;
        for (int inputValue : input) {
            long encoded = applyEncoded(inputValue, state);
            if ((encoded & PRESENT) != 0L) output[written++] = (int) encoded;
            if (state.stopped) break;
        }
        return written;
    }

    public int count(int[] input, Workspace workspace) {
        Objects.requireNonNull(input);
        Workspace state = prepared(workspace);
        int count = 0;
        for (int inputValue : input) {
            if ((applyEncoded(inputValue, state) & PRESENT) != 0L) count++;
            if (state.stopped) break;
        }
        return count;
    }

    public long sum(int[] input, Workspace workspace) {
        Objects.requireNonNull(input);
        Workspace state = prepared(workspace);
        long result = 0L;
        for (int inputValue : input) {
            long encoded = applyEncoded(inputValue, state);
            if ((encoded & PRESENT) != 0L) result += (int) encoded;
            if (state.stopped) break;
        }
        return result;
    }

    public int reduce(int[] input, int identity,
            IntBinaryOperator reducer, Workspace workspace) {
        Objects.requireNonNull(input);
        Objects.requireNonNull(reducer);
        Workspace state = prepared(workspace);
        int result = identity;
        for (int inputValue : input) {
            long encoded = applyEncoded(inputValue, state);
            if ((encoded & PRESENT) != 0L) result = reducer.applyAsInt(result, (int) encoded);
            if (state.stopped) break;
        }
        return result;
    }

    public boolean anyMatch(int[] input, IntPredicate predicate, Workspace workspace) {
        Objects.requireNonNull(input);
        Objects.requireNonNull(predicate);
        Workspace state = prepared(workspace);
        for (int inputValue : input) {
            long encoded = applyEncoded(inputValue, state);
            if ((encoded & PRESENT) != 0L && predicate.test((int) encoded)) return true;
            if (state.stopped) break;
        }
        return false;
    }

    public boolean allMatch(int[] input, IntPredicate predicate, Workspace workspace) {
        Objects.requireNonNull(input);
        Objects.requireNonNull(predicate);
        Workspace state = prepared(workspace);
        for (int inputValue : input) {
            long encoded = applyEncoded(inputValue, state);
            if ((encoded & PRESENT) != 0L && !predicate.test((int) encoded)) return false;
            if (state.stopped) break;
        }
        return true;
    }

    public void forEach(int[] input, IntConsumer consumer, Workspace workspace) {
        Objects.requireNonNull(input);
        Objects.requireNonNull(consumer);
        Workspace state = prepared(workspace);
        for (int inputValue : input) {
            long encoded = applyEncoded(inputValue, state);
            if ((encoded & PRESENT) != 0L) consumer.accept((int) encoded);
            if (state.stopped) break;
        }
    }

    public long findFirstEncoded(int[] input, Workspace workspace) {
        Objects.requireNonNull(input);
        Workspace state = prepared(workspace);
        for (int inputValue : input) {
            long encoded = applyEncoded(inputValue, state);
            if ((encoded & PRESENT) != 0L) {
                return encoded;
            }
            if (state.stopped) break;
        }
        return 0L;
    }

    public static boolean encodedPresent(long encoded) { return (encoded & PRESENT) != 0L; }
    public static int encodedInt(long encoded) {
        if (!encodedPresent(encoded)) throw new NoSuchElementException();
        return (int) encoded;
    }

    private Workspace prepared(Workspace workspace) {
        Workspace state = Objects.requireNonNull(workspace);
        state.prepare(stateCount);
        return state;
    }

    private long applyEncoded(int input, Workspace state) {
        if (state.stopped) return 0L;
        int value = input;
        boolean stopAfterCurrent = false;
        for (int stage = 0; stage < kinds.length; stage++) {
            switch (kinds[stage]) {
                case MAP -> value = ((IntUnaryOperator) functions[stage]).applyAsInt(value);
                case FILTER -> {
                    if (!((IntPredicate) functions[stage]).test(value)) return 0L;
                }
                case SKIP -> {
                    int slot = stateSlots[stage];
                    if (state.counters[slot] < operands[stage]) {
                        state.counters[slot]++;
                        return 0L;
                    }
                }
                case LIMIT -> {
                    int slot = stateSlots[stage];
                    if (state.counters[slot] >= operands[stage]) {
                        state.stopped = true;
                        return 0L;
                    }
                    if (++state.counters[slot] == operands[stage]) stopAfterCurrent = true;
                }
                default -> throw new AssertionError(kinds[stage]);
            }
        }
        if (stopAfterCurrent) state.stopped = true;
        return PRESENT | Integer.toUnsignedLong(value);
    }

    public static final class Workspace {
        private long[] counters;
        private boolean stopped;
        

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

        public Builder map(IntUnaryOperator operator) {
            ensureMutable();
            Objects.requireNonNull(operator);
            if (size > 0 && kinds[size - 1] == MAP
                    && functions[size - 1] instanceof MIndexIntUnaryOperator left
                    && operator instanceof MIndexIntUnaryOperator right) {
                functions[size - 1] = left.andThen(right);
                return this;
            }
            append(MAP, operator, 0L, -1);
            return this;
        }

        public Builder filter(IntPredicate predicate) {
            ensureMutable();
            Objects.requireNonNull(predicate);
            if (size > 0 && kinds[size - 1] == FILTER
                    && functions[size - 1] instanceof MIndexIntPredicate left
                    && predicate instanceof MIndexIntPredicate right) {
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

        public MIndexIntPipeline freeze() {
            ensureMutable();
            frozen = true;
            return new MIndexIntPipeline(
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
