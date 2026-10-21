/*
 * Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 * Generated from the canonical Synexia hash-pinned Java recipe.
 */
package com.m3.collections.interop;

import com.google.common.collect.ListMultimap;
import com.m3.collections.M3ArrayList;
import com.m3.collections.M3IndexMaskCompactor;
import com.m3.collections.M3LinkedHashMap;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import org.apache.commons.collections4.ListValuedMap;
import org.eclipse.collections.api.multimap.list.MutableListMultimap;
import org.springframework.util.MultiValueMap;

/**
 * Immutable, stable occurrence grouping over one captured M3 index/offset/mask view.
 *
 * <p>The source remains the sole key/value/state payload owner. A first-encounter
 * key directory uses the existing {@link M3LinkedHashMap}; a packed int[] stores
 * source occurrence ordinals, another int[] stores half-open group boundaries.
 * A primitive group-order int[] supports sorting, filtering, and zero-copy group
 * windows. No retained per-occurrence Entry, Node, List or Java wrapper is made.
 * Distinct-group keys are retained once in a small Object[] metadata directory;
 * the M3 hash directory may box a group ID per distinct key, never per occurrence.
 *
 * <p>Grouping uses key equals/hashCode, not comparator equivalence. Sorting uses
 * a caller-provided Comparator on distinct keys and preserves ties; values within
 * a group always retain input occurrence order, including duplicates and nulls.
 * The immutable source is captured by M3FiveLibraryIndexViews before grouping;
 * future caller mutations do not affect these groups. Grouped provider results
 * are independent mutable materializations, not backed/live filtered facades.
 * The five providers keep their different Map/multimap null, duplicate, size,
 * iterator and view protocols. No cross-provider identity is promised.
 *
 * <p>Build expected O(n) hash work and O(n+g) primitive metadata for n occurrences
 * and g distinct keys; worst-case hash behavior depends on caller key hashes.
 * Indexed group lookup is O(1); key lookup is expected O(1), and sorting visible
 * groups costs O(g log g) comparisons and O(g) scratch, without sorting payloads.
 * No concurrency/snapshot transaction, serialization equivalence, universal
 * memory/CPU speedup, native object ownership, or new JNI ABI is claimed.
 */
public final class M3FiveProviderKeyGroups<K, V> {
    private final M3FiveLibraryIndexViews<K, V> source;
    private final M3LinkedHashMap<K, Integer> groupIds;
    private final Object[] keys;
    private final int[] starts;
    private final int[] ordinals;
    private final int[] groupOrder;
    private final int from;
    private final int to;

    private M3FiveProviderKeyGroups(M3FiveLibraryIndexViews<K, V> source,
            M3LinkedHashMap<K, Integer> groupIds, Object[] keys, int[] starts,
            int[] ordinals, int[] groupOrder, int from, int to) {
        this.source = source;
        this.groupIds = groupIds;
        this.keys = keys;
        this.starts = starts;
        this.ordinals = ordinals;
        this.groupOrder = groupOrder;
        this.from = from;
        this.to = to;
    }

    /** Group by key equality in first-occurrence order, preserving duplicate rows. */
    public static <K, V> M3FiveProviderKeyGroups<K, V> snapshot(
            M3FiveLibraryIndexViews<K, V> source) {
        Objects.requireNonNull(source, "source");
        final int size = source.size();
        M3LinkedHashMap<K, Integer> directory = new M3LinkedHashMap<>();
        Object[] keys = new Object[Math.min(size, 8)];
        int[] counts = new int[keys.length];
        int groups = 0;
        for (int ordinal = 0; ordinal < size; ordinal++) {
            K key = source.keyAt(ordinal);
            Integer group = directory.get(key);
            if (group == null) {
                if (groups == keys.length) {
                    int grown = groups + Math.max(1, groups >>> 1) + 1;
                    keys = Arrays.copyOf(keys, Math.min(size, grown));
                    counts = Arrays.copyOf(counts, keys.length);
                }
                group = groups++;
                keys[group] = key;
                directory.put(key, group);
            }
            counts[group]++;
        }
        keys = Arrays.copyOf(keys, groups);
        int[] starts = new int[groups + 1];
        for (int group = 0; group < groups; group++) {
            starts[group + 1] = Math.addExact(starts[group], counts[group]);
        }
        if (starts[groups] != size) throw new IllegalStateException("group cardinality drift");
        int[] write = Arrays.copyOf(starts, groups);
        int[] ordinals = new int[size];
        for (int ordinal = 0; ordinal < size; ordinal++) {
            Integer group = directory.get(source.keyAt(ordinal));
            if (group == null) throw new IllegalStateException("key hash changed during grouping");
            ordinals[write[group]++] = ordinal;
        }
        int[] groupOrder = new int[groups];
        for (int group = 0; group < groups; group++) groupOrder[group] = group;
        return new M3FiveProviderKeyGroups<>(source, directory, keys, starts,
                ordinals, groupOrder, 0, groups);
    }

