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
package jdk.internal.m3.collections;

import java.util.Arrays;
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
 * <p>Retained keys use pool-identity and id lanes in fixed-size pages. Primitive entry offsets,
 * hash chains and result ids replace per-entry key, handle-array, node and boxed-value objects.
 * Hashes select candidates only; every argument is confirmed against its exact pool and id.</p>
 *
 * <p>Original absorption lineage: absorbed from Synexia {@code m3-mat-value/src/main/java/jdk/internal/m3/matvalue/MatMemoTable.java}
 * blob {@code 92cc01cc8a83e26888d8154c001742a836e29e03} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3MatMemoTable<R> {
    private static final int PAGE_SHIFT = 8;
    private static final int PAGE_SIZE = 1 << PAGE_SHIFT;
    private static final int PAGE_MASK = PAGE_SIZE - 1;
    private static final int[] NO_BUCKETS = new int[0];
    private static final int[][] NO_INT_PAGES = new int[0][];
    private static final long[][] NO_LONG_PAGES = new long[0][];
    private static final M3MatPool<?>[][] NO_POOL_PAGES = new M3MatPool<?>[0][];

    private final String operationId;
    private final M3MatPool<R> resultPool;
    private final M3MatLimits limits;
    private int[] buckets = NO_BUCKETS;
    // Four primitive words per entry: result id, hash, next entry plus one, arity.
    private int[][] entries = NO_INT_PAGES;
    private long[][] offsets = NO_LONG_PAGES;
    private M3MatPool<?>[][] argumentPools = NO_POOL_PAGES;
    private int[][] argumentIds = NO_INT_PAGES;
    private int size;
    private long argumentsUsed;
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
    public synchronized int size() { return size; }

    public OptionalInt find(M3MatRef<?>... arguments) {
        M3MatRef<?>[] key = key(arguments);
        int hash = Arrays.hashCode(key);
        synchronized (this) {
            int entry = findEntry(key, hash);
            if (entry < 0) return OptionalInt.empty();
            hits++;
            return OptionalInt.of(result(entry));
        }
    }

    /** Compute at most once per exact argument-handle tuple in this table. */
    public int getOrCompute(IntSupplier computation, M3MatRef<?>... arguments) {
        Objects.requireNonNull(computation, "computation");
        M3MatRef<?>[] key = key(arguments);
        int hash = Arrays.hashCode(key);
        synchronized (this) {
            int entry = findEntry(key, hash);
            if (entry >= 0) {
                hits++;
                return result(entry);
            }
            limits.memoEntries((long) size + 1L);
            int result = computation.getAsInt();
            resultPool.ref(result);
            put(key, hash, result);
            misses++;
            return result;
        }
    }

    public synchronized void clear() {
        buckets = NO_BUCKETS;
        entries = NO_INT_PAGES;
        offsets = NO_LONG_PAGES;
        argumentPools = NO_POOL_PAGES;
        argumentIds = NO_INT_PAGES;
        size = 0;
        argumentsUsed = 0;
    }

    public synchronized Stats stats() { return new Stats(size, hits, misses, rejects); }

    private M3MatRef<?>[] key(M3MatRef<?>[] arguments) {
        Objects.requireNonNull(arguments, "arguments");
        try {
            limits.memoArity(arguments.length);
        } catch (IllegalArgumentException failure) {
            synchronized (this) { rejects++; }
            throw failure;
        }
        M3MatRef<?>[] copy = arguments.clone();
        for (M3MatRef<?> argument : copy) Objects.requireNonNull(argument, "argument");
        return copy;
    }

    private int result(int entry) {
        return entries[entry >>> PAGE_SHIFT][(entry & PAGE_MASK) << 2];
    }

    private int findEntry(M3MatRef<?>[] key, int hash) {
        if (buckets.length == 0) return -1;
        int link = buckets[M3MatHash.mix(hash) & (buckets.length - 1)];
        while (link != 0) {
            int entry = link - 1;
            int page = entry >>> PAGE_SHIFT;
            int slot = entry & PAGE_MASK;
            int word = slot << 2;
            int[] data = entries[page];
            if (data[word + 1] == hash && data[word + 3] == key.length
                    && equalArguments(offsets[page][slot], key)) {
                return entry;
            }
            link = data[word + 2];
        }
        return -1;
    }

    private boolean equalArguments(long offset, M3MatRef<?>[] key) {
        for (M3MatRef<?> argument : key) {
            int page = (int) (offset >>> PAGE_SHIFT);
            int slot = (int) offset & PAGE_MASK;
            if (argumentPools[page][slot] != argument.pool()
                    || argumentIds[page][slot] != argument.id()) return false;
            offset++;
        }
        return true;
    }

    private void put(M3MatRef<?>[] key, int hash, int result) {
        // The supplier may reenter, clear, insert this key, or grow the index.
        // Resolve again after it returns, matching the original Map.put behavior.
        int existing = findEntry(key, hash);
        if (existing >= 0) {
            entries[existing >>> PAGE_SHIFT][(existing & PAGE_MASK) << 2] = result;
            return;
        }
        if (size == Integer.MAX_VALUE) throw new OutOfMemoryError("memo entry storage too large");
        long nextArguments = argumentsUsed + key.length;
        ensureEntryPage(size >>> PAGE_SHIFT);
        ensureArgumentPages(nextArguments);
        ensureBuckets(size + 1);

        long offset = argumentsUsed;
        for (M3MatRef<?> argument : key) {
            int page = (int) (offset >>> PAGE_SHIFT);
            int slot = (int) offset & PAGE_MASK;
            argumentPools[page][slot] = argument.pool();
            argumentIds[page][slot] = argument.id();
            offset++;
        }
        int page = size >>> PAGE_SHIFT;
        int slot = size & PAGE_MASK;
        int word = slot << 2;
        int bucket = M3MatHash.mix(hash) & (buckets.length - 1);
        int[] data = entries[page];
        data[word] = result;
        data[word + 1] = hash;
        data[word + 2] = buckets[bucket];
        data[word + 3] = key.length;
        offsets[page][slot] = argumentsUsed;
        buckets[bucket] = ++size;
        argumentsUsed = nextArguments;
    }

    private void ensureEntryPage(int page) {
        if (page >= entries.length) {
            int capacity = grownLength(entries.length, page + 1);
            int[][] nextEntries = Arrays.copyOf(entries, capacity);
            long[][] nextOffsets = Arrays.copyOf(offsets, capacity);
            entries = nextEntries;
            offsets = nextOffsets;
        }
        if (entries[page] == null) {
            int[] words = new int[PAGE_SIZE << 2];
            long[] starts = new long[PAGE_SIZE];
            entries[page] = words;
            offsets[page] = starts;
        }
    }

    private void ensureArgumentPages(long required) {
        long requiredPages = (required + PAGE_MASK) >>> PAGE_SHIFT;
        if (requiredPages > Integer.MAX_VALUE) {
            throw new OutOfMemoryError("memo argument storage too large");
        }
        int count = (int) requiredPages;
        if (count > argumentPools.length) {
            int capacity = grownLength(argumentPools.length, count);
            M3MatPool<?>[][] nextPools = Arrays.copyOf(argumentPools, capacity);
            int[][] nextIds = Arrays.copyOf(argumentIds, capacity);
            argumentPools = nextPools;
            argumentIds = nextIds;
        }
        int first = (int) (argumentsUsed >>> PAGE_SHIFT);
        for (int page = first; page < count; page++) {
            if (argumentPools[page] == null) {
                M3MatPool<?>[] pools = new M3MatPool<?>[PAGE_SIZE];
                int[] ids = new int[PAGE_SIZE];
                argumentPools[page] = pools;
                argumentIds[page] = ids;
            }
        }
    }

    private void ensureBuckets(int required) {
        int capacity = buckets.length == 0 ? 16 : buckets.length;
        if (capacity < (1 << 30) && (long) required * 4L > (long) capacity * 3L) {
            capacity <<= 1;
        }
        if (capacity == buckets.length) return;
        int[] nextBuckets = new int[capacity];
        for (int entry = 0; entry < size; entry++) {
            int[] data = entries[entry >>> PAGE_SHIFT];
            int word = (entry & PAGE_MASK) << 2;
            int bucket = M3MatHash.mix(data[word + 1]) & (capacity - 1);
            data[word + 2] = nextBuckets[bucket];
            nextBuckets[bucket] = entry + 1;
        }
        buckets = nextBuckets;
    }

    private static int grownLength(int current, int required) {
        return (int) Math.min(Integer.MAX_VALUE,
                Math.max((long) required, Math.max(4L, (long) current * 2L)));
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
}
