// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.collections;

/**
 * A domain that assigns non-negative primitive IDs to values and reverses them.
 *
 * <p>Collections in this module store only IDs. The space owns any canonical
 * object references needed to project an ID back to the public value type.
 */
public interface MIndexSpace<E> {
    /** Admit or return the existing ID for {@code value}. */
    int id(E value);

    /** Return the existing ID without admitting a new value, or {@code -1}. */
    int findId(E value);

    /** Project one admitted ID back to its value. */
    E value(int id);

    /** Number of IDs admitted to this space. */
    int size();

    /**
     * Whether ID equivalence and projected values preserve Java equals and hashCode.
     *
     * <p>Only a space that guarantees this relation for all current and future
     * values may opt in. The default rejects java.util views for domains whose
     * structural or reference identity can differ from Java equality.
     */
    default boolean javaEqualityCompatible() {
        return false;
    }

    /**
     * Object whose reference identity defines this domain for canonical
     * composite identity.
     *
     * <p>Normal spaces are their own authority. Adapters over another exact
     * identity domain may return that underlying domain so multiple wrappers do
     * not create false independent canonical universes.
     */
    default Object identityAuthority() {
        return this;
    }

    default boolean contains(E value) {
        return findId(value) >= 0;
    }

    default boolean containsId(int id) {
        return id >= 0 && id < size();
    }

    default void requireId(int id) {
        if (!containsId(id)) {
            throw new IndexOutOfBoundsException("MIndex id outside space: " + id);
        }
    }
}
