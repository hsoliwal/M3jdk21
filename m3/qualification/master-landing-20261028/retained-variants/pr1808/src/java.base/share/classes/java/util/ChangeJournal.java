/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
 */
package java.util;

/**
 * A bounded, versioned journal for positional model changes. Changes occupy
 * parallel primitive/reference lanes rather than retained event objects.
 * Readers materialize immutable batches only on demand. This is a non-standard
 * M3JDK21 API candidate, not an implementation of EMF's notification contract.
 *
 * <p>A cursor is the version of the last processed change. A cursor older than
 * the retained history is rejected rather than silently losing changes.
 * A RESET event invalidates a consumer's positional view; it does not carry
 * individual replaced elements. The owner serializes mutation externally.
 */
public final class ChangeJournal {
    /** Type of positional mutation. */
    public enum Kind {
        /** Elements were inserted at the reported position. */
        INSERT,
        /** Elements were removed from the reported position. */
        REMOVE,
        /** Elements at the reported position were replaced. */
        REPLACE,
        /** The consumer must refresh its positional view. */
        RESET
    }

    /** Receives a projected change, without allocating a per-change object. */
    @FunctionalInterface
    public interface Consumer {
        /**
         * Receives one change in journal order.
         *
         * @param version the sequence version of the change
         * @param kind the kind of positional mutation
         * @param index the nonnegative position reported by the producer
         * @param removedCount the number of removed elements
         * @param addedCount the number of added elements
         * @param previous the producer-supplied prior value, possibly {@code null}
         * @param current the producer-supplied current value, possibly {@code null}
         */
        void accept(long version, Kind kind, int index, int removedCount,
                    int addedCount, Object previous, Object current);
    }

    /** Immutable, independent snapshot of a contiguous retained version window. */
    public static final class Batch {
        private final long cursor;
        private final byte[] kinds;
        private final int[] indexes;
        private final int[] removed;
        private final int[] added;
        private final Object[] previous;
        private final Object[] current;

        private Batch(long cursor, byte[] kinds, int[] indexes,
                      int[] removed, int[] added, Object[] previous, Object[] current) {
            this.cursor = cursor;
            this.kinds = kinds;
            this.indexes = indexes;
            this.removed = removed;
            this.added = added;
            this.previous = previous;
            this.current = current;
        }

        /**
         * Returns the version of the last event in this batch, or the requested cursor if empty.
         *
         * @return the cursor to use for the next snapshot
         */
        public long cursor() { return cursor; }

        /**
         * Returns the number of retained events copied into this batch.
         *
         * @return the number of changes in the batch
         */
        public int size() { return kinds.length; }

        /**
         * Replays this stable batch in sequence; callbacks execute without a journal lock.
         *
         * @param receiver the consumer of each change
         * @throws NullPointerException if {@code receiver} is {@code null}
         */
        public void forEach(Consumer receiver) {
            Objects.requireNonNull(receiver, "receiver");
            long first = cursor - kinds.length + 1;
            for (int i = 0; i < kinds.length; i++) {
                receiver.accept(first + i, KINDS[kinds[i]], indexes[i],
                        removed[i], added[i], previous[i], current[i]);
            }
        }
    }

    private static final int MAX_CAPACITY = 1 << 20;
    private static final Kind[] KINDS = Kind.values();

    private final int capacity;
    private final long baseline;
    private final byte[] kinds;
    private final int[] indexes;
    private final int[] removed;
    private final int[] added;
    private final Object[] previous;
    private final Object[] current;
    private long version;

    /**
     * Creates a journal with a zero version.
     *
     * @param capacity the maximum number of retained changes, from 1 through 1,048,576
     * @throws IllegalArgumentException if {@code capacity} is outside the supported range
     */
    public ChangeJournal(int capacity) { this(capacity, 0); }

