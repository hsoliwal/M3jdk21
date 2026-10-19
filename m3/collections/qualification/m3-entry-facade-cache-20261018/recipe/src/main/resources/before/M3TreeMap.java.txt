// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified 2026 by Hitesh Soliwal and Contributors to the Synexia Project: Synexia-to-M3 package/type adaptation.
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.SortedMap;

/**
 * AVL map with reference payload arrays and integer child/parent/free links.
 * Search, insertion and deletion are O(log n); bounded-view size is O(n).
 * Views share storage. Only explicit entry APIs construct Map.Entry objects.
 * Empty owners share zero-length arrays; AVL heights use one byte per allocated slot.
 * An optional provider resolves explicitly deferred values on value access or touch.
 * Provider state is one short lane shared by all views; eager/key-only trees allocate none.
 * This collection is not thread-safe. Providers must not mutate its shared mappings.
 */
public final class M3TreeMap<K, V> extends M3OrderedSlotMap<K, V> implements NavigableMap<K, V> {
    private final Tree<K, V> tree;
    private final boolean hasLow;
    private final K low;
    private final boolean lowInclusive;
    private final boolean hasHigh;
    private final K high;
    private final boolean highInclusive;
    private final boolean descending;

    /** Natural key ordering; null keys are rejected. */
    public M3TreeMap() { this(null); }
    /** Comparator ordering, including comparator-defined null handling. */
    public M3TreeMap(Comparator<? super K> comparator) { this(comparator, false); }
    M3TreeMap(Comparator<? super K> comparator, boolean keyOnly) {
        this(new Tree<>(comparator, keyOnly, null), false, null, false, false, null, false, false);
    }
    /**
     * Opt-in lazy values; admit keys with defer before requesting them.
     * Missing get calls never admit keys. Explicit put values, including null, are loaded.
     */
    public M3TreeMap(Comparator<? super K> comparator,
            M3LazyValueProvider<? super K, ? extends V> provider) {
        this(new Tree<>(comparator, false, Objects.requireNonNull(provider, "provider")),
                false, null, false, false, null, false, false);
    }
    private M3TreeMap(Tree<K, V> tree, boolean hasLow, K low, boolean lowInclusive,
            boolean hasHigh, K high, boolean highInclusive, boolean descending) {
        this.tree = tree; this.hasLow = hasLow; this.low = low; this.lowInclusive = lowInclusive;
        this.hasHigh = hasHigh; this.high = high; this.highInclusive = highInclusive; this.descending = descending;
    }
    private K checked(Object key) {
        @SuppressWarnings("unchecked") K k = (K) key;
        tree.compare(k, k); return k;
    }
    private boolean within(K key) {
        int c;
        return !(hasLow && ((c = tree.compare(key, low)) < 0 || c == 0 && !lowInclusive)
                || hasHigh && ((c = tree.compare(key, high)) > 0 || c == 0 && !highInclusive));
    }
    private boolean slotWithin(int s) { return s >= 0 && within(keyAt(s)); }
    private void endpoint(K key, boolean inclusive) {
        checked(key);
        int c;
        if (hasLow && ((c = tree.compare(key, low)) < 0 || c == 0 && inclusive && !lowInclusive)
                || hasHigh && ((c = tree.compare(key, high)) > 0 || c == 0 && inclusive && !highInclusive)) {
            throw new IllegalArgumentException("endpoint outside view");
        }
    }
    private int lowest() {
        int s = hasLow ? tree.near(low, lowInclusive ? 1 : 2) : tree.extreme(tree.root, false);
        return slotWithin(s) ? s : -1;
    }
    private int highest() {
        int s = hasHigh ? tree.near(high, highInclusive ? -1 : -2) : tree.extreme(tree.root, true);
        return slotWithin(s) ? s : -1;
    }
    @Override int firstSlot() { return descending ? highest() : lowest(); }
    @Override int lastSlot() { return descending ? lowest() : highest(); }
    @Override int nextSlot(int slot) {
        int s = tree.step(slot, descending); return slotWithin(s) ? s : -1;
    }
    @Override int findSlot(Object key) {
        K k = checked(key); return within(k) ? tree.find(k) : -1;
    }
    @Override K keyAt(int slot) { return M3SlotArrays.get(tree.keys, slot); }
    @Override V valueAt(int slot) {
        return valueAt(slot, M3LazyOperation.READ, null, null);
    }
    private V valueAt(int slot, M3Progress monitor) {
        return valueAt(slot, M3LazyOperation.READ, null, monitor);
    }
    private V valueAt(
            int slot,
            M3LazyOperation operation,
            M3LazyEventPolicy eventPolicy,
            M3Progress monitor) {
        return tree.lazy == null ? (tree.keyOnly ? null : M3SlotArrays.get(tree.values, slot))
                : tree.lazy.read(slot, keyAt(slot), tree.values, operation, eventPolicy, monitor);
    }
    @Override V writeAt(int slot, V value) {
        tree.writable(); V old = valueAt(slot);
        if (!tree.keyOnly) { tree.values[slot] = value; }
        if (tree.lazy != null) { tree.lazy.admitted(slot, true); }
        return old;
    }
    @Override V eraseAt(int slot) { tree.writable(); V old = valueAt(slot); tree.erase(slot); return old; }
    @Override int revision() { return tree.modifications; }
    @Override public int size() {
        if (!hasLow && !hasHigh) { return tree.size; }
        int count = 0; for (int s = firstSlot(); s >= 0; s = nextSlot(s)) { count++; } return count;
    }
    @Override public boolean isEmpty() { return firstSlot() < 0; }
    @Override public V put(K key, V value) {
        tree.writable(); checked(key);
        if (!within(key)) { throw new IllegalArgumentException("key outside view"); }
        int expected = revision(), p = -1, s = tree.root, cmp = 0;
        while (s >= 0) {
            cmp = tree.compare(key, keyAt(s));
            if (cmp == 0) { check(expected); return writeAt(s, value); }
            p = s; s = cmp < 0 ? tree.left[s] : tree.right[s];
        }
        check(expected); tree.insert(p, cmp, key, value); return null;
    }
    @Override public void clear() {
        tree.writable();
        if (!hasLow && !hasHigh) { tree.clear(); return; }
        int s = firstSlot();
        while (s >= 0) { int next = nextSlot(s); tree.erase(s); s = next; }
    }
    /**
     * Admit a key without loading. Existing mappings are untouched and return false.
     * Requires the provider constructor; the current view's bounds still apply.
     */
    public boolean defer(K key) {
        if (tree.lazy == null) { throw new IllegalStateException("no lazy value provider"); }
        tree.writable(); checked(key);
        if (!within(key)) { throw new IllegalArgumentException("key outside view"); }
        int expected = revision(), p = -1, s = tree.root, cmp = 0;
        while (s >= 0) {
            cmp = tree.compare(key, keyAt(s));
            if (cmp == 0) { check(expected); return false; }
            p = s; s = cmp < 0 ? tree.left[s] : tree.right[s];
        }
        check(expected); tree.insert(p, cmp, key, null, false); return true;
    }
    /** State without invoking the provider; ABSENT denotes a key outside this view. */
    public short valueFlags(Object key) {
        int s = findSlot(key);
        return s < 0 ? M3LazyValueProvider.ABSENT
                : tree.lazy == null ? M3LazyValueProvider.LOADED : tree.lazy.flags(s);
    }
    /** True for resolved/explicit values, including null; never loads an element. */
    public boolean isValueLoaded(Object key) { return valueFlags(key) == M3LazyValueProvider.LOADED; }
    /** Synchronous pre-touch hook for a visible, hovered or selected model item. */
    public V touch(Object key) { return touch(key, null, null); }
    /** Value access with caller progress/cancellation; an absent key remains absent. */
    public V touch(Object key, M3Progress monitor) { return touch(key, null, monitor); }
    /** Value access with per-call event mask/granularity override. */
    public V touch(Object key, M3LazyEventPolicy eventPolicy, M3Progress monitor) {
        try (M3ProgressScope scope = M3ProgressScope.resolve(monitor, 1L)) {
            M3Progress progress = scope.monitor();
            M3LazyValueState.checkCancelled(progress);
            int s = findSlot(key);
            return s < 0 ? null : valueAt(s, M3LazyOperation.TOUCH, eventPolicy, progress);
        }
    }
    /** Resolve only the current bounded/descending view using system progress when absent. */
    public void preload() { preload(null, null); }
    /** Resolve the current view with caller progress/cancellation. */
    public void preload(M3Progress monitor) { preload(null, monitor); }
    /**
     * Resolve the current view in encounter order. Completed slots survive failure or
     * cancellation; no success is credited for the failing slot. Monitor interference
     * with map structure is detected before continuing traversal. The event policy may
     * enable range, element, progress or state-transition observations for this call.
     */
    public void preload(M3LazyEventPolicy eventPolicy, M3Progress monitor) {
        int expected = revision();
        int work = size();
        try (M3ProgressScope scope = M3ProgressScope.resolve(monitor, Math.max(1L, work))) {
            M3Progress progress = scope.monitor();
            M3LazyEventPolicy policy =
                    tree.lazy == null ? M3LazyEventPolicy.NONE : tree.lazy.eventPolicy(eventPolicy);
            Throwable failure = null;
            if (tree.lazy != null) {
                tree.lazy.emit(
                        policy, M3LazyEventBits.RANGE_BEGIN, M3LazyEventGranularity.RANGE,
                        M3LazyEventSink.NO_SLOT, (short) 0, (short) 0,
                        M3LazyOperation.PRELOAD, progress, null);
            }
            try {
                progress.beginTask("Load packed tree values", work); check(expected);
                for (int s = firstSlot(); s >= 0; s = nextSlot(s)) {
                    M3LazyValueState.checkCancelled(progress);
                    valueAt(s, M3LazyOperation.PRELOAD, eventPolicy, progress);
                    check(expected);
                    progress.worked(1);
                    if (tree.lazy != null) {
                        short state = tree.lazy.flags(s);
                        tree.lazy.emit(
                                policy, M3LazyEventBits.PROGRESS, M3LazyEventGranularity.ELEMENT,
                                s, state, state, M3LazyOperation.PRELOAD, progress, null);
                    }
                    check(expected);
                }
                M3LazyValueState.checkCancelled(progress);
                progress.done();
            } catch (RuntimeException | Error problem) {
                failure = problem;
                throw problem;
            } finally {
                if (tree.lazy != null) {
                    tree.lazy.emit(
                            policy, M3LazyEventBits.RANGE_END, M3LazyEventGranularity.RANGE,
                            M3LazyEventSink.NO_SLOT, (short) 0, (short) 0,
                            M3LazyOperation.PRELOAD, progress, failure);
                }
            }
        }
    }
    @Override public Comparator<? super K> comparator() {
        return descending ? Collections.reverseOrder(tree.comparator) : tree.comparator;
    }
    private int near(K key, int relation) {
        checked(key); int r = descending ? -relation : relation;
        int s = tree.near(key, r);
        if (s < 0) { return -1; }
        if (r < 0 && hasHigh) {
            int c = tree.compare(keyAt(s), high);
            if (c > 0 || c == 0 && !highInclusive) { s = highest(); }
        } else if (r > 0 && hasLow) {
            int c = tree.compare(keyAt(s), low);
            if (c < 0 || c == 0 && !lowInclusive) { s = lowest(); }
        }
        return slotWithin(s) ? s : -1;
    }
    private K nullableKey(int s) { return s < 0 ? null : keyAt(s); }
    private K requiredKey(int s) { if (s < 0) { throw new NoSuchElementException(); } return keyAt(s); }
    @Override public K firstKey() { return requiredKey(firstSlot()); }
    @Override public K lastKey() { return requiredKey(lastSlot()); }
    @Override public Entry<K, V> firstEntry() { return snapshot(firstSlot()); }
    @Override public Entry<K, V> lastEntry() { return snapshot(lastSlot()); }
    @Override public Entry<K, V> lowerEntry(K k) { return snapshot(near(k, -2)); }
    @Override public Entry<K, V> floorEntry(K k) { return snapshot(near(k, -1)); }
    @Override public Entry<K, V> ceilingEntry(K k) { return snapshot(near(k, 1)); }
    @Override public Entry<K, V> higherEntry(K k) { return snapshot(near(k, 2)); }
    @Override public K lowerKey(K k) { return nullableKey(near(k, -2)); }
    @Override public K floorKey(K k) { return nullableKey(near(k, -1)); }
    @Override public K ceilingKey(K k) { return nullableKey(near(k, 1)); }
    @Override public K higherKey(K k) { return nullableKey(near(k, 2)); }
    private Entry<K, V> poll(int s) { Entry<K, V> e = snapshot(s); if (s >= 0) { tree.erase(s); } return e; }
    @Override public Entry<K, V> pollFirstEntry() { return poll(firstSlot()); }
    @Override public Entry<K, V> pollLastEntry() { return poll(lastSlot()); }
    K pollKey(boolean last) {
        int s = last ? lastSlot() : firstSlot(); K k = nullableKey(s); if (s >= 0) { tree.erase(s); } return k;
    }
    @Override public M3TreeMap<K, V> reversed() { return descendingMap(); }
    @Override public M3TreeMap<K, V> descendingMap() {
        return new M3TreeMap<>(tree, hasLow, low, lowInclusive, hasHigh, high, highInclusive, !descending);
    }
    @Override public NavigableSet<K> sequencedKeySet() { return navigableKeySet(); }
    @Override public NavigableSet<K> navigableKeySet() { return new M3TreeSet<>(this, false); }
    @Override public NavigableSet<K> keySet() { return navigableKeySet(); }
    @Override public NavigableSet<K> descendingKeySet() { return descendingMap().navigableKeySet(); }
    @Override public M3TreeMap<K, V> subMap(K from, boolean fromInclusive, K to, boolean toInclusive) {
        endpoint(from, fromInclusive); endpoint(to, toInclusive);
        int c = tree.compare(from, to);
        if (descending ? c < 0 : c > 0) { throw new IllegalArgumentException("inverted range"); }
        return descending ? new M3TreeMap<>(tree, true, to, toInclusive, true, from, fromInclusive, true)
                : new M3TreeMap<>(tree, true, from, fromInclusive, true, to, toInclusive, false);
    }
    @Override public M3TreeMap<K, V> headMap(K to, boolean inclusive) {
        endpoint(to, inclusive);
        return descending ? new M3TreeMap<>(tree, true, to, inclusive, hasHigh, high, highInclusive, true)
                : new M3TreeMap<>(tree, hasLow, low, lowInclusive, true, to, inclusive, false);
    }
    @Override public M3TreeMap<K, V> tailMap(K from, boolean inclusive) {
        endpoint(from, inclusive);
        return descending ? new M3TreeMap<>(tree, hasLow, low, lowInclusive, true, from, inclusive, true)
                : new M3TreeMap<>(tree, true, from, inclusive, hasHigh, high, highInclusive, false);
    }
    @Override public SortedMap<K, V> subMap(K from, K to) { return subMap(from, true, to, false); }
    @Override public SortedMap<K, V> headMap(K to) { return headMap(to, false); }
    @Override public SortedMap<K, V> tailMap(K from) { return tailMap(from, true); }
    /** Throws AssertionError if ownership, ordering, heights or AVL balance is damaged. */
    public void verifyInvariants() { tree.verify(); }

