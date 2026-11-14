/* ******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.internal.m3.paradigm.context;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Stable top-k selection using the JDK heap. Admission precedes scoring and truncation.
 * Keeps at most k scored entries; scans the supplied iterable once. Callbacks must be
 * host-owned, bounded and side-effect-free. No executor or inventory is owned here.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/cop/dynamic/RankedCandidates.java}
 * blob {@code f3b6eef20b0b01f9d33421f0c5b80e577b023ba1} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
final class RankedCandidates {
    private RankedCandidates() { }

    private record Entry<R>(R value, long ordinal) { }

    static <T, R> List<R> select(
            Iterable<T> source, Predicate<? super T> eligible,
            Function<? super T, Optional<R>> score, Comparator<? super R> order,
            int limit, BooleanSupplier cancelled) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(eligible, "eligible");
        Objects.requireNonNull(score, "score");
        Objects.requireNonNull(order, "order");
        Objects.requireNonNull(cancelled, "cancelled");
        if (limit < 1 || limit > 1024) {
            throw new IllegalArgumentException("candidate limit must be within [1,1024]");
        }
        Comparator<Entry<R>> stable = (left, right) -> {
            int compared = order.compare(left.value(), right.value());
            return compared != 0 ? compared : Long.compare(left.ordinal(), right.ordinal());
        };
        PriorityQueue<Entry<R>> retained = new PriorityQueue<>(limit, stable.reversed());
        long ordinal = 0;
        check(cancelled);
        for (T item : source) {
            check(cancelled);
            Objects.requireNonNull(item, "candidate");
            if (!eligible.test(item)) {
                continue;
            }
            check(cancelled);
            Optional<R> value = Objects.requireNonNull(score.apply(item), "score result");
            check(cancelled);
            if (value.isEmpty()) {
                continue;
            }
            Entry<R> entry = new Entry<>(value.orElseThrow(), ordinal);
            ordinal = Math.incrementExact(ordinal);
            if (retained.size() < limit) {
                retained.add(entry);
            } else if (stable.compare(entry, retained.element()) < 0) {
                retained.remove();
                retained.add(entry);
            }
        }
        check(cancelled);
        List<R> result = retained.stream().sorted(stable).map(Entry::value).toList();
        check(cancelled);
        return result;
    }

    private static void check(BooleanSupplier cancelled) {
        if (Thread.currentThread().isInterrupted() || cancelled.getAsBoolean()) {
            throw new CancellationException("COP_CANDIDATE_RANKING_CANCELLED");
        }
    }
}
