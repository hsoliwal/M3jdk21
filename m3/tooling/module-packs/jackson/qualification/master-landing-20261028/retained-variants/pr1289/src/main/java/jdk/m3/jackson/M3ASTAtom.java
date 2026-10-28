// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Adapted from com.synexia.mindex.MIndexASTAtom, Synexia revision
// 50f48cbbe11f17fb8dc209bac3812f8940a1e267. Receiving changes: existing String
// owners, sealed document lifetime and uncached immutable wrappers.
package jdk.m3.jackson;

import java.util.Objects;

/**
 * Read-only atom handle over a sealed document's primitive coordinate lanes.
 *
 * <p>String access returns the document's retained immutable String reference. No spelling
 * buffer or child array is copied by this handle. Wrapper identity is not canonical identity.
 */
public final class M3ASTAtom {
    private final M3ASTPool pool;
    private final int atomIndex;

    M3ASTAtom(M3ASTPool pool, int atomIndex) {
        this.pool = Objects.requireNonNull(pool, "pool");
        pool.checkAtomIndex(atomIndex);
        this.atomIndex = atomIndex;
    }

    public int atomIndex() {
        return atomIndex;
    }

    public String kind() {
        return pool.kind(atomIndex);
    }

    /** Field name when present; otherwise null. */
    public String name() {
        return pool.name(atomIndex);
    }

    /** Scalar text or original numeric lexeme when present; otherwise null. */
    public String value() {
        return pool.value(atomIndex);
    }

    public int childCount() {
        return pool.childCount(atomIndex);
    }

    public int childAtomIndex(int childIndex) {
        return pool.childAt(atomIndex, childIndex);
    }

    public M3ASTAtom child(int childIndex) {
        return pool.atom(childAtomIndex(childIndex));
    }

    /** Deterministic candidate hash, never an equality oracle. */
    public long structuralHash64() {
        return pool.structuralHash64(atomIndex);
    }

    /** Returns a root view retaining this same sealed document and atom coordinate. */
    public M3AST asAST() {
        return new M3AST(pool, atomIndex);
    }

    @Override
    public boolean equals(Object other) {
        return this == other || (other instanceof M3ASTAtom value
                && pool == value.pool && atomIndex == value.atomIndex);
    }

    @Override
    public int hashCode() {
        return 31 * System.identityHashCode(pool) + atomIndex;
    }

    @Override
    public String toString() {
        return "M3ASTAtom[index=" + atomIndex + ", kind=" + kind()
                + ", children=" + childCount() + "]";
    }
}
