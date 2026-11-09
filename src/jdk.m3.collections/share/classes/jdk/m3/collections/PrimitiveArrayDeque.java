// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
// Materialized from hsoliwal/com.synexia@8513abf8eb0d911873c9127a138007a42dd4ab60
// Source: m3-primitives/src/main/java/jdk/internal/m3/primitives/PrimitiveArrayDeque.java
// Source-Git-blob: 7b2bfc81d5fe8fe69b6c5258d66f521d89ae1dec
package jdk.m3.collections;

import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Exact-width primitive deque retaining one circular primitive array, without per-entry wrappers.
 * Queue and stack operations share the same owner. This is not a {@link java.util.Deque};
 * carrier conversion, equality and BOOLEAN byte-array storage follow {@link PrimitiveKind}.
 *
 * <p>End operations are O(1), amortized for insertion because growth copies O(n) lanes.
 * Indexed reads are O(1); occurrence search/removal and bulk traversal are O(n).
 * Bulk append uses at most two typed copies after validation, except BOOLEAN normalization.
 * Clearing uses at most two typed fills, not one dispatch per element.
 *
 * <p>This mutable class is not thread-safe. Traversal callbacks must not mutate it.
 * Legacy peekFirstBits/peekLastBits throw on empty; getFirstBits/getLastBits are the
 * explicitly named throwing aliases. No carrier is reserved as an empty sentinel.
 * Snapshots are independent arrays; payloadBytes excludes headers and temporary storage.
 */
public final class PrimitiveArrayDeque {
    private final PrimitiveKind kind;
    private Object elements;
    private int head;
    private int size;

    /**
     * Creates an empty deque with capacity eight.
     * @param kind retained primitive kind
     * @throws NullPointerException if kind is null
     */
    public PrimitiveArrayDeque(PrimitiveKind kind) {
        this(kind, 8);
    }

    /**
     * Creates an empty deque with the requested reservation.
     * @param kind retained primitive kind
     * @param initialCapacity nonnegative capacity; zero reserves one lane
     * @throws NullPointerException if kind is null
     * @throws IllegalArgumentException if initialCapacity is negative
     * @throws OutOfMemoryError if allocation fails
     */
    public PrimitiveArrayDeque(PrimitiveKind kind, int initialCapacity) {
        this.kind = Objects.requireNonNull(kind, "kind");
        if (initialCapacity < 0) throw new IllegalArgumentException("initialCapacity < 0");
        elements = kind.newArray(Math.max(1, initialCapacity));
    }

    /**
     * Returns the retained primitive kind.
     * @return the retained primitive kind
     */
    public PrimitiveKind kind() { return kind; }
    /**
     * Returns number of retained values.
     * @return number of retained values
     */
    public int size() { return size; }
    /**
     * Returns whether there are no retained values.
     * @return whether there are no retained values
     */
    public boolean isEmpty() { return size == 0; }
    /**
     * Returns number of reserved primitive lanes.
     * @return number of reserved primitive lanes
     */
    public int capacity() { return java.lang.reflect.Array.getLength(elements); }
    /**
     * Returns reserved lane bytes, not total heap/native footprint.
     * @return primitive payload bytes excluding headers, alignment and temporary copies
     */
    public long payloadBytes() { return kind.payloadBytes(capacity()); }

    /**
     * Inserts at the head, growing if necessary.
     * @param bits value carrier
     * @throws OutOfMemoryError if size overflows or allocation fails
     */
    public void addFirstBits(long bits) {
        ensure();
        head = dec(head, capacity());
        kind.writeBits(elements, head, bits);
        size++;
    }

    /**
     * Inserts at the tail, growing if necessary.
     * @param bits value carrier
     * @throws OutOfMemoryError if size overflows or allocation fails
     */
    public void addLastBits(long bits) {
        ensure();
        int tail = physicalIndex(size);
        kind.writeBits(elements, tail, bits);
        size++;
    }

    /**
     * Examines the head. Unlike JDK peekFirst, this legacy method throws when empty.
     * @return first carrier
     * @throws NoSuchElementException if empty
     * @see #getFirstBits()
     */
    public long peekFirstBits() {
        if (size == 0) throw new NoSuchElementException();
        return kind.readBits(elements, head);
    }

