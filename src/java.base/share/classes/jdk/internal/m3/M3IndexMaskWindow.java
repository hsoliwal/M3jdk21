/*
 * Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.m3;

import java.util.Objects;

/**
 * Bounded, stable, resumable mask selection over an already ordered primitive index.
 *
 * <p>Inputs are aligned by source ordinal. Neither the owner order nor duplicate row IDs
 * are changed. This is an explicit projection, not a live sorted collection wrapper or
 * a source of JVM object-reference/lifecycle facts. The caller must keep the input arrays
 * unchanged during each scan call and revalidate its parent generation between calls.
 * No Java predicate or comparator crosses the optional JNI boundary.
 */
public final class M3IndexMaskWindow {
    /** Maximum values copied in one call; bounded even for very large source arrays. */
    public static final int MAX_WINDOW = 4096;

    /** Explicit native request never silently falls back to Java. */
    public enum Backend { JAVA, NATIVE_REQUIRED }

    /** Core JDK native libjava, not an optional external M3 application library. */
    private static final class NativeLibrary {
        static final boolean AVAILABLE = load();
        private static boolean load() {
            try {
                System.loadLibrary("java");
                // libjava also exists in JDKs that predate this receiver. Resolve both
                // existing methods with valid, nonaliasing, zero-work windows before
                // admitting any NATIVE_REQUIRED call, including Java-side early exits.
                int[] rows = new int[0];
                byte[] states = new byte[0];
                int[] output = new int[0];
                return scanNative(rows, states, 0, 0, 0, output, 0, 0) == 0L
                        && scanBoundedNative(rows, states, 0, 0, 0, 0, output, 0, 0) == 0L;
            } catch (UnsatisfiedLinkError unavailable) {
                return false;
            }
        }
    }

    /** True only when the JDK-owned library provides both mask-window methods. */
    public static boolean nativeAvailable() { return NativeLibrary.AVAILABLE; }

    private M3IndexMaskWindow() { }

    /** The exclusive index of the next unvisited source entry. */
    public static int nextIndex(long result) {
        return (int) (result >>> 32);
    }

    /** Count of selected row IDs written to the caller's output window. */
    public static int written(long result) {
        return (int) result;
    }

    /**
     * Selects matching row IDs into the caller's output without allocating a result array.
     *
     * <p>{@code requiredMask} bits must all be set, and {@code excludedMask} bits must all
     * be absent. Masks use the unsigned 8-bit state representation; overlapping masks
     * correctly select nothing. The cursor advances through inspected source entries,
     * even if none matches. A zero limit returns the input cursor unchanged.
     *
     * <p>All geometry, masks and aliases are checked before any output write. Aliasing
     * output with orderedRows is rejected. An explicit native request refuses when the
     * JDK-owned {@code libjava} library is unavailable or lacks the new ABI.
     * JNI never retains array pointers or invokes callbacks.
     *
     * @return high 32 bits: exclusive next input index; low 32 bits: written count
     */
    public static long scan(int[] orderedRows, byte[] rowStates, int firstIndex,
            int requiredMask, int excludedMask, int[] output, int outputOffset,
            int limit, Backend backend) {
        Objects.requireNonNull(orderedRows, "orderedRows");
        Objects.requireNonNull(rowStates, "rowStates");
        Objects.requireNonNull(output, "output");
        Objects.requireNonNull(backend, "backend");
        if (orderedRows == output) {
            throw new IllegalArgumentException("source/output alias");
        }
        if (rowStates.length != orderedRows.length) {
            throw new IllegalArgumentException("index and aligned state lengths differ");
        }
        checkMask(requiredMask);
        checkMask(excludedMask);
        if (firstIndex < 0 || firstIndex > orderedRows.length) {
            throw new IndexOutOfBoundsException(firstIndex);
        }
        if (limit < 0 || limit > MAX_WINDOW) {
            throw new IllegalArgumentException("invalid bounded window limit");
        }
        Objects.checkFromIndexSize(outputOffset, limit, output.length);
        if (backend == Backend.NATIVE_REQUIRED) {
            if (!nativeAvailable()) {
                throw new UnsatisfiedLinkError("M3JDK21 libjava mask-window JNI unavailable");
            }
            if (limit == 0) return pack(firstIndex, 0);
            long result = scanNative(orderedRows, rowStates, firstIndex, requiredMask,
                    excludedMask, output, outputOffset, limit);
            requireValidResult(result, firstIndex, orderedRows.length, limit);
            return result;
        }
        if (limit == 0) return pack(firstIndex, 0);
        // Keep the ordinary scan atom unchanged; dispatch constant expressions here.
        if ((requiredMask & excludedMask) != 0) return pack(orderedRows.length, 0);
        if ((requiredMask | excludedMask) == 0) {
            int copied = Math.min(limit, orderedRows.length - firstIndex);
            System.arraycopy(orderedRows, firstIndex, output, outputOffset, copied);
            return pack(firstIndex + copied, copied);
        }
        return scanJava(orderedRows, rowStates, firstIndex, requiredMask,
                excludedMask, output, outputOffset, limit);
    }

