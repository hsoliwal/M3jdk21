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

import jdk.internal.m3.collections.bits.LazyBitAddressSpace;
import jdk.internal.m3.collections.graph.IntBipartiteMatcher;
import jdk.internal.m3.collections.graph.LongPushRelabelMaxFlow;
import jdk.internal.m3.collections.text.IntSuffixArrayIndex;
import jdk.m3.collections.AdaptiveEnumPrimitiveMap;
import jdk.m3.collections.BigArray;
import jdk.m3.collections.BigList;
import jdk.m3.collections.DenseIdentityHashMap;
import jdk.m3.collections.DenseIdentityHashSet;
import jdk.m3.collections.DenseObjectHashSet;
import jdk.m3.collections.DenseObjectObjectHashMap;
import jdk.m3.collections.EnumPrimitiveMap;
import jdk.m3.collections.IntArrayDeque;
import jdk.m3.collections.IntArrayList;
import jdk.m3.collections.IntDisjointSet;
import jdk.m3.collections.IntHashSet;
import jdk.m3.collections.IntIntHashMap;
import jdk.m3.collections.IntLongHashMap;
import jdk.m3.collections.IntObjectHashMap;
import jdk.m3.collections.IntRollbackDisjointSet;
import jdk.m3.collections.IntSparseSet;
import jdk.m3.collections.LazyPrimitiveAddressSpace;
import jdk.m3.collections.LongArrayDeque;
import jdk.m3.collections.LongArrayList;
import jdk.m3.collections.LongByteHashMap;
import jdk.m3.collections.LongHashSet;
import jdk.m3.collections.LongIntHashMap;
import jdk.m3.collections.LongLongHashMap;
import jdk.m3.collections.LongLongPriorityQueue;
import jdk.m3.collections.LongLongRangeMap;
import jdk.m3.collections.LongLongSortedMap;
import jdk.m3.collections.LongObjectHashMap;
import jdk.m3.collections.LongOrderStatisticMultiset;
import jdk.m3.collections.LongSlidingWindow;
import jdk.m3.collections.LongSortedSet;
import jdk.m3.collections.LongTimingWheel;
import jdk.m3.collections.MonotoneLongDeque;
import jdk.m3.collections.ObjectCounterMap;
import jdk.m3.collections.ObjectIntHashMap;
import jdk.m3.collections.ObjectLongHashMap;
import jdk.m3.collections.ObjectPrimitiveHashMap;
import jdk.m3.collections.PrimitiveAdaptiveByteMap;
import jdk.m3.collections.PrimitiveAdaptiveCharMap;
import jdk.m3.collections.PrimitiveAdaptiveMap;
import jdk.m3.collections.PrimitiveAdaptiveShortMap;
import jdk.m3.collections.PrimitiveArrayDeque;
import jdk.m3.collections.PrimitiveArrayList;
import jdk.m3.collections.PrimitiveBiMap;
import jdk.m3.collections.PrimitiveBigHashMap;
import jdk.m3.collections.PrimitiveBigHashSet;
import jdk.m3.collections.PrimitiveDirectBooleanMap;
import jdk.m3.collections.PrimitiveDirectByteMap;
import jdk.m3.collections.PrimitiveDirectCharMap;
import jdk.m3.collections.PrimitiveDirectIntMap;
import jdk.m3.collections.PrimitiveDirectIntTable;
import jdk.m3.collections.PrimitiveDirectShortMap;
import jdk.m3.collections.PrimitiveFrozenList;
import jdk.m3.collections.PrimitiveFrozenMap;
import jdk.m3.collections.PrimitiveFrozenSet;
import jdk.m3.collections.PrimitiveHashMultiMap;
import jdk.m3.collections.PrimitiveHashSet;
import jdk.m3.collections.PrimitiveIndexView;
import jdk.m3.collections.PrimitiveKind;
import jdk.m3.collections.PrimitiveLruMap;
import jdk.m3.collections.PrimitiveMinHeap;
import jdk.m3.collections.PrimitiveMinMaxQueue;
import jdk.m3.collections.PrimitiveMpmcRingBuffer;
import jdk.m3.collections.PrimitiveMpscRingBuffer;
import jdk.m3.collections.PrimitiveObjectHashMap;
import jdk.m3.collections.PrimitivePairList;
import jdk.m3.collections.PrimitivePrimitiveHashMap;
import jdk.m3.collections.PrimitiveSemaphoreMpmcQueue;
import jdk.m3.collections.PrimitiveSoaPriorityQueue;
import jdk.m3.collections.PrimitiveSoaStore;
import jdk.m3.collections.PrimitiveSortedArrayMap;
import jdk.m3.collections.PrimitiveSortedArraySet;
import jdk.m3.collections.PrimitiveSortedGapMap;
import jdk.m3.collections.PrimitiveSortedGapSet;
import jdk.m3.collections.PrimitiveSortedMultiMap;
import jdk.m3.collections.PrimitiveSortedTable;
import jdk.m3.collections.PrimitiveSpmcRingBuffer;
import jdk.m3.collections.PrimitiveSpscRingBuffer;
import jdk.m3.collections.PrimitiveTopK;
import jdk.m3.collections.bits.LongBitSet;
import jdk.m3.collections.bits.RoaringIntSet;
import jdk.m3.collections.bits.RoaringLongSet;
import jdk.m3.collections.concurrent.IntSpscRingBuffer;
import jdk.m3.collections.concurrent.LongSpscRingBuffer;
import jdk.m3.collections.graph.IntBinaryLiftingLca;
import jdk.m3.collections.graph.IntHeavyLightIndex;
import jdk.m3.collections.graph.IntLongAdjacencyGraph;
import jdk.m3.collections.packed.DeltaEncodedLongList;
import jdk.m3.collections.packed.PackedBooleanList;
import jdk.m3.collections.packed.PackedUnsignedIntList;
import jdk.m3.collections.packed.RunLengthLongList;
import jdk.m3.collections.range.LongFenwickTree;
import jdk.m3.collections.range.LongRangeSet;
import jdk.m3.collections.range.LongSegmentTree;
import jdk.m3.collections.range.LongSparseTable;
import jdk.m3.collections.range.PersistentLongSumTree;
import jdk.m3.collections.range.RangeAddLongSumTree;
import jdk.m3.collections.sketch.LongBloomFilter;
import jdk.m3.collections.sketch.LongCountMinSketch;
import jdk.m3.collections.sketch.LongCountingBloomFilter;
import jdk.m3.collections.sketch.LongHistogram;
import jdk.m3.collections.sketch.LongHyperLogLog;
import jdk.m3.collections.sketch.LongReservoirSampler;
import jdk.m3.collections.sketch.LongSpaceSavingTopK;
import jdk.m3.collections.text.CharTrieSet;
import jdk.m3.collections.text.IntAhoCorasick;

