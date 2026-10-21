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
    public enum Kind { INSERT, REMOVE, REPLACE, RESET }

    /** Receives a projected change, without allocating a per-change object. */
    @FunctionalInterface
    public interface Consumer {
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

        /** Version immediately after the last event in this batch, or the requested cursor if empty. */
        public long cursor() { return cursor; }

        /** Number of retained events copied into this batch. */
        public int size() { return kinds.length; }

        /** Replays this stable batch in sequence; callbacks execute without a journal lock. */
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

    /** Creates a journal with a zero version. */
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

    /** Latest committed change version. */
    public synchronized long version() { return version; }

    /** Earliest usable cursor; a client must refresh when its cursor is older. */
    public synchronized long oldestCursor() {
        return Math.max(baseline, version - capacity);
    }

    /** Fixed history bound; no retained per-change object allocation. */
    public int capacity() { return capacity; }

    /**
     * Copies all changes after {@code afterVersion} into a stable batch.
     * An empty batch keeps the supplied cursor. Never invokes a callback while
     * the owner's monitor lock is held.
     *
     * @throws IllegalArgumentException if the cursor is negative or in the future
     * @throws IllegalStateException if some required events have been overwritten
     */
    public synchronized Batch snapshot(long afterVersion) {
        return snapshot(afterVersion, capacity);
    }

    /**
     * Copies no more than {@code maxChanges} consecutive model changes.
     * The returned cursor is the last copied version, not the journal head;
     * callers may resume at that cursor without losing retained changes.
     * A zero bound creates an empty batch while retaining the requested cursor.
     * A stale cursor is rejected even for zero-length requests. No model element
     * or per-change wrapper is retained beyond the batch's six compact lanes.
     *
     * @throws IllegalArgumentException if the cursor or bound is invalid
     * @throws IllegalStateException if the requested history was overwritten
     */
    public synchronized Batch snapshot(long afterVersion, int maxChanges) {
        if (afterVersion < 0 || afterVersion > version) {
            throw new IllegalArgumentException("cursor outside version range");
        }
        if (afterVersion < oldestCursor()) {
            throw new IllegalStateException("stale journal cursor: " + afterVersion
                    + "; oldest=" + oldestCursor());
        }
        if (maxChanges < 0) {
            throw new IllegalArgumentException("negative change limit");
        }
        int count = (int) Math.min(version - afterVersion, (long) maxChanges);
        byte[] bk = new byte[count];
        int[] bi = new int[count];
        int[] br = new int[count];
        int[] ba = new int[count];
        Object[] bp = new Object[count];
        Object[] bc = new Object[count];
        int first = (int) (afterVersion % capacity);
        int prefix = Math.min(count, capacity - first);
        copyLanes(first, 0, prefix, bk, bi, br, ba, bp, bc);
        int suffix = count - prefix;
        if (suffix != 0) {
            copyLanes(0, prefix, suffix, bk, bi, br, ba, bp, bc);
        }
        return new Batch(afterVersion + count, bk, bi, br, ba, bp, bc);
    }

    /** Copies only the requested contiguous physical span of the circular lanes. */
    private void copyLanes(int source, int target, int count, byte[] bk, int[] bi,
                           int[] br, int[] ba, Object[] bp, Object[] bc) {
        System.arraycopy(kinds, source, bk, target, count);
        System.arraycopy(indexes, source, bi, target, count);
        System.arraycopy(removed, source, br, target, count);
        System.arraycopy(added, source, ba, target, count);
        System.arraycopy(previous, source, bp, target, count);
        System.arraycopy(current, source, bc, target, count);
    }

    /** Commits a positional event and returns its sequence version. */
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
