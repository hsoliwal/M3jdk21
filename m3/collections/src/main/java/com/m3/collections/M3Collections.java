/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 * Adapted from the pinned Synexia packed collection factory through its canonical recipe.
 */
// Modified 2026 by Hitesh Soliwal and contributors: include the existing primitive hash/heap owners.
// Modified 2026 by Hitesh Soliwal and contributors: recover the qualified factory
// surface alongside the current primitive, enum and identity owners.
package com.m3.collections;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * Entry points for Java 21 collection contracts and stable-ID primitive storage.
 *
 * <p>Each factory preserves its declared contract. Packed node-heavy families use primitive
 * slot links; already dense JDK array, ring, heap, enum and copy-on-write owners are reused.
 * Locked alternatives explicitly acquire locks. These independent M3 APIs do not promise
 * concrete JDK class identity or serialized-form compatibility.
 *
 * <p>The existing sparse lanes remain available for stable addresses and lazy page growth.
 * Dense primitive sequences use the same M3LongCollection and M3LongIterator contracts.
 * Representation choice depends on workload; these factories do not assert universal speed.
 */
public final class M3Collections {
    private M3Collections() {}

    public static M3LongLane28 longLane() {
        return new M3LongLane28();
    }

    public static M3IntLane28 intLane() {
        return new M3IntLane28();
    }

    public static M3BitLane28 bitLane() {
        return new M3BitLane28();
    }

    public static M3SlotAllocator28 stableSlots() {
        return new M3SlotAllocator28();
    }

    public static M3LongLaneStore28 longRows(int laneCount) {
        return new M3LongLaneStore28(laneCount);
    }

    public static M3LongArena28 longArena() {
        return new M3LongArena28();
    }

    public static M3LongLinkedList28 longLinkedList() {
        return new M3LongLinkedList28();
    }
    /** Concurrent primitive values or metadata, with explicit per-slot atomic operations. */
    public static M3ConcurrentLongLane28 concurrentLongLane() { return new M3ConcurrentLongLane28(); }

    /** Concurrent int metadata; allocating pages is separate from prepared-page operations. */
    public static M3ConcurrentIntLane28 concurrentIntLane() { return new M3ConcurrentIntLane28(); }

