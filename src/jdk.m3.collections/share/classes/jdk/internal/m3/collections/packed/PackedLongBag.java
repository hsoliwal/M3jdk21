// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections.packed;

import java.util.Objects;
import jdk.m3.collections.LongLongConsumer;

/**
 * Primitive multiset with long occurrence counts and a checked long total.
 *
 * <p>Every long key is valid. Zero counts are absent; no wrapper, node, or repeated
 * element is retained per occurrence. Mutation and traversal are not thread-safe;
 * a traversal callback must not mutate either input. Table scans include unused
 * capacity but never expand an element into its individual occurrences.
 *
 * <p>Legacy {@code add} and {@code remove} return the NEW count and require a
 * positive amount. They are not Guava's old-count-returning operations. The added
 * {@code setCount} returns the OLD count. Algebra methods return independent
 * snapshots, unlike Guava's live algebra views; neither input is modified, even
 * when an output count/total overflows. This type is not a JDK Collection adapter.
 */
//FLLDCIM: RELOCATE - donor blob 329b632b8a78 at commit d5ad5090d87e (repository, path, URL and digests: make/m3/collections/recipe/four-library-bags-20261106/PROVENANCE.tsv); recipe make/m3/collections/recipe/four-library-bags-20261106/relocate.py v1
public final class PackedLongBag implements PackedLongBagContract {
    private final PackedLongLongHashMap counts;
    private long totalSize;

    /** Creates an empty bag with a small logical reservation. */
    public PackedLongBag() { this(8); }

    /**
     * Creates an empty bag, reserving for distinct keys rather than occurrences.
     * @param expectedDistinctValues nonnegative expected distinct-key count
     * @throws IllegalArgumentException if the reservation is invalid or too large
     */
    public PackedLongBag(int expectedDistinctValues) {
        counts = new PackedLongLongHashMap(expectedDistinctValues);
    }

    /**
     * Adds one occurrence.
     * @param value key
     * @return the new count
     * @throws ArithmeticException if the total would overflow
     */
    @Override public long add(long value) { return add(value, 1L); }

    /**
     * Adds a positive number of occurrences, checking arithmetic before mutation.
     * @param value key
     * @param occurrences positive amount
     * @return the new count, not the old count
     * @throws IllegalArgumentException if occurrences is not positive
     * @throws ArithmeticException if the total would overflow
     */
    @Override public long add(long value, long occurrences) {
        if (occurrences <= 0L) throw new IllegalArgumentException("occurrences must be positive");
        long nextTotal = Math.addExact(totalSize, occurrences);
        long next = counts.addTo(value, occurrences, 0L);
        totalSize = nextTotal;
        return next;
    }

    /**
     * @param value key
     * @return its count, or zero when absent
     */
    @Override public long count(long value) { return counts.getOrDefault(value, 0L); }

    /**
     * Removes up to the requested amount.
     * @param value key
     * @param occurrences positive amount
     * @return the new count, not the removed or old count
     * @throws IllegalArgumentException if occurrences is not positive
     */
    @Override public long remove(long value, long occurrences) {
        if (occurrences <= 0L) throw new IllegalArgumentException("occurrences must be positive");
        long current = count(value);
        if (current == 0L) return 0L;
        long removed = Math.min(current, occurrences);
        long next = current - removed;
        if (next == 0L) counts.remove(value);
        else counts.put(value, next);
        totalSize -= removed;
        return next;
    }

    /**
     * @param value key
     * @return the number of occurrences removed
     */
    @Override public long removeAll(long value) {
        long current = count(value);
        if (current != 0L) { counts.remove(value); totalSize -= current; }
        return current;
    }

    /**
     * Assigns the exact count; zero removes the key. Arithmetic admission precedes
     * mutation. A no-op does not allocate or rebuild the backing map.
     * @param value key
     * @param newCount nonnegative desired count
     * @return the previous count
     * @throws IllegalArgumentException if newCount is negative
     * @throws ArithmeticException if the resulting total would overflow
     */
    public long setCount(long value, long newCount) {
        if (newCount < 0L) throw new IllegalArgumentException("newCount must be nonnegative");
        long previous = count(value);
        if (previous == newCount) return previous;
        long nextTotal = Math.addExact(totalSize - previous, newCount);
        if (newCount == 0L) counts.remove(value);
        else counts.put(value, newCount);
        totalSize = nextTotal;
        return previous;
    }

