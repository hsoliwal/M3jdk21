package com.synexia.primitives;

import java.util.function.BiPredicate;
import java.util.function.LongPredicate;

/** Entry point for the no-boxing/no-entry-object collection family. */
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
    public static LongSortedSet longSortedSet() { return new LongSortedSet(); }
    public static LongByteSortedMap longByteSortedMap() { return new LongByteSortedMap(); }
    public static LongLongSortedMap longLongSortedMap() { return new LongLongSortedMap(); }
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
    public static LongBitSet bits() { return new LongBitSet(); }
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
    public static PrimitiveRoaringIntSet roaringInts() { return new PrimitiveRoaringIntSet(); }
    public static PrimitiveRoaringLongSet roaringLongs() { return new PrimitiveRoaringLongSet(); }
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

    public static PrimitiveBigArray primitiveBigArray(PrimitiveKind kind, long length) {
        return new PrimitiveBigArray(kind, length);
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

    public static PrimitiveBigList primitiveBigList(PrimitiveKind kind) {
        return new PrimitiveBigList(kind);
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

    public static PrimitiveDoubleEndedPriorityQueue primitiveDoubleEndedPriorityQueue(
            PrimitiveKind kind) {
        return new PrimitiveDoubleEndedPriorityQueue(kind);
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
        return new IntDisjointSet(size);
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
