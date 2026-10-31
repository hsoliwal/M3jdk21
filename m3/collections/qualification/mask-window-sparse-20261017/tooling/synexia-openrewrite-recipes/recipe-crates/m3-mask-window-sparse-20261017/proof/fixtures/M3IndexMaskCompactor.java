/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections;

import java.util.ConcurrentModificationException;
import java.util.Objects;

/**
 * Compacts an already-ordered primitive row index using a caller-precomputed state mask.
 *
 * <p>Position {@code i} in {@code rowStates} describes {@code orderedRows[i]};
 * values are unsigned bytes for mask tests. The selected output retains the original
 * index order and duplicate IDs. Neither input is mutated or retained. An explicit
 * JNI backend is optional and never receives predicates, comparators or object refs.
 *
 * <p>Selection is O(n) time, O(k) returned slots (two Java passes, one exact array).
 * Existing M3JDK collection owners retain domain lifetime, modification epochs,
 * order, and index composition; a detached mask is not a live collection view.
 */
public final class M3IndexMaskCompactor {
    /** Backends do not silently change based on machine configuration. */
    public enum Backend { JAVA, NATIVE_REQUIRED }

    private M3IndexMaskCompactor() {
    }

    /** Returns whether a compatible optional JNI binary could be loaded. */
    public static boolean nativeAvailable() {
        return NativeLibrary.AVAILABLE;
    }

    /**
     * Select row IDs satisfying all {@code requiredMask} bits, preserving order.
     * Exactly one input validation pass is performed before the JNI call.
     * Source aliases may change after return; the output is always detached.
     */
    public static int[] select(int[] orderedRows, byte[] rowStates,
                               int requiredMask, Backend backend) {
        Objects.requireNonNull(orderedRows, "orderedRows");
        Objects.requireNonNull(rowStates, "rowStates");
        Objects.requireNonNull(backend, "backend");
        validate(rowStates, orderedRows.length, requiredMask);
        if (backend == Backend.NATIVE_REQUIRED) {
            if (!NativeLibrary.AVAILABLE) {
                throw new UnsatisfiedLinkError("m3_index_mask JNI library unavailable");
            }
            return selectNative(orderedRows, rowStates, requiredMask);
        }
        return selectJava(orderedRows, rowStates, requiredMask);
    }

    /** Count set-flag matches without boxing, copying, or retaining a view. */
    public static int selectedCount(byte[] rowStates, int requiredMask) {
        Objects.requireNonNull(rowStates, "rowStates");
        requireMask(requiredMask);
        return countSelected(rowStates, requiredMask);
    }

    /** Primitive-long index selection, preserving source order and duplicate IDs. */
    public static long[] selectLong(long[] orderedRows, byte[] rowStates, int requiredMask) {
        Objects.requireNonNull(orderedRows, "orderedRows");
        validate(rowStates, orderedRows.length, requiredMask);
        int selectedCount = countSelected(rowStates, requiredMask);
        // Every row is selected: one defensive primitive copy, no second mask scan.
        if (selectedCount == orderedRows.length) return orderedRows.clone();
        long[] selected = new long[selectedCount];
        int written = 0;
        for (int i = 0; i < orderedRows.length; i++) {
            if (matches(rowStates[i], requiredMask)) {
                if (written == selected.length) {
                    throw new ConcurrentModificationException("aligned selection states changed");
                }
                selected[written++] = orderedRows[i];
            }
        }
        if (written != selected.length) {
            throw new ConcurrentModificationException("aligned selection states changed");
        }
        return selected;
    }

    /** Validate before an in-place primitive owner writes a single resident slot. */
    static void validate(byte[] states, int logicalSize, int requiredMask) {
        Objects.requireNonNull(states, "rowStates");
        if (states.length != logicalSize) {
            throw new IllegalArgumentException("index and aligned state lengths differ");
        }
        requireMask(requiredMask);
    }

    private static void requireMask(int requiredMask) {
        if ((requiredMask & ~0xff) != 0) {
            throw new IllegalArgumentException("required mask must fit an unsigned byte");
        }
    }

    static boolean matches(byte state, int requiredMask) {
        return ((state & 0xff) & requiredMask) == requiredMask;
    }

    private static int countSelected(byte[] states, int requiredMask) {
        if (requiredMask == 0) return states.length;
        int kept = 0;
        for (byte state : states) if (matches(state, requiredMask)) kept++;
        return kept;
    }

    /**
     * Java reference kernel: exactly one final primitive array, no O(n) scratch
     * or per-element object. Caller must keep both input arrays stable during use.
     */
    static int[] selectJava(int[] orderedRows, byte[] rowStates, int requiredMask) {
        int selectedCount = countSelected(rowStates, requiredMask);
        // Retain output isolation even when the mask selects every source index.
        if (selectedCount == orderedRows.length) return orderedRows.clone();
        int[] selected = new int[selectedCount];
        int written = 0;
        for (int i = 0; i < orderedRows.length; i++) {
            if (matches(rowStates[i], requiredMask)) {
                if (written == selected.length) {
                    throw new ConcurrentModificationException("aligned selection states changed");
                }
                selected[written++] = orderedRows[i];
            }
        }
        if (written != selected.length) {
            throw new ConcurrentModificationException("aligned selection states changed");
        }
        return selected;
    }

    private static final class NativeLibrary {
        private static final boolean AVAILABLE;

        static {
            boolean loaded;
            try {
                System.loadLibrary("m3_collections_mask");
                loaded = true;
            } catch (UnsatisfiedLinkError unsupported) {
                // Java remains the default; only NATIVE_REQUIRED demands the library.
                loaded = false;
            }
            AVAILABLE = loaded;
        }
    }

    private static native int[] selectNative(int[] orderedRows, byte[] rowStates,
                                              int requiredMask);
}
