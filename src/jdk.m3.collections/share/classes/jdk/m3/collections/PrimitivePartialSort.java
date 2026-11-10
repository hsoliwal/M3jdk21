/* *****************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.m3.collections;

import java.util.Objects;

/**
 * In-place partial sorting over an exact-width primitive array.
 *
 * <p>The first {@code k} elements are fully ordered and are globally smallest
 * (or largest); the rest of the range is left unordered. The selection path
 * uses the first {@code k} cells themselves as the heap, so retained and
 * temporary per-element object overhead is zero.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/PrimitivePartialSort.java}
 * blob {@code 7ecccfb469693a8eef44e7175079a936dd61fb76} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class PrimitivePartialSort {
    public static final float DEFAULT_FULL_SORT_RATIO = 1.5f;

    private PrimitivePartialSort() {}

    public static void sortSmallestKBits(
            PrimitiveKind kind, Object array, int from, int toExclusive, int k) {
        sortSmallestKBits(kind, array, from, toExclusive, k, DEFAULT_FULL_SORT_RATIO);
    }

    public static void sortSmallestKBits(
            PrimitiveKind kind, Object array, int from, int toExclusive, int k,
            float fullSortRatio) {
        validate(kind, array, from, toExclusive, k, fullSortRatio);
        int count = toExclusive - from;
        if (k == 0 || count < 2) return;
        if (useFullSort(count, k, fullSortRatio)) {
            kind.sort(array, from, toExclusive);
            return;
        }
        partial(kind, array, from, toExclusive, k, true);
    }

    public static void sortLargestKBits(
            PrimitiveKind kind, Object array, int from, int toExclusive, int k) {
        sortLargestKBits(kind, array, from, toExclusive, k, DEFAULT_FULL_SORT_RATIO);
    }

    public static void sortLargestKBits(
            PrimitiveKind kind, Object array, int from, int toExclusive, int k,
            float fullSortRatio) {
        validate(kind, array, from, toExclusive, k, fullSortRatio);
        int count = toExclusive - from;
        if (k == 0 || count < 2) return;
        if (useFullSort(count, k, fullSortRatio)) {
            kind.sort(array, from, toExclusive);
            reverse(kind, array, from, toExclusive);
            return;
        }
        partial(kind, array, from, toExclusive, k, false);
    }

    private static void partial(
            PrimitiveKind kind, Object array, int from, int toExclusive,
            int k, boolean smallest) {
        heapify(kind, array, from, k, smallest);
        for (int index = from + k; index < toExclusive; index++) {
            long candidate = kind.readBits(array, index);
            long root = kind.readBits(array, from);
            int comparison = kind.compareBits(candidate, root);
            boolean competitive = smallest ? comparison < 0 : comparison > 0;
            if (!competitive) continue;

            // Preserve the displaced boundary in the scanned tail, then restore
            // the heap with the candidate in the root hole.
            kind.writeBits(array, index, root);
            siftDown(kind, array, from, k, 0, candidate, smallest);
        }

        // Max-heap -> ascending smallest K. Min-heap -> descending largest K.
        for (int heapSize = k; heapSize > 1; heapSize--) {
            int last = from + heapSize - 1;
            long root = kind.readBits(array, from);
            long replacement = kind.readBits(array, last);
            kind.writeBits(array, last, root);
            siftDown(kind, array, from, heapSize - 1, 0, replacement, smallest);
        }
    }

    private static void heapify(
            PrimitiveKind kind, Object array, int from, int size, boolean maxHeap) {
        for (int parent = (size >>> 1) - 1; parent >= 0; parent--) {
            long value = kind.readBits(array, from + parent);
            siftDown(kind, array, from, size, parent, value, maxHeap);
        }
    }

    private static void siftDown(
            PrimitiveKind kind, Object array, int from, int size, int position,
            long value, boolean maxHeap) {
        int half = size >>> 1;
        while (position < half) {
            int child = (position << 1) + 1;
            long childValue = kind.readBits(array, from + child);
            int right = child + 1;
            if (right < size) {
                long rightValue = kind.readBits(array, from + right);
                int siblingOrder = kind.compareBits(rightValue, childValue);
                if (maxHeap ? siblingOrder > 0 : siblingOrder < 0) {
                    child = right;
                    childValue = rightValue;
                }
            }

            int order = kind.compareBits(value, childValue);
            if (maxHeap ? order >= 0 : order <= 0) break;
            kind.writeBits(array, from + position, childValue);
            position = child;
        }
        kind.writeBits(array, from + position, value);
    }

    private static void reverse(
            PrimitiveKind kind, Object array, int from, int toExclusive) {
        int left = from;
        int right = toExclusive - 1;
        while (left < right) {
            long a = kind.readBits(array, left);
            long b = kind.readBits(array, right);
            kind.writeBits(array, left++, b);
            kind.writeBits(array, right--, a);
        }
    }

    private static boolean useFullSort(int count, int k, float ratio) {
        return k == count || (double) count / k <= ratio;
    }

    private static void validate(
            PrimitiveKind kind, Object array, int from, int toExclusive, int k,
            float fullSortRatio) {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(array, "array");
        int length = java.lang.reflect.Array.getLength(array);
        if (from < 0 || toExclusive < from || toExclusive > length) {
            throw new IndexOutOfBoundsException(
                    "range [" + from + ',' + toExclusive + ") length=" + length);
        }
        int count = toExclusive - from;
        if (k < 0 || k > count) throw new IllegalArgumentException("k out of range");
        if (!(fullSortRatio >= 1.0f) || Float.isNaN(fullSortRatio)) {
            throw new IllegalArgumentException("fullSortRatio must be >= 1");
        }
        // Force type validation even for k==0.
        if (length > 0) {
            kind.readBits(array, Math.min(from, length - 1));
        } else {
            kind.copyOf(array, 0);
        }
    }

    /**
     * Creates a non-destructive, stable, demand-driven ordering of signed long keys.
     * The input range is copied once; later caller mutations do not invalidate evidence.
     * Construction does no key comparisons, but snapshot/zero-initialization is O(n).
     *
     * @param evidenceCapacity maximum retained comparison triples before refinement pauses
     * @param maxBytes retained direct-buffer payload budget (not total process memory)
     * @param checkpoint existing progress-monitor checkpoint, or null for interrupt-only checks
     */
    public static LongOrder lazyLongOrder(long[] keys, int from, int toExclusive,
            boolean descending, int evidenceCapacity, long maxBytes, Runnable checkpoint) {
        return new LongOrder(keys, from, toExclusive, descending, evidenceCapacity, maxBytes, checkpoint);
    }

    /**
     * Snapshot-local tournament ordering. Every actual comparison is retained as (left, right,
     * signed outcome); the live tournament reuses unchanged subtree winners. Published prefixes
     * are retained and reread without comparisons. Equal keys keep original-index order.
     *
     * <p>Not thread-safe: externally serialize all access, including reads. No arbitrary mutable
     * comparator, dynamic insertion, automatic global persistence or implicit native loading.
     * Archive evidence through {@link #writeEvidence} before releasing the session. Nothing is
     * evicted during its lifetime. The caller owns durable archival and direct-buffer reclamation
     * follows normal JVM reachability. Full retention costs O(n + number of comparisons).
     *
     * <p>Native execution is explicit and uses the same owned buffer, bounded batches and exact
     * Java semantics. A work step is one internal-node repair or one prefix publication, not a
     * wall-clock bound. Allocation, copying, checkpoints, observers and archival are separate costs.
     */
    public static final class LongOrder {
        public static final int EVIDENCE = 1;
        public static final int PREFIX = 2;
        public static final int PROGRESS = 4;
        private static final int MAGIC = 0x4d334f31;
        private static final int HEADER = 64;
        private static final int BATCH = 256;
        private java.nio.ByteBuffer image;
        private final int n, base, from, treeOffset, prefixOffset, evidenceOffset;
        private int eventMask;
        private java.util.function.IntConsumer observer;
        private boolean busy;

        private LongOrder(long[] keys, int from, int to, boolean descending,
                int capacity, long maxBytes, Runnable monitor) {
            Objects.requireNonNull(keys, "keys");
            Objects.checkFromToIndex(from, to, keys.length);
            this.n = to - from;
            this.from = from;
            int leaves = 1;
            while (leaves < n) {
                if (leaves > (1 << 28)) throw new IllegalArgumentException("range too large");
                leaves <<= 1;
            }
            this.base = leaves;
            long tree = HEADER + 8L * n;
            long prefix = tree + 8L * base;
            long evidence = prefix + 4L * n;
            int bytes = checkedBytes(evidence, capacity, maxBytes);
            treeOffset = (int) tree;
            prefixOffset = (int) prefix;
            evidenceOffset = (int) evidence;
            checkpoint(monitor);
            image = java.nio.ByteBuffer.allocateDirect(bytes).order(java.nio.ByteOrder.LITTLE_ENDIAN);
            image.putInt(0, MAGIC).putInt(4, n).putInt(8, base).putInt(12, descending ? 1 : 0);
            image.putInt(16, base - 1).putInt(32, capacity).putInt(36, from);
            for (int i = 0; i < n; i++) {
                if ((i & 4095) == 0) checkpoint(monitor);
                image.putLong(HEADER + 8 * i, keys[from + i]);
            }
            for (int i = 0; i < 2 * base; i++) {
                if ((i & 4095) == 0) checkpoint(monitor);
                tree(i, i >= base && i - base < n ? i - base : -1);
            }
            checkpoint(monitor);
        }

        public int size() { return n; }
        public int knownCount() { return image.getInt(24); }
        public int evidenceCount() { return image.getInt(28); }
        public int evidenceCapacity() { return image.getInt(32); }
        public long workSteps() { return image.getLong(40); }
        public long payloadBytes() { return image.capacity(); }

        /** Reads only an already-published rank; never silently performs ordering work. */
        public int indexAt(int rank) {
            Objects.checkIndex(rank, knownCount());
            return from + image.getInt(prefixOffset + 4 * rank);
        }

        public long valueAt(int rank) { return key(indexAt(rank) - from); }

        public int evidenceLeft(int row) { return from + evidence(row, 0); }
        public int evidenceRight(int row) { return from + evidence(row, 4); }
        /** Natural signed-key comparison (-1, 0, 1), before direction/stable tie-breaking. */
        public int evidenceOutcome(int row) { return evidence(row, 8); }

        private int evidence(int row, int column) {
            Objects.checkIndex(row, evidenceCount());
            return image.getInt(evidenceOffset + 12 * row + column);
        }

        /** Notifications are batch-level and independent of mandatory evidence retention. */
        public void events(int mask, java.util.function.IntConsumer consumer) {
            idle();
            if ((mask & ~7) != 0) throw new IllegalArgumentException("unknown event bits");
            if (mask != 0) Objects.requireNonNull(consumer, "consumer");
            eventMask = mask;
            observer = consumer;
        }

        /**
         * Refines toward a prefix, performing at most maxSteps work steps. Returns steps performed.
         * Zero may mean already known, zero work budget, or evidence capacity exhaustion. Inspect
         * knownCount and evidenceCount; grow evidence storage explicitly rather than dropping it.
         * Cancellation/observer failure preserves every completed step; calls are not transactions.
         */
        public int advance(int prefix, int maxSteps, Runnable checkpoint) {
            return advance(prefix, maxSteps, checkpoint, false);
        }

        /** Requires the host to have loaded the matching m3_order JNI library. No fallback. */
        public int advanceNativeRequired(int prefix, int maxSteps, Runnable checkpoint) {
            return advance(prefix, maxSteps, checkpoint, true);
        }

        private int advance(int prefix, int maxSteps, Runnable monitor, boolean nativeRequired) {
            idle();
            if (prefix < 0 || prefix > n || maxSteps < 0) throw new IllegalArgumentException("budget/rank");
            busy = true;
            int steps = 0;
            try {
                checkpoint(monitor);
                while (knownCount() < prefix && steps < maxSteps) {
                    int beforeEvidence = evidenceCount(), beforePrefix = knownCount();
                    int budget = Math.min(BATCH, maxSteps - steps);
                    int used = nativeRequired ? nativeAdvance(image, prefix, budget) : javaAdvance(prefix, budget);
                    steps += used;
                    int flags = (evidenceCount() != beforeEvidence ? EVIDENCE : 0)
                            | (knownCount() != beforePrefix ? PREFIX : 0) | (used != 0 ? PROGRESS : 0);
                    if ((flags & eventMask) != 0) observer.accept(flags & eventMask);
                    checkpoint(monitor);
                    if (used < budget) break;
                }
                return steps;
            } finally { busy = false; }
        }

        private int javaAdvance(int prefix, int budget) {
            int steps = 0;
            while (knownCount() < prefix && steps < budget) {
                int build = image.getInt(16), repair = image.getInt(20);
                int node = build != 0 ? build : repair;
                if (node != 0) {
                    int left = tree(2 * node), right = tree(2 * node + 1), winner;
                    if (left < 0) winner = right;
                    else if (right < 0) winner = left;
                    else {
                        int count = evidenceCount();
                        if (count == evidenceCapacity()) break; // reserve before comparison
                        int order = Long.compare(key(left), key(right));
                        int row = evidenceOffset + 12 * count;
                        image.putInt(row, left).putInt(row + 4, right).putInt(row + 8, order);
                        image.putInt(28, count + 1);
                        winner = order == 0 ? Math.min(left, right)
                                : ((image.getInt(12) == 0 ? order < 0 : order > 0) ? left : right);
                    }
                    tree(node, winner);
                    if (build != 0) image.putInt(16, build - 1);
                    else image.putInt(20, repair >>> 1);
                } else {
                    int winner = tree(1), count = knownCount();
                    image.putInt(prefixOffset + 4 * count, winner);
                    tree(base + winner, -1);
                    image.putInt(24, count + 1).putInt(20, (base + winner) >>> 1);
                }
                steps++;
                image.putLong(40, image.getLong(40) + 1);
            }
            return steps;
        }

        /**
         * Increases tape capacity without losing evidence or redoing comparisons. A failed copy
         * leaves the old image installed. maxBytes bounds the new retained payload; during growth
         * both old and new direct buffers may be live, and old reclamation is not immediate.
         */
        public void reserveEvidence(int capacity, long maxBytes, Runnable monitor) {
            idle();
            if (capacity < evidenceCapacity()) throw new IllegalArgumentException("cannot discard evidence");
            int bytes = checkedBytes(evidenceOffset, capacity, maxBytes);
            busy = true;
            try {
                checkpoint(monitor);
                if (capacity == evidenceCapacity()) return;
                var next = java.nio.ByteBuffer.allocateDirect(bytes).order(java.nio.ByteOrder.LITTLE_ENDIAN);
                int usedBytes = evidenceOffset + 12 * evidenceCount();
                for (int offset = 0; offset < usedBytes;) {
                    checkpoint(monitor);
                    int length = Math.min(8192, usedBytes - offset);
                    next.put(offset, image, offset, length);
                    offset += length;
                }
                next.putInt(32, capacity);
                checkpoint(monitor);
                image = next;
            } finally { busy = false; }
        }

        /**
         * Lossless evidence export to the caller's existing persistence owner. Big-endian format:
         * magic, size, source-from, direction, evidence-count (five ints); size signed longs;
         * then evidence-count triples of relative left index, relative right index, raw outcome.
         * No stream close/flush or session mutation. Failed output may contain a partial archive.
         * The archive contains ordering facts, not a resumable runtime image or authenticated proof.
         */
        public void writeEvidence(java.io.OutputStream destination, Runnable monitor) throws java.io.IOException {
            Objects.requireNonNull(destination, "destination");
            idle();
            busy = true;
            try {
                checkpoint(monitor);
                var output = new java.io.DataOutputStream(destination);
                output.writeInt(MAGIC);
                output.writeInt(n);
                output.writeInt(from);
                output.writeInt(image.getInt(12));
                output.writeInt(evidenceCount());
                for (int i = 0; i < n; i++) {
                    if ((i & 255) == 0) checkpoint(monitor);
                    output.writeLong(key(i));
                }
                for (int i = 0; i < evidenceCount(); i++) {
                    if ((i & 255) == 0) checkpoint(monitor);
                    output.writeInt(evidence(i, 0));
                    output.writeInt(evidence(i, 4));
                    output.writeInt(evidence(i, 8));
                }
                checkpoint(monitor);
            } finally { busy = false; }
        }

        private int tree(int node) { return image.getInt(treeOffset + 4 * node); }
        private void tree(int node, int value) { image.putInt(treeOffset + 4 * node, value); }
        private long key(int index) { return image.getLong(HEADER + 8 * index); }
        private void idle() { if (busy) throw new IllegalStateException("reentrant ordering operation"); }
        private static int checkedBytes(long evidenceOffset, int capacity, long maximum) {
            long bytes = evidenceOffset + 12L * capacity;
            if (capacity < 0 || maximum < 0 || bytes > Integer.MAX_VALUE || bytes > maximum) {
                throw new IllegalArgumentException("direct payload/evidence budget");
            }
            return (int) bytes;
        }
        private static void checkpoint(Runnable monitor) {
            if (Thread.currentThread().isInterrupted()) throw new java.util.concurrent.CancellationException();
            if (monitor != null) monitor.run();
            if (Thread.currentThread().isInterrupted()) throw new java.util.concurrent.CancellationException();
        }
        private static native int nativeAdvance(java.nio.ByteBuffer image, int prefix, int budget);
    }
}
