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
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections.packed;

import java.util.Objects;

/**
 * Copies a bounded page of values selected by a caller-owned packed admission mask.
 * The mask is relative to {@code from}; bit {@code 0} selects {@code source[from]}.
 * The source/mask must remain stable during a call. No per-element boxes, pages,
 * iterator objects or retained cache state are created.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/PackedLongMaskPage.java}
 * blob {@code 0528d72db36dde5761da9e9a4ffc566cb6b49f98} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (common collections batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class PackedLongMaskPage {
    private PackedLongMaskPage() { }

    /**
     * Skips {@code skip} selected values and copies at most {@code limit} later values.
     * All argument, mask and destination-capacity checks precede any output write.
     * Source/output alias is supported only when writing at or before {@code from};
     * mask alias with either data array is refused. Out-of-range tail bits are ignored.
     *
     * @return the count copied into {@code output} starting at {@code outputFrom}
     */
    public static int copyPage(long[] source, int from, int to, long[] decisions,
            int skip, int limit, long[] output, int outputFrom) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(output, "output");
        Objects.requireNonNull(decisions, "decisions");
        Objects.checkFromToIndex(from, to, source.length);
        Objects.checkFromIndexSize(outputFrom, 0, output.length);
        if (skip < 0 || limit < 0) {
            throw new IndexOutOfBoundsException("negative skip or limit");
        }
        if (source == decisions || output == decisions
                || (source == output && outputFrom > from)) {
            throw new IllegalArgumentException("unsafe alias");
        }
        int range = to - from;
        int accepted = PackedAdmissionMask.count(decisions, range);
        int needed = skip >= accepted ? 0 : Math.min(limit, accepted - skip);
        Objects.checkFromIndexSize(outputFrom, needed, output.length);
        if (needed == 0) {
            return 0;
        }
        return copySelected(source, from, range, decisions, skip, needed, output, outputFrom);
    }

    private static int copySelected(long[] source, int from, int range, long[] decisions,
            int skip, int needed, long[] output, int outputFrom) {
        int remainingSkip = skip;
        int written = 0;
        int words = PackedAdmissionMask.wordsRequired(range);
        for (int wordIndex = 0; wordIndex < words && written < needed; wordIndex++) {
            long bits = decisions[wordIndex];
            int remainingBits = range - (wordIndex << 6);
            if (remainingBits < 64) {
                bits &= -1L >>> (64 - remainingBits);
            }
            int inWord = Long.bitCount(bits);
            if (remainingSkip >= inWord) {
                remainingSkip -= inWord;
                continue;
            }
            while (remainingSkip > 0) {
                bits &= bits - 1L;
                remainingSkip--;
            }
            while (bits != 0L && written < needed) {
                int bit = (wordIndex << 6) + Long.numberOfTrailingZeros(bits);
                output[outputFrom + written++] = source[from + bit];
                bits &= bits - 1L;
            }
        }
        return written;
    }
}
