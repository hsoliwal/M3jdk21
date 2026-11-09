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
package jdk.internal.m3.collections.packed;

import jdk.internal.m3.collections.PackedByteLongMap;
import jdk.internal.m3.collections.PackedLongLongBiMap;
import jdk.internal.m3.collections.PackedLongLongOrderedMap;
import jdk.internal.m3.collections.PackedLongLongTable;
import jdk.internal.m3.collections.range.PackedLongRangeMap;
import jdk.internal.m3.collections.range.PackedLongRangeSet;
import jdk.m3.collections.LongLongPredicate;

/** Additive factory extension for the existing DenseCollections owner. These are missing
 * mutable shapes, not substitutes for its primitive type matrix, sorted arrays, heaps,
 * queues, multimaps, bitsets or external sorter. Explicit constructors remain available.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedCollectionShapes.java}
 * blob {@code e17f69b3da1a6cb6bdf3e8ad98541821d8c860a0} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (U1 batch (v6): identifiers and package only; bytecode equal to the source modulo names).
 */
public final class PackedCollectionShapes {
    private PackedCollectionShapes() { }
    public static PackedLongLongOrderedMap insertionOrdered(int expectedSize) {
        return new PackedLongLongOrderedMap(expectedSize);
    }
    public static PackedLongLongOrderedMap accessOrdered(int expectedSize) {
        return new PackedLongLongOrderedMap(expectedSize, true);
    }
    public static PackedLongLongBiMap biMap(int expectedSize) { return new PackedLongLongBiMap(expectedSize); }
    public static PackedLongLongTable table(int expectedCells) { return new PackedLongLongTable(expectedCells); }
    public static PackedLongRangeMap rangeMap() { return new PackedLongRangeMap(); }
    public static PackedLongRangeSet rangeSet() { return new PackedLongRangeSet(); }
    public static PackedLongLongLruCache lru(int capacity) { return new PackedLongLongLruCache(capacity); }
    public static PackedLongLongLruCache lru(int capacity, LongLongPredicate admission) {
        return new PackedLongLongLruCache(capacity, admission);
    }
    public static PackedByteLongMap byteKeys(int expectedSize, int arenaBytes, int maxArenaBytes) {
        return new PackedByteLongMap(expectedSize, arenaBytes, maxArenaBytes);
    }
    public static PackedLongLongSparseSortedMap sparseSortedMap(int expectedSize) {
        return new PackedLongLongSparseSortedMap(expectedSize);
    }
    public static PackedLongLongSparseSortedMap sparseSortedMap(
            int expectedSize, int blockWidth, double targetLoad) {
        return new PackedLongLongSparseSortedMap(expectedSize, blockWidth, targetLoad);
    }
    public static PackedLongLongBatchSortedMap batchSortedMap(int expectedSize, int batchSize) {
        return new PackedLongLongBatchSortedMap(expectedSize, batchSize);
    }
    public static PackedLongLongBatchSortedMap batchSortedMap(
            int expectedSize, int batchSize, LongLongPredicate admission) {
        return new PackedLongLongBatchSortedMap(expectedSize, batchSize, admission);
    }
    public static PackedLongLongGapSortedMap gapSortedMap(int expectedSize) {
        return new PackedLongLongGapSortedMap(expectedSize);
    }
    public static PackedLongLongGapSortedMap gapSortedMap(int expectedSize, double targetLoad) {
        return new PackedLongLongGapSortedMap(expectedSize, targetLoad);
    }
    public static PackedLongGapSortedSet gapSortedSet(int expectedSize) {
        return new PackedLongGapSortedSet(expectedSize);
    }
    public static PackedLongGapSortedSet gapSortedSet(int expectedSize, double targetLoad) {
        return new PackedLongGapSortedSet(expectedSize, targetLoad);
    }
}
