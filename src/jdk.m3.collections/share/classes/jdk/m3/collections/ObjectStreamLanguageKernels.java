// SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
// Copyright (c) 2026 Synexia contributors
package jdk.m3.collections;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Lazy one-pass kernels shared by the replay-aware ObjectStream facade.
 * Per-element operations retain only cursors/scalars, never heap Entry/Pair nodes.
 * All state belongs to a single opened stream, not to a reusable plan.
 */
final class ObjectStreamLanguageKernels {
    private ObjectStreamLanguageKernels() { }

    private static <T> Stream<T> wrap(Stream<?> input, Spliterator<T> output) {
        return StreamSupport.stream(output, false).onClose(input::close);
    }

    static <T, R> Stream<R> mapIndexedOrdinalFirst(Stream<T> input,
            ObjectStream.OrdinalFirstMapper<? super T, ? extends R> mapper) {
        Iterator<T> source = input.iterator();
        Spliterator<R> output = new Spliterators.AbstractSpliterator<>(
                Long.MAX_VALUE, Spliterator.ORDERED) {
            private long index;
            @Override public boolean tryAdvance(Consumer<? super R> action) {
                Objects.requireNonNull(action, "action");
                if (!source.hasNext()) return false;
                long current = index;
                index = Math.addExact(index, 1L);
                action.accept(mapper.apply(current, source.next()));
                return true;
            }
        };
        return wrap(input, output);
    }

    static <T, R> Stream<R> zipWithNext(Stream<T> input,
            BiFunction<? super T, ? super T, ? extends R> mapper) {
        Iterator<T> source = input.iterator();
        Spliterator<R> output = new Spliterators.AbstractSpliterator<>(
                Long.MAX_VALUE, Spliterator.ORDERED) {
            private boolean initialized;
            private T previous;
            @Override public boolean tryAdvance(Consumer<? super R> action) {
                Objects.requireNonNull(action, "action");
                if (!initialized) {
                    if (!source.hasNext()) return false;
                    previous = source.next();
                    initialized = true;
                }
                if (!source.hasNext()) { previous = null; return false; }
                T current = source.next();
                R mapped = mapper.apply(previous, current);
                previous = current;
                action.accept(mapped);
                return true;
            }
        };
        return wrap(input, output);
    }

    static <T> Stream<T> runningReduce(Stream<T> input, BinaryOperator<T> reducer) {
        Iterator<T> source = input.iterator();
        Spliterator<T> output = new Spliterators.AbstractSpliterator<>(
                Long.MAX_VALUE, Spliterator.ORDERED) {
            private boolean initialized;
            private T state;
            @Override public boolean tryAdvance(Consumer<? super T> action) {
                Objects.requireNonNull(action, "action");
                if (!source.hasNext()) return false;
                T next = source.next();
                state = initialized ? reducer.apply(state, next) : next;
                initialized = true;
                action.accept(state);
                return true;
            }
        };
        return wrap(input, output);
    }

    /** Group consecutive equal keys, not all matching keys across the source. */
    static <T, K> Stream<List<T>> chunkBy(Stream<T> input,
            Function<? super T, ? extends K> keyExtractor, int maxRunLength) {
        Iterator<T> source = input.iterator();
        Spliterator<List<T>> output = new Spliterators.AbstractSpliterator<>(
                Long.MAX_VALUE, Spliterator.ORDERED | Spliterator.NONNULL) {
            private boolean pending;
            private T pendingValue;
            private K pendingKey;
            @Override public boolean tryAdvance(Consumer<? super List<T>> action) {
                Objects.requireNonNull(action, "action");
                if (!pending && !source.hasNext()) return false;
                T first;
                K key;
                if (pending) {
                    first = pendingValue;
                    key = pendingKey;
                    pendingValue = null;
                    pendingKey = null;
                    pending = false;
                } else {
                    first = source.next();
                    key = keyExtractor.apply(first);
                }
                ArrayList<T> run = new ArrayList<>();
                run.add(first);
                while (source.hasNext()) {
                    T next = source.next();
                    K nextKey = keyExtractor.apply(next);
                    if (!Objects.equals(key, nextKey)) {
                        pending = true;
                        pendingValue = next;
                        pendingKey = nextKey;
                        break;
                    }
                    if (run.size() >= maxRunLength) {
                        throw new IllegalStateException("contiguous run budget exceeded");
                    }
                    run.add(next);
                }
                action.accept(Collections.unmodifiableList(run));
                return true;
            }
        };
        return wrap(input, output);
    }

    /** Equality is between adjacent projected keys; no global HashSet is retained. */
    static <T, K> Stream<T> distinctUntilChanged(Stream<T> input,
            Function<? super T, ? extends K> keyExtractor) {
        return input.filter(new java.util.function.Predicate<>() {
            private boolean initialized;
            private K previousKey;
            @Override public boolean test(T value) {
                K current = keyExtractor.apply(value);
                if (initialized && Objects.equals(previousKey, current)) return false;
                initialized = true;
                previousKey = current;
                return true;
            }
        });
    }

