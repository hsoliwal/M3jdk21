/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.m3.collections.concurrent;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Thread-safe multiset: an element and its count live in one slot of the element lane and the count
 * lane.
 *
 * <p>The slots are split into 32 stripe regions; the top bits of an element's hash pick its stripe,
 * and one lock per
 * stripe guards that region, so threads that touch different stripes do not wait for each other.
 * Inside a region an
 * open-addressed int lane maps a hash to a slot. A count that drops to zero marks its slot {@code
 * DELETED}; the slot is
 * reused by the next new element whose probe run passes it, and a full region is first packed in
 * place. Only when a
 * region is full of live elements do all regions double, under all stripe locks. No node, entry or
 * counter object is
 * stored. Every operation on one element is atomic. {@link #size()}, {@link #isEmpty()}, {@link
 * #clear()} and the
 * element set read or write one stripe after the other, so they are weakly consistent while other
 * threads write.
 * Null elements are refused.</p>
 *
 * <p>Provider comparison: Guava {@code ConcurrentHashMultiset} (Apache-2.0): the same results and
 * the same refusals
 * for the operations declared here. Not a {@code java.util} replacement.</p>
 */
public final class ConcurrentMultisetReference<E> {
    /** State-mask bit {@code OCCUPIED} (per-slot state word, bit 0). */
    static final long STATE_OCCUPIED = 1L << 0;
    /** State-mask bit {@code DELETED} (per-slot state word, bit 1). */
    static final long STATE_DELETED = 1L << 1;
    private static final int STRIPES = 32;
    private static final int STRIPE_SHIFT = 27;
    private static final int MIN_REGION = 16;
    /** Per-stripe tallies sit 64 bytes apart, so threads in different stripes do not write the same
     * cache line. */
    private static final int INT_PAD = 4;
    private static final int LONG_PAD = 3;
    private static final int MAX_REGION = 1 << 24;
    private static final int GOLDEN_RATIO_32 = 0x9E3779B9;
    private static final int FOLD_SHIFT = 16;
    private static final int FULL = -1;
    private static final int ADD = 0;
    private static final int REMOVE = 1;
    private static final int SET = 2;
    private static final int COMPARE_AND_SET = 3;
    private static final int SPINS = 64;

    /** Storage lane {@code elementLane}: the element of each slot; stripe {@code s} owns {@code
     * region} slots. */
    private Object[] elementLane = new Object[STRIPES * MIN_REGION];
    /** Storage lane {@code countLane}: the count of each slot. */
    private int[] countLane = new int[STRIPES * MIN_REGION];
    /** Storage lane {@code hashIndexLane}: per stripe, an open-addressed table of slot + 1; 0 is
     * empty; half full at most. */
    private int[] hashIndexLane = new int[STRIPES * MIN_REGION * 2];
    /** Per-slot state words (OCCUPIED,DELETED); zero for a slot that was never used. */
    private long[] stateMaskWords = new long[STRIPES * MIN_REGION];
    /** Slots per stripe region. */
    private int region = MIN_REGION;
    /** Per stripe: slots handed out so far, occupied or deleted. */
    private final int[] used = new int[STRIPES << INT_PAD];
    /** Per stripe: live slot count. */
    private final int[] live = new int[STRIPES << INT_PAD];
    /** Per stripe: the sum of its counts. */
    private final long[] total = new long[STRIPES << LONG_PAD];
    private final ReentrantLock[] locks = new ReentrantLock[STRIPES];
    private final Set<E> elements = new ElementView();

    /** Creates an empty multiset. */
    public ConcurrentMultisetReference() {
        Arrays.setAll(locks, stripe -> new ReentrantLock());
    }

    /** Reports the declared state-mask bits and lanes for ledgers; never allocates per element. */
    public static String scaffoldDescriptor() {
        return "ConcurrentMultisetReference|CONCURRENT_MULTISET|OBJECT|OCCUPIED,DELETED|Object[] " +
                "elementLane;int[] countLane;int[] hashIndexLane";
    }

    /** The count of an element; 0 when it is absent or null. */
    public int count(Object element) {
        if (element == null) {
            return 0;
        }
        int hash = spread(element);
        ReentrantLock lock = locks[hash >>> STRIPE_SHIFT];
        acquire(lock);
        try {
            int slot = find(element, hash);
            return slot < 0 ? 0 : countLane[slot];
        } finally {
            lock.unlock();
        }
    }

    /**
     * Adds occurrences of an element and returns its count before the call. Zero occurrences only
     * read the count. A
     * negative number, or a sum above {@code Integer.MAX_VALUE}, is refused with {@code
     * IllegalArgumentException}.
     */
    public int add(E element, int occurrences) {
        Objects.requireNonNull(element);
        if (occurrences == 0) {
            return count(element);
        }
        requirePositive(occurrences);
        return change(element, ADD, occurrences, 0);
    }

    /**
     * Removes occurrences of an element, all of them when it has fewer, and returns its count
     * before the call. Zero
     * occurrences only read the count; a negative number is refused with {@code
     * IllegalArgumentException}.
     */
    public int remove(Object element, int occurrences) {
        if (occurrences == 0) {
            return count(element);
        }
        requirePositive(occurrences);
        return element == null ? 0 : change(element, REMOVE, occurrences, 0);
    }

    /** Sets the count of an element, 0 removes it, and returns its count before the call. */
    public int setCount(E element, int count) {
        Objects.requireNonNull(element);
        requireNonNegative(count, "count");
        return change(element, SET, count, 0);
    }

    /** Sets the count of an element only if it is the expected one; tells whether it was. */
    public boolean setCount(E element, int expectedOldCount, int newCount) {
        Objects.requireNonNull(element);
        requireNonNegative(expectedOldCount, "oldCount");
        requireNonNegative(newCount, "newCount");
        return change(element, COMPARE_AND_SET, expectedOldCount, newCount) == expectedOldCount;
    }

    /**
     * The distinct elements as a live view: it follows later changes, removing an element through
     * it removes every
     * occurrence, and its iterator walks a copy taken stripe by stripe. Elements cannot be added
     * through it.
     */
    public Set<E> elementSet() {
        return elements;
    }

    /** The sum of all counts, at most {@code Integer.MAX_VALUE}. */
    public int size() {
        long sum = 0;
        for (int stripe = 0; stripe < STRIPES; stripe++) {
            sum += tally(stripe, total);
        }
        return (int) Math.min(sum, Integer.MAX_VALUE);
    }

    /** Tells whether no element is present. */
    public boolean isEmpty() {
        return distinct() == 0;
    }

    /** Removes every element, one stripe after the other. */
    public void clear() {
        for (int stripe = 0; stripe < STRIPES; stripe++) {
            acquire(locks[stripe]);
            try {
                wipe(stripe);
                live[stripe << INT_PAD] = 0;
                total[stripe << LONG_PAD] = 0;
            } finally {
                locks[stripe].unlock();
            }
        }
    }

    private int change(Object element, int kind, int first, int second) {
        int hash = spread(element);
        int old = apply(element, hash, kind, first, second);
        while (old == FULL) {
            grow(hash >>> STRIPE_SHIFT);
            old = apply(element, hash, kind, first, second);
        }
        return old;
    }

    /** One atomic step under the stripe lock: the count before it, or FULL when the region has no
     * slot for a new element. */
    private int apply(Object element, int hash, int kind, int first, int second) {
        ReentrantLock lock = locks[hash >>> STRIPE_SHIFT];
        acquire(lock);
        try {
            int slot = find(element, hash);
            int old = slot < 0 ? 0 : countLane[slot];
            int next = next(kind, old, first, second);
            return next == old || store(element, hash, slot, old, next) ? old : FULL;
        } finally {
            lock.unlock();
        }
    }

    private static int next(int kind, int old, int first, int second) {
        return switch (kind) {
            case ADD -> sum(old, first);
            case REMOVE -> Math.max(0, old - first);
            case SET -> first;
            default -> old == first ? second : old;
        };
    }

    private static int sum(int old, int occurrences) {
        if (occurrences > Integer.MAX_VALUE - old) {
            throw new IllegalArgumentException("Overflow adding " + occurrences +
                    " occurrences to a count of " + old);
        }
        return old + occurrences;
    }

    private boolean store(Object element, int hash, int slot, int old, int next) {
        int stripe = hash >>> STRIPE_SHIFT;
        int target = slot >= 0 ? slot : claimOrPack(hash);
        if (target < 0) {
            return false;
        }
        if (slot < 0) {
            elementLane[target] = element;
            stateMaskWords[target] = STATE_OCCUPIED;
            live[stripe << INT_PAD]++;
        }
        countLane[target] = next;
        total[stripe << LONG_PAD] += next - old;
        if (next == 0) {
            elementLane[target] = null;
            stateMaskWords[target] = STATE_DELETED;
            live[stripe << INT_PAD]--;
        }
        return true;
    }

    private int claimOrPack(int hash) {
        int stripe = hash >>> STRIPE_SHIFT;
        int slot = claim(hash);
        if (slot < 0 && live[stripe << INT_PAD] < used[stripe << INT_PAD]) {
            reseat(stripe, elementLane, countLane, stripe * region, used[stripe << INT_PAD]);
            slot = claim(hash);
        }
        return slot;
    }

    /** The slot of an element, or -1. */
    private int find(Object element, int hash) {
        int mask = (region << 1) - 1;
        int base = (hash >>> STRIPE_SHIFT) * (mask + 1);
        int position = hash & mask;
        for (int entry = hashIndexLane[base + position]; entry != 0; entry = hashIndexLane[base +
                position]) {
            if (stateMaskWords[entry - 1] == STATE_OCCUPIED && element.equals(elementLane[entry - 1]
                    )) {
                return entry - 1;
            }
            position = (position + 1) & mask;
        }
        return -1;
    }

    /** A slot for an absent element: the first deleted slot on its probe run, else a fresh one; -1
     * when none is left. */
    private int claim(int hash) {
        int stripe = hash >>> STRIPE_SHIFT;
        int mask = (region << 1) - 1;
        int base = stripe * (mask + 1);
        int position = hash & mask;
        int entry = hashIndexLane[base + position];
        while (entry != 0 && stateMaskWords[entry - 1] != STATE_DELETED) {
            position = (position + 1) & mask;
            entry = hashIndexLane[base + position];
        }
        if (entry != 0 || used[stripe << INT_PAD] == region) {
            return entry - 1;
        }
        int slot = stripe * region + used[stripe << INT_PAD]++;
        hashIndexLane[base + position] = slot + 1;
        return slot;
    }

    /** Empties a stripe region, then seats the live elements of the given slots again. */
    private void reseat(int stripe, Object[] fromElements, int[] fromCounts, int from, int count) {
        Object[] kept = Arrays.copyOfRange(fromElements, from, from + count);
        int[] counts = Arrays.copyOfRange(fromCounts, from, from + count);
        wipe(stripe);
        for (int i = 0; i < count; i++) {
            if (kept[i] != null) {
                int slot = claim(spread(kept[i]));
                elementLane[slot] = kept[i];
                stateMaskWords[slot] = STATE_OCCUPIED;
                countLane[slot] = counts[i];
            }
        }
    }

    private void wipe(int stripe) {
        int from = stripe * region;
        Arrays.fill(elementLane, from, from + region, null);
        Arrays.fill(countLane, from, from + region, 0);
        Arrays.fill(stateMaskWords, from, from + region, 0L);
        Arrays.fill(hashIndexLane, from << 1, (from + region) << 1, 0);
        used[stripe << INT_PAD] = 0;
    }

    /** Doubles every region under all stripe locks, unless another thread already made room in the
     * stripe. */
    private void grow(int stripe) {
        for (ReentrantLock lock : locks) {
            lock.lock();
        }
        try {
            if (live[stripe << INT_PAD] == region) {
                resize();
            }
        } finally {
            for (ReentrantLock lock : locks) {
                lock.unlock();
            }
        }
    }

    private void resize() {
        if (region == MAX_REGION) {
            throw new IllegalStateException("too many distinct elements in one stripe");
        }
        Object[] oldElements = elementLane;
        int[] oldCounts = countLane;
        int oldRegion = region;
        region = oldRegion << 1;
        elementLane = new Object[STRIPES * region];
        countLane = new int[STRIPES * region];
        stateMaskWords = new long[STRIPES * region];
        hashIndexLane = new int[STRIPES * region * 2];
        for (int stripe = 0; stripe < STRIPES; stripe++) {
            reseat(stripe, oldElements, oldCounts, stripe * oldRegion, used[stripe << INT_PAD]);
        }
    }

    private long tally(int stripe, long[] lane) {
        acquire(locks[stripe]);
        try {
            return lane == total ? total[stripe << LONG_PAD] : live[stripe << INT_PAD];
        } finally {
            locks[stripe].unlock();
        }
    }

    private int distinct() {
        long sum = 0;
        for (int stripe = 0; stripe < STRIPES; stripe++) {
            sum += tally(stripe, null);
        }
        return (int) Math.min(sum, Integer.MAX_VALUE);
    }

    private void collect(int stripe, List<Object> into) {
        acquire(locks[stripe]);
        try {
            int from = stripe * region;
            for (int slot = from; slot < from + used[stripe << INT_PAD]; slot++) {
                if (elementLane[slot] != null) {
                    into.add(elementLane[slot]);
                }
            }
        } finally {
            locks[stripe].unlock();
        }
    }

    /** Takes a stripe lock: a short spin first, because a holder keeps it for a few array reads and
     * writes only. */
    private static void acquire(ReentrantLock lock) {
        for (int spin = 0; spin < SPINS; spin++) {
            if (lock.tryLock()) {
                return;
            }
            Thread.onSpinWait();
        }
        lock.lock();
    }

    private static int spread(Object element) {
        int hash = element.hashCode() * GOLDEN_RATIO_32;
        return hash ^ (hash >>> FOLD_SHIFT);
    }

    private static void requirePositive(int occurrences) {
        if (occurrences <= 0) {
            throw new IllegalArgumentException("occurrences must be positive but was: " +
                    occurrences);
        }
    }

    private static void requireNonNegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " cannot be negative but was: " + value);
        }
    }

    /** The live view behind {@link #elementSet()}. */
    private final class ElementView extends AbstractSet<E> {
        @Override
        public int size() {
            return distinct();
        }

        @Override
        public boolean contains(Object element) {
            return count(element) > 0;
        }

        @Override
        public boolean remove(Object element) {
            return element != null && change(element, SET, 0, 0) > 0;
        }

        @Override
        public void clear() {
            ConcurrentMultisetReference.this.clear();
        }

        @Override
        public Iterator<E> iterator() {
            List<Object> copy = new ArrayList<>();
            for (int stripe = 0; stripe < STRIPES; stripe++) {
                collect(stripe, copy);
            }
            return new ElementIterator(copy.iterator());
        }
    }

    /** Walks a copy of the elements; {@code remove} removes every occurrence of the last one
     * returned. */
    private final class ElementIterator implements Iterator<E> {
        private final Iterator<Object> copy;
        private Object last;

        ElementIterator(Iterator<Object> copy) {
            this.copy = copy;
        }

        @Override
        public boolean hasNext() {
            return copy.hasNext();
        }

        @Override
        @SuppressWarnings("unchecked")
        public E next() {
            if (!copy.hasNext()) {
                throw new NoSuchElementException();
            }
            last = copy.next();
            return (E) last;
        }

        @Override
        public void remove() {
            if (last == null) {
                throw new IllegalStateException();
            }
            change(last, SET, 0, 0);
            last = null;
        }
    }
}
