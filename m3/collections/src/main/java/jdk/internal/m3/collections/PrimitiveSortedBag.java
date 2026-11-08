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
import jdk.m3.collections.PrimitiveKind;
import jdk.m3.collections.M3PrimitiveSortedGapMap;

/**
 * Sorted primitive multiset using the sparse-gap sorted map for value-to-count storage.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/PrimitiveSortedBag.java}
 * blob {@code d98f9327049924c8ad09fa467cd7075ce0b39f7e} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class PrimitiveSortedBag {
    @FunctionalInterface
    public interface CountConsumer {
        void accept(long valueBits, int count);
    }

    private final PrimitiveKind kind;
    private final M3PrimitiveSortedGapMap counts;
    private long size;

    public PrimitiveSortedBag(PrimitiveKind kind) {
        this.kind = Objects.requireNonNull(kind, "kind");
        counts = new M3PrimitiveSortedGapMap(kind, PrimitiveKind.INT);
    }

    public PrimitiveKind kind() { return kind; }
    public long size() { return size; }
    public int distinctSize() { return counts.size(); }
    public boolean isEmpty() { return size == 0; }
    public long payloadBytes() { return counts.payloadBytes(); }

    public int countBits(long valueBits) {
        return (int) counts.getOrDefaultBits(valueBits, 0L);
    }

    public int addBits(long valueBits, int occurrences) {
        if (occurrences < 0) throw new IllegalArgumentException("occurrences < 0");
        if (occurrences == 0) return countBits(valueBits);
        int old = countBits(valueBits);
        long next = (long) old + occurrences;
        if (next > Integer.MAX_VALUE) throw new IllegalStateException("count overflow");
        counts.putBits(valueBits, next, 0L);
        size = Math.addExact(size, occurrences);
        return (int) next;
    }

    public int addBits(long valueBits) {
        return addBits(valueBits, 1);
    }

    public int removeBits(long valueBits, int occurrences) {
        if (occurrences < 0) throw new IllegalArgumentException("occurrences < 0");
        int old = countBits(valueBits);
        if (old == 0 || occurrences == 0) return old;
        int removed = Math.min(old, occurrences);
        int next = old - removed;
        if (next == 0) counts.removeOrDefaultBits(valueBits, 0L);
        else counts.putBits(valueBits, next, 0L);
        size -= removed;
        return next;
    }

    public int removeBits(long valueBits) {
        return removeBits(valueBits, 1);
    }

    public void clear() {
        counts.clear();
        size = 0;
    }

    public void forEachCount(CountConsumer action) {
        Objects.requireNonNull(action, "action");
        counts.forEach((value, count) -> action.accept(value, (int) count));
    }
}
