// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.collections;

import java.util.Objects;

/**
 * Stable recursive MIndex domain over canonical collection coordinates.
 *
 * <p>One instance is owned by each {@link MIndexCompositeIndex}. It allows a
 * parent primitive collection lane to contain child collection IDs directly.
 */
public final class MIndexCompositeSpace implements MIndexSpace<MIndexCompositeRef> {
    private final MIndexCompositeIndex index;

    MIndexCompositeSpace(MIndexCompositeIndex index) {
        this.index = Objects.requireNonNull(index, "index");
    }

    public MIndexCompositeIndex compositeIndex() {
        return index;
    }

    @Override
    public int id(MIndexCompositeRef value) {
        MIndexCompositeRef actual = Objects.requireNonNull(value, "value");
        if (actual.compositeIndex() != index) {
            throw new IllegalArgumentException(
                    "composite reference belongs to a different MIndexCompositeIndex");
        }
        int id = actual.canonicalId();
        requireId(id);
        return id;
    }

    public int id(MIndexCanonicalCollection value) {
        MIndexCanonicalCollection actual = Objects.requireNonNull(value, "value");
        if (actual.compositeIndex() != index) {
            throw new IllegalArgumentException(
                    "canonical collection belongs to a different MIndexCompositeIndex");
        }
        int id = actual.canonicalId();
        requireId(id);
        return id;
    }

    @Override
    public int findId(MIndexCompositeRef value) {
        if (value == null
                || value.compositeIndex() != index
                || !index.containsId(value.canonicalId())) {
            return -1;
        }
        return value.canonicalId();
    }

    public int findId(MIndexCanonicalCollection value) {
        if (value == null
                || value.compositeIndex() != index
                || !index.containsId(value.canonicalId())) {
            return -1;
        }
        return value.canonicalId();
    }

    @Override
    public MIndexCompositeRef value(int id) {
        requireId(id);
        return new MIndexCompositeRef(index, id);
    }

    public MIndexCompositeRef ref(MIndexCanonicalCollection value) {
        return this.value(id(value));
    }

    @Override
    public int size() {
        return index.size();
    }

    @Override
    public boolean containsId(int id) {
        return index.containsId(id);
    }
}
