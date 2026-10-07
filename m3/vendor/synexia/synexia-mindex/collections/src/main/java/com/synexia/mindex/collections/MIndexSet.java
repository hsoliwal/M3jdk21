// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.collections;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * Unique MIndex IDs with either direct dense bits or sparse open addressing.
 *
 * <p>Dense mode automatically promotes to sparse mode if an admitted ID lies
 * outside the declared bound; correctness never depends on the hint.
 */
public final class MIndexSet<E> {
    private final MIndexSpace<E> space;
    private int[] table;
    private long[] bits;
    private int denseUpperBound;
    private int size;

    public MIndexSet(MIndexSpace<E> space) {
        this(space, 16, 0);
    }

    public MIndexSet(MIndexSpace<E> space, int expectedSize) {
        this(space, expectedSize, 0);
    }

    MIndexSet(MIndexSpace<E> space, int expectedSize, int denseUpperBound) {
        this.space = Objects.requireNonNull(space, "space");
        if (expectedSize < 0 || denseUpperBound < 0) {
            throw new IllegalArgumentException("negative size/bound");
        }
        if (denseUpperBound > 0) {
            this.denseUpperBound = denseUpperBound;
            bits = new long[IdSupport.bitWordCount(denseUpperBound)];
            table = new int[0];
        } else {
            table = new int[IdSupport.tableCapacity(expectedSize)];
        }
    }

    public static <E> MIndexSet<E> dense(MIndexSpace<E> space, int upperIdExclusive) {
        if (upperIdExclusive <= 0) {
            throw new IllegalArgumentException("upperIdExclusive must be positive");
        }
        return new MIndexSet<>(space, 0, upperIdExclusive);
    }

