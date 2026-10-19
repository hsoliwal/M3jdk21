// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.ObjLongConsumer;
import java.util.function.Supplier;
import java.util.stream.LongStream;

/** Deferred terminal adapter; independent state per evaluation, no retained input elements. */
public final class M3VirtualCollector<A, R> {
    private final M3LongDag plan;
    private final Supplier<? extends A> seed;
    private final ObjLongConsumer<? super A> accumulator;
    private final Function<? super A, ? extends R> finisher;

    M3VirtualCollector(M3LongDag plan, Supplier<? extends A> seed,
            ObjLongConsumer<? super A> accumulator, Function<? super A, ? extends R> finisher) {
        this.plan = Objects.requireNonNull(plan, "plan");
        this.seed = Objects.requireNonNull(seed, "seed");
        this.accumulator = Objects.requireNonNull(accumulator, "accumulator");
        this.finisher = Objects.requireNonNull(finisher, "finisher");
    }

    /** A new traversal and one new state; finish only after successful input exhaustion. */
    public R evaluate() {
        A current = seed.get();
        try (LongStream source = plan.stream()) {
            source.forEachOrdered(value -> accumulator.accept(current, value));
        }
        return finisher.apply(current);
    }

    /** Post-collection projection; never starts another input traversal itself. */
    public <U> M3VirtualCollector<A, U> mapResult(Function<? super R, ? extends U> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        return new M3VirtualCollector<>(plan, seed, accumulator,
                state -> mapper.apply(finisher.apply(state)));
    }

    /**
     * Fan two accumulators into one traversal. Both must refer to the exact same plan;
     * otherwise this method refuses rather than silently dropping another source.
     * No per-element tuple is allocated; only the one paired state is retained.
     */
    public <B, S, U> M3VirtualCollector<Pair<A, B>, U> tee(
            M3VirtualCollector<B, S> other, BiFunction<? super R, ? super S, ? extends U> merger) {
        Objects.requireNonNull(other, "other");
        Objects.requireNonNull(merger, "merger");
        if (plan != other.plan) throw new IllegalArgumentException("tee requires one identical DAG plan");
        return new M3VirtualCollector<>(plan,
                () -> new Pair<>(seed.get(), other.seed.get()),
                (pair, value) -> {
                    accumulator.accept(pair.left(), value);
                    other.accumulator.accept(pair.right(), value);
                },
                pair -> merger.apply(finisher.apply(pair.left()), other.finisher.apply(pair.right())));
    }

    /** One pair per invocation; contained accumulator state can be mutable. */
    public record Pair<X, Y>(X left, Y right) { }
}
