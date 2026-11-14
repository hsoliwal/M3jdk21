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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Immutable registry of caller-owned pools. Releasing/clearing a pool is explicit.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/cop/pool/ValuePools.java}
 * blob {@code 5e861227173f490cf75f30a9b8ef5950e7d8ad75} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class ValuePools {
    private final Map<Class<?>, ImmutablePool<?>> pools;

    public ValuePools() { this(Map.of()); }
    private ValuePools(Map<Class<?>, ImmutablePool<?>> pools) { this.pools = Map.copyOf(pools); }

    /** Returns an extended registry; conflicting registration cannot silently replace live IDs. */
    public ValuePools with(ImmutablePool<?> pool) {
        Objects.requireNonNull(pool, "pool");
        Map<Class<?>, ImmutablePool<?>> extended = new HashMap<>(pools);
        if (extended.putIfAbsent(pool.type(), pool) != null) {
            throw new IllegalArgumentException("type already registered: " + pool.type().getName());
        }
        return new ValuePools(extended);
    }

    public boolean contains(Class<?> type) { return pools.containsKey(type); }

    public Object canonicalize(Object value) {
        if (value == null) return null;
        ImmutablePool<?> pool = pools.get(value.getClass());
        return pool == null ? value : intern(pool, value);
    }

    /** The class-key invariant makes this cast safe; callers cannot mutate the registry. */
    @SuppressWarnings("unchecked")
    public <T> Optional<ImmutablePool<T>> pool(Class<T> type) {
        return Optional.ofNullable((ImmutablePool<T>) pools.get(type));
    }

    private static <T> T intern(ImmutablePool<T> pool, Object value) {
        return pool.intern(pool.type().cast(value));
    }

    /** Standard exact JDK value classes; each pool receives its own entry/weight budget. */
    public static ValuePools standard(int maxEntries, long maxWeight) {
        ValuePools registry = new ValuePools().with(new ImmutablePool<>(
                String.class, maxEntries, maxWeight, value -> 2L * value.length()));
        for (Class<?> type : new Class<?>[] {Boolean.class, Byte.class, Short.class,
                Character.class, Integer.class, Long.class, Float.class, Double.class,
                UUID.class, BigInteger.class, BigDecimal.class}) {
            registry = addStandard(registry, type, maxEntries, maxWeight);
        }
        return registry;
    }

    private static <T> ValuePools addStandard(ValuePools registry, Class<T> type,
                                              int maxEntries, long maxWeight) {
        return registry.with(new ImmutablePool<>(type, maxEntries, maxWeight, value -> {
            if (value instanceof BigInteger integer) return 16L + (integer.bitLength() + 7L) / 8L;
            if (value instanceof BigDecimal decimal) return 24L + (decimal.unscaledValue().bitLength() + 7L) / 8L;
            return 16L;
        }));
    }
}
