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
package jdk.internal.m3.collections.select;

import jdk.internal.m3.collections.LongLongMultiMap;
import jdk.internal.m3.collections.packed.PackedLongHashSet;
import jdk.internal.m3.collections.packed.PackedLongLongHashMap;
import jdk.internal.m3.collections.packed.PackedLongLongMap;
import jdk.internal.m3.collections.packed.PackedLongLongMultiMap;
import jdk.internal.m3.collections.packed.PackedMutableLongSet;
import jdk.m3.collections.LongArrayList;
import jdk.m3.collections.LongCollection;
import jdk.m3.collections.LongSequence;
import java.util.Objects;
import java.util.function.LongBinaryOperator;
import java.util.function.LongPredicate;
import java.util.function.LongUnaryOperator;

/**
 * Borrowed-input primitive programs. Callback failure escapes without modifying borrowed inputs.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/selection/PrimitiveCollectionPrograms.java}
 * blob {@code 9e81a252828bdfe95d572e09fd3375eda118841e} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections receipt-aware batch (v6, RECEIPT_MAP): identifiers and package only; bytecode equal to the source modulo names).
 */
public final class PrimitiveCollectionPrograms {
    public record Partition(LongSequence accepted, LongSequence rejected) {}
    public enum DuplicateKeys { REJECT, KEEP_FIRST, KEEP_LAST }
    private PrimitiveCollectionPrograms() {}
    public static LongSequence filter(LongCollection input, LongPredicate keep) {
        Objects.requireNonNull(input, "input"); Objects.requireNonNull(keep, "keep");
        var result = new LongArrayList(0); var it = input.iterator();
        while (it.hasNext()) { long value = it.nextLong(); if (keep.test(value)) result.add(value); }
        return result;
    }
    public static LongSequence map(LongCollection input, LongUnaryOperator transform) {
        Objects.requireNonNull(input, "input"); Objects.requireNonNull(transform, "transform");
        var result = new LongArrayList(input.size()); var it = input.iterator();
        while (it.hasNext()) result.add(transform.applyAsLong(it.nextLong()));
        return result;
    }
    public static Partition partition(LongCollection input, LongPredicate keep) {
        Objects.requireNonNull(input, "input"); Objects.requireNonNull(keep, "keep");
        var yes = new LongArrayList(0); var no = new LongArrayList(0); var it = input.iterator();
        while (it.hasNext()) { long value = it.nextLong(); (keep.test(value) ? yes : no).add(value); }
        return new Partition(yes, no);
    }
    public static PackedMutableLongSet intersection(LongCollection left, LongCollection right) {
        return selectMembers(left, right, true);
    }
    public static PackedMutableLongSet difference(LongCollection left, LongCollection right) {
        return selectMembers(left, right, false);
    }
    private static PackedMutableLongSet selectMembers(LongCollection left, LongCollection right, boolean included) {
        Objects.requireNonNull(left, "left"); var membership = members(right); var result = new PackedLongHashSet(0);
        var it = left.iterator();
        while (it.hasNext()) { long value = it.nextLong(); if (membership.contains(value) == included) result.add(value); }
        return result;
    }
    public static PackedMutableLongSet symmetricDifference(LongCollection left, LongCollection right) {
        var a = members(left); var b = members(right); var result = new PackedLongHashSet(0);
        a.forEachLong(v -> { if (!b.contains(v)) result.add(v); });
        b.forEachLong(v -> { if (!a.contains(v)) result.add(v); }); return result;
    }
    /** Each group's values retain input encounter order, including duplicates. */
    public static LongLongMultiMap groupBy(LongCollection input, LongUnaryOperator keyOf, int expectedKeys) {
        Objects.requireNonNull(input, "input"); Objects.requireNonNull(keyOf, "keyOf");
        var result = new PackedLongLongMultiMap(expectedKeys, input.size()); var it = input.iterator();
        while (it.hasNext()) { long value = it.nextLong(); result.add(keyOf.applyAsLong(value), value); }
        return result;
    }
    /** Values are original elements. Duplicate handling is explicit, including equal duplicates. */
    public static PackedLongLongMap indexBy(LongCollection input, LongUnaryOperator keyOf, DuplicateKeys duplicates) {
        Objects.requireNonNull(input, "input"); Objects.requireNonNull(keyOf, "keyOf"); Objects.requireNonNull(duplicates, "duplicates");
        var result = new PackedLongLongHashMap(input.size()); var it = input.iterator();
        while (it.hasNext()) {
            long value = it.nextLong(), key = keyOf.applyAsLong(value);
            if (result.containsKey(key)) {
                if (duplicates == DuplicateKeys.REJECT) throw new IllegalArgumentException("duplicate key: " + key);
                if (duplicates == DuplicateKeys.KEEP_FIRST) continue;
            }
            result.put(key, value);
        }
        return result;
    }
    /** Equijoin by map key; combine receives left/right values; absent differs from stored zero. */
    public static PackedLongLongMap innerJoin(PackedLongLongMap left, PackedLongLongMap right, LongBinaryOperator combine) {
        Objects.requireNonNull(left, "left"); Objects.requireNonNull(right, "right"); Objects.requireNonNull(combine, "combine");
        var result = new PackedLongLongHashMap(Math.min(left.size(), right.size()));
        left.forEach((k, v) -> { if (right.containsKey(k)) result.put(k, combine.applyAsLong(v, right.getOrThrow(k))); });
        return result;
    }
    /** Left-to-right Math.addExact fold; intermediate overflow throws. */
    public static long sumExact(LongCollection input) {
        Objects.requireNonNull(input, "input"); long sum = 0; var it = input.iterator();
        while (it.hasNext()) sum = Math.addExact(sum, it.nextLong()); return sum;
    }
    /** One exact cumulative value per input; intermediate overflow throws. */
    public static LongSequence prefixSumExact(LongCollection input) {
        Objects.requireNonNull(input, "input"); var result = new LongArrayList(input.size());
        long sum = 0; var it = input.iterator();
        while (it.hasNext()) { sum = Math.addExact(sum, it.nextLong()); result.add(sum); } return result;
    }
    public static LongSequence adjacentDistinct(LongCollection input) {
        Objects.requireNonNull(input, "input"); var result = new LongArrayList(0); var it = input.iterator();
        boolean first = true; long previous = 0;
        while (it.hasNext()) {
            long value = it.nextLong(); if (first || value != previous) result.add(value);
            first = false; previous = value;
        }
        return result;
    }
    private static PackedMutableLongSet members(LongCollection input) {
        Objects.requireNonNull(input, "input"); var result = new PackedLongHashSet(input.size());
        input.forEachLong(result::add); return result;
    }
}