    /**
     * Select at most limit rows after examining at most maxExamined source slots.
     * A nonterminal zero-result cursor is valid: resume at nextIndex(result).
     * Only primitive indices and byte state masks cross optional JNI.
     */
    public static long scanBounded(int[] orderedRows, byte[] rowStates, int firstIndex,
            int maxExamined, int requiredMask, int excludedMask, int[] output,
            int outputOffset, int limit, Backend backend) {
        Objects.requireNonNull(orderedRows, "orderedRows");
        Objects.requireNonNull(rowStates, "rowStates");
        Objects.requireNonNull(output, "output");
        Objects.requireNonNull(backend, "backend");
        if (orderedRows == output) throw new IllegalArgumentException("source/output alias");
        if (rowStates.length != orderedRows.length) {
            throw new IllegalArgumentException("index and aligned state lengths differ");
        }
        checkMask(requiredMask);
        checkMask(excludedMask);
        if (firstIndex < 0 || firstIndex > orderedRows.length) {
            throw new IndexOutOfBoundsException(firstIndex);
        }
        if (limit < 0 || limit > MAX_WINDOW) {
            throw new IllegalArgumentException("invalid bounded window limit");
        }
        Objects.checkFromIndexSize(outputOffset, limit, output.length);
        if (maxExamined < 0) throw new IllegalArgumentException("negative input scan budget");
        int end = firstIndex + Math.min(maxExamined, orderedRows.length - firstIndex);
        if (backend == Backend.NATIVE_REQUIRED) {
            if (!nativeAvailable()) {
                throw new UnsatisfiedLinkError("M3JDK21 libjava mask-window JNI unavailable");
            }
            if (limit == 0 || end == firstIndex) return pack(firstIndex, 0);
            long result = scanBoundedNative(orderedRows, rowStates, firstIndex, end,
                    requiredMask, excludedMask, output, outputOffset, limit);
            requireValidResult(result, firstIndex, end, limit);
            return result;
        }
        if (limit == 0 || end == firstIndex) return pack(firstIndex, 0);
        if ((requiredMask & excludedMask) != 0) return pack(end, 0);
        if ((requiredMask | excludedMask) == 0) {
            int copied = Math.min(limit, end - firstIndex);
            System.arraycopy(orderedRows, firstIndex, output, outputOffset, copied);
            return pack(firstIndex + copied, copied);
        }
        return scanRangeJava(orderedRows, rowStates, firstIndex, end, requiredMask,
                excludedMask, output, outputOffset, limit);
    }

    /** Pure Java reference atom; call only after public validation. */
    static long scanJava(int[] rows, byte[] states, int first, int required,
            int excluded, int[] output, int outputOffset, int limit) {
        return scanRangeJava(rows, states, first, rows.length, required,
                excluded, output, outputOffset, limit);
    }

    /** Single primitive scan atom used by the legacy and bounded Java paths. */
    private static long scanRangeJava(int[] rows, byte[] states, int first, int end,
            int required, int excluded, int[] output, int outputOffset, int limit) {
        // Compile the two unsigned-byte conditions once per window. The impossible
        // value -1 preserves contradictory masks even for package-local scanJava calls.
        final int testedMask = required | excluded;
        final int expected = (required & excluded) == 0 ? required : -1;
        int next = first;
        int copied = 0;
        while (next < end && copied < limit) {
            int flags = states[next] & 0xff;
            if ((flags & testedMask) == expected) {
                output[outputOffset + copied++] = rows[next];
            }
            next++;
        }
        return pack(next, copied);
    }

    private static void checkMask(int mask) {
        if ((mask & ~0xff) != 0) {
            throw new IllegalArgumentException("state mask must fit unsigned byte");
        }
    }

