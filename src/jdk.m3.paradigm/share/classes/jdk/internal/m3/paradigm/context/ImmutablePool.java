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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.ToLongFunction;

/**
 * Bounded, thread-safe, strong canonicalization of one explicitly immutable value type.
 *
 * <p>Construction asserts a caller-reviewed contract: values are deeply immutable,
 * equals/hashCode are stable and express interchangeable values, and weight is pure.
 * Final fields/records alone are insufficient. Exact classes only; no subclass inference.
 * Identity-sensitive objects, monitors and live contexts must not be admitted.
 * IDs are dense within one pool generation; no eviction can silently change their meaning.
 * Weight is a caller-defined budget, not a measured Java heap size.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/cop/pool/ImmutablePool.java}
 * blob {@code 4edcc5bb977aabdacf213ce261b00e295d1d219e} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class ImmutablePool<T> implements AutoCloseable {
    private final Class<T> type;
    private final UUID scope = UUID.randomUUID();
    private final int maxEntries;
    private final long maxWeight;
    private final ToLongFunction<? super T> weight;
    private final Map<T, Integer> indices = new HashMap<>();
    private final ArrayList<T> values = new ArrayList<>();
    private long generation;
    private long retainedWeight;
    private long hits;
    private boolean closed;

    public ImmutablePool(Class<T> type, int maxEntries, long maxWeight,
                         ToLongFunction<? super T> weight) {
        this.type = Objects.requireNonNull(type, "type");
        if (type.isPrimitive() || maxEntries < 0 || maxWeight < 0) {
            throw new IllegalArgumentException("reference type and non-negative budgets required");
        }
        this.maxEntries = maxEntries;
        this.maxWeight = maxWeight;
        this.weight = Objects.requireNonNull(weight, "weight");
    }

    public Class<T> type() { return type; }

    public synchronized T intern(T value) { return values.get(index(value)); }

    public synchronized Id internId(T value) { return new Id(scope, generation, index(value)); }

    public synchronized Optional<T> find(T value) {
        check(value);
        Integer index = indices.get(value);
        return index == null ? Optional.empty() : Optional.of(values.get(index));
    }

    public synchronized T resolve(Id id) {
        ensureOpen();
        Objects.requireNonNull(id, "id");
        if (!scope.equals(id.scope())) throw new IllegalArgumentException("foreign pool id");
        if (generation != id.generation()) throw new IllegalStateException("stale pool id");
        return values.get(id.index());
    }

    /** Explicit identity boundary: old IDs become invalid, old objects may remain held by callers. */
    public synchronized void clear() {
        ensureOpen();
        generation = Math.incrementExact(generation);
        indices.clear();
        values.clear();
        retainedWeight = 0;
    }

    public synchronized Stats stats() {
        return new Stats(values.size(), retainedWeight, hits, generation, closed);
    }

    @Override
    public synchronized void close() {
        indices.clear();
        values.clear();
        retainedWeight = 0;
        closed = true;
    }

    private int index(T value) {
        check(value);
        Integer existing = indices.get(value);
        if (existing != null) { hits++; return existing; }
        long cost = weight.applyAsLong(value);
        if (cost < 0) throw new IllegalArgumentException("negative value weight");
        if (values.size() >= maxEntries || cost > maxWeight - retainedWeight) {
            throw new IllegalStateException("immutable pool capacity exhausted");
        }
        int index = values.size();
        indices.put(value, index);
        values.add(value);
        retainedWeight += cost;
        return index;
    }

    private void check(T value) {
        ensureOpen();
        if (Objects.requireNonNull(value, "value").getClass() != type) {
            throw new IllegalArgumentException("value must have exact registered type " + type.getName());
        }
    }

    private void ensureOpen() {
        if (closed) throw new IllegalStateException("immutable pool closed");
    }

    public record Id(UUID scope, long generation, int index) {
        public Id {
            Objects.requireNonNull(scope, "scope");
            if (generation < 0 || index < 0) throw new IllegalArgumentException("invalid pool id");
        }
    }

    public record Stats(int entries, long retainedWeight, long hits, long generation, boolean closed) { }
}