    public MIndexSpace<E> space() {
        return space;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public boolean isDense() {
        return bits != null;
    }

    public boolean add(E value) {
        return addId(space.id(value));
    }

    public boolean addId(int id) {
        space.requireId(id);
        if (bits != null) {
            if (id >= denseUpperBound) {
                promoteToSparse();
            } else {
                int word = id >>> 6;
                long mask = 1L << id;
                if ((bits[word] & mask) != 0L) {
                    return false;
                }
                bits[word] |= mask;
                size++;
                return true;
            }
        }
        ensureSparseCapacity(size + 1);
        int mask = table.length - 1;
        int slot = IdSupport.mix(id) & mask;
        while (table[slot] != 0) {
            if (table[slot] - 1 == id) {
                return false;
            }
            slot = (slot + 1) & mask;
        }
        table[slot] = id + 1;
        size++;
        return true;
    }

    public boolean contains(E value) {
        int id = space.findId(value);
        return id >= 0 && containsId(id);
    }

    public boolean containsId(int id) {
        if (id < 0) {
            return false;
        }
        if (bits != null) {
            return id < denseUpperBound && (bits[id >>> 6] & (1L << id)) != 0L;
        }
        int mask = table.length - 1;
        int slot = IdSupport.mix(id) & mask;
        while (table[slot] != 0) {
            if (table[slot] - 1 == id) {
                return true;
            }
            slot = (slot + 1) & mask;
        }
        return false;
    }

    public boolean remove(E value) {
        int id = space.findId(value);
        return id >= 0 && removeId(id);
    }

    public boolean removeId(int id) {
        if (id < 0) {
            return false;
        }
        if (bits != null) {
            if (id >= denseUpperBound) {
                return false;
            }
            int word = id >>> 6;
            long mask = 1L << id;
            if ((bits[word] & mask) == 0L) {
                return false;
            }
            bits[word] &= ~mask;
            size--;
            return true;
        }
        int mask = table.length - 1;
        int slot = IdSupport.mix(id) & mask;
        while (table[slot] != 0) {
            if (table[slot] - 1 == id) {
                deleteSparseSlot(slot);
                return true;
            }
            slot = (slot + 1) & mask;
        }
        return false;
    }

    /** Return the number of newly admitted members. All input IDs are validated first. */
    public int addAllIds(int[] source, int offset, int length) {
        IdSupport.requireIds(space, source, offset, length);
        int previous = size;
        for (int end = offset + length; offset < end; offset++) addId(source[offset]);
        return size - previous;
    }

    /** Queries do not admit IDs into the domain; unknown IDs are absent. */
    public boolean containsAllIds(int[] source, int offset, int length) {
        Objects.requireNonNull(source, "source");
        Objects.checkFromIndexSize(offset, length, source.length);
        for (int end = offset + length; offset < end; offset++) {
            if (!containsId(source[offset])) return false;
        }
        return true;
    }

    /** Return the number removed; duplicates and absent IDs do not count twice. */
    public int removeAllIds(int[] source, int offset, int length) {
        Objects.requireNonNull(source, "source");
        Objects.checkFromIndexSize(offset, length, source.length);
        int previous = size;
        for (int end = offset + length; offset < end; offset++) removeId(source[offset]);
        return previous - size;
    }

    public MIndexSet<E> union(MIndexSet<E> other) {
        requireSameSpace(other);
        MIndexSet<E> result = new MIndexSet<>(space, size + other.size);
        forEachId(result::addId);
        other.forEachId(result::addId);
        return result;
    }

    public MIndexSet<E> intersection(MIndexSet<E> other) {
        requireSameSpace(other);
        MIndexSet<E> smaller = size <= other.size ? this : other;
        MIndexSet<E> larger = smaller == this ? other : this;
        MIndexSet<E> result = new MIndexSet<>(space, smaller.size);
        smaller.forEachId(id -> {
            if (larger.containsId(id)) {
                result.addId(id);
            }
        });
        return result;
    }

    public MIndexSet<E> difference(MIndexSet<E> other) {
        requireSameSpace(other);
        MIndexSet<E> result = new MIndexSet<>(space, size);
        forEachId(id -> {
            if (!other.containsId(id)) {
                result.addId(id);
            }
        });
        return result;
    }

    public void forEach(Consumer<? super E> action) {
        Objects.requireNonNull(action, "action");
        forEachId(id -> action.accept(space.value(id)));
    }

    public void forEachId(IntConsumer action) {
        Objects.requireNonNull(action, "action");
        int expected = size;
        if (bits != null) {
            for (int wordIndex = 0; wordIndex < bits.length; wordIndex++) {
                long word = bits[wordIndex];
                while (word != 0L) {
                    int bit = Long.numberOfTrailingZeros(word);
                    action.accept((wordIndex << 6) + bit);
                    if (size != expected) {
                        throw new IllegalStateException("collection modified during traversal");
                    }
                    word &= word - 1L;
                }
            }
            return;
        }
        for (int encoded : table) {
            if (encoded != 0) {
                action.accept(encoded - 1);
                if (size != expected) {
                    throw new IllegalStateException("collection modified during traversal");
                }
            }
        }
    }

    public int[] snapshotIdsSorted() {
        int[] values = new int[size];
        int[] cursor = {0};
        forEachId(id -> values[cursor[0]++] = id);
        Arrays.sort(values);
        return values;
    }

    public int canonicalId(MIndexCompositeIndex index) {
        return Objects.requireNonNull(index, "index")
                .intern(MIndexCompositeIndex.KIND_SET, space, null, snapshotIdsSorted());
    }

    public MIndexFrozenSet<E> freeze(MIndexCompositeIndex index) {
        return MIndexFrozenSet.copyOf(this, Objects.requireNonNull(index, "index"));
    }

    public void clear() {
        if (bits != null) {
            Arrays.fill(bits, 0L);
        } else {
            Arrays.fill(table, 0);
        }
        size = 0;
    }

    /** Shrink sparse probe storage after a long-lived workload has contracted. */
    public void compact() {
        if (bits == null) {
            int target = IdSupport.tableCapacity(size);
            if (target < table.length) {
                rehash(target);
            }
        }
    }

    int sparseCapacity() {
        return bits == null ? table.length : 0;
    }

    private void requireSameSpace(MIndexSet<E> other) {
        if (other == null || other.space != space) {
            throw new IllegalArgumentException("sets must share one MIndexSpace");
        }
    }

    private void ensureSparseCapacity(int needed) {
        if (table.length == 0) {
            table = new int[IdSupport.tableCapacity(needed)];
            return;
        }
        if ((long) needed * 3L >= (long) table.length * 2L) {
            rehash(table.length << 1);
        }
    }

    private void promoteToSparse() {
        int expected = size;
        long[] previous = bits;
        table = new int[IdSupport.tableCapacity(Math.max(8, expected))];
        bits = null;
        denseUpperBound = 0;
        size = 0;
        for (int wordIndex = 0; wordIndex < previous.length; wordIndex++) {
            long word = previous[wordIndex];
            while (word != 0L) {
                int bit = Long.numberOfTrailingZeros(word);
                addId((wordIndex << 6) + bit);
                word &= word - 1L;
            }
        }
    }

    private void deleteSparseSlot(int slot) {
        shiftSparseKeys(slot);
        size--;
    }

    private void shiftSparseKeys(int position) {
        int mask = table.length - 1;
        int last;
        int encoded;
        while (true) {
            position = ((last = position) + 1) & mask;
            while ((encoded = table[position]) != 0) {
                int home = IdSupport.mix(encoded - 1) & mask;
                if (last <= position
                        ? last >= home || home > position
                        : last >= home && home > position) {
                    break;
                }
                position = (position + 1) & mask;
            }
            if (encoded == 0) {
                table[last] = 0;
                return;
            }
            table[last] = encoded;
        }
    }

    private void rehash(int requested) {
        int[] previous = table;
        int capacity = 8;
        while (capacity < requested) {
            capacity <<= 1;
        }
        table = new int[capacity];
        int previousSize = size;
        size = 0;
        for (int encoded : previous) {
            if (encoded != 0) {
                addId(encoded - 1);
            }
        }
        if (size != previousSize) {
            throw new AssertionError("rehash size mismatch");
        }
    }
}
