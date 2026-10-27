/*******************************************************************************
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
package com.m3.collections;

import java.util.ConcurrentModificationException;
import java.util.Objects;

/**
 * Detached primitive-index selection against parent-owned M3 object state.
 *
 * <p>Rows are physical slot coordinates; the logical path is a bounded signed-stride
 * selection over {@code orderedSlots}. Repeated slots remain repeated and output
 * order is retained. The caller owns the coordinate array and must keep it stable
 * for the duration of a call. The parent monitor protects residency state and
 * canonical 64-bit parent words throughout each Java selection. No child is
 * materialized or retained, and no cooperative lease/GC reachability is changed.
 *
 * <p>The Java backend allocates only the exact returned int[] (O(k) heap);
 * JNI_REQUIRED creates temporary primitive snapshots (O(n) heap) before crossing
 * JNI, without passing an object reference or retaining any native pointer.
 */
public final class M3ResidencyStateMaskIndex {
    /** Explicit native requests fail rather than silently taking the Java path. */
    public enum Backend { JAVA, JNI_REQUIRED }

    private static final int ALL_STATES = (1 << M3ObjectResidency.State.values().length) - 1;

    private M3ResidencyStateMaskIndex() { }

    /** Report whether the optional JNI implementation is installed. */
    public static boolean nativeAvailable() { return NativePeer.AVAILABLE; }

    /** One bit per lifecycle state, following State.ordinal(). */
    public static int states(M3ObjectResidency.State... selected) {
        Objects.requireNonNull(selected, "selected");
        int mask = 0;
        for (var state : selected) mask |= 1 << Objects.requireNonNull(state, "state").ordinal();
        return mask;
    }

    /** Detached exact-size primitive row selection; no map entries or child references. */
    public static int[] select(M3ObjectResidency<?> arena, int[] orderedSlots,
            int offset, int count, int stride, long requiredBits, long forbiddenBits,
            int allowedStates, Backend backend) {
        Objects.requireNonNull(backend, "backend");
        synchronized (Objects.requireNonNull(arena, "arena")) {
            checkPath(arena, orderedSlots, offset, count, stride, requiredBits,
                    forbiddenBits, allowedStates);
            if (backend == Backend.JNI_REQUIRED) {
                if (!NativePeer.AVAILABLE) {
                    throw new UnsatisfiedLinkError("m3_residency_state_mask JNI library unavailable");
                }
                int[] coordinates = new int[count];
                long[] words = new long[count];
                byte[] states = new byte[count];
                for (int i = 0; i < count; i++) {
                    int slot = orderedSlots[(int) ((long) offset + (long) i * stride)];
                    coordinates[i] = slot;
                    words[i] = arena.readParentBits(slot);
                    states[i] = (byte) arena.state(slot).ordinal();
                }
                return select0(coordinates, words, states, requiredBits, forbiddenBits, allowedStates);
            }
            int matches = 0;
            for (int i = 0; i < count; i++) {
                int slot = orderedSlots[(int) ((long) offset + (long) i * stride)];
                if (matches(arena.readParentBits(slot), arena.state(slot).ordinal(),
                        requiredBits, forbiddenBits, allowedStates)) matches++;
            }
            int[] selected = new int[matches];
            // After a complete preflight, no second traversal is needed for zero hits.
            if (matches == 0) return selected;
            int written = 0;
            for (int i = 0; i < count; i++) {
                int slot = orderedSlots[(int) ((long) offset + (long) i * stride)];
                if (matches(arena.readParentBits(slot), arena.state(slot).ordinal(),
                        requiredBits, forbiddenBits, allowedStates)) {
                    if (written >= selected.length) {
                        throw new ConcurrentModificationException("coordinate path mutated during selection");
                    }
                    selected[written++] = slot;
                }
            }
            if (written != selected.length) {
                throw new ConcurrentModificationException("coordinate path mutated during selection");
            }
            return selected;
        }
    }

