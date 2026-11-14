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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Heterogeneous compatibility domain backed by the bounded, generation-aware value pool.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/ContextIdentityPool.java}
 * blob {@code c747be799bdf4af7ebca889503705a46522b8917} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class IdentityPool implements AutoCloseable {
    private record Value(Class<?> type, Object value) { }
    private final ImmutablePool<Value> values;

    public IdentityPool(int capacity) {
        values = new ImmutablePool<>(Value.class, capacity, capacity, ignored -> 1L);
    }

    /** Admission requires a codec (including structurally checked records) or a deep-immutability marker. */
    public static boolean isKnownImmutable(Object value) {
        return value != null && (value instanceof Immutable
                || ImmutablePools.tryPool(value.getClass()).isPresent());
    }

    public <T> T intern(T value) {
        if (!isKnownImmutable(value)) throw new IllegalArgumentException("Explicit immutable contract required");
        return admit(value);
    }

    public <T> Optional<T> tryIntern(T value) {
        if (!isKnownImmutable(value)) return Optional.empty();
        try { return Optional.of(intern(value)); }
        catch (IllegalStateException capacityOrClosed) { return Optional.empty(); }
    }

    @SuppressWarnings("unchecked")
    private <T> T admit(T value) {
        return (T) values.intern(new Value(value.getClass(), value)).value();
    }

    public <E> List<E> internList(Iterable<? extends E> source) {
        List<E> copy = new ArrayList<>();
        source.forEach(value -> copy.add(intern(value)));
        return admit(List.copyOf(copy));
    }

    public <E> Set<E> internSet(Iterable<? extends E> source) {
        Set<E> copy = new LinkedHashSet<>();
        source.forEach(value -> copy.add(intern(value)));
        return admit(Set.copyOf(copy));
    }

    public <K, V> Map<K, V> internMap(Map<? extends K, ? extends V> source) {
        Map<K, V> copy = new LinkedHashMap<>();
        source.forEach((key, value) -> copy.put(intern(key), intern(value)));
        return admit(Map.copyOf(copy));
    }

    public ImmutablePool.Stats stats() { return values.stats(); }
    public int size() { return values.stats().entries(); }
    public void clear() { values.clear(); }
    @Override public void close() { values.close(); }
}