    private static final class Tree<K, V> {
        private Object[] keys = M3SlotArrays.EMPTY_OBJECTS;
        private Object[] values = M3SlotArrays.EMPTY_OBJECTS;
        private int[] left = M3SlotArrays.EMPTY_INTS, right = M3SlotArrays.EMPTY_INTS, parent = M3SlotArrays.EMPTY_INTS;
        // N(45)=2,971,215,072 exceeds the slot limit; even a transient height fits a byte.
        private byte[] height = M3SlotArrays.EMPTY_BYTES;
        private int root = -1, free = -1, used, size, modifications;
        private final Comparator<? super K> comparator;
        private final boolean keyOnly;
        private final M3LazyValueState<K, V> lazy;
        Tree(Comparator<? super K> comparator, boolean keyOnly,
                M3LazyValueProvider<? super K, ? extends V> provider) {
            this.comparator = comparator; this.keyOnly = keyOnly;
            lazy = provider == null ? null : new M3LazyValueState<>(provider);
        }
        void writable() { if (lazy != null) { lazy.writable(); } }
        @SuppressWarnings("unchecked") int compare(K a, K b) {
            return comparator == null ? ((Comparable<? super K>) Objects.requireNonNull(a)).compareTo(b) : comparator.compare(a, b);
        }
        int find(K key) {
            int s = root;
            while (s >= 0) { int c = compare(key, M3SlotArrays.get(keys, s)); if (c == 0) { return s; } s = c < 0 ? left[s] : right[s]; }
            return -1;
        }
        int extreme(int s, boolean maximum) {
            if (s < 0) { return -1; }
            while ((maximum ? right[s] : left[s]) >= 0) { s = maximum ? right[s] : left[s]; } return s;
        }
        int step(int s, boolean back) {
            int child = back ? left[s] : right[s];
            if (child >= 0) { return extreme(child, back); }
            int p = parent[s];
            while (p >= 0 && s == (back ? left[p] : right[p])) { s = p; p = parent[p]; }
            return p;
        }
        int near(K key, int relation) {
            int s = root, candidate = -1;
            while (s >= 0) {
                int c = compare(key, M3SlotArrays.get(keys, s));
                if (c == 0) { return Math.abs(relation) == 1 ? s : step(s, relation < 0); }
                if (c < 0) { if (relation > 0) { candidate = s; } s = left[s]; }
                else { if (relation < 0) { candidate = s; } s = right[s]; }
            }
            return candidate;
        }
        int h(int s) { return s < 0 ? 0 : height[s]; }
        void update(int s) { height[s] = (byte) (1 + Math.max(h(left[s]), h(right[s]))); }
        void connect(int old, int replacement) {
            int p = parent[old];
            if (p < 0) { root = replacement; } else if (left[p] == old) { left[p] = replacement; } else { right[p] = replacement; }
            if (replacement >= 0) { parent[replacement] = p; }
        }
        int rotate(int s, boolean toRight) {
            int pivot = toRight ? left[s] : right[s];
            int middle = toRight ? right[pivot] : left[pivot];
            connect(s, pivot);
            if (toRight) { right[pivot] = s; left[s] = middle; } else { left[pivot] = s; right[s] = middle; }
            parent[s] = pivot; if (middle >= 0) { parent[middle] = s; }
            update(s); update(pivot); return pivot;
        }
        void balance(int s) {
            while (s >= 0) {
                update(s); int delta = h(left[s]) - h(right[s]);
                if (delta > 1) {
                    if (h(left[left[s]]) < h(right[left[s]])) { rotate(left[s], false); }
                    s = rotate(s, true);
                } else if (delta < -1) {
                    if (h(right[right[s]]) < h(left[right[s]])) { rotate(right[s], true); }
                    s = rotate(s, false);
                }
                s = parent[s];
            }
        }
        void grow() {
            int n = M3SlotArrays.grow(keys.length);
            Object[] k = Arrays.copyOf(keys, n), v = keyOnly ? values : Arrays.copyOf(values, n);
            int[] l = Arrays.copyOf(left, n), r = Arrays.copyOf(right, n), p = Arrays.copyOf(parent, n);
            byte[] h = Arrays.copyOf(height, n);
            if (lazy != null) { lazy.grow(n); }
            keys = k; values = v; left = l; right = r; parent = p; height = h;
        }
        void insert(int p, int direction, K key, V value) { insert(p, direction, key, value, true); }
        void insert(int p, int direction, K key, V value, boolean loaded) {
            writable();
            if (free < 0 && used == keys.length) { grow(); }
            int s;
            if (free < 0) { s = used++; } else { s = free; free = left[s]; }
            keys[s] = key; if (!keyOnly) { values[s] = value; }
            if (lazy != null) { lazy.admitted(s, loaded); }
            left[s] = -1; right[s] = -1; parent[s] = p; height[s] = 1;
            if (p < 0) { root = s; } else if (direction < 0) { left[p] = s; } else { right[p] = s; }
            size++; modifications++; balance(p);
        }
        void erase(int s) {
            writable();
            int rebalance;
            if (left[s] < 0 || right[s] < 0) {
                rebalance = parent[s]; connect(s, left[s] < 0 ? right[s] : left[s]);
            } else {
                int successor = extreme(right[s], false);
                if (parent[successor] == s) { rebalance = successor; }
                else {
                    rebalance = parent[successor]; connect(successor, right[successor]);
                    right[successor] = right[s]; parent[right[successor]] = successor;
                }
                connect(s, successor); left[successor] = left[s]; parent[left[successor]] = successor;
                update(successor);
            }
            keys[s] = null; if (!keyOnly) { values[s] = null; }
            if (lazy != null) { lazy.erased(s); }
            height[s] = 0; parent[s] = -1; right[s] = -1; left[s] = free; free = s;
            size--; modifications++; balance(rebalance);
        }
        void clear() {
            writable();
            if (size == 0) { return; }
            Arrays.fill(keys, null); Arrays.fill(values, null); Arrays.fill(height, (byte) 0);
            if (lazy != null) { lazy.cleared(); }
            root = -1; free = -1; used = 0; size = 0; modifications++;
        }
        void verify() {
            if (lazy != null && lazy.capacity() != keys.length) { throw new AssertionError("lazy lane capacity"); }
            boolean[] seen = new boolean[used];
            if (verify(root, -1, seen) != size) { throw new AssertionError("tree size"); }
            int n = size;
            for (int s = free; s >= 0; s = left[s]) {
                if (s >= used || seen[s] || height[s] != 0 || keys[s] != null || !keyOnly && values[s] != null) { throw new AssertionError("free slot"); }
                if (lazy != null) { lazy.verifySlot(s, false, values); }
                seen[s] = true; n++;
            }
            if (n != used) { throw new AssertionError("unowned slots"); }
            int previous = -1;
            for (int s = extreme(root, false); s >= 0; s = step(s, false)) {
                if (previous >= 0 && compare(M3SlotArrays.get(keys, previous), M3SlotArrays.get(keys, s)) >= 0) { throw new AssertionError("ordering"); }
                previous = s;
            }
        }
        int verify(int s, int p, boolean[] seen) {
            if (s < 0) { return 0; }
            if (s >= used || seen[s] || parent[s] != p) { throw new AssertionError("tree links"); }
            if (lazy != null) { lazy.verifySlot(s, true, values); }
            seen[s] = true; int n = 1 + verify(left[s], s, seen) + verify(right[s], s, seen);
            if (height[s] != 1 + Math.max(h(left[s]), h(right[s])) || Math.abs(h(left[s]) - h(right[s])) > 1) { throw new AssertionError("AVL balance"); }
            return n;
        }
    }
}
