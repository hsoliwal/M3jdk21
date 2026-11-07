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

/**
 * Template Method for the nearest qualifying boundary of a fixed indexed source.
 * The hook must implement a stable total preorder, not an arbitrary pair predicate.
 * Source data and comparator behavior must remain unchanged during compute().
 * Each position is pushed once and popped at most once: at most 2*n comparisons.
 * No per-position objects are created by this class; hook allocation is caller-owned.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-algo/src/main/java/jdk/internal/m3/algorithms/shapes/packed/MonotonicBoundaryTemplate.java}
 * blob {@code b9b40b685068bfad7412293c3fd6951cad52a2eb} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (algorithms shapes.packed batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public abstract class M3MonotonicBoundaryTemplate {
    private final int size;

    protected M3MonotonicBoundaryTemplate(int size) {
        if (size < 0) throw new IllegalArgumentException("size");
        this.size = size;
    }

    public final int size() { return size; }

    /** Sign of the source values' order. Must not mutate source, output or scratch. */
    protected abstract int compareAt(int left, int right);

    /** Optional representation-specific preflight, before any output or scratch write. */
    protected void validateBuffers(int[] output, int[] scratch) { }

    /**
     * Writes nearest source indices, or -1 when absent, into output[0,size).
     * output and scratch must be distinct and disjoint from the source representation.
     * Configuration errors make no writes. A hook exception propagates unchanged and
     * may leave partial output and scratch; no transactional rollback is promised.
     * Both arrays are caller-owned and reusable; positions outside [0,size) stay intact.
     */
    public final void compute(M3BoundarySide side, M3BoundaryRelation relation,
            int[] output, int[] scratch) {
        Objects.requireNonNull(side, "side");
        Objects.requireNonNull(relation, "relation");
        Objects.requireNonNull(output, "output");
        Objects.requireNonNull(scratch, "scratch");
        if (output == scratch) throw new IllegalArgumentException("aliased output/scratch");
        if (output.length < size || scratch.length < size) {
            throw new IllegalArgumentException("buffer capacity");
        }
        validateBuffers(output, scratch);

        int step = side == M3BoundarySide.NEXT ? -1 : 1;
        int index = side == M3BoundarySide.NEXT ? size - 1 : 0;
        int count = 0;
        for (int visited = 0; visited < size; visited++, index += step) {
            // Eliminated candidates are dominated for all subsequent positions.
            while (count > 0 && !relation.accepts(compareAt(scratch[count - 1], index))) {
                count--;
            }
            output[index] = count == 0 ? -1 : scratch[count - 1];
            scratch[count++] = index;
        }
    }
}