import java.util.function.BiPredicate;
import java.util.function.LongPredicate;

/**
 * Entry point for the no-boxing/no-entry-object collection family.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/DenseCollections.java}
 * blob {@code 90992d52a69e3eac025d33aec1b4b21f07b49d37} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (primitives DenseCollections chain after API_UNION U7/U8 (v6.3): identifiers and package only; bytecode equal to the source modulo names).
 */
public final class DenseCollections {

    /** Unified deterministic intent selector for primitive structures and algorithms. */
    public static FuzzyPrimitiveFactory fuzzy() {
        return FuzzyPrimitiveFactory.standard();
    }

    /** Provider-backed lazy rows/tree construction without a hot-path wrapper. */
    public static ProviderCollectionFactory providers() {
        return ProviderCollectionFactory.standard();
    }

    public static ProviderBackedPrimitiveRows providerRows(
            PrimitiveRowProvider provider,
            PrimitiveKind... laneKinds) {
        return providers().rows(provider, laneKinds);
    }

    public static ProviderBackedPrimitiveList providerList(
            long logicalSize,
            PrimitiveRowProvider provider,
            PrimitiveKind... laneKinds) {
        return providers().list(logicalSize, provider, laneKinds);
    }

    public static ProviderBackedPrimitiveTree providerTree(
            PagedPrimitiveTreeProvider provider,
            int pageSize,
            PrimitiveKind... laneKinds) {
        return providers().tree(provider, pageSize, laneKinds);
    }