    /** Contiguous positional sequence. */
    public static <E> ArrayList<E> arrayList() { return new ArrayList<>(); }
    /** Linked list/deque using primitive links and reusable slots. */
    public static <E> M3LinkedList<E> linkedList() { return new M3LinkedList<>(); }
    /** Open-addressed stable-slot hash map. */
    public static <K, V> M3LinkedHashMap<K, V> hashMap() { return new M3LinkedHashMap<>(); }
    /** Insertion-ordered map. */
    public static <K, V> M3LinkedHashMap<K, V> linkedHashMap() { return new M3LinkedHashMap<>(); }
    /** Access- or insertion-ordered map. */
    public static <K, V> M3LinkedHashMap<K, V> linkedHashMap(boolean accessOrder) { return new M3LinkedHashMap<>(accessOrder); }
    /** Natural-order balanced tree. */
    public static <K, V> M3TreeMap<K, V> treeMap() { return new M3TreeMap<>(); }
    /** Comparator-order balanced tree. */
    public static <K, V> M3TreeMap<K, V> treeMap(Comparator<? super K> comparator) { return new M3TreeMap<>(comparator); }
    /** Key-only open-addressed set. */
    public static <E> M3LinkedHashSet<E> hashSet() { return new M3LinkedHashSet<>(); }
    /** Insertion-ordered, key-only set with primitive links. */
    public static <E> M3LinkedHashSet<E> linkedHashSet() { return new M3LinkedHashSet<>(); }
    /** Natural-order, key-only balanced tree set. */
    public static <E> M3TreeSet<E> treeSet() { return new M3TreeSet<>(); }
    /** Comparator-order, key-only balanced tree set. */
    public static <E> M3TreeSet<E> treeSet(Comparator<? super E> comparator) { return new M3TreeSet<>(comparator); }
    /** Ring deque already storing payload references directly. */
    public static <E> ArrayDeque<E> arrayDeque() { return new ArrayDeque<>(); }
    /** Binary heap already storing payload references directly, without entry objects. */
    public static <E> PriorityQueue<E> priorityQueue() { return new PriorityQueue<>(); }
    /** Comparator-based binary heap. */
    public static <E> PriorityQueue<E> priorityQueue(Comparator<? super E> comparator) { return new PriorityQueue<>(comparator); }
    /** Alternating key/value array with reference-identity equality. */
    public static <K, V> IdentityHashMap<K, V> identityMap() { return new IdentityHashMap<>(); }
    /** Weak keys, with reference queue cleanup and parallel payload/control lanes. */
    public static <K, V> M3WeakHashMap<K, V> weakMap() { return new M3WeakHashMap<>(); }
    /** Ordinal-indexed value array. */
    public static <K extends Enum<K>, V> EnumMap<K, V> enumMap(Class<K> type) { return new EnumMap<>(type); }
    /** Word/word-array membership by enum ordinal. */
    public static <E extends Enum<E>> EnumSet<E> enumSet(Class<E> type) { return EnumSet.noneOf(type); }
    /** Bit-addressed membership. */
    public static BitSet bitSet() { return new BitSet(); }
    /** Legacy synchronized array list. */
    public static <E> Vector<E> vector() { return new Vector<>(); }
    /** Legacy synchronized array stack. */
    public static <E> Stack<E> stack() { return new Stack<>(); }
    /** Immutable snapshot arrays for readers. */
    public static <E> CopyOnWriteArrayList<E> copyOnWriteList() { return new CopyOnWriteArrayList<>(); }
    /** Copy-on-write array set. */
    public static <E> CopyOnWriteArraySet<E> copyOnWriteSet() { return new CopyOnWriteArraySet<>(); }
    /** Hash ConcurrentMap alternative using a shared lock. */
    public static <K, V> M3LockedMap<K, V> lockedMap() { return new M3LockedMap<>(); }
    /** Non-null, synchronized Map alternative to Hashtable; no Dictionary/serialization emulation. */
    public static <K, V> M3LockedMap<K, V> lockedHashtableMap() { return new M3LockedMap<>(); }
    /** Concurrent sorted map using the packed AVL tree and one shared lock. */
    public static <K, V> M3LockedTreeMap<K, V> lockedTreeMap() { return new M3LockedTreeMap<>(); }
    /** Comparator-ordered concurrent map. */
    public static <K, V> M3LockedTreeMap<K, V> lockedTreeMap(Comparator<? super K> comparator) { return new M3LockedTreeMap<>(comparator); }
    /** Concurrent hash set over packed map storage. */
    public static <E> Set<E> lockedHashSet() { return Collections.newSetFromMap(new M3LockedMap<>()); }
    /** Concurrent sorted set with shared-lock range views. */
    public static <E> M3LockedTreeSet<E> lockedTreeSet() { return new M3LockedTreeSet<>(); }
    /** Comparator-ordered concurrent set. */
    public static <E> M3LockedTreeSet<E> lockedTreeSet(Comparator<? super E> comparator) { return new M3LockedTreeSet<>(comparator); }
    /** Concurrent FIFO alternative; operations acquire a lock. */
    public static <E> M3BlockingDeque<E> lockedQueue() { return new M3BlockingDeque<>(); }
    /** Concurrent deque alternative; operations acquire a lock. */
    public static <E> M3BlockingDeque<E> lockedDeque() { return new M3BlockingDeque<>(); }
    /** Growable bounded FIFO/deque with condition waiting. */
    public static <E> M3BlockingDeque<E> blockingQueue(int capacity) { return new M3BlockingDeque<>(capacity); }
    /** Growable bounded FIFO/deque with condition waiting. */
    public static <E> M3BlockingDeque<E> blockingDeque(int capacity) { return new M3BlockingDeque<>(capacity); }
    /** Fixed array blocking queue, using the JDK implementation. */
    public static <E> ArrayBlockingQueue<E> arrayBlockingQueue(int capacity, boolean fair) { return new ArrayBlockingQueue<>(capacity, fair); }
    /** Blocking heap with direct element storage. */
    public static <E> PriorityBlockingQueue<E> priorityBlockingQueue(Comparator<? super E> comparator) { return new PriorityBlockingQueue<>(11, comparator); }
    /** Delay-ordered blocking heap. */
    public static <E extends Delayed> DelayQueue<E> delayQueue() { return new DelayQueue<>(); }
    /** FIFO transfer with array-backed pending data, requests and acknowledgements. */
    public static <E> M3TransferQueue<E> transferQueue() { return new M3TransferQueue<>(); }
    /** Zero-capacity FIFO rendezvous; fairness selects the lock admission policy. */
    public static <E> M3TransferQueue<E> synchronousQueue(boolean fair) { return new M3TransferQueue<>(true, fair); }
    /** Null-rejecting immutable list snapshot. */
    public static <E> List<E> immutableList(Collection<? extends E> source) { return List.copyOf(source); }
    /** Null-rejecting immutable packed set snapshot. */
    public static <E> Set<E> immutableSet(Collection<? extends E> source) {
        M3LinkedHashSet<E> set = new M3LinkedHashSet<>(); source.forEach(e -> set.add(Objects.requireNonNull(e)));
        return Collections.unmodifiableSet(set);
    }
    /** Null-rejecting immutable packed map snapshot. */
    public static <K, V> Map<K, V> immutableMap(Map<? extends K, ? extends V> source) {
        M3LinkedHashMap<K, V> map = new M3LinkedHashMap<>();
        source.forEach((k, v) -> map.put(Objects.requireNonNull(k), Objects.requireNonNull(v)));
        return Collections.unmodifiableMap(map);
    }

    /** Dense primitive-long positional list, with no retained Long wrappers. */
    public static M3LongArrayList longArrayList() { return new M3LongArrayList(); }

    /** Dense primitive-long positional list with an explicit initial capacity. */
    public static M3LongArrayList longArrayList(int expectedSize) {
        return new M3LongArrayList(expectedSize);
    }

    /** Primitive-long ring deque. */
    public static M3LongArrayDeque longArrayDeque() { return new M3LongArrayDeque(); }

    /** Primitive-long ring deque with an explicit initial capacity. */
    public static M3LongArrayDeque longArrayDeque(int expectedSize) {
        return new M3LongArrayDeque(expectedSize);
    }

    /** Open-addressed primitive-long set with independent occupancy bytes for all long values. */
    public static M3LongHashSet longHashSet() { return new M3LongHashSet(); }

    /** Open-addressed primitive-long key/value map; insertion reports whether a key is new. */
    public static M3LongLongHashMap longLongHashMap() { return new M3LongLongHashMap(); }

    /** Primitive-long binary min-heap in natural order. */
    public static M3LongPriorityQueue longPriorityQueue() { return new M3LongPriorityQueue(); }
}