    /**
     * Examines the tail. Unlike JDK peekLast, this legacy method throws when empty.
     * @return last carrier
     * @throws NoSuchElementException if empty
     * @see #getLastBits()
     */
    public long peekLastBits() {
        if (size == 0) throw new NoSuchElementException();
        return kind.readBits(elements, physicalIndex(size - 1));
    }

    /**
     * Removes and clears the head lane.
     * @return removed first carrier
     * @throws NoSuchElementException if empty
     */
    public long removeFirstBits() {
        if (size == 0) throw new NoSuchElementException();
        int index = head;
        long value = kind.readBits(elements, index);
        kind.clear(elements, index, index + 1);
        head = inc(head, capacity());
        size--;
        if (size == 0) head = 0;
        return value;
    }

    /**
     * Removes and clears the tail lane.
     * @return removed last carrier
     * @throws NoSuchElementException if empty
     */
    public long removeLastBits() {
        if (size == 0) throw new NoSuchElementException();
        int index = physicalIndex(size - 1);
        long value = kind.readBits(elements, index);
        kind.clear(elements, index, index + 1);
        size--;
        if (size == 0) head = 0;
        return value;
    }

    /**
     * Reads by zero-based encounter-order index without removal.
     * @param index index in [0, size)
     * @return value carrier
     * @throws IndexOutOfBoundsException if index is invalid
     */
    public long getBits(int index) {
        if (index < 0 || index >= size) throw new IndexOutOfBoundsException(index);
        return kind.readBits(elements, physicalIndex(index));
    }

    /**
     * Removes and clears all retained lanes in O(n), preserving capacity.
     */
    public void clear() {
        kind.clearRing(elements, head, size, capacity());
        head = 0;
        size = 0;
    }

    /**
     * Returns an independent encounter-order snapshot with no unused capacity.
     * @return matching primitive array; BOOLEAN uses byte[]
     */
    public Object toPrimitiveArray() {
        Object result = kind.newArray(size);
        int first = Math.min(size, capacity() - head);
        if (first > 0) System.arraycopy(elements, head, result, 0, first);
        int second = size - first;
        if (second > 0) System.arraycopy(elements, 0, result, first, second);
        return result;
    }


    /**
     * Appends a validated source slice in encounter order. Self-aliases are snapshotted before
     * growth or copying; no source array is retained. BOOLEAN bytes are normalized to zero/one.
     * @param source matching primitive array, using byte[] for BOOLEAN
     * @param from first source index
     * @param length number of values
     * @return length
     * @throws NullPointerException if source is null
     * @throws IllegalArgumentException if the array kind is wrong, including empty ranges
     * @throws IndexOutOfBoundsException if the source range is invalid
     * @throws OutOfMemoryError if size overflows or storage cannot be allocated
     */
    public int addAllBits(Object source, int from, int length) {
        kind.checkRange(source, from, length);
        int required = PrimitiveKind.checkedSize(size, length);
        if (length == 0) return 0;
        if (source == elements) {
            Object snapshot = kind.newArray(length);
            kind.copyStorage(source, from, snapshot, 0, length);
            source = snapshot;
            from = 0;
        }
        ensure(required);
        int tail = physicalIndex(size);
        int first = Math.min(length, capacity() - tail);
        kind.copyStorage(source, from, elements, tail, first);
        if (length > first) kind.copyStorage(source, from + first, elements, 0, length - first);
        size = required;
        return length;
    }

