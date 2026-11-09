// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.compiler;

import java.util.Arrays;
import java.util.Objects;

/**
 * Read-only compiler candidate planner for half-open local lifetime intervals.
 * Input offsets refer to one verified source/CFG generation and must be supplied
 * by an existing attributed liveness/escape owner. This class does NOT verify
 * those facts, touch objects, recycle Java references or authorize a rewrite.
 * Unknown/escaped/JNI-pinned intervals must never be submitted as reusable.
 *
 * <p>Interval partitioning gives an exact minimum number of distinct logical
 * slots for the supplied intervals, using only primitive scratch arrays. Slots
 * are candidates for parent-owned storage, not JVM object addresses.</p>
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/64652588f0d4c23b1f6e0411090b2a796325ae0e/synexia-openrewrite-recipes/src/main/java/com/synexia/rewrite/M3DoopLifetimeSlotPlanner.java blob 257b89b5022b; recipe m3/compiler/lifetime-slots/recipe/relocate.py v1
public final class DoopLifetimeSlotPlanner {
    public static final int MAX_WINDOWS = 262_144;

    private DoopLifetimeSlotPlanner() { }

    public static final class Plan {
        private final int[] slotsByInput;
        private final int requiredSlots;

        private Plan(int[] slotsByInput, int requiredSlots) {
            this.slotsByInput = slotsByInput;
            this.requiredSlots = requiredSlots;
        }

        /** Smallest slot count for the admitted half-open interval geometry. */
        public int requiredSlots() { return requiredSlots; }
        public int count() { return slotsByInput.length; }
        public int slotOf(int originalIndex) { return slotsByInput[originalIndex]; }
        public int[] slotsByInput() { return slotsByInput.clone(); }
    }

    /**
     * No per-interval objects, no Map.Entry and no mutation of caller arrays.
     * Source-atom IDs must be unique so input permutation cannot alter ties.
     * This is candidate geometry ONLY; it is not a proof of Java reachability.
     */
    public static Plan planCandidates(long[] startInput, long[] endInput, long[] atomIdInput) {
        return planByCompatibility(startInput, endInput, atomIdInput, null);
    }

    /**
     * Compute minimal physical slots without crossing incompatible projection owners.
     * Each compatibility ID is a compiler-verified identity of the same physical Java class,
     * owning parent, reset schema and JNI lifetime contract. Unverified IDs may not be used
     * as a reuse permission. Results are logical slot candidates, not Java object pointers.
     * Runtime borrower, pin and GC checks remain with the canonical DOOPS residency owner.
     */
    public static Plan planCompatibleCandidates(long[] startInput, long[] endInput,
            long[] atomIdInput, long[] compatibilityIdInput) {
        Objects.requireNonNull(compatibilityIdInput, "compatibilityIdInput");
        return planByCompatibility(startInput, endInput, atomIdInput, compatibilityIdInput);
    }

    private static Plan planByCompatibility(long[] startInput, long[] endInput,
            long[] atomIdInput, long[] compatibilityIdInput) {
        Objects.requireNonNull(startInput, "startInput");
        Objects.requireNonNull(endInput, "endInput");
        Objects.requireNonNull(atomIdInput, "atomIdInput");
        final int n = startInput.length;
        if (n != endInput.length || n != atomIdInput.length
                || (compatibilityIdInput != null && n != compatibilityIdInput.length)
                || n > MAX_WINDOWS) {
            throw new IllegalArgumentException("inconsistent or excessive intervals");
        }
        final long[] starts = startInput.clone();
        final long[] ends = endInput.clone();
        final long[] ids = atomIdInput.clone();
        final long[] groups = compatibilityIdInput == null ? null : compatibilityIdInput.clone();
        for (int i = 0; i < n; i++) {
            if (starts[i] < 0 || ends[i] <= starts[i] || ids[i] < 0
                    || (groups != null && groups[i] < 0)) {
                throw new IllegalArgumentException("invalid interval at index " + i);
            }
        }
        final long[] uniqueIds = ids.clone();
        Arrays.sort(uniqueIds);
        for (int i = 1; i < n; i++) {
            if (uniqueIds[i] == uniqueIds[i - 1]) {
                throw new IllegalArgumentException("duplicate atom ID");
            }
        }
        final int[] order = new int[n];
        final int[] scratch = new int[n];
        for (int i = 0; i < n; i++) order[i] = i;
        for (int width = 1; width < n; width *= 2) {
            final int stride = width * 2;
            for (int left = 0; left < n; left += stride) {
                final int middle = Math.min(left + width, n);
                final int right = Math.min(left + stride, n);
                int first = left;
                int second = middle;
                for (int out = left; out < right; out++) {
                    if (second == right || (first < middle
                            && compare(order[first], order[second], starts, ends, ids, groups) <= 0)) {
                        scratch[out] = order[first++];
                    } else {
                        scratch[out] = order[second++];
                    }
                }
            }
            System.arraycopy(scratch, 0, order, 0, n);
        }
        final ActiveHeap active = new ActiveHeap(n);
        final FreeHeap free = new FreeHeap(n);
        final int[] assignment = new int[n];
        int allocated = 0;
        long currentGroup = Long.MIN_VALUE;
        for (int index : order) {
            if (groups != null && groups[index] != currentGroup) {
                // Keep heap arrays but do not share an allocated slot across incompatible owners.
                active.size = 0;
                free.size = 0;
                currentGroup = groups[index];
            }
            while (active.size > 0 && active.firstEnd() <= starts[index]) {
                free.offer(active.releaseFirst());
            }
            final int slot = free.size > 0 ? free.takeFirst() : allocated++;
            assignment[index] = slot;
            active.offer(ends[index], slot);
        }
        return new Plan(assignment, allocated);
    }

    private static int compare(int a, int b, long[] starts, long[] ends, long[] ids,
            long[] groups) {
        if (groups != null) {
            int groupOrder = Long.compare(groups[a], groups[b]);
            if (groupOrder != 0) return groupOrder;
        }
        return compare(a, b, starts, ends, ids);
    }

    private static int compare(int a, int b, long[] starts, long[] ends, long[] ids) {
        int result = Long.compare(starts[a], starts[b]);
        if (result == 0) result = Long.compare(ends[a], ends[b]);
        if (result == 0) result = Long.compare(ids[a], ids[b]);
        return result;
    }

    /** Min-heap of active (exclusive end, slot) pairs in parallel primitive arrays. */
    private static final class ActiveHeap {
        private final long[] end;
        private final int[] slot;
        private int size;
        ActiveHeap(int capacity) { end = new long[capacity]; slot = new int[capacity]; }
        long firstEnd() { return end[0]; }
        void offer(long value, int id) {
            int i = size++;
            while (i > 0) {
                int parent = (i - 1) >>> 1;
                if (compare(value, id, end[parent], slot[parent]) >= 0) break;
                end[i] = end[parent]; slot[i] = slot[parent]; i = parent;
            }
            end[i] = value; slot[i] = id;
        }
        int releaseFirst() {
            int result = slot[0];
            long replacementEnd = end[--size];
            int replacementSlot = slot[size];
            int i = 0;
            while ((i * 2 + 1) < size) {
                int child = i * 2 + 1;
                if (child + 1 < size && compare(end[child + 1], slot[child + 1],
                        end[child], slot[child]) < 0) child++;
                if (compare(replacementEnd, replacementSlot, end[child], slot[child]) <= 0) break;
                end[i] = end[child]; slot[i] = slot[child]; i = child;
            }
            if (size > 0) { end[i] = replacementEnd; slot[i] = replacementSlot; }
            return result;
        }
        private static int compare(long aEnd, int aSlot, long bEnd, int bSlot) {
            int c = Long.compare(aEnd, bEnd);
            return c == 0 ? Integer.compare(aSlot, bSlot) : c;
        }
    }

    /** Smallest-free-slot first ensures deterministic assignments for a fixed atom set. */
    private static final class FreeHeap {
        private final int[] slot;
        private int size;
        FreeHeap(int capacity) { slot = new int[capacity]; }
        void offer(int value) {
            int i = size++;
            while (i > 0 && value < slot[(i - 1) >>> 1]) {
                slot[i] = slot[(i - 1) >>> 1]; i = (i - 1) >>> 1;
            }
            slot[i] = value;
        }
        int takeFirst() {
            int result = slot[0];
            int replacement = slot[--size];
            int i = 0;
            while (i * 2 + 1 < size) {
                int child = i * 2 + 1;
                if (child + 1 < size && slot[child + 1] < slot[child]) child++;
                if (replacement <= slot[child]) break;
                slot[i] = slot[child]; i = child;
            }
            if (size > 0) slot[i] = replacement;
            return result;
        }
    }
}
