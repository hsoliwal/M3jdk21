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
import java.util.function.IntBinaryOperator;

/**
 * Strategy adapter for caller-owned indexed data: object arrays, columns or other views.
 * The supplied comparator takes two indices, not two primitive *values*. One strategy
 * is retained per binding, not per entry. Source size/order must stay stable; output
 * and scratch must not alias data captured by the comparator. Arbitrary closure aliasing
 * and total-preorder laws cannot be inferred or certified by this adapter.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-algo/src/main/java/jdk/internal/m3/algorithms/shapes/packed/IndexedBoundaryTemplate.java}
 * blob {@code 16f798ba9cfc46643a265d1d2e42d4e84f4673f2} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (algorithms shapes.packed batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class IndexedBoundaryTemplate extends MonotonicBoundaryTemplate {
    private final IntBinaryOperator comparison;

    public IndexedBoundaryTemplate(int size, IntBinaryOperator comparison) {
        super(size);
        this.comparison = Objects.requireNonNull(comparison, "comparison");
    }

    @Override protected int compareAt(int left, int right) {
        return comparison.applyAsInt(left, right);
    }
}