    private static long pack(int next, int copied) {
        return ((long) next << 32) | (copied & 0xffff_ffffL);
    }

    private static void requireValidResult(long result, int first, int length, int limit) {
        int next = nextIndex(result);
        int copied = written(result);
        if (next < first || next > length || copied < 0 || copied > limit
                || copied > next - first || (next < length && copied != limit)) {
            throw new IllegalStateException("invalid native mask-window cursor");
        }
    }

    private static native long scanNative(int[] rows, byte[] states, int first,
            int required, int excluded, int[] output, int outputOffset, int limit);

    /** Native bounded ABI is additive; explicit requests reject older shared libraries. */
    private static native long scanBoundedNative(int[] rows, byte[] states, int first,
            int end, int required, int excluded, int[] output, int outputOffset, int limit);

    /** Lazy, independent capability; older libraries retain the original scan ABI. */
    private static final class NativeCount {
        static final boolean AVAILABLE = load();
        private static boolean load() {
            if (!nativeAvailable()) return false;
            try {
                return countBoundedNative(new byte[0], 0, 0, 0, 0) == 0L;
            } catch (UnsatisfiedLinkError unavailable) {
                return false;
            }
        }
    }

    /** True only when both existing scans and the additive count entry resolve. */
    public static boolean nativeCountAvailable() { return NativeCount.AVAILABLE; }

    /**
     * Counts matching byte-state rows without reading row IDs or writing output arrays.
     * Examines at most {@code maxExamined} slots beginning at {@code firstIndex}.
     * The exclusive end is clamped to the state array length without integer overflow.
     *
     * <p>Every required bit must be present and every excluded bit absent. A byte with
     * several set bits still contributes at most one match; contradictory masks count
     * zero. Inputs must remain stable during the call. The caller owns synchronization
     * and must revalidate its parent generation between resumptions; this is not a
     * cached size of an arbitrary predicate-backed collection.
     *
     * <p>Validation order is states, backend, masks, first index, then scan budget.
     * An explicit native request requires the additive count symbol even for empty,
     * zero-budget or contradictory-mask calls. A legacy library may still scan but
     * cannot silently substitute Java or an index-producing scan for this operation.
     *
     * @return high 32 bits: exclusive next source index; low 32 bits: match count,
     *         decoded with {@link #nextIndex(long)} and {@link #written(long)}
     */
    public static long countBounded(byte[] rowStates, int firstIndex, int maxExamined,
            int requiredMask, int excludedMask, Backend backend) {
        Objects.requireNonNull(rowStates, "rowStates");
        Objects.requireNonNull(backend, "backend");
        checkMask(requiredMask);
        checkMask(excludedMask);
        if (firstIndex < 0 || firstIndex > rowStates.length) {
            throw new IndexOutOfBoundsException(firstIndex);
        }
        if (maxExamined < 0) throw new IllegalArgumentException("negative input scan budget");
        int end = firstIndex + Math.min(maxExamined, rowStates.length - firstIndex);
        if (backend == Backend.NATIVE_REQUIRED) {
            if (!nativeCountAvailable()) {
                throw new UnsatisfiedLinkError("M3JDK21 libjava mask-window count JNI unavailable");
            }
            long result = countBoundedNative(rowStates, firstIndex, end, requiredMask, excludedMask);
            int count = written(result);
            if (nextIndex(result) != end || count < 0 || count > end - firstIndex) {
                throw new IllegalStateException("invalid native mask-window count");
            }
            return result;
        }
        if ((requiredMask & excludedMask) != 0) return pack(end, 0);
        if ((requiredMask | excludedMask) == 0) return pack(end, end - firstIndex);
        return pack(end, countRangeJava(rowStates, firstIndex, end, requiredMask, excludedMask));
    }

    /** Scalar reduction over stable caller-owned states; no row or result storage. */
    private static int countRangeJava(byte[] states, int first, int end,
            int required, int excluded) {
        final int testedMask = required | excluded;
        int count = 0;
        for (int index = first; index < end; index++) {
            if (((states[index] & 0xff) & testedMask) == required) count++;
        }
        return count;
    }

    /** Additive count ABI; no row IDs, result arrays or object callbacks cross JNI. */
    private static native long countBoundedNative(byte[] states, int first, int end,
            int required, int excluded);
}
