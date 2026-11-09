// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.ObjLongConsumer;
import java.util.function.Supplier;

/** Deferred DOOP accumulator: only the client-supplied state is created at evaluation. */
public final class VirtualAccumulator<A> {
    private final LongDag plan;
    private final Supplier<? extends A> seed;
    private final ObjLongConsumer<? super A> accumulator;

    VirtualAccumulator(LongDag plan, Supplier<? extends A> seed,
            ObjLongConsumer<? super A> accumulator) {
        this.plan = Objects.requireNonNull(plan, "plan");
        this.seed = Objects.requireNonNull(seed, "seed");
        this.accumulator = Objects.requireNonNull(accumulator, "accumulator");
    }

    /** Re-run the parent DAG; never retain a previous accumulator state. */
    public A evaluate() { return finish(Function.identity()).evaluate(); }

    /** Deferred finalization, sharing the same parent-owned DAG. */
    public <R> VirtualCollector<A, R> finish(Function<? super A, ? extends R> finisher) {
        return new VirtualCollector<>(plan, seed, accumulator, finisher);
    }
}
