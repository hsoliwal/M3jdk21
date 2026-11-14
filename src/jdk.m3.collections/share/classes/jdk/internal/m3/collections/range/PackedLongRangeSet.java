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
package jdk.internal.m3.collections.range;

import jdk.m3.collections.LongLongConsumer;

/** Mutable inclusive range set. Exactly two long-array payload lanes; no dummy value lane.
 * Adjacent ranges coalesce. A small collection-level delegate shares interval mechanics.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedLongRangeSet.java}
 * blob {@code 76262d184946ea8c3d367489c7915210f0516116} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections batch 2: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class PackedLongRangeSet {
    /** Creates an instance (explicit form of the default constructor; required by -Xlint:all -Werror). */
    public PackedLongRangeSet() {
    }

    private final PackedLongRangeMap ranges = new PackedLongRangeMap(false);
    public void add(long from, long to) { ranges.put(from, to, 0); }
    public void remove(long from, long to) { ranges.remove(from, to); }
    public boolean contains(long point) { return ranges.contains(point); }
    public boolean containsRange(long from, long to) { return ranges.containsRange(from, to); }
    public int rangeCount() { return ranges.rangeCount(); }
    public boolean isEmpty() { return ranges.isEmpty(); }
    public long startAt(int row) { return ranges.startAt(row); }
    public long endAt(int row) { return ranges.endAt(row); }
    public long arrayPayloadBytes() { return ranges.arrayPayloadBytes(); }
    /** Throws ArithmeticException when the exact element count exceeds signed long. */
    public long cardinalityExact() {
        long count = 0;
        for (int i = 0; i < rangeCount(); i++) {
            long span = PackedRangeAtoms.inclusiveSpanExact(startAt(i), endAt(i));
            count = Math.addExact(count, span);
        }
        return count;
    }
    public void clear() { ranges.clear(); }
    public void forEach(LongLongConsumer consumer) {
        java.util.Objects.requireNonNull(consumer, "consumer");
        ranges.forEach((a, b, ignored) -> consumer.accept(a, b));
    }
}
