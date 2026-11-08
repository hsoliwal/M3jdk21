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

import java.util.Objects;
import jdk.m3.collections.M3RuntimeHeap;

/**
 * Primitive-only admission of optional sort-item cache state and viewport prefetch.
 * Required rows are never dropped because of an estimated heap-pressure signal.
 * A value is an accounted byte estimate, not a serialized Java object or a GC root.
 *
 * <p>Use once at a batch/viewport boundary, never once per comparator invocation. Existing
 * comparator and signal caches remain authoritative; a denied speculative admission does
 * not invalidate published evidence or change the sorted order. Callers can provide the
 * estimated weight of variable-size data separately from the fixed index lane bytes.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/SortItemBudget.java}
 * blob {@code 58d6fd0b69282955637ee34e2971ad6fad0c61ca} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class SortItemBudget {
    private static final long DEFAULT_SHARE_DENOMINATOR = 8L;
    private final long optionalBytes;
    private final long fixedPerItemBytes;
    private long admitted;
    private int optionalItems;

    private SortItemBudget(long optionalBytes, long fixedPerItemBytes) {
        this.optionalBytes = optionalBytes;
        this.fixedPerItemBytes = fixedPerItemBytes;
    }

    /**
     * Determine a bounded speculative allocation from the current effective Xmx.
     * Item estimates include the sort key, ID and any separately measured/cache weight.
     */
    public static SortItemBudget current(long fixedPerItemBytes) {
        return from(M3RuntimeHeap.current(), fixedPerItemBytes);
    }

    /** Pure deterministic policy seam for tests and compiler admission. */
    public static SortItemBudget from(M3RuntimeHeap.Snapshot heap, long fixedPerItemBytes) {
        Objects.requireNonNull(heap, "heap");
        if (fixedPerItemBytes < 0) throw new IllegalArgumentException("negative fixed item bytes");
        long headroom = Math.max(0L, heap.freeToMaximum() - heap.safetyReserve());
        long share = headroom / DEFAULT_SHARE_DENOMINATOR;
        if (heap.pressure(0L) == M3RuntimeHeap.Pressure.HIGH) share = 0;
        else if (heap.pressure(0L) == M3RuntimeHeap.Pressure.WARM) share /= 2;
        return new SortItemBudget(share, fixedPerItemBytes);
    }

    /**
     * Admit only optional cached item state, without allocating anything here.
     * No counter changes on a rejected admission; overflow is rejected safely.
     */
    public boolean tryAdmit(long additionalCacheBytes) {
        if (additionalCacheBytes < 0) throw new IllegalArgumentException("negative item cache bytes");
        if (optionalItems == Integer.MAX_VALUE) return false;
        if (additionalCacheBytes > Long.MAX_VALUE - fixedPerItemBytes) return false;
        long cost = fixedPerItemBytes + additionalCacheBytes;
        if (cost > optionalBytes - admitted) return false;
        admitted += cost;
        optionalItems++;
        return true;
    }

    /** Release accounting only after the owner actually evicts unpinned optional state. */
    public void release(long previouslyAdmittedCacheBytes) {
        if (previouslyAdmittedCacheBytes < 0 || previouslyAdmittedCacheBytes > Long.MAX_VALUE - fixedPerItemBytes)
            throw new IllegalArgumentException("invalid release weight");
        long cost = fixedPerItemBytes + previouslyAdmittedCacheBytes;
        if (optionalItems == 0 || cost > admitted) throw new IllegalStateException("unbalanced release");
        admitted -= cost;
        optionalItems--;
    }

    public long optionalBudgetBytes() { return optionalBytes; }
    public long admittedBytes() { return admitted; }
    public long availableOptionalBytes() { return optionalBytes - admitted; }
    public int admittedOptionalItems() { return optionalItems; }
}