    public static ProviderBackedPrimitiveLongTree providerLongTree(
            CursorPagedPrimitiveTreeProvider provider,
            int pageSize,
            PrimitiveKind... laneKinds) {
        return providers().longTree(provider, pageSize, laneKinds);
    }
    private DenseCollections() {}

    // Zero-dispatch hot paths.
    public static IntArrayList ints() { return new IntArrayList(); }
    public static LongArrayList longs() { return new LongArrayList(); }
    public static IntHashSet intSet() { return new IntHashSet(); }
    public static LongHashSet longSet() { return new LongHashSet(); }
    public static IntIntHashMap intIntMap() { return new IntIntHashMap(); }
    public static IntLongHashMap intLongMap() { return new IntLongHashMap(); }
    public static LongIntHashMap longIntMap() { return new LongIntHashMap(); }
    public static LongLongHashMap longLongMap() { return new LongLongHashMap(); }
    public static LongByteHashMap longByteMap() { return new LongByteHashMap(); }
    public static LongSortedSet longSortedSet() { return LongSortedSet.of(); }
    public static LongByteSortedMap longByteSortedMap() { return new LongByteSortedMap(); }
    public static LongLongSortedMap longLongSortedMap() { return LongLongSortedMap.of(); }
    public static LongLongPriorityQueue longPriorityQueue() { return new LongLongPriorityQueue(); }
    public static LongLongPriorityQueue longPriorityQueue(boolean minFirst) {
        return new LongLongPriorityQueue(8, minFirst);
    }
    public static <V> IntObjectHashMap<V> intObjectMap() { return new IntObjectHashMap<>(); }
    public static <V> LongObjectHashMap<V> longObjectMap() { return new LongObjectHashMap<>(); }
    public static <K> ObjectIntHashMap<K> objectIntMap() { return new ObjectIntHashMap<>(); }
    public static <K> ObjectLongHashMap<K> objectLongMap() { return new ObjectLongHashMap<>(); }
    public static IntArrayDeque intDeque() { return new IntArrayDeque(); }
    public static LongArrayDeque longDeque() { return new LongArrayDeque(); }
    public static IntMinHeap intHeap() { return new IntMinHeap(); }
    public static LongMinHeap longHeap() { return new LongMinHeap(); }
    public static IntSpscRingBuffer intSpsc(int capacity) { return new IntSpscRingBuffer(capacity); }
    public static LongSpscRingBuffer longSpsc(int capacity) { return new LongSpscRingBuffer(capacity); }
    public static LongBitSet bits() { return LongBitSet.of(); }
    public static PackedBooleanList packedBooleans() { return new PackedBooleanList(); }
    public static <E> DenseObjectHashSet<E> objectSet() { return new DenseObjectHashSet<>(); }
    public static <E> DenseDominanceSet<E> dominanceSet(
            BiPredicate<? super E, ? super E> dominates) {
        return new DenseDominanceSet<>(dominates);
    }
    public static LongRangeSet longRanges() { return new LongRangeSet(); }
    public static LongLongRangeMap longRangeMap() { return new LongLongRangeMap(); }
    public static PackedUnsignedIntList packedUnsignedInts(int bitsPerValue) {
        return new PackedUnsignedIntList(bitsPerValue);
    }
    public static RoaringIntSet roaringInts() { return new RoaringIntSet(); }
    public static RoaringLongSet roaringLongs() { return new RoaringLongSet(); }
    public static <K,V> DenseObjectObjectHashMap<K,V> objectMap() {
        return new DenseObjectObjectHashMap<>();
    }
    public static <E> DenseIdentityHashSet<E> identitySet() {
        return new DenseIdentityHashSet<>();
    }
    public static <K,V> DenseIdentityHashMap<K,V> identityMap() {
        return new DenseIdentityHashMap<>();
    }

    // Exact-width universal paths.
    public static PrimitiveArrayList primitiveList(PrimitiveKind kind) {
        return new PrimitiveArrayList(kind);
    }

    public static PrimitivePairList primitivePairs(
            PrimitiveKind leftKind, PrimitiveKind rightKind) {
        return new PrimitivePairList(leftKind, rightKind);
    }

