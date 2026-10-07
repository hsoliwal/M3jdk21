// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.collections;

import com.synexia.mindex.collection.MIndexDomain;
import java.util.Objects;

/**
 * Exact MIndexSpace view over the legacy runtime MIndexDomain contract.
 *
 * <p>No identity translation occurs. The legacy domain remains the admission,
 * resolution and exact-collision authority; this adapter only exposes the
 * canonical-collection MIndexSpace vocabulary.
 *
 * <p>{@link #identityAuthority()} returns the wrapped domain itself, so two
 * adapter instances over the same legacy domain participate in one canonical
 * composite domain rather than creating false independent universes.
 */
public final class MIndexLegacyDomainSpace<E>
        implements MIndexSpace<E> {
    private final MIndexDomain<E> domain;

    MIndexLegacyDomainSpace(
            MIndexDomain<E> domain) {
        this.domain =
                Objects.requireNonNull(domain, "domain");
    }

    public MIndexDomain<E> domain() {
        return domain;
    }

    @Override
    public int id(E value) {
        return domain.intern(
                Objects.requireNonNull(value, "value"));
    }

    @Override
    public int findId(E value) {
        return value == null ? -1 : domain.findId(value);
    }

    @Override
    public E value(int id) {
        if (id < 0 || id >= domain.size()) {
            throw new IndexOutOfBoundsException(
                    "legacy MIndex domain id: " + id);
        }
        return domain.resolve(id);
    }

    @Override
    public int size() {
        return domain.size();
    }

    @Override
    public boolean javaEqualityCompatible() {
        return domain.javaEqualityCompatible();
    }

    @Override
    public Object identityAuthority() {
        return domain;
    }

    @Override
    public String toString() {
        return "MIndexLegacyDomainSpace[" + domain.name() + ']';
    }
}
