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
package jdk.internal.m3.collections.packed;

import java.util.Arrays;

/**
 * Dense directed semantic relation matrix stored in one flattened primitive {@code long[]}.
 *
 * <p>For {@code N} facts, each source fact owns a row containing {@code N} destination
 * bits. The row width is {@code ceil(N / 64)} longs and the flattened storage size is
 * {@code N * ceil(N / 64)} longs. No node, edge, entry or adjacency-list object is
 * retained per relation.
 *
 * <p>For exactly 64 facts a complete directed relation matrix occupies 64 longs,
 * or 512 primitive payload bytes. The representation is therefore appropriate for
 * dense/local semantic neighborhoods. Sparse/global universes should use a compressed
 * boundary representation and project the active neighborhood into this dense form.
 *
 * <p>This class is mutable during semantic-program construction. It is not thread-safe.
 * A compiled {@link PackedSemanticMachine} freezes its relation matrices by convention.
 *
 * @since 1.0
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/semantic/PackedRelationMatrix.java}
 * blob {@code 1d07f08dc3d9e9f566cb1ec709bf6a89a2770358} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (common non-language batch b28 (canonical, utils, semantic, convergence, io, extracted, validation, relationbits) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class PackedRelationMatrix {
    private final int factCount;
    private final int wordsPerRow;
    private final long[] rows;

    /**
     * Creates an empty {@code factCount x factCount} directed relation matrix.
     *
     * @param factCount number of semantic facts represented by both axes
     * @throws IllegalArgumentException if {@code factCount <= 0} or storage size overflows
     */
    public PackedRelationMatrix(int factCount) {
        if (factCount <= 0) {
            throw new IllegalArgumentException("factCount");
        }
        this.factCount = factCount;
        this.wordsPerRow = PackedSemanticMasks.wordCount(factCount);
        this.rows = new long[Math.multiplyExact(factCount, wordsPerRow)];
    }

    /**
     * Returns the semantic fact count represented by each matrix axis.
     *
     * @return fact count
     */
    public int factCount() {
        return factCount;
    }

    /**
     * Returns the number of primitive words in one relation row.
     *
     * @return {@code ceil(factCount / 64)}
     */
    public int wordsPerRow() {
        return wordsPerRow;
    }

    /**
     * Returns the number of retained primitive long slots.
     *
     * @return flattened matrix length in longs
     */
    public int payloadLongs() {
        return rows.length;
    }

    /**
     * Returns primitive payload bytes retained by the matrix.
     *
     * <p>Java object and array headers are intentionally excluded.
     *
     * @return {@code payloadLongs() * 8}
     */
    public long payloadBytes() {
        return (long) rows.length * Long.BYTES;
    }

    /**
     * Adds a directed relation {@code from -> to}.
     *
     * <p>The operation is idempotent and runs in constant time.
     *
     * @param from source fact index
     * @param to destination fact index
     * @throws IndexOutOfBoundsException if either fact index is outside the matrix
     */
    public void relate(int from, int to) {
        checkFact(from);
        checkFact(to);
        rows[rowOffset(from) + (to >>> 6)] |= 1L << (to & 63);
    }

    /**
     * Removes the directed relation {@code from -> to}.
     *
     * @param from source fact index
     * @param to destination fact index
     * @throws IndexOutOfBoundsException if either fact index is outside the matrix
     */
    public void unrelate(int from, int to) {
        checkFact(from);
        checkFact(to);
        rows[rowOffset(from) + (to >>> 6)] &= ~(1L << (to & 63));
    }

    /**
     * Adds both {@code left -> right} and {@code right -> left}.
     *
     * @param left first fact
     * @param right second fact
     */
    public void relateMutual(int left, int right) {
        relate(left, right);
        relate(right, left);
    }

    /**
     * Tests a directed relation.
     *
     * @param from source fact index
     * @param to destination fact index
     * @return true when the relation bit is set
     * @throws IndexOutOfBoundsException if either fact index is outside the matrix
     */
    public boolean relates(int from, int to) {
        checkFact(from);
        checkFact(to);
        return (rows[rowOffset(from) + (to >>> 6)] & (1L << (to & 63))) != 0L;
    }

    /**
     * Clears all outgoing relations for one source fact.
     *
     * @param from source fact index
     */
    public void clearRow(int from) {
        checkFact(from);
        Arrays.fill(rows, rowOffset(from), rowOffset(from) + wordsPerRow, 0L);
    }

    /**
     * Clears the complete relation matrix.
     */
    public void clearAll() {
        Arrays.fill(rows, 0L);
    }

    /**
     * ORs one source row into an existing semantic mask.
     *
     * <p>This is the primary runtime operation used for compiled implication/exclusion
     * propagation. No allocation occurs.
     *
     * @param from source fact whose relation row is consumed
     * @param target destination mask to mutate
     * @throws IllegalArgumentException if {@code target} has the wrong word length
     */
    public void orRowInto(int from, long[] target) {
        checkFact(from);
        requireWords(target);
        int offset = rowOffset(from);
        for (int word = 0; word < wordsPerRow; word++) {
            target[word] |= rows[offset + word];
        }
    }

    /**
     * Removes every bit present in one relation row from an existing target mask.
     *
     * @param from source fact whose row is consumed
     * @param target destination mask to mutate
     * @throws IllegalArgumentException if {@code target} has the wrong word length
     */
    public void andNotRowInto(int from, long[] target) {
        checkFact(from);
        requireWords(target);
        int offset = rowOffset(from);
        for (int word = 0; word < wordsPerRow; word++) {
            target[word] &= ~rows[offset + word];
        }
    }

    /**
     * Copies one source row into a caller-provided mask.
     *
     * @param from source fact
     * @param target destination array with exactly {@link #wordsPerRow()} words
     * @throws IllegalArgumentException if the target length is wrong
     */
    public void copyRowInto(int from, long[] target) {
        checkFact(from);
        requireWords(target);
        System.arraycopy(rows, rowOffset(from), target, 0, wordsPerRow);
    }

    /**
     * Computes the directed transitive closure in place using bitset Warshall propagation.
     *
     * <p>After closure, if {@code A -> B} and {@code B -> C}, the row for A also contains C.
     * This moves path traversal from the runtime hot path into the compilation/configuration
     * phase. For each reachable intermediate fact, a complete primitive row is ORed at once.
     *
     * <p>The method is deterministic and idempotent. It does not add reflexive relations
     * unless a cycle in the original graph makes a fact reachable from itself.
     */
    public void transitiveClosureInPlace() {
        for (int via = 0; via < factCount; via++) {
            int viaWord = via >>> 6;
            long viaMask = 1L << (via & 63);
            int viaOffset = rowOffset(via);
            for (int from = 0; from < factCount; from++) {
                int fromOffset = rowOffset(from);
                if ((rows[fromOffset + viaWord] & viaMask) == 0L) {
                    continue;
                }
                for (int word = 0; word < wordsPerRow; word++) {
                    rows[fromOffset + word] |= rows[viaOffset + word];
                }
            }
        }
        clearUnusedBitsInEveryRow();
    }

    /**
     * Materializes a boundary copy of the flattened primitive matrix.
     *
     * @return newly allocated copy in row-major order
     */
    public long[] wordsCopy() {
        return Arrays.copyOf(rows, rows.length);
    }

    int rowOffset(int fact) {
        return fact * wordsPerRow;
    }

    long wordAt(int fact, int word) {
        return rows[rowOffset(fact) + word];
    }

    private void clearUnusedBitsInEveryRow() {
        int remainder = factCount & 63;
        if (remainder == 0) {
            return;
        }
        long mask = (1L << remainder) - 1L;
        for (int row = 0; row < factCount; row++) {
            rows[rowOffset(row) + wordsPerRow - 1] &= mask;
        }
    }

    private void requireWords(long[] words) {
        if (words.length != wordsPerRow) {
            throw new IllegalArgumentException(
                    "word-length mismatch: " + words.length + " != " + wordsPerRow);
        }
    }

    private void checkFact(int fact) {
        if (fact < 0 || fact >= factCount) {
            throw new IndexOutOfBoundsException("fact=" + fact + ", factCount=" + factCount);
        }
    }
}
