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
package jdk.m3.collections;

import java.util.Arrays;
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
 * <p>Selection is O(n) time and O(k) returned slots: at most one counting pass
 * and one emission pass, with one exact result array. Inputs must remain stable
 * during a call; concurrent-change checks are best effort, not a snapshot guarantee.
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

    /**
     * Select logical positions satisfying all required state bits without a caller-owned
     * identity row array. The result is detached, ascending encounter-order coordinates.
     * New native binaries generate coordinates directly from the byte-state lane; legacy
     * binaries remain compatible through the original row selector.
     */
    public static int[] selectPositions(byte[] rowStates, int requiredMask, Backend backend) {
        Objects.requireNonNull(rowStates, "rowStates");
        Objects.requireNonNull(backend, "backend");
        requireMask(requiredMask);
        if (backend == Backend.NATIVE_REQUIRED) {
            if (!NativeLibrary.AVAILABLE) {
                throw new UnsatisfiedLinkError("m3_index_mask JNI library unavailable");
            }
            if (NativePositions.AVAILABLE) {
                return selectPositionsNative(rowStates, requiredMask);
            }
            return selectNative(identityPositions(rowStates.length), rowStates, requiredMask);
        }
        return selectPositionsJava(rowStates, requiredMask);
    }

    /**
     * Select logical positions satisfying every required bit and no excluded bit.
     * Existing required-only callers retain their original ABI and fallback behavior.
     * An explicit native request with a nonzero excluded mask requires the additive
     * required/excluded position ABI; older native libraries fail instead of weakening semantics.
     */
    public static int[] selectPositions(byte[] rowStates, int requiredMask,
            int excludedMask, Backend backend) {
        Objects.requireNonNull(rowStates, "rowStates");
        Objects.requireNonNull(backend, "backend");
        requireMask(requiredMask);
        requireMask(excludedMask);
        if (excludedMask == 0) return selectPositions(rowStates, requiredMask, backend);
        if (backend == Backend.NATIVE_REQUIRED) {
            if (!NativeLibrary.AVAILABLE) {
                throw new UnsatisfiedLinkError("m3_index_mask JNI library unavailable");
            }
            if (!NativePositionMasks.AVAILABLE) {
                throw new UnsatisfiedLinkError(
                        "m3_index_mask required/excluded position ABI unavailable");
            }
            return selectPositionsMasksNative(rowStates, requiredMask, excludedMask);
        }
        if ((requiredMask & excludedMask) != 0) return new int[0];
        return selectPositionsJava(rowStates, requiredMask, excludedMask);
    }

    /**
     * One-pass, entryless paging across an owner's byte-state lane.
     * Writes ascending absolute physical positions into the caller's reused output
     * buffer. The high 32 bits of the result contain the next absolute position;
     * the low 32 bits contain the number of positions written. Resume with
     * {@code first = (int)(result >>> 32)} and a remaining bounded window.
     * Input states must remain stable for the call. No backing arrays or entries
     * are allocated, published, cached or retained by this method.
     *
     * <p>A nonempty window requires at least one available output slot to ensure
     * progress. All Java arguments are validated before writing any output.
     * NATIVE_REQUIRED refuses older JNI binaries instead of silently falling back.
     */
    public static long selectPositionsPage(byte[] states, int first, int length,
            int requiredMask, int excludedMask, int[] output, int outputOffset,
            int capacity, Backend backend) {
        Objects.requireNonNull(states, "states");
        Objects.requireNonNull(output, "output");
        Objects.requireNonNull(backend, "backend");
        Objects.checkFromIndexSize(first, length, states.length);
        Objects.checkFromIndexSize(outputOffset, capacity, output.length);
        requireMask(requiredMask);
        requireMask(excludedMask);
        if (length != 0 && capacity == 0) {
            throw new IllegalArgumentException("nonempty page requires output capacity");
        }
        if (backend == Backend.NATIVE_REQUIRED) {
            if (!NativeLibrary.AVAILABLE || !NativePositionPage.AVAILABLE) {
                throw new UnsatisfiedLinkError("m3_index_mask position-page JNI ABI unavailable");
            }
            return selectPositionsPageNative(states, first, length,
                    requiredMask, excludedMask, output, outputOffset, capacity);
        }
        return selectPositionsPageJava(states, first, length,
                requiredMask, excludedMask, output, outputOffset, capacity);
    }

    /** Java oracle for exact native parity, with no per-entry or per-page result allocation. */
    static long selectPositionsPageJava(byte[] states, int first, int length,
            int requiredMask, int excludedMask, int[] output, int outputOffset,
            int capacity) {
        int end = first + length;
        if ((requiredMask & excludedMask) != 0) return (long) end << 32;
        int written = 0;
        for (int position = first; position < end; position++) {
            if (matches(states[position], requiredMask, excludedMask)) {
                output[outputOffset + written++] = position;
                if (written == capacity) {
                    return ((long) (position + 1) << 32) | (written & 0xffff_ffffL);
                }
            }
        }
        return ((long) end << 32) | (written & 0xffff_ffffL);
    }

    /** Optional additive native ABI; legacy m3_collections_mask binaries still load. */
    private static final class NativePositionPage {
        private static final boolean AVAILABLE = probe();
        private static boolean probe() {
            try {
                return positionsPageAbiNative() == 1;
            } catch (UnsatisfiedLinkError legacy) {
                return false;
            }
        }
    }

    private static native int positionsPageAbiNative();

    private static native long selectPositionsPageNative(byte[] states, int first,
            int length, int requiredMask, int excludedMask, int[] output,
            int outputOffset, int capacity);

    /**
     * Internal native selection over an owner's stable half-open index window.
     * States describe logical positions, not physical row IDs. This never retains inputs.
     * Old binaries keep using their original native selector and defensive window copy;
     * an absent library still fails, and a native request never falls back to Java.
     */
    static int[] selectNativeRange(int[] orderedRows, int first, int length,
            byte[] rowStates, int requiredMask) {
        Objects.requireNonNull(orderedRows, "orderedRows");
        Objects.checkFromIndexSize(first, length, orderedRows.length);
        validate(rowStates, length, requiredMask);
        if (!NativeLibrary.AVAILABLE) {
            throw new UnsatisfiedLinkError("m3_index_mask JNI library unavailable");
        }
        if (NativeRange.AVAILABLE) {
            return selectRangeNative(orderedRows, rowStates, first, length, requiredMask);
        }
        return selectNative(java.util.Arrays.copyOfRange(orderedRows, first, first + length),
                rowStates, requiredMask);
    }

    /** Count set-flag matches without boxing, copying, or retaining a view. */
    public static int selectedCount(byte[] rowStates, int requiredMask) {
        Objects.requireNonNull(rowStates, "rowStates");
        requireMask(requiredMask);
        return countSelected(rowStates, requiredMask);
    }

    /**
     * Explicit backend for count-only state-mask queries; no row-ID array or
     * selected-position array is materialized.
     *
     * <p>The two-argument {@link #selectedCount(byte[], int)} remains the
     * Java-only compatibility API. A required native backend never falls back
     * silently: a library built before this additive JNI entry point fails
     * with an explicit ABI diagnostic while older selector calls still work.
     * The state lane must remain unchanged during this operation.
     */
    public static int selectedCount(byte[] rowStates, int requiredMask, Backend backend) {
        Objects.requireNonNull(rowStates, "rowStates");
        Objects.requireNonNull(backend, "backend");
        requireMask(requiredMask);
        if (backend == Backend.NATIVE_REQUIRED) {
            if (!NativeLibrary.AVAILABLE) {
                throw new UnsatisfiedLinkError("m3_index_mask JNI library unavailable");
            }
            try {
                return selectedCountNative(rowStates, requiredMask);
            } catch (UnsatisfiedLinkError olderBinary) {
                UnsatisfiedLinkError failure = new UnsatisfiedLinkError(
                        "m3_collections_mask native count ABI unavailable");
                failure.initCause(olderBinary);
                throw failure;
            }
        }
        return countSelected(rowStates, requiredMask);
    }

    /** Count required/excluded matches without allocating an index array. */
    public static int selectedCount(byte[] rowStates, int requiredMask, int excludedMask) {
        Objects.requireNonNull(rowStates, "rowStates");
        requireMask(requiredMask);
        requireMask(excludedMask);
        if ((requiredMask & excludedMask) != 0) return 0;
        return countSelected(rowStates, requiredMask, excludedMask);
    }

    /** Primitive-long index selection, preserving source order and duplicate IDs. */
    public static long[] selectLong(long[] orderedRows, byte[] rowStates, int requiredMask) {
        Objects.requireNonNull(orderedRows, "orderedRows");
        validate(rowStates, orderedRows.length, requiredMask);
        return selectLongWindowJava(orderedRows, 0, rowStates, 0, orderedRows.length, requiredMask);
    }

    /**
     * Primitive-long selection requiring every included flag and no excluded flag.
     * Existing required-only APIs remain byte-for-byte unchanged. A conflicting
     * mask returns an empty detached result; both inputs must remain stable.
     */
    public static long[] selectLong(long[] orderedRows, byte[] rowStates,
            int requiredMask, int excludedMask) {
        Objects.requireNonNull(orderedRows, "orderedRows");
        validate(rowStates, orderedRows.length, requiredMask);
        requireMask(excludedMask);
        if (excludedMask == 0) return selectLong(orderedRows, rowStates, requiredMask);
        if ((requiredMask & excludedMask) != 0) return new long[0];
        int count = countSelected(rowStates, requiredMask, excludedMask);
        if (count == orderedRows.length) return orderedRows.clone();
        long[] selected = new long[count];
        if (selected.length == 0) return selected; // Stable input: no emission pass is needed.
        int written = 0;
        for (int index = 0; index < orderedRows.length; index++) {
            if (matches(rowStates[index], requiredMask, excludedMask)) {
                if (written == count) {
                    throw new ConcurrentModificationException("aligned selection states changed");
                }
                selected[written++] = orderedRows[index];
            }
        }
        if (written != count) {
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
        return matches(state, requiredMask, 0);
    }

    static boolean matches(byte state, int requiredMask, int excludedMask) {
        int bits = state & 0xff;
        return (bits & requiredMask) == requiredMask && (bits & excludedMask) == 0;
    }

    private static int countSelected(byte[] states, int requiredMask) {
        return countWindow(states, 0, states.length, requiredMask);
    }

    private static int countSelected(byte[] states, int requiredMask, int excludedMask) {
        if ((requiredMask | excludedMask) == 0) return states.length;
        int kept = 0;
        for (byte state : states) {
            if (matches(state, requiredMask, excludedMask)) kept++;
        }
        return kept;
    }

    /**
     * Java reference kernel: exactly one final primitive array, no O(n) scratch
     * or per-element object. Caller must keep both input arrays stable during use.
     */
    static int[] selectJava(int[] orderedRows, byte[] rowStates, int requiredMask) {
        return selectIntWindowJava(orderedRows, 0, rowStates, 0, orderedRows.length, requiredMask);
    }

    /** Exact-sized logical-position selection; no O(n) identity input scratch. */
    static int[] selectPositionsJava(byte[] rowStates, int requiredMask) {
        return selectPositionsJava(rowStates, requiredMask, 0);
    }

    static int[] selectPositionsJava(byte[] rowStates, int requiredMask, int excludedMask) {
        int selectedCount = countSelected(rowStates, requiredMask, excludedMask);
        if (selectedCount == rowStates.length) return identityPositions(rowStates.length);
        int[] selected = new int[selectedCount];
        if (selected.length == 0) return selected; // Stable input: no emission pass is needed.
        int written = 0;
        for (int i = 0; i < rowStates.length; i++) {
            if (matches(rowStates[i], requiredMask, excludedMask)) {
                if (written == selected.length) {
                    throw new ConcurrentModificationException("aligned selection states changed");
                }
                selected[written++] = i;
            }
        }
        if (written != selected.length) {
            throw new ConcurrentModificationException("aligned selection states changed");
        }
        return selected;
    }

    private static int[] identityPositions(int length) {
        int[] positions = new int[length];
        for (int i = 0; i < length; i++) positions[i] = i;
        return positions;
    }

    /**
     * Select a bounded logical window with independently validated offsets into
     * the dynamic row and state lanes; no input slices or per-row objects.
     * The detached result preserves source encounter order and duplicate IDs.
     * Both inputs must remain stable during this call.
     */
    public static int[] selectWindow(int[] orderedRows, int rowOffset,
                                     byte[] states, int stateOffset, int length,
                                     int requiredMask, Backend backend) {
        Objects.requireNonNull(orderedRows, "orderedRows");
        Objects.requireNonNull(states, "states");
        Objects.requireNonNull(backend, "backend");
        Objects.checkFromIndexSize(rowOffset, length, orderedRows.length);
        Objects.checkFromIndexSize(stateOffset, length, states.length);
        requireMask(requiredMask);
        if (backend == Backend.NATIVE_REQUIRED) {
            if (!NativeLibrary.AVAILABLE) {
                throw new UnsatisfiedLinkError("m3_index_mask JNI library unavailable");
            }
            return selectWindowNative(orderedRows, rowOffset, states, stateOffset,
                    length, requiredMask);
        }
        return selectIntWindowJava(orderedRows, rowOffset, states, stateOffset, length, requiredMask);
    }

    /** Primitive-long counterpart, preserving signed extremes and duplicates. */
    public static long[] selectLongWindow(long[] orderedRows, int rowOffset,
                                          byte[] states, int stateOffset, int length,
                                          int requiredMask, Backend backend) {
        Objects.requireNonNull(orderedRows, "orderedRows");
        Objects.requireNonNull(states, "states");
        Objects.requireNonNull(backend, "backend");
        Objects.checkFromIndexSize(rowOffset, length, orderedRows.length);
        Objects.checkFromIndexSize(stateOffset, length, states.length);
        requireMask(requiredMask);
        if (backend == Backend.NATIVE_REQUIRED) {
            if (!NativeLibrary.AVAILABLE) {
                throw new UnsatisfiedLinkError("m3_index_mask JNI library unavailable");
            }
            return selectLongWindowNative(orderedRows, rowOffset, states, stateOffset,
                    length, requiredMask);
        }
        return selectLongWindowJava(orderedRows, rowOffset, states, stateOffset, length, requiredMask);
    }

    /** Existing checked callers share one stable int emission atom; no retained inputs. */
    private static int[] selectIntWindowJava(int[] orderedRows, int rowOffset,
            byte[] states, int stateOffset, int length, int requiredMask) {
        int kept = countWindow(states, stateOffset, length, requiredMask);
        if (kept == length) {
            return java.util.Arrays.copyOfRange(orderedRows, rowOffset, rowOffset + length);
        }
        int[] selected = new int[kept];
        if (selected.length == 0) return selected; // Stable input: no emission pass is needed.
        int written = 0;
        for (int i = 0; i < length; i++) {
            if (matches(states[stateOffset + i], requiredMask)) {
                if (written == kept) {
                    throw new ConcurrentModificationException("aligned selection states changed");
                }
                selected[written++] = orderedRows[rowOffset + i];
            }
        }
        if (written != kept) {
            throw new ConcurrentModificationException("aligned selection states changed");
        }
        return selected;
    }

    /** Existing checked callers share one stable long emission atom; no retained inputs. */
    private static long[] selectLongWindowJava(long[] orderedRows, int rowOffset,
            byte[] states, int stateOffset, int length, int requiredMask) {
        int kept = countWindow(states, stateOffset, length, requiredMask);
        if (kept == length) {
            return java.util.Arrays.copyOfRange(orderedRows, rowOffset, rowOffset + length);
        }
        long[] selected = new long[kept];
        if (selected.length == 0) return selected; // Stable input: no emission pass is needed.
        int written = 0;
        for (int i = 0; i < length; i++) {
            if (matches(states[stateOffset + i], requiredMask)) {
                if (written == kept) {
                    throw new ConcurrentModificationException("aligned selection states changed");
                }
                selected[written++] = orderedRows[rowOffset + i];
            }
        }
        if (written != kept) {
            throw new ConcurrentModificationException("aligned selection states changed");
        }
        return selected;
    }

    /**
     * Filter a bounded parent-owned int row window with required AND excluded state
     * bits. Row and state offsets are independent; no aligned-state copy or boxed
     * predicate is constructed. The detached output preserves every surviving
     * occurrence, signed row ID and encounter order of the existing row lane.
     *
     * <p>Existing required-only API and JNI symbols remain unchanged. Explicit
     * native selection requires the additive window-mask ABI; an old binary
     * throws instead of silently ignoring excluded bits or falling back to Java.
     * Source arrays must stay stable for the duration of both selection passes.
     */
    public static int[] selectWindow(int[] orderedRows, int rowOffset,
            byte[] states, int stateOffset, int length,
            int requiredMask, int excludedMask, Backend backend) {
        Objects.requireNonNull(orderedRows, "orderedRows");
        Objects.requireNonNull(states, "states");
        Objects.requireNonNull(backend, "backend");
        Objects.checkFromIndexSize(rowOffset, length, orderedRows.length);
        Objects.checkFromIndexSize(stateOffset, length, states.length);
        requireMask(requiredMask);
        requireMask(excludedMask);
        if (excludedMask == 0) {
            return selectWindow(orderedRows, rowOffset, states, stateOffset,
                    length, requiredMask, backend);
        }
        if (backend == Backend.NATIVE_REQUIRED) {
            requireWindowMasksNative();
            return selectWindowMasksNative(orderedRows, rowOffset,
                    states, stateOffset, length, requiredMask, excludedMask);
        }
        if ((requiredMask & excludedMask) != 0) return new int[0];
        int kept = countWindowMasks(states, stateOffset, length,
                requiredMask, excludedMask);
        if (kept == length) {
            return java.util.Arrays.copyOfRange(orderedRows, rowOffset, rowOffset + length);
        }
        int[] selected = new int[kept];
        if (selected.length == 0) return selected; // Stable input: no emission pass is needed.
        int written = 0;
        for (int i = 0; i < length; i++) {
            if (matches(states[stateOffset + i], requiredMask, excludedMask)) {
                if (written == kept) {
                    throw new ConcurrentModificationException("aligned selection states changed");
                }
                selected[written++] = orderedRows[rowOffset + i];
            }
        }
        if (written != kept) {
            throw new ConcurrentModificationException("aligned selection states changed");
        }
        return selected;
    }

    /** Same independent-offset state-mask contract for primitive long rows. */
    public static long[] selectLongWindow(long[] orderedRows, int rowOffset,
            byte[] states, int stateOffset, int length,
            int requiredMask, int excludedMask, Backend backend) {
        Objects.requireNonNull(orderedRows, "orderedRows");
        Objects.requireNonNull(states, "states");
        Objects.requireNonNull(backend, "backend");
        Objects.checkFromIndexSize(rowOffset, length, orderedRows.length);
        Objects.checkFromIndexSize(stateOffset, length, states.length);
        requireMask(requiredMask);
        requireMask(excludedMask);
        if (excludedMask == 0) {
            return selectLongWindow(orderedRows, rowOffset, states, stateOffset,
                    length, requiredMask, backend);
        }
        if (backend == Backend.NATIVE_REQUIRED) {
            requireWindowMasksNative();
            return selectLongWindowMasksNative(orderedRows, rowOffset,
                    states, stateOffset, length, requiredMask, excludedMask);
        }
        if ((requiredMask & excludedMask) != 0) return new long[0];
        int kept = countWindowMasks(states, stateOffset, length,
                requiredMask, excludedMask);
        if (kept == length) {
            return java.util.Arrays.copyOfRange(orderedRows, rowOffset, rowOffset + length);
        }
        long[] selected = new long[kept];
        if (selected.length == 0) return selected; // Stable input: no emission pass is needed.
        int written = 0;
        for (int i = 0; i < length; i++) {
            if (matches(states[stateOffset + i], requiredMask, excludedMask)) {
                if (written == kept) {
                    throw new ConcurrentModificationException("aligned selection states changed");
                }
                selected[written++] = orderedRows[rowOffset + i];
            }
        }
        if (written != kept) {
            throw new ConcurrentModificationException("aligned selection states changed");
        }
        return selected;
    }

    private static int countWindowMasks(byte[] states, int offset, int length,
            int requiredMask, int excludedMask) {
        if ((requiredMask | excludedMask) == 0) return length;
        int kept = 0;
        for (int i = 0; i < length; i++) {
            if (matches(states[offset + i], requiredMask, excludedMask)) kept++;
        }
        return kept;
    }

    private static void requireWindowMasksNative() {
        if (!NativeLibrary.AVAILABLE) {
            throw new UnsatisfiedLinkError("m3_index_mask JNI library unavailable");
        }
        if (!NativeWindowMasks.AVAILABLE) {
            throw new UnsatisfiedLinkError(
                    "m3_index_mask required/excluded window ABI unavailable");
        }
    }

    private static final class NativeWindowMasks {
        private static final boolean AVAILABLE = available();

        private static boolean available() {
            try {
                return windowMasksAbiNative() == 1;
            } catch (UnsatisfiedLinkError oldNativeLibrary) {
                return false;
            }
        }
    }

    private static native int windowMasksAbiNative();

    private static native int[] selectWindowMasksNative(int[] rows, int rowOffset,
            byte[] states, int stateOffset, int length, int required, int excluded);

    private static native long[] selectLongWindowMasksNative(long[] rows, int rowOffset,
            byte[] states, int stateOffset, int length, int required, int excluded);

    /** Zero-mask selection is known without reading any state words. */
    private static int countWindow(byte[] states, int offset, int length, int mask) {
        if (mask == 0) return length;
        int kept = 0;
        for (int i = 0; i < length; i++) {
            if (matches(states[offset + i], mask)) kept++;
        }
        return kept;
    }

    private static native int[] selectWindowNative(int[] rows, int rowOffset,
            byte[] states, int stateOffset, int length, int mask);

    private static native long[] selectLongWindowNative(long[] rows, int rowOffset,
            byte[] states, int stateOffset, int length, int mask);

    //FLLDCIM: MRL - union of the Synexia M3IndexMaskCompactor API (count, firstEqualBits) for the PrimitiveBig* absorption
    /**
     * Counts rows whose state has every {@code requiredMask} bit, under the Synexia name; the same contract as
     * {@link #selectedCount(byte[], int, Backend)}.
     */
    public static int count(byte[] rowStates, int requiredMask, Backend backend) {
        return selectedCount(rowStates, requiredMask, backend);
    }

    /**
     * Finds the first equality-normalized primitive value in one bounded physical page range.
     *
     * <p>The result is an absolute index within {@code values}, or {@code -1} when absent. BOOLEAN uses byte storage
     * with nonzero meaning true; integral carriers narrow as Java casts do; floating NaNs compare equal and
     * positive/negative zero remain distinct. The input array is neither mutated nor retained and must not change
     * during the call. JAVA performs the typed scan; NATIVE_REQUIRED needs the optional library and its
     * {@code firstEqualBits0} symbol and never silently falls back.
     *
     * @throws NullPointerException when kind, values or backend is null
     * @throws IllegalArgumentException when the array type does not match kind
     * @throws IndexOutOfBoundsException when from/count do not describe a valid array range
     * @throws UnsatisfiedLinkError when the requested native library or symbol is unavailable
     */
    public static int firstEqualBits(M3PrimitiveKind kind, Object values, int from, int count,
                                     long bits, Backend backend) {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(values, "values");
        Objects.requireNonNull(backend, "backend");
        kind.checkRange(values, from, count);
        if (backend == Backend.NATIVE_REQUIRED) {
            if (!NativeLibrary.AVAILABLE) {
                throw new UnsatisfiedLinkError("m3_index_mask JNI library unavailable");
            }
            return firstEqualBits0(kind.ordinal(), values, from, count, bits);
        }
        return kind.indexOfBits(values, from, from + count, bits);
    }

    /*
     * JNI ABI kind IDs are the M3PrimitiveKind declaration order (identical to the Synexia PrimitiveKind order):
     * BOOLEAN=0, BYTE=1, SHORT=2, CHAR=3, INT=4, LONG=5, FLOAT=6, DOUBLE=7.
     */
    private static native int firstEqualBits0(int kind, Object values, int from, int count, long bits);

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

    /** Lazy capability probe keeps old JNI binaries compatible with position selection. */
    private static final class NativePositions {
        private static final boolean AVAILABLE = available();
        private static boolean available() {
            try {
                return positionsAbiNative() == 1;
            } catch (UnsatisfiedLinkError legacyBinary) {
                return false;
            }
        }
    }

    private static native int positionsAbiNative();

    private static native int[] selectPositionsNative(byte[] rowStates, int requiredMask);

    /** Lazy probe for the additive required/excluded logical-position ABI. */
    private static final class NativePositionMasks {
        private static final boolean AVAILABLE = available();
        private static boolean available() {
            try {
                return positionsMasksAbiNative() == 1;
            } catch (UnsatisfiedLinkError legacyBinary) {
                return false;
            }
        }
    }

    private static native int positionsMasksAbiNative();

    private static native int[] selectPositionsMasksNative(
            byte[] rowStates, int requiredMask, int excludedMask);

    /** Lazy probing does not change initialization of the legacy public native API. */
    private static final class NativeRange {
        private static final boolean AVAILABLE = available();
        private static boolean available() {
            try {
                return rangeAbiNative() == 1;
            } catch (UnsatisfiedLinkError legacyBinary) {
                return false;
            }
        }
    }

    private static native int rangeAbiNative();

    private static native int[] selectRangeNative(int[] orderedRows, byte[] rowStates,
            int first, int length, int requiredMask);

    private static native int selectedCountNative(byte[] states, int requiredMask);

    private static native int[] selectNative(int[] orderedRows, byte[] rowStates,
                                              int requiredMask);

    //FLLDCIM: RELOCATE-MERGE begin - donor blob c4f8c5865932 at ac5feec00a6d; recipe m3/collections/recipe/entryless-lane-kernel-20261106/relocate.py v3
    public static final int LIVE = 1;

    public static boolean laneKernelNativeAvailable() { return NativeLaneKernel.AVAILABLE; }

    /**
     * Select occupied slots with key/value in inclusive signed ranges.
     * No keys or values are moved, no pairs or facade objects are created.
     * Input arrays are only borrowed, never retained, and may be longer than
     * the requested window. Concurrency requires external synchronization.
     *
     * @throws NullPointerException if any lane is null
     * @throws IndexOutOfBoundsException for an invalid window or mismatched lanes
     * @throws IllegalArgumentException for inverted key/value ranges
     */
    public static int[] select(long[] keys, long[] values, byte[] state,
            int start, int count, long keyMin, long keyMax, long valueMin, long valueMax) {
        validate(keys, values, state, start, count, keyMin, keyMax, valueMin, valueMax);
        if (count == 0) return new int[0];
        return NativeLaneKernel.AVAILABLE
                ? nativeSelect(keys, values, state, start, count, keyMin, keyMax, valueMin, valueMax)
                : selectJavaValidated(keys, values, state, start, count,
                        keyMin, keyMax, valueMin, valueMax);
    }

    /**
     * Write matching physical slots to a reusable output lane and return the number written.
     * Insufficient capacity fails before any output write for stable inputs. The caller
     * must keep all input lanes stable throughout both selection passes. This method
     * never retains arrays and creates no Entry, Node or Cell per mapping.
     */
    public static int selectInto(long[] keys, long[] values, byte[] state,
            int start, int count, long keyMin, long keyMax, long valueMin, long valueMax,
            int[] output, int outputStart) {
        validate(keys, values, state, start, count, keyMin, keyMax, valueMin, valueMax);
        validateOutput(output, outputStart);
        return NativeLaneKernel.AVAILABLE
                ? nativeSelectInto(keys, values, state, start, count,
                        keyMin, keyMax, valueMin, valueMax, output, outputStart)
                : selectJavaIntoValidated(keys, values, state, start, count,
                        keyMin, keyMax, valueMin, valueMax, output, outputStart);
    }

    /** Java-only, allocation-free oracle for caller-owned reusable output arrays. */
    public static int selectIntoJava(long[] keys, long[] values, byte[] state,
            int start, int count, long keyMin, long keyMax, long valueMin, long valueMax,
            int[] output, int outputStart) {
        validate(keys, values, state, start, count, keyMin, keyMax, valueMin, valueMax);
        validateOutput(output, outputStart);
        return selectJavaIntoValidated(keys, values, state, start, count,
                keyMin, keyMax, valueMin, valueMax, output, outputStart);
    }

    /** JNI-only variant for native-runtime qualification; never silently falls back. */
    public static int selectIntoNativeRequired(long[] keys, long[] values, byte[] state,
            int start, int count, long keyMin, long keyMax, long valueMin, long valueMax,
            int[] output, int outputStart) {
        validate(keys, values, state, start, count, keyMin, keyMax, valueMin, valueMax);
        validateOutput(output, outputStart);
        if (!NativeLaneKernel.AVAILABLE) throw new UnsatisfiedLinkError("DOOPS native lane candidate is not loaded");
        return nativeSelectInto(keys, values, state, start, count,
                keyMin, keyMax, valueMin, valueMax, output, outputStart);
    }

    /**
     * Scan a bounded page of matching physical slots, without Entry objects or result allocations.
     * The high 32 bits of the result are the number written; the low 32 bits contain
     * the absolute next scan position (equal to {@code start + count} when exhausted).
     * Resume by passing that position and the remaining count in the same original window.
     * A nonempty input window requires at least one available output position. Traversal
     * is stable and never relocates or mutates input lanes. The caller must externally
     * synchronize all lanes across page calls and must not recycle slot identities.
     *
     * <p>Unlike two-pass {@link #selectInto}, this performs one bounded input scan and
     * immediately stops on filling the destination. The caller retains its output array.
     */
    public static long scanPageInto(long[] keys, long[] values, byte[] state,
            int start, int count, long keyMin, long keyMax, long valueMin, long valueMax,
            int[] output, int outputStart) {
        validate(keys, values, state, start, count, keyMin, keyMax, valueMin, valueMax);
        validatePageOutput(output, outputStart, count);
        return NativeLaneKernel.AVAILABLE
                ? nativeScanPageInto(keys, values, state, start, count,
                        keyMin, keyMax, valueMin, valueMax, output, outputStart)
                : scanPageJavaValidated(keys, values, state, start, count,
                        keyMin, keyMax, valueMin, valueMax, output, outputStart);
    }

    /** Java-only single-pass paged selection oracle, even when JNI is loaded. */
    public static long scanPageIntoJava(long[] keys, long[] values, byte[] state,
            int start, int count, long keyMin, long keyMax, long valueMin, long valueMax,
            int[] output, int outputStart) {
        validate(keys, values, state, start, count, keyMin, keyMax, valueMin, valueMax);
        validatePageOutput(output, outputStart, count);
        return scanPageJavaValidated(keys, values, state, start, count,
                keyMin, keyMax, valueMin, valueMax, output, outputStart);
    }

    /** JNI-only paged selection; requires a successfully loaded native library. */
    public static long scanPageIntoNativeRequired(long[] keys, long[] values, byte[] state,
            int start, int count, long keyMin, long keyMax, long valueMin, long valueMax,
            int[] output, int outputStart) {
        validate(keys, values, state, start, count, keyMin, keyMax, valueMin, valueMax);
        validatePageOutput(output, outputStart, count);
        if (!NativeLaneKernel.AVAILABLE) throw new UnsatisfiedLinkError("DOOPS native lane candidate is not loaded");
        return nativeScanPageInto(keys, values, state, start, count,
                keyMin, keyMax, valueMin, valueMax, output, outputStart);
    }

    /** Number of physical slot indices written by a paged scan. */
    public static int pageWritten(long result) { return (int) (result >>> 32); }

    /** Absolute next input position; equals window end on exhaustion. */
    public static int pageNext(long result) { return (int) result; }

    /** Force Java-only selection as the differential oracle, even with JNI loaded. */
    public static int[] selectJava(long[] keys, long[] values, byte[] state,
            int start, int count, long keyMin, long keyMax, long valueMin, long valueMax) {
        validate(keys, values, state, start, count, keyMin, keyMax, valueMin, valueMax);
        return selectJavaValidated(keys, values, state, start, count,
                keyMin, keyMax, valueMin, valueMax);
    }

    /** Count matching occupied slots without allocating a result index array. */
    public static int countMatching(long[] keys, long[] values, byte[] state,
            int start, int count, long keyMin, long keyMax, long valueMin, long valueMax) {
        validate(keys, values, state, start, count, keyMin, keyMax, valueMin, valueMax);
        return NativeLaneKernel.AVAILABLE
                ? nativeCount(keys, values, state, start, count, keyMin, keyMax, valueMin, valueMax)
                : countJavaValidated(keys, values, state, start, count,
                        keyMin, keyMax, valueMin, valueMax);
    }

    /** Return the first matching physical slot, or -1; no Map.Entry or result array. */
    public static int firstMatching(long[] keys, long[] values, byte[] state,
            int start, int count, long keyMin, long keyMax, long valueMin, long valueMax) {
        validate(keys, values, state, start, count, keyMin, keyMax, valueMin, valueMax);
        return NativeLaneKernel.AVAILABLE
                ? nativeFirst(keys, values, state, start, count, keyMin, keyMax, valueMin, valueMax)
                : firstJavaValidated(keys, values, state, start, count,
                        keyMin, keyMax, valueMin, valueMax);
    }

    /** Deterministic allocation-free Java count oracle even when native is loaded. */
    public static int countMatchingJava(long[] keys, long[] values, byte[] state,
            int start, int count, long keyMin, long keyMax, long valueMin, long valueMax) {
        validate(keys, values, state, start, count, keyMin, keyMax, valueMin, valueMax);
        return countJavaValidated(keys, values, state, start, count,
                keyMin, keyMax, valueMin, valueMax);
    }

    /** Deterministic early-exit Java search oracle even when native is loaded. */
    public static int firstMatchingJava(long[] keys, long[] values, byte[] state,
            int start, int count, long keyMin, long keyMax, long valueMin, long valueMax) {
        validate(keys, values, state, start, count, keyMin, keyMax, valueMin, valueMax);
        return firstJavaValidated(keys, values, state, start, count,
                keyMin, keyMax, valueMin, valueMax);
    }

    /** Count via real JNI or refuse when no native library is loaded. */
    public static int countMatchingNativeRequired(long[] keys, long[] values, byte[] state,
            int start, int count, long keyMin, long keyMax, long valueMin, long valueMax) {
        validate(keys, values, state, start, count, keyMin, keyMax, valueMin, valueMax);
        if (!NativeLaneKernel.AVAILABLE) throw new UnsatisfiedLinkError("DOOPS native lane candidate is not loaded");
        return nativeCount(keys, values, state, start, count, keyMin, keyMax, valueMin, valueMax);
    }

    /** Early-exit slot search via real JNI or fail closed when not loaded. */
    public static int firstMatchingNativeRequired(long[] keys, long[] values, byte[] state,
            int start, int count, long keyMin, long keyMax, long valueMin, long valueMax) {
        validate(keys, values, state, start, count, keyMin, keyMax, valueMin, valueMax);
        if (!NativeLaneKernel.AVAILABLE) throw new UnsatisfiedLinkError("DOOPS native lane candidate is not loaded");
        return nativeFirst(keys, values, state, start, count, keyMin, keyMax, valueMin, valueMax);
    }

    /** Fail closed rather than quietly falling back when native evidence is required. */
    public static int[] selectNativeRequired(long[] keys, long[] values, byte[] state,
            int start, int count, long keyMin, long keyMax, long valueMin, long valueMax) {
        validate(keys, values, state, start, count, keyMin, keyMax, valueMin, valueMax);
        if (!NativeLaneKernel.AVAILABLE) throw new UnsatisfiedLinkError("DOOPS native lane candidate is not loaded");
        return nativeSelect(keys, values, state, start, count, keyMin, keyMax, valueMin, valueMax);
    }

    private static void validate(long[] keys, long[] values, byte[] state,
            int start, int count, long keyMin, long keyMax, long valueMin, long valueMax) {
        Objects.requireNonNull(keys, "keys");
        Objects.requireNonNull(values, "values");
        Objects.requireNonNull(state, "state");
        if (keyMin > keyMax || valueMin > valueMax) {
            throw new IllegalArgumentException("inverted lane filter bounds");
        }
        // Subtract before comparing to avoid start + count integer overflow.
        if (start < 0 || count < 0 || start > keys.length || count > keys.length - start
                || start > values.length || count > values.length - start
                || start > state.length || count > state.length - start) {
            throw new IndexOutOfBoundsException("lane window exceeds backing arrays");
        }
    }

    private static void validateOutput(int[] output, int outputStart) {
        Objects.requireNonNull(output, "output");
        if (outputStart < 0 || outputStart > output.length) {
            throw new IndexOutOfBoundsException("invalid output start");
        }
    }

    private static void validatePageOutput(int[] output, int outputStart, int count) {
        validateOutput(output, outputStart);
        if (count != 0 && outputStart == output.length) {
            throw new IndexOutOfBoundsException("nonempty page requires output capacity");
        }
    }

    private static long pageResult(int next, int written) {
        return ((long) written << 32) | (next & 0xffffffffL);
    }

    private static long scanPageJavaValidated(long[] keys, long[] values, byte[] state,
            int start, int count, long keyMin, long keyMax, long valueMin, long valueMax,
            int[] output, int outputStart) {
        final int capacity = output.length - outputStart;
        final int end = start + count;
        int written = 0;
        for (int pos = start; pos < end; pos++) {
            if ((state[pos] & LIVE) == 0 || keys[pos] < keyMin || keys[pos] > keyMax
                    || values[pos] < valueMin || values[pos] > valueMax) continue;
            output[outputStart + written++] = pos;
            if (written == capacity) return pageResult(pos + 1, written);
        }
        return pageResult(end, written);
    }

    private static int selectJavaIntoValidated(long[] keys, long[] values, byte[] state,
            int start, int count, long keyMin, long keyMax, long valueMin, long valueMax,
            int[] output, int outputStart) {
        int required = countJavaValidated(keys, values, state, start, count,
                keyMin, keyMax, valueMin, valueMax);
        if (required > output.length - outputStart) {
            throw new IndexOutOfBoundsException("insufficient output capacity");
        }
        int write = outputStart;
        for (int i = start, end = start + count; i < end; i++) {
            if ((state[i] & LIVE) != 0 && keys[i] >= keyMin && keys[i] <= keyMax
                    && values[i] >= valueMin && values[i] <= valueMax) output[write++] = i;
        }
        if (write - outputStart != required) {
            throw new java.util.ConcurrentModificationException("lane selection changed during scan");
        }
        return required;
    }

    private static int[] selectJavaValidated(long[] keys, long[] values, byte[] state,
            int start, int count, long keyMin, long keyMax, long valueMin, long valueMax) {
        int[] matching = null;
        int length = 0;
        final int end = start + count; // validated without overflow above
        for (int i = start; i < end; i++) {
            if ((state[i] & LIVE) == 0 || keys[i] < keyMin || keys[i] > keyMax
                    || values[i] < valueMin || values[i] > valueMax) continue;
            if (matching == null) {
                matching = new int[Math.min(count, 32)];
            } else if (length == matching.length) {
                matching = Arrays.copyOf(matching, Math.min(count,
                        matching.length <= (count >>> 1) ? Math.max(1, matching.length << 1) : count));
            }
            matching[length++] = i;
        }
        if (matching == null) return new int[0];
        return length == matching.length ? matching : Arrays.copyOf(matching, length);
    }

    private static int countJavaValidated(long[] keys, long[] values, byte[] state,
            int start, int count, long keyMin, long keyMax, long valueMin, long valueMax) {
        int total = 0;
        for (int i = start, end = start + count; i < end; i++) {
            if ((state[i] & LIVE) != 0 && keys[i] >= keyMin && keys[i] <= keyMax
                    && values[i] >= valueMin && values[i] <= valueMax) total++;
        }
        return total;
    }

    private static int firstJavaValidated(long[] keys, long[] values, byte[] state,
            int start, int count, long keyMin, long keyMax, long valueMin, long valueMax) {
        for (int i = start, end = start + count; i < end; i++) {
            if ((state[i] & LIVE) != 0 && keys[i] >= keyMin && keys[i] <= keyMax
                    && values[i] >= valueMin && values[i] <= valueMax) return i;
        }
        return -1;
    }


    private static native long nativeScanPageInto(long[] keys, long[] values, byte[] state,
            int start, int count, long keyMin, long keyMax, long valueMin, long valueMax,
            int[] output, int outputStart);

    private static native int nativeCount(long[] keys, long[] values, byte[] state,
            int start, int count, long keyMin, long keyMax, long valueMin, long valueMax);

    private static native int nativeFirst(long[] keys, long[] values, byte[] state,
            int start, int count, long keyMin, long keyMax, long valueMin, long valueMax);

    private static native int nativeSelectInto(long[] keys, long[] values, byte[] state,
            int start, int count, long keyMin, long keyMax, long valueMin, long valueMax,
            int[] output, int outputStart);

    private static native int[] nativeSelect(long[] keys, long[] values, byte[] state,
            int start, int count, long keyMin, long keyMax, long valueMin, long valueMax);

    /** Lazy probe for the additive key/value signed-range lane kernel ABI; a legacy binary keeps its older exports. */
    private static final class NativeLaneKernel {
        private static final boolean AVAILABLE = available();
        private static boolean available() {
            if (!NativeLibrary.AVAILABLE) return false;
            try {
                return laneKernelAbiNative() == 1;
            } catch (UnsatisfiedLinkError legacyBinary) {
                return false;
            }
        }
    }

    private static native int laneKernelAbiNative();
    //FLLDCIM: RELOCATE-MERGE end - donor blob c4f8c5865932
}
