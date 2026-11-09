/* ******************************************************************************
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
package jdk.internal.m3.collections.graph;

import jdk.internal.m3.collections.PlaceholderLogger;

import jdk.internal.m3.collections.ConvergenceChunk;
import jdk.internal.m3.collections.ConvergenceDiff;
import jdk.internal.m3.collections.DiffType;

import java.util.Map;
/**
 * Convergence Matcher - CPU-based chunk matching + diff.
 *
 * <p>
 * From ChatGPT: "Memory here is not what survived storage — it is what survived
 * comparison."
 * </p>
 *
 * <h2>SPECIFICATION</h2>
 *
 * <h3>Similarity Algorithm</h3>
 * <p>
 * Jaccard similarity on signal sets:
 * </p>
 *
 * <pre>{@code
 * similarity(A, B) = |A ∩ B| / max(|A|, |B|)
 *
 * where A = signals(chunk_a), B = signals(chunk_b)
 * }</pre>
 *
 * <h3>Diff Classification</h3>
 *
 * <pre>{@code
 * match = best_match(incoming, memory)
 *
 * if match.existing == null       → NEW
 * if match.similarity > 0.85      → UNCHANGED
 * else                            → MODIFIED
 *
 * chunks in memory not seen this pass → VANISHED (handled by caller)
 * }</pre>
 *
 * @author Hitesh Soliwal (hsoliwal@gmail.com)
 * @see ConvergenceChunk
 * @see DiffType
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/convergence/ConvergenceMatcher.java}
 * blob {@code e89d8b15b5503c13a2712d274fe1003c12f7ba7f} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (common batch b29 (slf4j owners on the PlaceholderLogger facade) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public class ConvergenceMatcher {
    private static final PlaceholderLogger log = PlaceholderLogger.of(ConvergenceMatcher.class);

    /** Creates an instance (explicit form of the default constructor; required by -Xlint:all -Werror). */
    public ConvergenceMatcher() {
    }


    /** Similarity threshold for unchanged classification. */
    private static final double UNCHANGED_THRESHOLD = 0.85;

    /**
     * Match result - best matching chunk with similarity score.
     *
     * @param existing
     *            the best matching chunk (null if none)
     * @param similarity
     *            similarity score [0.0, 1.0]
      * @since 1.0
     */
    public record MatchResult(ConvergenceChunk existing, double similarity) {

        /**
         * Creates a no-match result.
         *
         * @return result with null existing and 0.0 similarity
         */
        public static MatchResult none() {
            return new MatchResult(null, 0.0);
        }

        /**
         * Checks if a match was found.
         *
         * @return true if existing != null
         */
        public boolean hasMatch() {
            return existing != null;
        }
    }

    /**
     * Finds the best matching chunk in memory.
     *
     * <p>
     * <b>CPU-ONLY:</b> O(n) scan with Jaccard similarity.
     * </p>
     *
     * @param incoming
     *            the incoming chunk to match
     * @param memory
     *            current memory state
     * @return match result (existing may be : null)
     */
    public MatchResult match(ConvergenceChunk incoming, Map<Long, ConvergenceChunk> memory) {
        if (incoming == null || memory == null || memory.isEmpty()) {
            return MatchResult.none();
        }

        return memory.values().stream()
                .map(existing -> new MatchResult(existing, similarity(incoming, existing)))
                .max((a, b) -> Double.compare(a.similarity(), b.similarity()))
                .filter(result -> result.similarity() > 0.0)
                .orElse(MatchResult.none());
    }

    /**
     * Computes Jaccard similarity between two chunks.
     *
     * <p>
     * Formula: |A ∩ B| / max(|A|, |B|)
     * </p>
     *
     * @param a
     *            first chunk
     * @param b
     *            second chunk
     * @return similarity in range [0.0, 1.0]
     */
    public double similarity(ConvergenceChunk a, ConvergenceChunk b) {
        if (a == null || b == null) {
            return 0.0;
        }

        var signalsA = a.signals();
        var signalsB = b.signals();

        if (signalsA.isEmpty() || signalsB.isEmpty()) {
            return 0.0;
        }

        long common = signalsA.stream().filter(signalsB::contains).count();

        int maxSize = Math.max(signalsA.size(), signalsB.size());
        return maxSize > 0 ? (double) common / maxSize : 0.0;
    }

    /**
     * Classifies the difference between incoming and matched chunk.
     *
     * @param incoming
     *            the incoming chunk
     * @param match
     *            the match result
     * @return diff classification
     */
    public ConvergenceDiff diff(ConvergenceChunk incoming, MatchResult match) {
        if (!match.hasMatch()) {
            return new ConvergenceDiff(DiffType.NEW, incoming, null);
        }

        if (match.similarity() > UNCHANGED_THRESHOLD) {
            return new ConvergenceDiff(DiffType.UNCHANGED, incoming, match.existing());
        }

        return new ConvergenceDiff(DiffType.MODIFIED, incoming, match.existing());
    }
}
