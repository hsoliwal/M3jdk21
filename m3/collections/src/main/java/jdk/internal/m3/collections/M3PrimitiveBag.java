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
import jdk.m3.collections.M3PrimitiveKind;
import jdk.m3.collections.M3PrimitivePrimitiveHashMap;

/**
 * Primitive multiset/bag backed by a primitive-key to int-count hash table.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/PrimitiveBag.java}
 * blob {@code 5ce46524a3b776c8264575b60fbfb1312c23082b} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class M3PrimitiveBag {
    @FunctionalInterface
    public interface CountConsumer {
        void accept(long valueBits, int count);
    }

    private final M3PrimitiveKind kind;
    private final M3PrimitivePrimitiveHashMap counts;
    private long size;

    public M3PrimitiveBag(M3PrimitiveKind kind) {
        this.kind = Objects.requireNonNull(kind, "kind");
        counts = new M3PrimitivePrimitiveHashMap(kind, M3PrimitiveKind.INT);
    }

    public M3PrimitiveKind kind() { return kind; }
    public long size() { return size; }
    public int distinctSize() { return counts.size(); }
    public boolean isEmpty() { return size == 0; }
    public long payloadBytes() { return counts.payloadBytes(); }

    public int countBits(long valueBits) {
        return (int) counts.getOrDefaultBits(valueBits, 0L);
    }

    public int addBits(long valueBits) {
        return addBits(valueBits, 1);
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

    public int removeBits(long valueBits) {
        return removeBits(valueBits, 1);
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

    /**
     * Assigns an exact occurrence count and returns the previous count.
     * Zero removes the value. Unlike the legacy add/remove methods, this method
     * returns the count before the change. This bag is not thread-safe.
     *
     * @throws IllegalArgumentException if count is negative
     */
    public int setCountBits(long valueBits, int count) {
        if (count < 0) throw new IllegalArgumentException("count < 0");
        int old = countBits(valueBits);
        if (old == count) return old;
        long nextSize = Math.addExact(size, (long) count - old);
        if (count == 0) counts.removeOrDefaultBits(valueBits, 0L);
        else counts.putBits(valueBits, count, 0L);
        size = nextSize;
        return old;
    }

    /**
     * Assigns count only when the current count equals expectedCount.
     * Both counts are validated before comparison. This is a conditional update,
     * not a concurrent compare-and-set operation.
     *
     * @throws IllegalArgumentException if either count is negative
     */
    public boolean setCountBits(long valueBits, int expectedCount, int count) {
        if (expectedCount < 0 || count < 0) throw new IllegalArgumentException("negative count");
        if (countBits(valueBits) != expectedCount) return false;
        setCountBits(valueBits, count);
        return true;
    }

    /** Removes every occurrence of a value and returns the number removed. */
    public int removeAllBits(long valueBits) {
        return setCountBits(valueBits, 0);
    }

    /** Releases unused count-table capacity without removing any occurrence. */
    public void compact() {
        counts.compact();
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