    public static PrimitiveArrayDeque primitiveDeque(PrimitiveKind kind) {
        return new PrimitiveArrayDeque(kind);
    }

    public static PrimitiveSpscRingBuffer primitiveSpsc(PrimitiveKind kind, int capacity) {
        return new PrimitiveSpscRingBuffer(kind, capacity);
    }

    public static PrimitiveMpscRingBuffer primitiveMpsc(PrimitiveKind kind, int capacity) {
        return new PrimitiveMpscRingBuffer(kind, capacity);
    }

    public static PrimitiveSpmcRingBuffer primitiveSpmc(PrimitiveKind kind, int capacity) {
        return new PrimitiveSpmcRingBuffer(kind, capacity);
    }

    public static PrimitiveMpmcRingBuffer primitiveMpmc(PrimitiveKind kind, int capacity) {
        return new PrimitiveMpmcRingBuffer(kind, capacity);
    }

    public static PrimitiveSemaphoreMpmcQueue primitiveSemaphoreMpmc(
            PrimitiveKind kind, int capacity) {
        return new PrimitiveSemaphoreMpmcQueue(kind, capacity);
    }

    public static PrimitiveHashSet primitiveSet(PrimitiveKind kind) {
        return new PrimitiveHashSet(kind);
    }

    public static BigArray primitiveBigArray(PrimitiveKind kind, long length) {
        return new BigArray(kind, length);
    }

    public static LazyPrimitiveAddressSpace lazyPrimitiveAddressSpace(PrimitiveKind kind) {
        return new LazyPrimitiveAddressSpace(kind);
    }

    public static LazyPrimitiveAddressSpace lazyPrimitiveAddressSpace(
            PrimitiveKind kind, int rootBits, int directoryBits, int leafBits) {
        return new LazyPrimitiveAddressSpace(kind, rootBits, directoryBits, leafBits);
    }

    public static LazyBitAddressSpace lazyBits() {
        return new LazyBitAddressSpace();
    }

    public static LazyBitAddressSpace lazyBits(
            int rootBits, int directoryBits, int leafBits) {
        return new LazyBitAddressSpace(rootBits, directoryBits, leafBits);
    }

    public static LazyPrimitiveSoaTable lazyPrimitiveRows(PrimitiveKind... laneKinds) {
        return new LazyPrimitiveSoaTable(laneKinds);
    }

    public static LazyPrimitiveSoaTable lazyPrimitiveRows(
            int rootBits, int directoryBits, int leafBits, PrimitiveKind... laneKinds) {
        return new LazyPrimitiveSoaTable(rootBits, directoryBits, leafBits, laneKinds);
    }

    public static BigList primitiveBigList(PrimitiveKind kind) {
        return new BigList(kind);
    }

    public static PrimitiveBigHashSet primitiveBigSet(PrimitiveKind kind) {
        return new PrimitiveBigHashSet(kind);
    }

    public static PrimitiveBigHashMap primitiveBigMap(
            PrimitiveKind keyKind, PrimitiveKind valueKind) {
        return new PrimitiveBigHashMap(keyKind, valueKind);
    }

    public static PrimitiveDirectIntMap primitiveDirectIntMap(PrimitiveKind valueKind) {
        return new PrimitiveDirectIntMap(valueKind);
    }

    public static PrimitiveDirectBooleanMap primitiveDirectBooleanMap(PrimitiveKind valueKind) {
        return new PrimitiveDirectBooleanMap(valueKind);
    }

    public static PrimitiveDirectByteMap primitiveDirectByteMap(PrimitiveKind valueKind) {
        return new PrimitiveDirectByteMap(valueKind);
    }

    public static PrimitiveAdaptiveByteMap primitiveAdaptiveByteMap(PrimitiveKind valueKind) {
        return new PrimitiveAdaptiveByteMap(valueKind);
    }

    public static PrimitiveDirectShortMap primitiveDirectShortMap(PrimitiveKind valueKind) {
        return new PrimitiveDirectShortMap(valueKind);
    }