    /** Insert a separator strictly between values; null separators are permitted. */
    static <T> Stream<T> intersperse(Stream<T> input, T separator) {
        Iterator<T> source = input.iterator();
        Spliterator<T> output = new Spliterators.AbstractSpliterator<>(
                Long.MAX_VALUE, Spliterator.ORDERED) {
            private boolean started;
            private boolean buffered;
            private T next;
            @Override public boolean tryAdvance(Consumer<? super T> action) {
                Objects.requireNonNull(action, "action");
                if (!started) {
                    if (!source.hasNext()) return false;
                    started = true;
                    action.accept(source.next());
                    return true;
                }
                if (buffered) {
                    buffered = false;
                    T value = next;
                    next = null;
                    action.accept(value);
                    return true;
                }
                if (!source.hasNext()) return false;
                next = source.next();
                buffered = true;
                action.accept(separator);
                return true;
            }
        };
        return wrap(input, output);
    }

    /** Pair to the shorter side, without making tuples or buffering either input. */
    static <L, R, O> Stream<O> zip(Stream<? extends L> left, Stream<? extends R> right,
            BiFunction<? super L, ? super R, ? extends O> mapper) {
        Iterator<? extends L> a = left.iterator();
        Iterator<? extends R> b = right.iterator();
        Spliterator<O> output = new Spliterators.AbstractSpliterator<>(
                Long.MAX_VALUE, Spliterator.ORDERED) {
            @Override public boolean tryAdvance(Consumer<? super O> action) {
                Objects.requireNonNull(action, "action");
                if (!a.hasNext() || !b.hasNext()) return false;
                action.accept(mapper.apply(a.next(), b.next()));
                return true;
            }
        };
        return StreamSupport.stream(output, false)
                .onClose(left::close).onClose(right::close);
    }

    /** Stepped windows with bounded ring storage and demand-driven gap traversal. */
    static <T> Stream<List<T>> collate(Stream<T> input, int width, int step,
            ObjectStream.TailPolicy tailPolicy) {
        CollatingSpliterator<T> windows =
                new CollatingSpliterator<>(input.iterator(), width, step, tailPolicy);
        return StreamSupport.stream(windows, false)
                .onClose(windows::clear).onClose(input::close);
    }

    /**
     * Each traversal owns its cursor and ring. A window start advances by a
     * bounded removal followed by a gap countdown, never by multiplying or
     * adding unbounded source ordinals.
     */
    private static final class CollatingSpliterator<T>
            extends Spliterators.AbstractSpliterator<List<T>> {
        private Iterator<T> source;
        private final int width;
        private final int step;
        private final ObjectStream.TailPolicy tailPolicy;
        private Object[] ring;
        private int head;
        private int size;
        private boolean started;
        private boolean sourceExhausted;
        private boolean finished;

        CollatingSpliterator(Iterator<T> source, int width, int step,
                ObjectStream.TailPolicy tailPolicy) {
            super(Long.MAX_VALUE, Spliterator.ORDERED | Spliterator.NONNULL);
            this.source = source;
            this.width = width;
            this.step = step;
            this.tailPolicy = tailPolicy;
        }

        @Override public boolean tryAdvance(Consumer<? super List<T>> action) {
            Objects.requireNonNull(action, "action");
            if (finished) return false;
            if (started) {
                advanceStart();
            } else {
                started = true;
            }
            while (size < width && sourceHasNext()) append(source.next());
            if (size == 0 || size < width && tailPolicy == ObjectStream.TailPolicy.DROP) {
                clear();
                return false;
            }
            action.accept(snapshot());
            return true;
        }

        @Override public Spliterator<List<T>> trySplit() {
            // Splitting would otherwise consume and buffer future windows.
            return null;
        }

        private boolean sourceHasNext() {
            if (sourceExhausted) return false;
            if (source.hasNext()) return true;
            sourceExhausted = true;
            return false;
        }

        private void advanceStart() {
            int removed = Math.min(step, size);
            for (int i = 0; i < removed; i++) {
                ring[head] = null;
                if (++head == ring.length) head = 0;
            }
            size -= removed;
            if (size == 0) head = 0;
            int gap = step - removed;
            while (gap > 0 && sourceHasNext()) {
                source.next();
                gap--;
            }
        }

        private void append(T value) {
            if (ring == null) {
                ring = new Object[Math.min(width, 16)];
            } else if (size == ring.length) {
                int capacity = Math.min(width, ring.length * 2);
                Object[] grown = new Object[capacity];
                int first = Math.min(size, ring.length - head);
                System.arraycopy(ring, head, grown, 0, first);
                System.arraycopy(ring, 0, grown, first, size - first);
                ring = grown;
                head = 0;
            }
            int tail = head + size;
            if (tail >= ring.length) tail -= ring.length;
            ring[tail] = value;
            size++;
        }

        private List<T> snapshot() {
            ArrayList<T> values = new ArrayList<>(size);
            int cursor = head;
            for (int i = 0; i < size; i++) {
                @SuppressWarnings("unchecked") T value = (T) ring[cursor];
                values.add(value);
                if (++cursor == ring.length) cursor = 0;
            }
            return Collections.unmodifiableList(values);
        }

        private void clear() {
            if (ring != null) {
                for (int i = 0; i < ring.length; i++) ring[i] = null;
                ring = null;
            }
            source = null;
            head = 0;
            size = 0;
            sourceExhausted = true;
            finished = true;
        }
    }
}
