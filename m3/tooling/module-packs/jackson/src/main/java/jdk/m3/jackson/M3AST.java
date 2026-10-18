// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Adapted from com.synexia.mindex.MIndexAST, Synexia revision
// 50f48cbbe11f17fb8dc209bac3812f8940a1e267. Receiving changes: sealed document
// lifetime and document counts; no process-global pool or legacy hash-format claim.
package jdk.m3.jackson;

import java.util.Objects;

/**
 * Immutable root handle over one sealed JSON atom document.
 *
 * <p>Atoms share exact kind, name, value and ordered-child coordinates within their document.
 * Independently parsed documents retain distinct identities even when their hashes match.
 * Source positions and compiler attribution are not part of this representation.
 */
public final class M3AST {
    private final M3ASTPool pool;
    private final int rootAtomIndex;

    M3AST(M3ASTPool pool, int rootAtomIndex) {
        this.pool = Objects.requireNonNull(pool, "pool");
        pool.checkAtomIndex(rootAtomIndex);
        this.rootAtomIndex = rootAtomIndex;
    }

    public int rootAtomIndex() {
        return rootAtomIndex;
    }

    public M3ASTAtom root() {
        return pool.atom(rootAtomIndex);
    }

    /** Returns an atom from this document, including atoms outside a subtree root view. */
    public M3ASTAtom atom(int atomIndex) {
        return pool.atom(atomIndex);
    }

    /** Primitive indexed lookup; unlike {@link #atom(int)}, creates no node wrapper. */
    public String kindAt(int atomIndex) { return pool.kind(atomIndex); }

    /** Field name at an indexed atom; null for non-field values. */
    public String nameAt(int atomIndex) { return pool.name(atomIndex); }

    /** Scalar text or original numeric lexeme at an indexed atom. */
    public String valueAt(int atomIndex) { return pool.value(atomIndex); }

    /** Number of child references, including repeated occurrences of shared atoms. */
    public int childCountAt(int atomIndex) { return pool.childCount(atomIndex); }

    /** Ordered child atom coordinate without allocating an M3ASTAtom. */
    public int childAtomIndexAt(int atomIndex, int childIndex) {
        return pool.childAt(atomIndex, childIndex);
    }

    /** Array element coordinate without creating a Java node or iterator. */
    public int arrayElementAt(int arrayAtomIndex, int elementIndex) {
        if (!"ARRAY".equals(kindAt(arrayAtomIndex))) {
            throw new IllegalArgumentException("expected JSON array atom");
        }
        return childAtomIndexAt(arrayAtomIndex, elementIndex);
    }

    /**
     * Find an object's named field occurrence, including duplicates, by zero-based
     * occurrence number. Returns the field's VALUE atom index or -1 if absent.
     * This performs a bounded linear scan of primitive child coordinates and
     * creates no field/entry objects; a secondary field index may be added later.
     */
    public int objectFieldValueAt(int objectAtomIndex, String fieldName, int occurrence) {
        Objects.requireNonNull(fieldName, "fieldName");
        if (occurrence < 0) throw new IllegalArgumentException("negative field occurrence");
        if (!"OBJECT".equals(kindAt(objectAtomIndex))) {
            throw new IllegalArgumentException("expected JSON object atom");
        }
        for (int ordinal = 0, count = childCountAt(objectAtomIndex); ordinal < count; ordinal++) {
            int field = childAtomIndexAt(objectAtomIndex, ordinal);
            if ("FIELD".equals(kindAt(field)) && fieldName.equals(nameAt(field))) {
                if (occurrence-- == 0) return childAtomIndexAt(field, 0);
            }
        }
        return -1;
    }

    /** Structural candidate hash of an indexed atom. */
    public long structuralHash64At(int atomIndex) { return pool.structuralHash64(atomIndex); }

    /**
     * Deterministic structural candidate hash in the receiving UTF-16 domain.
     * Hash equality alone does not establish structure or document identity.
     */
    public long structuralHash64() {
        return pool.structuralHash64(rootAtomIndex);
    }

    /** Number of unique atoms in the complete retained document pool. */
    public int atomCount() {
        return pool.size();
    }

    /** Number of ordered child references in the complete retained document pool. */
    public int childReferenceCount() {
        return pool.childReferenceCount();
    }

    long retainedTextUnits() {
        return pool.retainedTextUnits();
    }

    /** Exact canonical identity: the same document pool and the same root coordinate. */
    public boolean sameStructure(M3AST other) {
        return other != null && pool == other.pool && rootAtomIndex == other.rootAtomIndex;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || (other instanceof M3AST value && sameStructure(value));
    }

    @Override
    public int hashCode() {
        return 31 * System.identityHashCode(pool) + rootAtomIndex;
    }

    @Override
    public String toString() {
        return "M3AST[root=" + rootAtomIndex + ", atoms=" + atomCount()
                + ", hash=" + Long.toUnsignedString(structuralHash64()) + "]";
    }
}
