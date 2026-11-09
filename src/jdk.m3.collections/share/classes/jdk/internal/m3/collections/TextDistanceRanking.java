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

import java.util.Objects;
import java.util.function.IntFunction;
import jdk.internal.m3.collections.sketch.StreamingDistanceTopK;

/**
 * Explicit fixed-query UTF-16 edit distance; never invokes arbitrary Object.toString().
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/foss/TextDistanceRanking.java}
 * blob {@code 125af82f56c492b68ef1060aa3fc9abd15b2b12d} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections subpackages batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
public final class TextDistanceRanking {
    private TextDistanceRanking() { }
    /**
     * Returns nearest source ordinals with stable ties. Extracts each admitted input once.
     * The query and each input must be stable during this call. maxChars bounds both;
     * edit-distance work is O(queryLength * candidateLength), not a semantic similarity model.
     * Retained selection is O(K), with two reusable int[queryLength+1] rows. k=0 skips extraction.
     */
    public static long[] nearest(CharSequence query, int size, int k, int maxChars,
            IntFunction<? extends CharSequence> text) {
        Objects.requireNonNull(query, "query"); Objects.requireNonNull(text, "text");
        if (size < 0 || k < 0 || maxChars < 0 || maxChars > Integer.MAX_VALUE - 8)
            throw new IllegalArgumentException("bounds");
        if (query.length() > maxChars) throw new IllegalArgumentException("query too long");
        if (k == 0 || size == 0) return new long[0];
        // Copy only the explicit query to protect it from ordinary mutable CharSequence changes.
        char[] q = new char[query.length()];
        for (int i = 0; i < q.length; i++) q[i] = query.charAt(i);
        int[] previous = new int[q.length + 1], current = new int[q.length + 1];
        StreamingDistanceTopK selected = new StreamingDistanceTopK(Math.min(size, k));
        for (int ordinal = 0; ordinal < size; ordinal++) {
            CharSequence candidate = Objects.requireNonNull(text.apply(ordinal), "candidate");
            int length = candidate.length();
            if (length > maxChars) throw new IllegalArgumentException("candidate too long");
            // |length difference| is an exact lower bound on unit-cost edit distance.
            if (selected.size() == selected.limit()
                    && Math.abs((long) length - q.length) >= selected.threshold()) {
                // Equal-bound candidates have later ordinals and cannot improve a full selection.
                continue;
            }
            for (int j = 0; j <= q.length; j++) previous[j] = j;
            for (int i = 0; i < length; i++) {
                current[0] = i + 1;
                for (int j = 1; j <= q.length; j++) {
                    int replace = previous[j - 1] + (candidate.charAt(i) == q[j - 1] ? 0 : 1);
                    current[j] = Math.min(replace, Math.min(previous[j], current[j - 1]) + 1);
                }
                int[] swap = previous; previous = current; current = swap;
            }
            selected.offer(ordinal, previous[q.length]);
        }
        return selected.toSortedOrdinals();
    }
}
