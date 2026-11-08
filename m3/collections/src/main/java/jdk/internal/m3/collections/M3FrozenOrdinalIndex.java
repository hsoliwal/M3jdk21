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

import jdk.m3.collections.bits.PackedBitVector;
import java.util.Objects;

/**
 * Immutable shared key layout for columns over one bounded ordinal domain.
 * Membership and ordinal-to-position lookup are O(1) through the existing
 * rank/select owner. Payload scales with the domain, even when membership is sparse.
 * Domain identity is explicit; integer ordinals alone do not establish compatibility.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-algo/src/main/java/jdk/internal/m3/algorithms/collections/FrozenOrdinalIndex.java}
 * blob {@code 31b9b889eacb30ac22eb6b73b38fbd3723e2511f} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (algorithms.collections batch 2 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3FrozenOrdinalIndex {
    private final Object domain;
    private final PackedBitVector membership;

    private M3FrozenOrdinalIndex(Object domain, PackedBitVector membership) {
        this.domain = domain;
        this.membership = membership;
    }

    /** Copies strictly ascending unique ordinals; source mutation during construction is forbidden. */
    public static M3FrozenOrdinalIndex of(Object domain, int domainSize, int[] sortedOrdinals) {
        return of(domain, domainSize, sortedOrdinals, Long.MAX_VALUE);
    }

    /** Rejects an insufficient retained-index payload budget before bitmap allocation. */
    public static M3FrozenOrdinalIndex of(
            Object domain, int domainSize, int[] sortedOrdinals, long maxIndexPayloadBytes) {
        Objects.requireNonNull(domain, "domain");
        Objects.requireNonNull(sortedOrdinals, "sortedOrdinals");
        long required = payloadBytesForDomain(domainSize);
        if (maxIndexPayloadBytes < required) {
            throw new IllegalArgumentException("index payload budget: required=" + required);
        }
        int previous = -1;
        for (int ordinal : sortedOrdinals) {
            if (ordinal <= previous || ordinal >= domainSize) {
                throw new IllegalArgumentException("ordinals must be unique, ascending and in domain");
            }
            previous = ordinal;
        }
        int wordCount = (int) (((long) domainSize + 63L) >>> 6);
        long[] words = new long[wordCount];
        for (int ordinal : sortedOrdinals) {
            words[ordinal >>> 6] |= 1L << (ordinal & 63);
        }
        return new M3FrozenOrdinalIndex(domain, new PackedBitVector(words, domainSize));
    }

    /** Primitive array payload only: excludes object headers, domain and construction workspace. */
    public static long payloadBytesForDomain(int domainSize) {
        if (domainSize < 0) {
            throw new IllegalArgumentException("domainSize");
        }
        long words = ((long) domainSize + 63L) >>> 6;
        return 8L * words + 4L * (words + 1L);
    }

    public Object domain() { return domain; }
    public int domainSize() { return membership.length(); }
    public int size() { return membership.ones(); }
    public long payloadBytes() { return membership.payloadBytes(); }

    /** Call at an adapter boundary before converting a foreign handle to a raw ordinal. */
    public void requireDomain(Object expectedDomain) {
        if (domain != expectedDomain) {
            throw new IllegalArgumentException("foreign ordinal domain");
        }
    }

    /** Out-of-domain ordinals throw; absent ordinals inside the domain return false. */
    public boolean contains(int ordinal) { return membership.get(ordinal); }

    /** Dense value position, or -1 for an absent in-domain ordinal. */
    public int positionOf(int ordinal) {
        return membership.get(ordinal) ? membership.rank1(ordinal) : -1;
    }

    /** Reverse lookup delegates to select1: O(log(words) + 64), not constant time. */
    public int ordinalAt(int position) { return membership.select1(position); }

    /** Number of present ordinals in [0,end); end may equal domainSize. */
    public int rankBefore(int end) { return membership.rank1(end); }

    /** Cardinality of a half-open ordinal interval using two rank queries. */
    public int countInRange(int fromInclusive, int toExclusive) {
        Objects.checkFromToIndex(fromInclusive, toExclusive, domainSize());
        return membership.rank1(toExclusive) - membership.rank1(fromInclusive);
    }
}