    /**
     * Assigns a count only when the expected count matches. This is NOT an atomic
     * concurrent compare-and-set. A matching no-op returns true.
     * @param value key
     * @param expectedCount nonnegative expected count
     * @param newCount nonnegative desired count
     * @return whether the expected count matched
     * @throws IllegalArgumentException if either count is negative, even on mismatch
     * @throws ArithmeticException if an admitted replacement total would overflow
     */
    public boolean setCount(long value, long expectedCount, long newCount) {
        if (expectedCount < 0L || newCount < 0L) throw new IllegalArgumentException("negative count");
        if (count(value) != expectedCount) return false;
        setCount(value, newCount);
        return true;
    }

    /**
     * @param value key
     * @return whether its count is positive
     */
    @Override public boolean contains(long value) { return counts.containsKey(value); }
    /** @return the distinct-key count */
    @Override public int distinctSize() { return counts.size(); }
    /** @return the exact total number of occurrences */
    @Override public long size() { return totalSize; }
    /** @return whether the bag has no occurrences */
    @Override public boolean isEmpty() { return totalSize == 0L; }

    /**
     * Visits each distinct key/count once; order is unspecified. No occurrence expansion.
     * @param consumer callback which must not mutate this bag
     * @throws NullPointerException if consumer is null
     */
    @Override public void forEachCount(LongLongConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        counts.forEach(consumer);
    }

    /** Removes all entries, retaining the current backing capacity. */
    @Override public void clear() { counts.clear(); totalSize = 0L; }

    /** Compacts the backing map; an empty bag releases its per-instance arrays. */
    public void trimToSize() { counts.trimToSize(); }

    /**
     * Copies the distinct entries without expanding occurrences.
     * @return an independent bag with identical counts
     */
    public PackedLongBag copy() {
        PackedLongBag result = new PackedLongBag(distinctSize());
        forEachCount(result::add);
        return result;
    }

    /**
     * Tests multiplicity containment, not just key membership.
     * @param other required occurrences; must remain unchanged during this call
     * @return whether every other count is at most this count
     * @throws NullPointerException if other is null
     */
    public boolean containsOccurrences(PackedLongBag other) {
        Objects.requireNonNull(other, "other");
        if (other.totalSize > totalSize) return false;
        boolean[] result = {true};
        other.forEachCount((key, amount) -> { if (count(key) < amount) result[0] = false; });
        return result[0];
    }

    /**
     * Returns the maximum count for each key, in an independent bag.
     * @param other second bag, unchanged during traversal
     * @return maximum-count snapshot
     * @throws NullPointerException if other is null
     * @throws ArithmeticException if the result total exceeds Long.MAX_VALUE
     */
    public PackedLongBag union(PackedLongBag other) {
        Objects.requireNonNull(other, "other");
        PackedLongBag result = copy();
        other.forEachCount((key, amount) -> {
            if (amount > result.count(key)) result.setCount(key, amount);
        });
        return result;
    }

    /**
     * Returns the minimum count for shared keys, in an independent bag.
     * @param other second bag, unchanged during traversal
     * @return minimum-count snapshot
     * @throws NullPointerException if other is null
     */
    public PackedLongBag intersection(PackedLongBag other) {
        Objects.requireNonNull(other, "other");
        PackedLongBag result = new PackedLongBag(Math.min(distinctSize(), other.distinctSize()));
        forEachCount((key, amount) -> {
            long retained = Math.min(amount, other.count(key));
            if (retained > 0L) result.add(key, retained);
        });
        return result;
    }

    /**
     * Adds corresponding counts. Unlike union, multiplicities accumulate.
     * @param other second bag, unchanged during traversal
     * @return independent sum snapshot
     * @throws NullPointerException if other is null
     * @throws ArithmeticException if a resulting total cannot be represented
     */
    public PackedLongBag sum(PackedLongBag other) {
        Objects.requireNonNull(other, "other");
        Math.addExact(totalSize, other.totalSize);
        PackedLongBag result = copy();
        other.forEachCount(result::add);
        return result;
    }

    /**
     * Subtracts corresponding counts, flooring at zero.
     * @param other second bag, unchanged during traversal
     * @return independent difference snapshot
     * @throws NullPointerException if other is null
     */
    public PackedLongBag difference(PackedLongBag other) {
        Objects.requireNonNull(other, "other");
        PackedLongBag result = new PackedLongBag(distinctSize());
        forEachCount((key, amount) -> {
            long retained = Math.max(0L, amount - other.count(key));
            if (retained > 0L) result.add(key, retained);
        });
        return result;
    }
}
