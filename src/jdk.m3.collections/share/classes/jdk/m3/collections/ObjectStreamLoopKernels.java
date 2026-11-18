// SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
// Copyright (c) 2026 Synexia contributors
package jdk.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * One-pass, non-node-allocating stream kernels for proven hand-written loop idioms.
 * Every opened traversal owns its state; no state lives in the reusable plan.
 */
final class ObjectStreamLoopKernels {
    private ObjectStreamLoopKernels() { }

    private static <R> Stream<R> wrap(Stream<?> input, Spliterator<R> output) {
        return StreamSupport.stream(output, false).onClose(input::close);
    }

    /** Rust Iterator::map_while shape, using null as stop, not an output value. */
    static <T, R> Stream<R> mapWhile(Stream<T> input,
            Function<? super T, ? extends R> mapper) {
        Iterator<T> source = input.iterator();
        Spliterator<R> output = new Spliterators.AbstractSpliterator<>(
                Long.MAX_VALUE, Spliterator.ORDERED | Spliterator.NONNULL) {
            private boolean stopped;
            @Override public boolean tryAdvance(Consumer<? super R> action) {
                Objects.requireNonNull(action, "action");
                if (stopped || !source.hasNext()) return false;
                R mapped = mapper.apply(source.next());
                if (mapped == null) { stopped = true; return false; }
                action.accept(mapped);
                return true;
            }
        };
        return wrap(input, output);
    }

    /** Rust step_by and Python islice with step: retain every stride-th value. */
    static <T> Stream<T> stepBy(Stream<T> input, int stride) {
        Iterator<T> source = input.iterator();
        Spliterator<T> output = new Spliterators.AbstractSpliterator<>(
                Long.MAX_VALUE, Spliterator.ORDERED) {
            private boolean first = true;
            @Override public boolean tryAdvance(Consumer<? super T> action) {
                Objects.requireNonNull(action, "action");
                if (!first) {
                    for (int skipped = 1; skipped < stride; skipped++) {
                        if (!source.hasNext()) return false;
                        source.next();
                    }
                }
                if (!source.hasNext()) return false;
                first = false;
                action.accept(source.next());
                return true;
            }
        };
        return wrap(input, output);
    }

    /** Constant-space merge of two already-sorted sources; equal keys go left first. */
    static <T> Stream<T> mergeSorted(Stream<? extends T> left,
            Stream<? extends T> right, Comparator<? super T> comparator) {
        Iterator<? extends T> l = left.iterator();
        Iterator<? extends T> r = right.iterator();
        Spliterator<T> output = new Spliterators.AbstractSpliterator<>(
                Long.MAX_VALUE, Spliterator.ORDERED) {
            private boolean lReady;
            private boolean rReady;
            private T lValue;
            private T rValue;
            @Override public boolean tryAdvance(Consumer<? super T> action) {
                Objects.requireNonNull(action, "action");
                if (!lReady && l.hasNext()) { lValue = l.next(); lReady = true; }
                if (!rReady && r.hasNext()) { rValue = r.next(); rReady = true; }
                if (!lReady && !rReady) return false;
                if (lReady && (!rReady || comparator.compare(lValue, rValue) <= 0)) {
                    T emit = lValue;
                    lValue = null;
                    lReady = false;
                    action.accept(emit);
                } else {
                    T emit = rValue;
                    rValue = null;
                    rReady = false;
                    action.accept(emit);
                }
                return true;
            }
        };
        return StreamSupport.stream(output, false).onClose(left::close).onClose(right::close);
    }

    /** Lazy bounded lookahead: drop final count values without buffering all input. */
    static <T> Stream<T> dropLast(Stream<T> input, int count) {
        if (count == 0) return input;
        Iterator<T> source = input.iterator();
        Spliterator<T> output = new Spliterators.AbstractSpliterator<>(
                Long.MAX_VALUE, Spliterator.ORDERED) {
            private final Object[] ring = new Object[count];
            private boolean initialized;
            private boolean exhausted;
            private int head;
            @Override public boolean tryAdvance(Consumer<? super T> action) {
                Objects.requireNonNull(action, "action");
                if (exhausted) return false;
                if (!initialized) {
                    initialized = true;
                    for (int i = 0; i < count; i++) {
                        if (!source.hasNext()) {
                            exhausted = true;
                            Arrays.fill(ring, null);
                            return false;
                        }
                        ring[i] = source.next();
                    }
                }
                if (!source.hasNext()) {
                    exhausted = true;
                    Arrays.fill(ring, null);
                    return false;
                }
                @SuppressWarnings("unchecked") T emit = (T) ring[head];
                ring[head] = source.next();
                head = (head + 1) % count;
                action.accept(emit);
                return true;
            }
        };
        return wrap(input, output);
    }

    /** Blocking but strictly bounded tail collector; closes source before yielding. */
    static <T> Stream<T> takeLast(Stream<T> input, int count) {
        if (count == 0) {
            input.close();
            return Stream.empty();
        }
        Object[] ring = new Object[count];
        int size = 0;
        int head = 0;
        try (input) {
            Iterator<T> source = input.iterator();
            while (source.hasNext()) {
                Object value = source.next();
                if (size < count) {
                    ring[size++] = value;
                } else {
                    ring[head] = value;
                    head = (head + 1) % count;
                }
            }
        }
        ArrayList<T> tail = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            @SuppressWarnings("unchecked") T value = (T) ring[(head + i) % count];
            tail.add(value);
        }
        return Collections.unmodifiableList(tail).stream();
    }

    /** Kotlin runningFoldIndexed includes the initializer even for empty input. */
    static <T, A> Stream<A> runningFoldIndexed(Stream<T> input,
            Supplier<? extends A> initializer, ObjectStream.IndexedFolder<A, T> folder) {
        Iterator<T> source = input.iterator();
        Spliterator<A> output = new Spliterators.AbstractSpliterator<>(
                Long.MAX_VALUE, Spliterator.ORDERED) {
            private boolean seeded;
            private long index;
            private A state;
            @Override public boolean tryAdvance(Consumer<? super A> action) {
                Objects.requireNonNull(action, "action");
                if (!seeded) {
                    seeded = true;
                    state = initializer.get();
                    action.accept(state);
                    return true;
                }
                if (!source.hasNext()) return false;
                long current = index;
                index = Math.addExact(index, 1L);
                state = folder.apply(current, state, source.next());
                action.accept(state);
                return true;
            }
        };
        return wrap(input, output);
    }

    /** Rust itertools coalesce idea: fuse adjacent compatible elements without nodes. */
    static <T> Stream<T> coalesceAdjacent(Stream<T> input,
            BiPredicate<? super T, ? super T> compatible, BinaryOperator<T> merge) {
        Iterator<T> source = input.iterator();
        Spliterator<T> output = new Spliterators.AbstractSpliterator<>(
                Long.MAX_VALUE, Spliterator.ORDERED) {
            private boolean initialized;
            private boolean finished;
            private T current;
            @Override public boolean tryAdvance(Consumer<? super T> action) {
                Objects.requireNonNull(action, "action");
                if (finished) return false;
                if (!initialized) {
                    if (!source.hasNext()) { finished = true; return false; }
                    current = source.next();
                    initialized = true;
                }
                while (source.hasNext()) {
                    T next = source.next();
                    if (compatible.test(current, next)) {
                        current = merge.apply(current, next);
                    } else {
                        T emit = current;
                        current = next;
                        action.accept(emit);
                        return true;
                    }
                }
                T emit = current;
                current = null;
                finished = true;
                action.accept(emit);
                return true;
            }
        };
        return wrap(input, output);
    }
}
