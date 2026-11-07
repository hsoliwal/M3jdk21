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
package jdk.internal.m3.collections;

import java.util.Objects;
import java.util.function.LongPredicate;

/**
 * Explicit frozen primitive projection of a sorted long lane, not a live collection view.
 * Reuses the canonical packed decision-mask evaluator, Java stable compaction and optional
 * host-loaded JNI peer. No Java predicate is called by native code; it runs exactly once per
 * source element in ascending source order before output allocation. The caller must not
 * mutate the input while evaluating, and JNI never retains array pointers or Java objects.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/M3FilteredLongSnapshots.java}
 * blob {@code 429bab8cc4244046dc901b5983d93c3bc2c1941e} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (common collections batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3FilteredLongSnapshots {
    public enum Backend { JAVA, JNI_REQUIRED }
    private M3FilteredLongSnapshots() { }

    public static long[] filterSorted(long[] sorted, LongPredicate predicate) {
        return filterSorted(sorted, predicate, Backend.JAVA);
    }

    /** JNI_REQUIRED fails if the host has not explicitly loaded the JNI library. */
    public static long[] filterSorted(long[] sorted, LongPredicate predicate, Backend backend) {
        Objects.requireNonNull(sorted, "sorted");
        Objects.requireNonNull(predicate, "predicate");
        Objects.requireNonNull(backend, "backend");
        for (int i = 1; i < sorted.length; i++) {
            if (sorted[i - 1] > sorted[i]) {
                throw new IllegalArgumentException("long input must be in signed ascending order");
            }
        }
        long[] decisions = new long[M3PackedAdmissionMask.wordsRequired(sorted.length)];
        int count = M3PackedAdmissionMask.evaluate(0, sorted.length,
                index -> predicate.test(sorted[index]), decisions);
        long[] selected = new long[count];
        int written = backend == Backend.JNI_REQUIRED
                ? M3PackedSelectionNative.compact(sorted, 0, sorted.length, decisions, selected, 0)
                : M3PackedPrimitiveStableFilter.compact(sorted, 0, sorted.length, decisions, selected, 0);
        if (written != count) throw new IllegalStateException("mask/compaction parity failure");
        return selected;
    }
}