    /**
     * Examines the first value without removal.
     * @return first carrier
     * @throws NoSuchElementException if empty
     */
    public long getFirstBits() { return peekFirstBits(); }
    /**
     * Examines the last value without removal.
     * @return last carrier
     * @throws NoSuchElementException if empty
     */
    public long getLastBits() { return peekLastBits(); }
    /**
     * Examines the queue head without removal.
     * @return first carrier
     * @throws NoSuchElementException if empty
     */
    public long elementBits() { return getFirstBits(); }
    /**
     * Pushes a value onto the head, following the JDK Deque stack direction.
     * @param bits value carrier
     * @throws OutOfMemoryError if growth fails
     */
    public void pushBits(long bits) { addFirstBits(bits); }
    /**
     * Pops the head, following the JDK Deque stack direction.
     * @return removed first carrier
     * @throws NoSuchElementException if empty
     */
    public long popBits() { return removeFirstBits(); }
    /**
     * Finds a value using kind equality, without boxing.
     * @param bits carrier to find
     * @return whether a matching value exists
     */
    public boolean containsBits(long bits) {
        for (int i = 0; i < size; i++) if (kind.equalBits(getBits(i), bits)) return true;
        return false;
    }
    /**
     * Removes the first matching value in encounter order, moving the shorter side.
     * @param bits carrier to find
     * @return whether a value was removed
     */
    public boolean removeFirstOccurrenceBits(long bits) {
        for (int i = 0; i < size; i++) {
            if (kind.equalBits(getBits(i), bits)) { removeAt(i); return true; }
        }
        return false;
    }
    /**
     * Removes the last matching value in encounter order, moving the shorter side.
     * @param bits carrier to find
     * @return whether a value was removed
     */
    public boolean removeLastOccurrenceBits(long bits) {
        for (int i = size - 1; i >= 0; i--) {
            if (kind.equalBits(getBits(i), bits)) { removeAt(i); return true; }
        }
        return false;
    }
    /**
     * Visits carriers from first to last without a snapshot. Exceptions stop traversal and
     * propagate; the action must not mutate this deque.
     * @param action consumer, including when the deque is empty
     * @throws NullPointerException if action is null
     */
    public void forEachBits(java.util.function.LongConsumer action) {
        Objects.requireNonNull(action, "action");
        for (int i = 0; i < size; i++) action.accept(getBits(i));
    }
    private void removeAt(int index) {
        int capacity = capacity();
        int suffix = size - index - 1;
        if (index < suffix) {
            PrimitiveKind.moveRing(elements, head, capacity, 0, 1, index);
            kind.clear(elements, head, head + 1);
            head = inc(head, capacity);
        } else {
            PrimitiveKind.moveRing(elements, head, capacity, index + 1, index, suffix);
            int tail = physicalIndex(size - 1);
            kind.clear(elements, tail, tail + 1);
        }
        if (--size == 0) head = 0;
    }

    Object backingArray() { return elements; }

    private int physicalIndex(int logicalIndex) {
        return PrimitiveKind.ringIndex(head, logicalIndex, capacity());
    }

    private void ensure() { ensure(PrimitiveKind.checkedSize(size, 1)); }

    private void ensure(int required) {
        if (required <= capacity()) return;
        int oldCapacity = capacity();
        int newCapacity = PrimitiveKind.grownCapacity(oldCapacity, required);
        Object next = kind.newArray(newCapacity);
        int first = Math.min(size, oldCapacity - head);
        if (first > 0) System.arraycopy(elements, head, next, 0, first);
        int second = size - first;
        if (second > 0) System.arraycopy(elements, 0, next, first, second);
        elements = next;
        head = 0;
    }

    private static int inc(int index, int capacity) {
        index++;
        return index == capacity ? 0 : index;
    }

    private static int dec(int index, int capacity) {
        return index == 0 ? capacity - 1 : index - 1;
    }

    // API_UNION U5 (2026-11-09): Synexia primitives PrimitiveArrayDeque.Big, the segmented long-capacity variant,
    // ported unchanged; its storage is BigArray (the received PrimitiveBigArray).
    /**
     * Creates an empty long-capacity deque using lazily resident fixed primitive pages.
     * Existing int-capacity constructors and methods keep their original implementation.
     * Zero initial capacity reserves one logical lane without allocating a payload page.
     *
     * @throws NullPointerException when kind is null
     * @throws IllegalArgumentException when capacity is negative or segmentShift is outside 10..26
     */
    public static Big big(PrimitiveKind kind, long initialCapacity, int segmentShift) {
        return new Big(kind, initialCapacity, segmentShift);
    }

