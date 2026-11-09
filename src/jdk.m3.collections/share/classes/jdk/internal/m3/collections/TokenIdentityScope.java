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

/**
 * A bounded, non-evicting identity domain backed by the existing TokenInterner.
 *
 * <p>Every non-null result of {@link #intern(String)} is canonical for this scope.
 * Equal values returned by this scope are the same reference. Distinct values at
 * capacity are rejected, never evicted or silently returned uncached. Close ends
 * admission and releases the scope's references; caller-owned strings remain valid.
 * Separate scopes and external strings have no shared identity guarantee.
 *
 * <p>Admission/close synchronize only on this scope, not a global analysis lock.
 * Comparison of already admitted references needs no pool lookup or lock. Budget
 * accounting bounds unique entries and the sum of their String.length() values;
 * it does not claim an exact heap-byte bound. Measure retained heap separately.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/utils/TokenIdentityScope.java}
 * blob {@code 419f1b9e506db8941f8141525ce12952feaaabad} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (common non-language batch b28 (canonical, utils, semantic, convergence, io, extracted, validation, relationbits) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class TokenIdentityScope implements AutoCloseable {
    public static final String INVARIANT_ID = "M3-FILE-ATOM-CANONICALIZATION-1";
    private final int maxTokens;
    private final long maxUtf16Characters;
    private TokenInterner pool = new TokenInterner();
    private long retainedCharacters;
    private Snapshot closedSnapshot;

    TokenIdentityScope(int maxTokens, long maxUtf16Characters) {
        if (maxTokens <= 0) throw new IllegalArgumentException("maxTokens must be > 0");
        if (maxUtf16Characters < 0) throw new IllegalArgumentException("maxUtf16Characters must be >= 0");
        this.maxTokens = maxTokens;
        this.maxUtf16Characters = maxUtf16Characters;
    }

    /**
     * Return the scope's canonical reference; null consumes no budget.
     * @throws IllegalStateException if closed or a distinct value exceeds either budget
     */
    public synchronized String intern(String value) {
        requireOpen();
        if (value == null) return null;
        if (pool.contains(value)) return pool.intern(value);
        requireAdmissionBudget(value.length());
        String canonical = pool.intern(value);
        retainedCharacters += value.length();
        return canonical;
    }

    /**
     * Validate a reference at an identity-optimized pipeline boundary, without
     * admitting, copying, evicting or changing accounting. Null denotes absence.
     *
     * <p>An equal external copy is rejected instead of silently canonicalized.
     * Validate each reference when it enters the pipeline; subsequent comparisons
     * of those references can use == without another lookup. This method itself
     * performs a map lookup and is not intended for every hot-loop comparison.
     * A shared reference explicitly admitted into two scopes is valid in both.
     * This cannot intercept arbitrary Java == operations outside this API.
     *
     * @throws IllegalArgumentException if a non-null reference is not canonical here
     * @throws IllegalStateException if the scope has closed
     */
    public synchronized String requireCanonical(String value) {
        requireOpen();
        if (!pool.isCanonicalReference(value)) {
            throw new IllegalArgumentException(INVARIANT_ID + ": NON_CANONICAL_REFERENCE");
        }
        return value;
    }

    /** Snapshot uses delegate counters; rejected admissions are not pool lookups. */
    public synchronized Snapshot snapshot() {
        return closedSnapshot != null ? closedSnapshot
                : new Snapshot(pool.size(), retainedCharacters, pool.lookupCount(), pool.hitCount(), false);
    }

    /** Idempotently release all pool ownership and permanently reject new admission. */
    @Override public synchronized void close() {
        if (closedSnapshot != null) return;
        closedSnapshot = new Snapshot(0, 0, pool.lookupCount(), pool.hitCount(), true);
        pool.clear();
        pool = null;
        retainedCharacters = 0;
    }

    private void requireOpen() {
        if (closedSnapshot != null) throw new IllegalStateException(INVARIANT_ID + ": CANONICAL_SCOPE_CLOSED");
    }

    private void requireAdmissionBudget(int characters) {
        if (pool.size() >= maxTokens || characters > maxUtf16Characters - retainedCharacters) {
            throw new IllegalStateException(INVARIANT_ID + ": CANONICAL_SCOPE_CAPACITY: maxTokens=" + maxTokens
                    + ", maxUtf16Characters=" + maxUtf16Characters);
        }
    }

    /** Current ownership and lifetime successful-lookup statistics. */
    public record Snapshot(int uniqueTokens, long retainedUtf16Characters,
                           long lookups, long hits, boolean closed) { }
}
