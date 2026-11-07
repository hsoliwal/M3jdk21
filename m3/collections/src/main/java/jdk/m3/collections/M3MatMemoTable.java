/*******************************************************************************
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
package jdk.m3.collections;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.function.IntSupplier;

/**
 * Bounded memoization from immutable Mat handles to one immutable result-pool handle.
 *
 * <p>The operation id must include semantic/version context. Callers may use this only for pure
 * computations whose result is determined entirely by the supplied immutable handles and operation
 * id. Time, I/O, randomness, ambient context and other effects are intentionally outside this API.</p>
 *
 * <p>Lineage: absorbed from Synexia {@code m3-mat-value/src/main/java/jdk/internal/m3/matvalue/MatMemoTable.java}
 * blob {@code 92cc01cc8a83e26888d8154c001742a836e29e03} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3MatMemoTable<R> {
    private final String operationId;
    private final M3MatPool<R> resultPool;
    private final M3MatLimits limits;
    private final Map<Key, Integer> results = new HashMap<>();
    private long hits;
    private long misses;
    private long rejects;

    public M3MatMemoTable(String operationId, M3MatPool<R> resultPool, M3MatLimits limits) {
        this.operationId = requireOperation(operationId);
        this.resultPool = Objects.requireNonNull(resultPool, "resultPool");
        this.limits = Objects.requireNonNull(limits, "limits");
    }

    public String operationId() { return operationId; }
    public M3MatPool<R> resultPool() { return resultPool; }
    public synchronized int size() { return results.size(); }

    public OptionalInt find(M3MatRef<?>... arguments) {
        Key key = key(arguments);
        synchronized (this) {
            Integer result = results.get(key);
            if (result == null) return OptionalInt.empty();
            hits++;
            return OptionalInt.of(result);
        }
    }

    /** Compute at most once per exact argument-handle tuple in this table. */
    public int getOrCompute(IntSupplier computation, M3MatRef<?>... arguments) {
        Objects.requireNonNull(computation, "computation");
        Key key = key(arguments);
        synchronized (this) {
            Integer existing = results.get(key);
            if (existing != null) {
                hits++;
                return existing;
            }
            limits.memoEntries((long) results.size() + 1L);
            int result = computation.getAsInt();
            resultPool.ref(result);
            results.put(key, result);
            misses++;
            return result;
        }
    }

    public synchronized void clear() { results.clear(); }
    public synchronized Stats stats() { return new Stats(results.size(), hits, misses, rejects); }

    private Key key(M3MatRef<?>[] arguments) {
        Objects.requireNonNull(arguments, "arguments");
        try {
            limits.memoArity(arguments.length);
        } catch (IllegalArgumentException failure) {
            synchronized (this) { rejects++; }
            throw failure;
        }
        M3MatRef<?>[] copy = arguments.clone();
        for (M3MatRef<?> argument : copy) Objects.requireNonNull(argument, "argument");
        return new Key(copy);
    }

    private static String requireOperation(String value) {
        String operation = Objects.requireNonNull(value, "operationId").strip();
        if (operation.isEmpty() || operation.length() > 256) {
            throw new IllegalArgumentException("operationId must contain 1..256 characters");
        }
        return operation;
    }

    public record Stats(int entries, long hits, long misses, long rejects) {
        public Stats {
            if (entries < 0 || hits < 0 || misses < 0 || rejects < 0) {
                throw new IllegalArgumentException("negative memo statistic");
            }
        }
    }

    private static final class Key {
        private final M3MatRef<?>[] arguments;
        private final int hash;

        private Key(M3MatRef<?>[] arguments) {
            this.arguments = arguments;
            this.hash = Arrays.hashCode(arguments);
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Key key && Arrays.equals(arguments, key.arguments);
        }

        @Override
        public int hashCode() { return hash; }
    }
}
