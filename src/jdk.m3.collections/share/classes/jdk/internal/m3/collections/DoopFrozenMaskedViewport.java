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
package jdk.internal.m3.collections;

import java.util.Arrays;
import java.util.Objects;
import jdk.internal.m3.collections.bits.FrozenRankSelect;

/**
 * Immutable visible-rank projection of parent-owned logical IDs and short flags.
 *
 * <p>This optional DOOP snapshot reuses the canonical {@link FrozenRankSelect}
 * instead of maintaining another Fenwick tree or creating a child facade per row.
 * A one-time O(n) selection pass creates a succinct immutable bit index; subsequent
 * deep visible-rank jumps are O(log(n/512) + 8) and bounded windows cost
 * O(window * (log(n/512) + 8)). No JVM reference or native pointer is retained by JNI.
 *
 * <p>Source arrays must not be mutated concurrently with {@link #prepare}. The
 * prepared projection owns a copy of each logical ID and does not retain caller
 * flag arrays. Subsequent caller mutations cannot change this snapshot. Callers
 * must rebuild it when the authoritative parent generation changes. In particular,
 * no native widget or exposed Java facade can be replaced by a logical ID.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/DoopFrozenMaskedViewport.java}
 * blob {@code 49aed1fc4eba036b2f01eb6504b2a25950915353} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public final class DoopFrozenMaskedViewport {
    /** Maximum selected IDs materialized in one caller-owned output window. */
    public static final int MAX_WINDOW = 4096;

    private static final boolean NATIVE_AVAILABLE = loadNative();

    private final long[] logicalIds;
    private final FrozenRankSelect selected;

    private DoopFrozenMaskedViewport(long[] logicalIds, FrozenRankSelect selected) {
        this.logicalIds = logicalIds;
        this.selected = selected;
    }

    public static boolean nativeAvailable() {
        return NATIVE_AVAILABLE;
    }

    public static DoopFrozenMaskedViewport prepare(long[] ids, short[] flags,
            int count, short required, short excluded) {
        return prepare(ids, flags, count, required, excluded, false);
    }

    /** Explicit opt-in to native mask construction; Java remains the default. */
    public static DoopFrozenMaskedViewport prepare(long[] ids, short[] flags,
            int count, short required, short excluded, boolean preferNative) {
        Objects.requireNonNull(ids, "ids");
        Objects.requireNonNull(flags, "flags");
        if (count < 0 || count > ids.length || count > flags.length) {
            throw new IllegalArgumentException("invalid DOOP snapshot length");
        }
        long[] ownedIds = Arrays.copyOf(ids, count);
        short[] ownedFlags = Arrays.copyOf(flags, count);
        for (int i = 0; i < count; i++) {
            if (ownedIds[i] <= 0L) {
                throw new IllegalArgumentException("invalid logical ID at " + i);
            }
        }
        int wordCount = count == 0 ? 0 : 1 + ((count - 1) >>> 6);
        long[] words = new long[wordCount];
        if (preferNative && NATIVE_AVAILABLE) {
            try {
                nativeBuildMask(ownedFlags, count, required, excluded, words);
            } catch (UnsatisfiedLinkError olderBinary) {
                Arrays.fill(words, 0L);
                buildMaskJava(ownedFlags, count, required, excluded, words);
            }
        } else {
            buildMaskJava(ownedFlags, count, required, excluded, words);
        }
        return new DoopFrozenMaskedViewport(ownedIds, FrozenRankSelect.copyOf(words, count));
    }

    public int sourceRows() {
        return logicalIds.length;
    }

    public int visibleRows() {
        return selected.ones();
    }

    /** Estimated primitive payload bytes, excluding array and object headers. */
    public long payloadBytes() {
        return (long) logicalIds.length * Long.BYTES + selected.payloadBytes();
    }

    /** Count visible rows in source ordinals [0, endExclusive). */
    public int visibleRankBefore(int endExclusive) {
        return selected.rank1(endExclusive);
    }

    /** Resolve a zero-based visible rank to a source ordinal; does not create a facade. */
    public int sourceOrdinalAt(int visibleRank) {
        return selected.select1(visibleRank);
    }

    /** Resolve a zero-based visible rank to the captured, stable logical ID. */
    public long logicalIdAt(int visibleRank) {
        return logicalIds[selected.select1(visibleRank)];
    }

    /**
     * Fill at most {@link #MAX_WINDOW} visible logical IDs in original source order.
     * Validates the entire requested output slice before writing. Unused positions
     * are untouched; an end-of-index or zero-demand call creates no temporary row
     * or facade objects.
     *
     * @return number of IDs written, 0 through {@code maximum}
     */
    public int fillWindow(int firstVisibleRank, int maximum, long[] output, int offset) {
        Objects.requireNonNull(output, "output");
        if (maximum < 0 || maximum > MAX_WINDOW) {
            throw new IllegalArgumentException("invalid DOOP visible window size");
        }
        if (firstVisibleRank < 0 || firstVisibleRank > selected.ones()) {
            throw new IndexOutOfBoundsException(firstVisibleRank);
        }
        int actual = Math.min(maximum, selected.ones() - firstVisibleRank);
        Objects.checkFromIndexSize(offset, actual, output.length);
        for (int i = 0; i < actual; i++) {
            output[offset + i] = logicalIds[selected.select1(firstVisibleRank + i)];
        }
        return actual;
    }

    private static void buildMaskJava(short[] flags, int count, short required,
            short excluded, long[] words) {
        for (int i = 0; i < count; i++) {
            int bits = flags[i];
            if ((bits & required) == required && (bits & excluded) == 0) {
                words[i >>> 6] |= 1L << (i & 63);
            }
        }
    }

    private static boolean loadNative() {
        try {
            System.loadLibrary("m3_doop_frozen_mask");
            return true;
        } catch (UnsatisfiedLinkError | SecurityException unavailable) {
            return false;
        }
    }

    private static native void nativeBuildMask(short[] flags, int count, short required,
            short excluded, long[] outputWords);
}