    public static PrimitiveAdaptiveShortMap primitiveAdaptiveShortMap(PrimitiveKind valueKind) {
        return new PrimitiveAdaptiveShortMap(valueKind);
    }

    public static PrimitiveDirectCharMap primitiveDirectCharMap(PrimitiveKind valueKind) {
        return new PrimitiveDirectCharMap(valueKind);
    }

    public static PrimitiveAdaptiveCharMap primitiveAdaptiveCharMap(PrimitiveKind valueKind) {
        return new PrimitiveAdaptiveCharMap(valueKind);
    }

    public static <E extends Enum<E>> EnumPrimitiveMap<E> enumPrimitiveMap(
            Class<E> keyType, PrimitiveKind valueKind) {
        return new EnumPrimitiveMap<>(keyType, valueKind);
    }

    public static <E extends Enum<E>> AdaptiveEnumPrimitiveMap<E> adaptiveEnumPrimitiveMap(
            Class<E> keyType, PrimitiveKind valueKind) {
        return new AdaptiveEnumPrimitiveMap<>(keyType, valueKind);
    }

    public static PrimitiveAdaptiveMap adaptivePrimitiveMap(
            PrimitiveKind keyKind, PrimitiveKind valueKind, int expectedSize) {
        return new PrimitiveAdaptiveMap(keyKind, valueKind, expectedSize);
    }

    public static PrimitiveDirectIntTable primitiveDirectIntTable(
            PrimitiveKind valueKind, int rowCount, int columnCount) {
        return new PrimitiveDirectIntTable(valueKind, rowCount, columnCount);
    }

    public static PrimitiveSortedTable primitiveTable(
            PrimitiveKind rowKind, PrimitiveKind columnKind, PrimitiveKind valueKind) {
        return new PrimitiveSortedTable(rowKind, columnKind, valueKind);
    }

    public static PrimitiveCounterMap primitiveCounterMap(PrimitiveKind keyKind) {
        return new PrimitiveCounterMap(keyKind);
    }

    public static <K> ObjectCounterMap<K> objectCounterMap() {
        return new ObjectCounterMap<>();
    }

    public static IntSparseSet intSparseSet() {
        return new IntSparseSet();
    }

    public static PrimitiveStack primitiveStack(PrimitiveKind kind) {
        return new PrimitiveStack(kind);
    }

    public static PrimitiveQueue primitiveQueue(PrimitiveKind kind) {
        return new PrimitiveQueue(kind);
    }

    public static PrimitiveOrderedSet primitiveOrderedSet(PrimitiveKind kind) {
        return new PrimitiveOrderedSet(kind);
    }

    public static PrimitiveOrderedMap primitiveOrderedMap(
            PrimitiveKind keyKind, PrimitiveKind valueKind) {
        return new PrimitiveOrderedMap(keyKind, valueKind);
    }

    public static PrimitiveBiMap primitiveBiMap(
            PrimitiveKind keyKind, PrimitiveKind valueKind) {
        return new PrimitiveBiMap(keyKind, valueKind);
    }

    public static PrimitiveSortedMultiMap primitiveMultiMap(
            PrimitiveKind keyKind, PrimitiveKind valueKind) {
        return new PrimitiveSortedMultiMap(keyKind, valueKind);
    }

    public static PrimitiveHashMultiMap primitiveHashMultiMap(
            PrimitiveKind keyKind, PrimitiveKind valueKind) {
        return new PrimitiveHashMultiMap(keyKind, valueKind);
    }

    public static PrimitiveSoaStore primitiveRows(PrimitiveKind... laneKinds) {
        return new PrimitiveSoaStore(laneKinds);
    }

    public static PrimitiveSoaStore primitiveRows(
            int initialCapacity, int metadataMask, PrimitiveKind... laneKinds) {
        return new PrimitiveSoaStore(initialCapacity, metadataMask, laneKinds);
    }

    public static PrimitiveIndexView primitiveIndexView(PrimitiveSoaStore rows) {
        return PrimitiveIndexView.identity(rows);
    }

    public static PrimitiveSoaPriorityQueue primitiveRowPriorityQueue(
            int initialCapacity,
            int metadataMask,
            int priorityLane,
            boolean minFirst,
            PrimitiveKind... laneKinds) {
        return new PrimitiveSoaPriorityQueue(
                initialCapacity, metadataMask, priorityLane, minFirst, laneKinds);
    }