    /**
     * Long-position deque over the existing {@link BigArray} sparse owner.
     *
     * <p>Capacity is a logical reservation, not an assertion that all lanes reside in memory.
     * Absent pages read as zero. Growth copies the two live circular spans into a replacement
     * owner and publishes it only after both copies succeed. No entry object is retained.
     * The instance is mutable and is not thread-safe.
     *
     * <p>Encounter-order indices, counters and iterator cursors are long-valued. Primitive
     * carriers follow {@link PrimitiveKind}; empty peeks/removes throw without reserving a
     * carrier as an empty sentinel. Iterators detect structural changes on a best-effort basis.
     */
    public static final class Big {
        private final PrimitiveKind kind;
        private final int segmentShift;
        private BigArray elements;
        private long head;
        private long size;
        private long version;

        private Big(PrimitiveKind kind, long initialCapacity, int segmentShift) {
            this.kind = Objects.requireNonNull(kind, "kind");
            if (initialCapacity < 0L) throw new IllegalArgumentException("initialCapacity < 0");
            this.segmentShift = segmentShift;
            elements = BigArray.sparse(kind, Math.max(1L, initialCapacity), segmentShift);
        }

        public PrimitiveKind kind() { return kind; }
        public long size64() { return size; }
        public long size() { return size; }
        public boolean isEmpty() { return size == 0L; }
        public long capacity64() { return elements.length(); }
        public long capacity() { return capacity64(); }
        public long maximumCapacity64() { return elements.maximumLength(); }
        public long allocatedPayloadBytes() { return elements.allocatedPayloadBytes(); }
        public long directoryReferenceSlots64() { return elements.directoryReferenceSlots64(); }

        /** Logical payload reservation; throws if capacity times lane width exceeds long range. */
        public long payloadBytes() { return elements.payloadBytes(); }

        public long getBits(long index) {
            return elements.getBits(physicalIndex(checkedIndex(index)));
        }

        /** Replaces one value without structurally invalidating an encounter-order iterator. */
        public long setBits(long index, long bits) {
            long physical = physicalIndex(checkedIndex(index));
            long previous = elements.getBits(physical);
            elements.setBits(physical, bits);
            return previous;
        }

        public void addFirstBits(long bits) {
            ensureCapacity(checkedNextSize());
            long nextHead = head == 0L ? capacity64() - 1L : head - 1L;
            // A failed lazy payload allocation must not publish a new head.
            elements.setBits(nextHead, bits);
            head = nextHead;
            size++;
            version++;
        }

        public void addLastBits(long bits) {
            ensureCapacity(checkedNextSize());
            elements.setBits(physicalIndex(size), bits);
            size++;
            version++;
        }

        public long peekFirstBits() {
            requireElement();
            return elements.getBits(head);
        }

        public long peekLastBits() {
            requireElement();
            return elements.getBits(physicalIndex(size - 1L));
        }

        public long removeFirstBits() {
            requireElement();
            long value = elements.getBits(head);
            elements.setBits(head, 0L);
            head = head == capacity64() - 1L ? 0L : head + 1L;
            size--;
            if (size == 0L) head = 0L;
            version++;
            return value;
        }

        public long removeLastBits() {
            requireElement();
            long physical = physicalIndex(size - 1L);
            long value = elements.getBits(physical);
            elements.setBits(physical, 0L);
            size--;
            if (size == 0L) head = 0L;
            version++;
            return value;
        }

        public long getFirstBits() { return peekFirstBits(); }
        public long getLastBits() { return peekLastBits(); }
        public long elementBits() { return peekFirstBits(); }
        public void pushBits(long bits) { addFirstBits(bits); }
        public long popBits() { return removeFirstBits(); }

        /** Releases sparse payload through the existing owner while preserving logical capacity. */
        public void clear() {
            elements.fillBits(0L, capacity64(), 0L);
            if (size == 0L) return;
            head = 0L;
            size = 0L;
            version++;
        }