    /** Select unsigned-byte state bits using the existing Java/checked-JNI selector first. */
    public static <K, V> M3FiveProviderKeyGroups<K, V> snapshotSelected(
            M3FiveLibraryIndexViews<K, V> source, int required, int excluded,
            M3IndexMaskCompactor.Backend backend) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(backend, "backend");
        return snapshot(source.filteredByState(required, excluded, backend));
    }

    public int groupCount() { return to - from; }

    /** Number of selected key/value occurrences (not the number of distinct keys). */
    public int occurrenceCount() {
        int sum = 0;
        for (int index = from; index < to; index++) {
            int id = groupOrder[index];
            sum = Math.addExact(sum, starts[id + 1] - starts[id]);
        }
        return sum;
    }

    private int groupId(int index) {
        return groupOrder[from + Objects.checkIndex(index, groupCount())];
    }

    @SuppressWarnings("unchecked")
    private K keyFor(int id) { return (K) keys[id]; }

    public K keyAt(int groupIndex) { return keyFor(groupId(groupIndex)); }

    public int countAt(int groupIndex) {
        int id = groupId(groupIndex);
        return starts[id + 1] - starts[id];
    }

    /** Index of a selected grouped occurrence within the immutable source's row directory. */
    public int sourceOrdinalAt(int groupIndex, int withinGroupIndex) {
        int id = groupId(groupIndex);
        return ordinals[starts[id] + Objects.checkIndex(withinGroupIndex,
                starts[id + 1] - starts[id])];
    }

    public V valueAt(int groupIndex, int withinGroupIndex) {
        return source.valueAt(sourceOrdinalAt(groupIndex, withinGroupIndex));
    }

    /** Strictly read-only, null-preserving indexed view; no per-element wrapper. */
    public List<V> valuesAt(int groupIndex) {
        int id = groupId(groupIndex);
        return Collections.unmodifiableList(new GroupValues(id));
    }

    /** Empty read-only list for absent or currently excluded groups. */
    public List<V> valuesFor(Object key) {
        Integer id = groupIds.get(key);
        if (id == null) return List.of();
        for (int i = from; i < to; i++) {
            if (groupOrder[i] == id.intValue()) {
                return Collections.unmodifiableList(new GroupValues(id));
            }
        }
        return List.of();
    }

    private final class GroupValues extends AbstractList<V> implements RandomAccess {
        private final int id;
        private GroupValues(int id) { this.id = id; }
        @Override public int size() { return starts[id + 1] - starts[id]; }
        @Override public V get(int index) {
            int logical = ordinals[starts[id] + Objects.checkIndex(index, size())];
            return source.valueAt(logical);
        }
    }

    /** Zero-copy of payload/group metadata; selected group order still shares the int[]. */
    public M3FiveProviderKeyGroups<K, V> groupWindow(int first, int last) {
        Objects.checkFromToIndex(first, last, groupCount());
        return new M3FiveProviderKeyGroups<>(source, groupIds, keys, starts, ordinals,
                groupOrder, from + first, from + last);
    }

    /** Evaluate predicate once per visible distinct key; rejected writes never happen. */
    public M3FiveProviderKeyGroups<K, V> filteredKeys(Predicate<? super K> predicate) {
        Objects.requireNonNull(predicate, "predicate");
        int[] accepted = new int[groupCount()];
        int kept = 0;
        for (int i = from; i < to; i++) {
            int group = groupOrder[i];
            if (predicate.test(keyFor(group))) accepted[kept++] = group;
        }
        if (kept == groupCount()) return this;
        return new M3FiveProviderKeyGroups<>(source, groupIds, keys, starts, ordinals,
                Arrays.copyOf(accepted, kept), 0, kept);
    }

    /** Stable mergesort of group IDs only; comparator-equal distinct keys stay distinct. */
    public M3FiveProviderKeyGroups<K, V> sortedGroups(Comparator<? super K> comparator) {
        Objects.requireNonNull(comparator, "comparator");
        int[] sorted = Arrays.copyOfRange(groupOrder, from, to);
        if (sorted.length < 2) {
            return new M3FiveProviderKeyGroups<>(source, groupIds, keys, starts,
                    ordinals, sorted, 0, sorted.length);
        }
        int[] scratch = new int[sorted.length];
        for (long width = 1; width < sorted.length; width <<= 1) {
            for (long base = 0; base < sorted.length; base += width << 1) {
                int a = (int) base;
                int mid = (int) Math.min(base + width, sorted.length);
                int b = mid;
                int end = (int) Math.min(base + (width << 1), sorted.length);
                int out = a;
                while (a < mid && b < end) {
                    if (comparator.compare(keyFor(sorted[a]), keyFor(sorted[b])) <= 0) {
                        scratch[out++] = sorted[a++];
                    } else {
                        scratch[out++] = sorted[b++];
                    }
                }
                while (a < mid) scratch[out++] = sorted[a++];
                while (b < end) scratch[out++] = sorted[b++];
            }
            int[] swap = sorted; sorted = scratch; scratch = swap;
        }
        return new M3FiveProviderKeyGroups<>(source, groupIds, keys, starts,
                ordinals, sorted, 0, sorted.length);
    }

    /** Traversal is group-order first; all values remain in their original group order. */
    public void forEachPair(BiConsumer<? super K, ? super V> consumer) {
        Objects.requireNonNull(consumer, "consumer");
        for (int outer = from; outer < to; outer++) {
            int id = groupOrder[outer];
            K key = keyFor(id);
            for (int at = starts[id]; at < starts[id + 1]; at++) {
                consumer.accept(key, source.valueAt(ordinals[at]));
            }
        }
    }


    /**
     * Filter GROUPS (not occurrences) by caller-owned physical 32-bit state
     * planes, keeping their current order and every admitted occurrence.
     * Physical plane coordinate is the immutable distinct-group ID. The
     * existing M3IndexPathMasks Java/C17 JNI owner evaluates all stages;
     * object payloads and Java predicates never cross the native boundary.
     *
     * <p>Each plane must have exactly {@code keys.length} physical slots.
     * All supplied metadata must remain stable for the duration of the call.
     * A required native backend never silently switches to Java, including
     * empty group windows. No new M3 payload/index owner is created.</p>
     */
    public M3FiveProviderKeyGroups<K, V> filteredGroupStates(
            int[][] planes, int[] required, int[] forbidden,
            com.m3.collections.M3IndexPathMasks.Backend backend) {
        checkGroupPlanes(planes);
        Objects.requireNonNull(backend, "backend");
        int[] selected = Arrays.copyOfRange(groupOrder, from, to);
        int[] accepted = com.m3.collections.M3IndexPathMasks.select(
                selected, selected, planes, required, forbidden,
                0, selected.length, 1, backend);
        if (backend == com.m3.collections.M3IndexPathMasks.Backend.JAVA
                && accepted.length == selected.length) return this;
        return new M3FiveProviderKeyGroups<>(source, groupIds, keys,
                starts, ordinals, accepted, 0, accepted.length);
    }

    public M3FiveProviderKeyGroups<K, V> filteredGroupStates(
            int[][] planes, int[] required, int[] forbidden) {
        return filteredGroupStates(planes, required, forbidden,
                com.m3.collections.M3IndexPathMasks.Backend.JAVA);
    }

    /** Zero-based current group ordinal of first matching mask, or -1. */
    public int firstMatchingGroupOrdinal(
            int[][] planes, int[] required, int[] forbidden) {
        checkGroupPlanes(planes);
        int[] current = Arrays.copyOfRange(groupOrder, from, to);
        return com.m3.collections.M3IndexPathMasks.firstMatchingIndex(
                current, current, planes, required, forbidden,
                0, current.length, 1);
    }

    private void checkGroupPlanes(int[][] planes) {
        Objects.requireNonNull(planes, "planes");
        for (int stage = 0; stage < planes.length; stage++) {
            int[] lane = Objects.requireNonNull(planes[stage], "planes[" + stage + "]");
            if (lane.length != keys.length) {
                throw new IllegalArgumentException(
                        "group mask physical capacity differs from grouped key count");
            }
        }
    }

    /** Filter without inspecting source values or materializing any entry. */
    public M3FiveProviderKeyGroups<K, V> filteredGroupCounts(
            int minimumInclusive, int maximumInclusive) {
        if (minimumInclusive < 0 || maximumInclusive < minimumInclusive) {
            throw new IllegalArgumentException("invalid inclusive group-count range");
        }
        int[] result = new int[groupCount()];
        int size = 0;
        for (int i = from; i < to; i++) {
            int id = groupOrder[i];
            int count = starts[id + 1] - starts[id];
            if (count >= minimumInclusive && count <= maximumInclusive) {
                result[size++] = id;
            }
        }
        if (size == groupCount()) return this;
        return new M3FiveProviderKeyGroups<>(source, groupIds, keys,
                starts, ordinals, Arrays.copyOf(result, size), 0, size);
    }

    /**
     * Stable occurrence-count rank: sorts only group IDs, not payloads or
     * ordinal offsets. Equal counts preserve the incoming group order.
     */
    public M3FiveProviderKeyGroups<K, V> rankedGroupsByCount(boolean descending) {
        int[] sorted = Arrays.copyOfRange(groupOrder, from, to);
        int[] scratch = new int[sorted.length];
        for (long width = 1; width < sorted.length; width <<= 1) {
            for (long base = 0; base < sorted.length; base += width << 1) {
                int a = (int) base;
                int mid = (int) Math.min(base + width, sorted.length);
                int b = mid;
                int end = (int) Math.min(base + (width << 1), sorted.length);
                int out = a;
                while (a < mid && b < end) {
                    int left = starts[sorted[a] + 1] - starts[sorted[a]];
                    int right = starts[sorted[b] + 1] - starts[sorted[b]];
                    int cmp = descending ? Integer.compare(right, left)
                            : Integer.compare(left, right);
                    scratch[out++] = cmp <= 0 ? sorted[a++] : sorted[b++];
                }
                while (a < mid) scratch[out++] = sorted[a++];
                while (b < end) scratch[out++] = sorted[b++];
            }
            int[] swap = sorted; sorted = scratch; scratch = swap;
        }
        return new M3FiveProviderKeyGroups<>(source, groupIds, keys,
                starts, ordinals, sorted, 0, sorted.length);
    }

    /** JDK LinkedHashSet-shaped key view over the existing M3 indexed set. */
    public java.util.Set<K> asJdkDistinctKeySet() {
        java.util.Set<K> result = new com.m3.collections.M3LinkedHashSet<>();
        for (int i = 0; i < groupCount(); i++) result.add(keyAt(i));
        return result;
    }

    /** JDK Map of independent M3 key-group value sets, not list/multimap API. */
    public Map<K, java.util.Set<V>> asJdkMapOfSets() {
        Map<K, java.util.Set<V>> result = new M3LinkedHashMap<>();
        for (int i = 0; i < groupCount(); i++) {
            java.util.Set<V> values = new com.m3.collections.M3LinkedHashSet<>();
            for (int j = 0; j < countAt(i); j++) values.add(valueAt(i, j));
            result.put(keyAt(i), values);
        }
        return result;
    }

    /** JDK Map materializes one primitive group count per visible key. */
    public Map<K, Integer> asJdkOccurrenceCounts() {
        Map<K, Integer> result = new M3LinkedHashMap<>();
        for (int i = 0; i < groupCount(); i++) result.put(keyAt(i), countAt(i));
        return result;
    }

    /** Guava SetMultimap: suppress equal values within each key's M3 set. */
    public com.google.common.collect.SetMultimap<K, V> asGuavaSetMultimap() {
        com.google.common.collect.SetMultimap<K, V> result =
                M3LibraryCollections.guavaSetMultimap();
        forEachPair((key, value) -> result.put(key, value));
        return result;
    }

    /** Guava SortedSetMultimap: comparator-equivalent values collapse per key. */
    public com.google.common.collect.SortedSetMultimap<K, V>
            asGuavaSortedSetMultimap(Comparator<? super V> valueOrder) {
        com.google.common.collect.SortedSetMultimap<K, V> result =
                M3LibraryCollections.guavaSortedSetMultimap(
                        Objects.requireNonNull(valueOrder, "valueOrder"));
        forEachPair((key, value) -> result.put(key, value));
        return result;
    }

    /** Guava Multiset of KEY occurrences, using indexed counts not repeated nodes. */
    public com.google.common.collect.Multiset<K> asGuavaKeyMultiset() {
        com.google.common.collect.Multiset<K> result = new M3GuavaMultiset<>();
        for (int i = 0; i < groupCount(); i++) result.add(keyAt(i), countAt(i));
        return result;
    }

    /** Eclipse MutableSetMultimap over M3 key and set-value storage owners. */
    public org.eclipse.collections.api.multimap.set.MutableSetMultimap<K, V>
            asEclipseSetMultimap() {
        org.eclipse.collections.api.multimap.set.MutableSetMultimap<K, V> result =
                new M3EclipseSetMultimap<>();
        forEachPair((key, value) -> result.put(key, value));
        return result;
    }

    /** Eclipse MutableBagMultimap: duplicates are counted independently per key. */
    public org.eclipse.collections.api.multimap.bag.MutableBagMultimap<K, V>
            asEclipseBagMultimap() {
        org.eclipse.collections.api.multimap.bag.MutableBagMultimap<K, V> result =
                new M3EclipseBagMultimap<>();
        forEachPair((key, value) -> result.put(key, value));
        return result;
    }

    /** Eclipse MutableBag of grouped KEYS, retaining occurrence multiplicity. */
    public org.eclipse.collections.api.bag.MutableBag<K> asEclipseKeyBag() {
        org.eclipse.collections.api.bag.MutableBag<K> result = new M3EclipseBag<>();
        for (int i = 0; i < groupCount(); i++) {
            result.addOccurrences(keyAt(i), countAt(i));
        }
        return result;
    }

    /** Spring scalar-to-list adapter with last value of each grouped key. */
    public MultiValueMap<K, V> asSpringSingleValueMapLastWins() {
        MultiValueMap<K, V> result = M3LibraryCollections.springSingleValueMap();
        for (int i = 0; i < groupCount(); i++) {
            result.set(keyAt(i), valueAt(i, countAt(i) - 1));
        }
        return result;
    }

    /** Apache SetValuedMap preserves Apache pair cardinality and set views. */
    public org.apache.commons.collections4.SetValuedMap<K, V>
            asApacheSetValuedMap() {
        org.apache.commons.collections4.SetValuedMap<K, V> result =
                M3LibraryCollections.apacheSetValuedMap();
        forEachPair((key, value) -> result.put(key, value));
        return result;
    }

    /** Apache MultiSet of key occurrences over M3 counted-bag slots. */
    public org.apache.commons.collections4.MultiSet<K> asApacheKeyMultiSet() {
        org.apache.commons.collections4.MultiSet<K> result = new M3ApacheMultiSet<>();
        for (int i = 0; i < groupCount(); i++) result.add(keyAt(i), countAt(i));
        return result;
    }

    /** Actual JDK Map-of-lists: size counts distinct keys; values use dense M3ArrayList. */
    public Map<K, List<V>> asJdkMapOfLists() {
        Map<K, List<V>> result = M3FiveListFamilies.jdk();
        for (int i = from; i < to; i++) {
            int id = groupOrder[i];
            M3ArrayList<V> list = new M3ArrayList<>(starts[id + 1] - starts[id]);
            for (int at = starts[id]; at < starts[id + 1]; at++) {
                list.add(source.valueAt(ordinals[at]));
            }
            result.put(keyFor(id), list);
        }
        return result;
    }

    /** Actual Guava ListMultimap: size counts key/value occurrences. */
    public ListMultimap<K, V> asGuavaListMultimap() {
        ListMultimap<K, V> result = M3FiveListFamilies.guava();
        forEachPair((k, v) -> result.put(k, v));
        return result;
    }

    /** Actual Eclipse MutableListMultimap: provider retains its own view protocol. */
    public MutableListMultimap<K, V> asEclipseListMultimap() {
        MutableListMultimap<K, V> result = M3FiveListFamilies.eclipse();
        forEachPair((k, v) -> result.put(k, v));
        return result;
    }

    /** Actual Spring MultiValueMap: Map.size counts distinct keys, unlike multimaps. */
    public MultiValueMap<K, V> asSpringMultiValueMap() {
        MultiValueMap<K, V> result = M3FiveListFamilies.spring();
        forEachPair(result::add);
        return result;
    }

    /** Actual Apache ListValuedMap: mapping-count size, live mutable get semantics. */
    public ListValuedMap<K, V> asApacheListValuedMap() {
        ListValuedMap<K, V> result = M3FiveListFamilies.apache();
        forEachPair((k, v) -> result.put(k, v));
        return result;
    }
}
