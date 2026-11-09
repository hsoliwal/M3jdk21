/* *****************************************************************************
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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * TokenInterner — Thread-safe string canonicalization with optional LRU bounds.
 *
 * <p>The global/base pool retains entries until {@link #clear()}. The bounded LRU
 * variant may evict entries still referenced by callers: equal strings interned
 * before and after eviction need not be identical. Never replace general value
 * equality with identity equality across eviction, clear, or separate pools.
 *
 * <p>Use {@link #identityScope(int, long)} for explicitly budgeted, non-evicting
 * identity domains. Admission fails at capacity rather than returning an
 * uncanonicalized value. Capacity measures tokens and UTF-16 units, not heap bytes.
 *
 * @since 1.0.0
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/utils/TokenInterner.java}
 * blob {@code 432d0b6da0c39289a51f0a2ad2c356a3f366e484} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (common non-language batch b28 (canonical, utils, semantic, convergence, io, extracted, validation, relationbits) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public class TokenInterner {

    private static final int DEFAULT_CAPACITY = 1024;
    private static final float LF = 0.75f;
    private static final TokenInterner INSTANCE = new TokenInterner(0);

    private final AtomicLong lookups = new AtomicLong(), hits = new AtomicLong(), evictions = new AtomicLong();
    private final Map<String, String> pool;

    protected TokenInterner() { this(0); }
    protected TokenInterner(int maxSize) {
        this.pool = maxSize > 0 ? new ConcurrentHashMap<>(Math.min(maxSize, DEFAULT_CAPACITY)) : new ConcurrentHashMap<>();
    }

    public static TokenInterner getInstance() { return INSTANCE; }
    public static TokenInterner withMaxSize(int maxSize) {
        if (maxSize <= 0) throw new IllegalArgumentException("maxSize must be > 0");
        return new BoundedTokenInterner(maxSize);
    }

    /** Create a non-evicting identity domain with explicit admission budgets. */
    public static TokenIdentityScope identityScope(int maxTokens, long maxUtf16Characters) {
        return new TokenIdentityScope(maxTokens, maxUtf16Characters);
    }

    /** Intern a string value. Thread-safe; null is returned unchanged. */
    public String intern(String value) {
        if (value == null) return null;
        lookups.incrementAndGet();
        return findOrAdmitCanonical(value);
    }

    /** File-local atom; preserves lookup, hit accounting and atomic admission order. */
    private String findOrAdmitCanonical(String value) {
        String existing = pool.get(value);
        if (existing != null) { hits.incrementAndGet(); return existing; }
        String canonical = pool.putIfAbsent(value, value);
        return canonical != null ? canonical : value;
    }

    /** Package-local evidence lookup; never admits values or changes counters. */
    boolean isCanonicalReference(String value) {
        return value == null || pool.get(value) == value;
    }

    public String internTrimmed(String v) { return v == null ? null : intern(v.trim()); }
    public String internIfNotBlank(String v) { return (v == null || v.isBlank()) ? null : intern(v.trim()); }
    public String internLower(String v) { return v == null ? null : intern(v.toLowerCase(java.util.Locale.ROOT)); }
    public String internUpper(String v) { return v == null ? null : intern(v.toUpperCase(java.util.Locale.ROOT)); }
    public String[] internAll(String... values) {
        return values == null ? null : java.util.Arrays.stream(values).map(this::intern).toArray(String[]::new);
    }
    public List<String> internAll(Iterable<String> values) {
        return values == null ? List.of() : java.util.stream.StreamSupport.stream(values.spliterator(), false).map(this::intern).toList();
    }

    // --- Statistics ---
    public double hitRatio() { long t = lookups.get(); return t > 0 ? (double) hits.get() / t : 0.0; }
    public long lookupCount() { return lookups.get(); }
    public long hitCount() { return hits.get(); }
    public long evictionCount() { return evictions.get(); }
    public int size() { return pool.size(); }
    public boolean contains(String v) { return v != null && pool.containsKey(v); }
    public void clear() { pool.clear(); lookups.set(0); hits.set(0); evictions.set(0); }
    public void resetStats() { lookups.set(0); hits.set(0); evictions.set(0); }

    @Override public String toString() {
        return "TokenInterner[size=%d, hitRatio=%.2f%%, lookups=%d, evictions=%d]"
                .formatted(size(), hitRatio() * 100, lookupCount(), evictionCount());
    }

    // --- Static convenience ---
    public static String global(String v) { return INSTANCE.intern(v); }
    public static String globalTrimmed(String v) { return INSTANCE.internTrimmed(v); }
    public static String globalLower(String v) { return INSTANCE.internLower(v); }

    /** Bounded LRU TokenInterner. */
    private static final class BoundedTokenInterner extends TokenInterner {
        private final Map<String, String> lruPool;
        private final int maxSize;
        private final AtomicLong bLookups = new AtomicLong(), bHits = new AtomicLong(), bEvictions = new AtomicLong();

        BoundedTokenInterner(int maxSize) {
            super(0); this.maxSize = maxSize;
            this.lruPool = Collections.synchronizedMap(new LinkedHashMap<>(maxSize, LF, true) {
                @Override protected boolean removeEldestEntry(Map.Entry<String, String> e) {
                    boolean rm = size() > maxSize; if (rm) bEvictions.incrementAndGet(); return rm;
                }
            });
        }

        @Override public String intern(String value) {
            if (value == null) return null;
            bLookups.incrementAndGet();
            synchronized (lruPool) {
                String ex = lruPool.get(value);
                if (ex != null) { bHits.incrementAndGet(); return ex; }
                lruPool.put(value, value); return value;
            }
        }
        /** An evicting pool must never certify a stable identity domain. */
        @Override boolean isCanonicalReference(String value) {
            throw new IllegalStateException(TokenIdentityScope.INVARIANT_ID
                    + ": EVICTING_POOL_HAS_NO_STABLE_IDENTITY_DOMAIN");
        }
        @Override public int size() { return lruPool.size(); }
        @Override public boolean contains(String v) { return v != null && lruPool.containsKey(v); }
        @Override public void clear() { lruPool.clear(); bLookups.set(0); bHits.set(0); bEvictions.set(0); }
        @Override public void resetStats() { bLookups.set(0); bHits.set(0); bEvictions.set(0); }
        @Override public long lookupCount() { return bLookups.get(); }
        @Override public long hitCount() { return bHits.get(); }
        @Override public long evictionCount() { return bEvictions.get(); }
        @Override public double hitRatio() { long t = bLookups.get(); return t > 0 ? (double) bHits.get() / t : 0.0; }
        @Override public String toString() { return "BoundedTokenInterner[size=%d/%d, hitRatio=%.2f%%, evictions=%d]".formatted(size(), maxSize, hitRatio() * 100, evictionCount()); }
    }
}