        /** Grows with zero values or truncates the tail, preserving encounter order. */
        public void resize(long newSize) {
            if (newSize < 0L) throw new IllegalArgumentException("newSize < 0");
            if (newSize == size) return;
            if (newSize == 0L) {
                clear();
                return;
            }
            ensureCapacity(newSize);
            clearLogicalRange(Math.min(size, newSize), Math.max(size, newSize));
            size = newSize;
            if (size == 0L) head = 0L;
            version++;
        }

        public void ensureCapacity(long required) {
            ensureCapacity(required, STORAGE);
        }

        /** Cold-path failure-injection seam; the existing BigArray remains allocation authority. */
        interface GrowthOps {
            BigArray allocate(PrimitiveKind kind, long capacity, int segmentShift);
            void copy(BigArray source, long from,
                      BigArray target, long offset, long count);
        }

        private static final GrowthOps STORAGE = new GrowthOps() {
            @Override
            public BigArray allocate(PrimitiveKind kind, long capacity, int segmentShift) {
                return BigArray.sparse(kind, capacity, segmentShift);
            }

            @Override
            public void copy(BigArray source, long from,
                             BigArray target, long offset, long count) {
                source.copyTo(from, target, offset, count);
            }
        };

        void ensureCapacity(long required, GrowthOps operations) {
            if (required < 0L) throw new IllegalArgumentException("required < 0");
            Objects.requireNonNull(operations, "operations");
            long current = capacity64();
            if (required <= current) return;
            long maximum = maximumCapacity64();
            if (required > maximum) throw new IllegalArgumentException("capacity exceeds address range");
            long extra = (current >>> 1) + 1L;
            long grown = current > maximum - extra ? maximum : current + extra;
            long nextCapacity = Math.max(required, grown);
            BigArray next = Objects.requireNonNull(
                    operations.allocate(kind, nextCapacity, segmentShift), "replacement storage");
            if (next == elements || next.kind() != kind || !next.isSparse()
                    || next.length() != nextCapacity || next.segmentShift() != segmentShift) {
                throw new IllegalStateException("invalid replacement storage");
            }
            long first = Math.min(size, current - head);
            if (first > 0L) operations.copy(elements, head, next, 0L, first);
            long second = size - first;
            if (second > 0L) operations.copy(elements, 0L, next, first, second);
            // No live state is published until allocation and both independent spans succeed.
            elements = next;
            head = 0L;
        }

        public java.util.PrimitiveIterator.OfLong iterator() {
            long expected = version;
            return new java.util.PrimitiveIterator.OfLong() {
                private long cursor;
                @Override
                public boolean hasNext() {
                    checkVersion(expected);
                    return cursor < size;
                }
                @Override
                public long nextLong() {
                    checkVersion(expected);
                    if (cursor >= size) throw new NoSuchElementException();
                    return getBits(cursor++);
                }
            };
        }

        /** Visits raw carriers in encounter order; consumer exceptions propagate unchanged. */
        public void forEachBits(java.util.function.LongConsumer action) {
            Objects.requireNonNull(action, "action");
            long expected = version;
            for (long cursor = 0L; cursor < size; cursor++) {
                checkVersion(expected);
                action.accept(getBits(cursor));
                checkVersion(expected);
            }
        }

        private void clearLogicalRange(long from, long to) {
            if (from == to) return;
            long physical = physicalIndex(from);
            long count = to - from;
            long first = Math.min(count, capacity64() - physical);
            elements.fillBits(physical, physical + first, 0L);
            if (count > first) elements.fillBits(0L, count - first, 0L);
        }

        private long physicalIndex(long logical) {
            long remaining = capacity64() - head;
            return logical < remaining ? head + logical : logical - remaining;
        }

        private long checkedIndex(long index) {
            if (index < 0L || index >= size) throw new IndexOutOfBoundsException(Long.toString(index));
            return index;
        }

        private long checkedNextSize() {
            if (size == maximumCapacity64()) {
                throw new OutOfMemoryError("required primitive deque size exceeds address range");
            }
            return size + 1L;
        }

        private void requireElement() {
            if (size == 0L) throw new NoSuchElementException();
        }

        private void checkVersion(long expected) {
            if (version != expected) throw new java.util.ConcurrentModificationException();
        }
    }
}