    // A late subscriber starts at the owner's current version, without fabricated history.
    ChangeJournal(int capacity, long baseline) {
        if (capacity <= 0 || capacity > MAX_CAPACITY) {
            throw new IllegalArgumentException("journal capacity out of range");
        }
        if (baseline < 0) throw new IllegalArgumentException("negative baseline");
        this.capacity = capacity;
        this.baseline = baseline;
        this.version = baseline;
        kinds = new byte[capacity];
        indexes = new int[capacity];
        removed = new int[capacity];
        added = new int[capacity];
        previous = new Object[capacity];
        current = new Object[capacity];
    }

    /**
     * Returns the latest committed change version.
     *
     * @return the current sequence version
     */
    public synchronized long version() { return version; }

    /**
     * Returns the earliest usable cursor; a client must refresh when its cursor is older.
     *
     * @return the oldest cursor for which all subsequent changes remain available
     */
    public synchronized long oldestCursor() {
        return Math.max(baseline, version - capacity);
    }

    /**
     * Returns the fixed history bound; no per-change event objects are retained.
     *
     * @return the maximum number of retained changes
     */
    public int capacity() { return capacity; }

    /**
     * Copies all changes after {@code afterVersion} into a stable batch.
     * An empty batch keeps the supplied cursor. Never invokes a callback while
     * the owner's monitor lock is held.
     *
     * @param afterVersion the version of the last change already processed
     * @return a snapshot containing all retained changes after the supplied cursor
     * @throws IllegalArgumentException if the cursor is negative or in the future
     * @throws IllegalStateException if some required events have been overwritten
     */
    public synchronized Batch snapshot(long afterVersion) {
        if (afterVersion < 0 || afterVersion > version) {
            throw new IllegalArgumentException("cursor outside version range");
        }
        if (afterVersion < oldestCursor()) {
            throw new IllegalStateException("stale journal cursor: " + afterVersion
                    + "; oldest=" + oldestCursor());
        }
        int count = (int) (version - afterVersion);
        byte[] bk = new byte[count];
        int[] bi = new int[count];
        int[] br = new int[count];
        int[] ba = new int[count];
        Object[] bp = new Object[count];
        Object[] bc = new Object[count];
        for (int i = 0; i < count; i++) {
            int slot = (int) ((afterVersion + i) % capacity);
            bk[i] = kinds[slot];
            bi[i] = indexes[slot];
            br[i] = removed[slot];
            ba[i] = added[slot];
            bp[i] = previous[slot];
            bc[i] = current[slot];
        }
        return new Batch(version, bk, bi, br, ba, bp, bc);
    }

    /**
     * Commits a positional event and returns its sequence version.
     *
     * @param kind the kind of positional mutation
     * @param index the nonnegative position reported by the producer
     * @param removedCount the number of removed elements
     * @param addedCount the number of added elements
     * @param prior the producer-supplied prior value, possibly {@code null}
     * @param next the producer-supplied current value, possibly {@code null}
     * @return the sequence version assigned to the committed change
     * @throws NullPointerException if {@code kind} is {@code null}
     * @throws IllegalArgumentException if a position or count is negative
     * @throws IllegalStateException if the version counter is exhausted
     */
    public synchronized long append(Kind kind, int index, int removedCount,
                                    int addedCount, Object prior, Object next) {
        record(kind, index, removedCount, addedCount, prior, next);
        return version;
    }

    synchronized void record(Kind kind, int index, int removedCount,
                             int addedCount, Object prior, Object next) {
        Objects.requireNonNull(kind, "kind");
        if (index < 0 || removedCount < 0 || addedCount < 0) {
            throw new IllegalArgumentException("negative change coordinate");
        }
        if (version == Long.MAX_VALUE) {
            throw new IllegalStateException("journal version exhausted");
        }
        int slot = (int) (version % capacity);
        kinds[slot] = (byte) kind.ordinal();
        indexes[slot] = index;
        removed[slot] = removedCount;
        added[slot] = addedCount;
        previous[slot] = prior;
        current[slot] = next;
        version++;
    }
}