    public static PrimitiveHandleTable primitiveHandleTable(
            int initialCapacity, int metadataMask, PrimitiveKind... laneKinds) {
        return new PrimitiveHandleTable(initialCapacity, metadataMask, laneKinds);
    }

    public static PrimitiveBag primitiveBag(PrimitiveKind kind) {
        return new PrimitiveBag(kind);
    }

    public static PrimitiveSortedBag primitiveSortedBag(PrimitiveKind kind) {
        return new PrimitiveSortedBag(kind);
    }

    public static PrimitivePrimitiveHashMap primitiveMap(
            PrimitiveKind keyKind, PrimitiveKind valueKind) {
        return new PrimitivePrimitiveHashMap(keyKind, valueKind);
    }

    public static <V> PrimitiveObjectHashMap<V> primitiveObjectMap(PrimitiveKind keyKind) {
        return new PrimitiveObjectHashMap<>(keyKind);
    }

    public static <K> ObjectPrimitiveHashMap<K> objectPrimitiveMap(PrimitiveKind valueKind) {
        return new ObjectPrimitiveHashMap<>(valueKind);
    }

    public static PrimitiveFrozenList freeze(PrimitiveArrayList source) {
        return PrimitiveFrozenList.copyOf(source);
    }

    public static PrimitiveFrozenSet freeze(PrimitiveHashSet source) {
        return PrimitiveFrozenSet.copyOf(source);
    }

    public static PrimitiveFrozenMap freeze(PrimitivePrimitiveHashMap source) {
        return PrimitiveFrozenMap.copyOf(source);
    }

    public static PrimitiveSortedArraySet primitiveSortedSet(PrimitiveKind kind) {
        return new PrimitiveSortedArraySet(kind);
    }

    public static PrimitiveSortedArrayMap primitiveSortedMap(
            PrimitiveKind keyKind, PrimitiveKind valueKind) {
        return new PrimitiveSortedArrayMap(keyKind, valueKind);
    }

    public static PrimitiveSortedGapSet primitiveGapSortedSet(PrimitiveKind kind) {
        return new PrimitiveSortedGapSet(kind);
    }

    public static PrimitiveSortedGapMap primitiveGapSortedMap(
            PrimitiveKind keyKind, PrimitiveKind valueKind) {
        return new PrimitiveSortedGapMap(keyKind, valueKind);
    }

    public static PrimitiveMinHeap primitiveHeap(PrimitiveKind kind) {
        return new PrimitiveMinHeap(kind);
    }

    public static PrimitiveMinMaxQueue primitiveDoubleEndedPriorityQueue(
            PrimitiveKind kind) {
        return new PrimitiveMinMaxQueue(kind);
    }

    public static PrimitiveLruMap primitiveLruMap(
            PrimitiveKind keyKind, PrimitiveKind valueKind, int maxEntries) {
        return new PrimitiveLruMap(keyKind, valueKind, maxEntries);
    }

    public static PrimitiveTopK primitiveTopK(
            PrimitiveKind kind, int limit, PrimitiveTopK.Mode mode) {
        return new PrimitiveTopK(kind, limit, mode);
    }

    public static PrimitiveFilteredList filteredPrimitiveList(
            PrimitiveKind kind, LongPredicate admission) {
        return new PrimitiveFilteredList(kind, admission);
    }

    public static PrimitiveFilteredSet filteredPrimitiveSet(
            PrimitiveKind kind, LongPredicate admission) {
        return new PrimitiveFilteredSet(kind, admission);
    }

    // Purpose-specific primitive structures.
    public static LongBloomFilter longBloomFilter(int bitCount, int hashCount) {
        return new LongBloomFilter(bitCount, hashCount);
    }

    public static LongCountingBloomFilter longCountingBloomFilter(int counters, int hashes) {
        return new LongCountingBloomFilter(counters, hashes);
    }

    public static LongCountMinSketch longCountMinSketch(int width, int depth) {
        return new LongCountMinSketch(width, depth);
    }

