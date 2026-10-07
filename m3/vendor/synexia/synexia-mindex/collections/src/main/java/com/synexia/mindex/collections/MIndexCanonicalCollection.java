// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.collections;

/** Common contract for first-class immutable canonical collection values. */
public interface MIndexCanonicalCollection {
    MIndexCompositeIndex compositeIndex();

    int canonicalId();

    int kind();

    int size();

    long structuralHash64();

    long signal64();

    /** Prepares or reuses role-specific indexes without changing canonical identity. */
    default MIndexCollectionMetadata precompute(MIndexCollectionPrecompute owner) {
        return java.util.Objects.requireNonNull(owner, "owner").prepare(this);
    }
}
