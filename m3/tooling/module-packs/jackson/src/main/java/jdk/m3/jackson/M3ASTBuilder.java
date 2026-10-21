// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Adapted from com.synexia.mindex.MIndexASTBuilder, Synexia revision
// 50f48cbbe11f17fb8dc209bac3812f8940a1e267. Receiving changes: package-private
// JSON construction boundary, existing String owners, budgets and one-time sealing.
package jdk.m3.jackson;

import java.util.Objects;

/** Bottom-up construction used only by the Jackson token projection in this package. */
final class M3ASTBuilder {
    private static final int[] NO_CHILDREN = new int[0];

    private final M3ASTPool pool;
    // The builder is single-thread owned. The pool copies child coordinates immediately.
    private final int[] oneChild = new int[1];

    M3ASTBuilder(M3JsonLimits limits) {
        pool = new M3ASTPool(Objects.requireNonNull(limits, "limits"));
    }

    int leaf(String kind, String name, String value) {
        return atom(kind, name, value, NO_CHILDREN);
    }

    /** Avoid one temporary int[1] allocation per JSON field occurrence. */
    int field(String name, int valueAtomIndex) {
        oneChild[0] = valueAtomIndex;
        return pool.intern("FIELD", name, null, oneChild);
    }

    /** Child coordinates must already exist in this builder's document. */
    int atom(String kind, String name, String value, int[] childAtomIndexes) {
        return pool.intern(kind, name, value, childAtomIndexes);
    }

    /** Reuse a depth-local child array without copying its unused capacity. */
    int atom(String kind, String name, String value, int[] children, int childCount) {
        return pool.intern(kind, name, value, children, childCount);
    }

    /** Seals the only construction owner. A second build or any later write is refused. */
    M3AST build(int rootAtomIndex) {
        pool.seal(rootAtomIndex);
        return new M3AST(pool, rootAtomIndex);
    }
}