    public static LongHyperLogLog longHyperLogLog(int precision) {
        return new LongHyperLogLog(precision);
    }

    public static LongReservoirSampler longReservoirSampler(int capacity) {
        return new LongReservoirSampler(capacity);
    }

    public static LongSlidingWindow longSlidingWindow(int capacity) {
        return new LongSlidingWindow(capacity);
    }

    public static IntLongAdjacencyGraph intLongAdjacencyGraph(
            int vertexCount, int expectedEdges) {
        return new IntLongAdjacencyGraph(vertexCount, expectedEdges);
    }

    public static IntDisjointSet intDisjointSet(int size) {
        return IntDisjointSet.of(size);
    }

    public static DeltaEncodedLongList deltaEncodedLongs() {
        return new DeltaEncodedLongList();
    }

    public static RunLengthLongList runLengthLongs() {
        return new RunLengthLongList();
    }

    public static LongFenwickTree longFenwickTree(int size) {
        return new LongFenwickTree(size);
    }

    public static LongSegmentTree longSegmentTree(
            int size, long identity, java.util.function.LongBinaryOperator operation) {
        return new LongSegmentTree(size, identity, operation);
    }

    public static RangeAddLongSumTree rangeAddLongSumTree(int size) {
        return new RangeAddLongSumTree(size);
    }

    public static LongSparseTable longSparseTable(
            long[] values, java.util.function.LongBinaryOperator operation) {
        return new LongSparseTable(values, operation);
    }

    public static MonotoneLongDeque monotoneLongDeque(
            int capacity, MonotoneLongDeque.Mode mode) {
        return new MonotoneLongDeque(capacity, mode);
    }

    public static IntRollbackDisjointSet rollbackDisjointSet(int size) {
        return new IntRollbackDisjointSet(size);
    }

    public static PersistentLongSumTree persistentLongSumTree(int size) {
        return new PersistentLongSumTree(size);
    }

    public static LongOrderStatisticMultiset longOrderStatisticMultiset() {
        return new LongOrderStatisticMultiset();
    }

    public static IntWaveletMatrix intWaveletMatrix(int[] values) {
        return new IntWaveletMatrix(values);
    }

    public static IntSuffixArrayIndex intSuffixArrayIndex(int[] values) {
        return new IntSuffixArrayIndex(values);
    }

    public static IntAhoCorasick intAhoCorasick(int[][] patterns) {
        return IntAhoCorasick.compile(patterns);
    }

    public static IntBipartiteMatcher intBipartiteMatcher(
            int leftCount, int rightCount, int[] leftVertices, int[] rightVertices) {
        return new IntBipartiteMatcher(leftCount, rightCount, leftVertices, rightVertices);
    }

    public static IntHeavyLightIndex intHeavyLightIndex(
            IntLongAdjacencyGraph tree, int root) {
        return new IntHeavyLightIndex(tree, root);
    }

    public static IntCoordinateCompressor intCoordinateCompressor(int[] values) {
        return new IntCoordinateCompressor(values);
    }

    public static IntBinaryLiftingLca intBinaryLiftingLca(
            IntLongAdjacencyGraph tree, int root) {
        return new IntBinaryLiftingLca(tree, root);
    }

    public static LongPushRelabelMaxFlow longPushRelabelMaxFlow(
            int vertexCount, int expectedEdges) {
        return new LongPushRelabelMaxFlow(vertexCount, expectedEdges);
    }

    public static IntDoubleSparseVector intDoubleSparseVector() {
        return new IntDoubleSparseVector();
    }

    public static LongTimingWheel longTimingWheel(
            int wheelSize, long tickNanos, long startNanos) {
        return new LongTimingWheel(wheelSize, tickNanos, startNanos);
    }

    public static LongSpaceSavingTopK longSpaceSavingTopK(int capacity) {
        return new LongSpaceSavingTopK(capacity);
    }

    public static LongHistogram longHistogram(
            long minInclusive, long maxExclusive, int buckets) {
        return new LongHistogram(minInclusive, maxExclusive, buckets);
    }

    public static CharTrieSet charTrieSet() {
        return new CharTrieSet();
    }
}