    /**
     * Stable, allocation-free output into a caller-owned primitive lane. This is useful
     * when M3 collections reuse an already allocated dynamic-array segment. The
     * destination may NOT alias the input coordinate array: that could overwrite an
     * unvisited coordinate and make the logical-to-physical mapping unstable.
     *
     * <p>Validation and a complete match-count preflight occur before the first
     * write; an invalid path, state mask or insufficient destination capacity leaves
     * the destination unchanged. The caller must keep both arrays stable while this
     * operation runs, just as for select(). Source identity and ordering are retained.
     * This method does not materialize or retain any child projection, and does not
     * change an existing collection's public contract.
     *
     * @return the number of physical slot IDs written beginning at destinationOffset
     */
    public static int selectInto(M3ObjectResidency<?> arena, int[] orderedSlots,
            int offset, int count, int stride, long requiredBits, long forbiddenBits,
            int allowedStates, int[] destination, int destinationOffset) {
        Objects.requireNonNull(destination, "destination");
        if (destination == orderedSlots) {
            throw new IllegalArgumentException("input coordinates cannot alias output lane");
        }
        Objects.checkFromIndexSize(destinationOffset, 0, destination.length);
        synchronized (Objects.requireNonNull(arena, "arena")) {
            checkPath(arena, orderedSlots, offset, count, stride, requiredBits,
                    forbiddenBits, allowedStates);
            int kept = 0;
            for (int i = 0; i < count; i++) {
                int slot = orderedSlots[(int) ((long) offset + (long) i * stride)];
                if (matches(arena.readParentBits(slot), arena.state(slot).ordinal(),
                        requiredBits, forbiddenBits, allowedStates)) kept++;
            }
            if (kept > destination.length - destinationOffset) {
                throw new IndexOutOfBoundsException("destination cannot contain selected coordinates");
            }
            // Preserve alias, bounds and contract checks; skip only the empty emission pass.
            if (kept == 0) return 0;
            int written = 0;
            for (int i = 0; i < count; i++) {
                int slot = orderedSlots[(int) ((long) offset + (long) i * stride)];
                if (matches(arena.readParentBits(slot), arena.state(slot).ordinal(),
                        requiredBits, forbiddenBits, allowedStates)) {
                    if (written == kept) {
                        throw new ConcurrentModificationException("coordinate path mutated during selection");
                    }
                    destination[destinationOffset + written++] = slot;
                }
            }
            if (written != kept) {
                throw new ConcurrentModificationException("coordinate path mutated during selection");
            }
            return written;
        }
    }

    /** Count matches without allocating output or materializing child objects. */
    public static int count(M3ObjectResidency<?> arena, int[] orderedSlots,
            int offset, int count, int stride, long requiredBits, long forbiddenBits,
            int allowedStates) {
        synchronized (Objects.requireNonNull(arena, "arena")) {
            checkPath(arena, orderedSlots, offset, count, stride, requiredBits,
                    forbiddenBits, allowedStates);
            int kept = 0;
            for (int i = 0; i < count; i++) {
                int slot = orderedSlots[(int) ((long) offset + (long) i * stride)];
                if (matches(arena.readParentBits(slot), arena.state(slot).ordinal(),
                        requiredBits, forbiddenBits, allowedStates)) kept++;
            }
            return kept;
        }
    }

    /** Return first selected physical slot, or -1; no result array allocation. */
    public static int first(M3ObjectResidency<?> arena, int[] orderedSlots,
            int offset, int count, int stride, long requiredBits, long forbiddenBits,
            int allowedStates) {
        synchronized (Objects.requireNonNull(arena, "arena")) {
            checkPath(arena, orderedSlots, offset, count, stride, requiredBits,
                    forbiddenBits, allowedStates);
            for (int i = 0; i < count; i++) {
                int slot = orderedSlots[(int) ((long) offset + (long) i * stride)];
                if (matches(arena.readParentBits(slot), arena.state(slot).ordinal(),
                        requiredBits, forbiddenBits, allowedStates)) return slot;
            }
            return -1;
        }
    }

    /** A logical 64-bit parent-state requirement is separate from lifecycle state. */
    private static boolean matches(long bits, int state, long required,
            long forbidden, int allowedStates) {
        return ((1 << state) & allowedStates) != 0
                && (bits & required) == required && (bits & forbidden) == 0L;
    }

    private static void checkPath(M3ObjectResidency<?> arena, int[] rows,
            int offset, int count, int stride, long required, long forbidden,
            int allowedStates) {
        Objects.requireNonNull(rows, "orderedSlots");
        if ((required & forbidden) != 0L) {
            throw new IllegalArgumentException("required and forbidden parent masks overlap");
        }
        if ((allowedStates & ~ALL_STATES) != 0) {
            throw new IllegalArgumentException("unknown residency lifecycle state bits");
        }
        if (count < 0 || stride == 0) {
            throw new IllegalArgumentException("negative path length or zero stride");
        }
        if (count == 0) {
            if (offset < 0 || offset > rows.length) throw new IndexOutOfBoundsException(offset);
            return;
        }
        long last = (long) offset + (long) (count - 1) * stride;
        if (offset < 0 || offset >= rows.length || last < 0L || last >= rows.length) {
            throw new IndexOutOfBoundsException("logical path exceeds coordinate extent");
        }
        int slots = arena.slots();
        for (int i = 0; i < count; i++) {
            int physical = rows[(int) ((long) offset + (long) i * stride)];
            if (physical < 0 || physical >= slots) {
                throw new IndexOutOfBoundsException("physical slot " + physical + " outside " + slots);
            }
        }
    }

    private static final class NativePeer {
        private static final boolean AVAILABLE;
        static {
            boolean loaded;
            try {
                System.loadLibrary("m3_residency_state_mask");
                loaded = true;
            } catch (UnsatisfiedLinkError notInstalled) {
                loaded = false;
            }
            AVAILABLE = loaded;
        }
    }

    private static native int[] select0(int[] coordinates, long[] words, byte[] lifecycle,
            long required, long forbidden, int allowedStates);
}
