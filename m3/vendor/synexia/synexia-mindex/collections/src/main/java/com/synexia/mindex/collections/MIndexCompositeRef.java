// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.collections;

import java.util.Objects;

/**
 * Lightweight public projection of one canonical composite coordinate.
 *
 * <p>Equality is store identity plus canonical integer ID. The resident parent
 * representation stores only the integer ID; this object is materialized at a
 * Java API boundary.
 */
public final class MIndexCompositeRef {
    private final MIndexCompositeIndex index;
    private final int canonicalId;

    MIndexCompositeRef(MIndexCompositeIndex index, int canonicalId) {
        this.index = Objects.requireNonNull(index, "index");
        if (!index.containsId(canonicalId)) {
            throw new IndexOutOfBoundsException("composite id: " + canonicalId);
        }
        this.canonicalId = canonicalId;
    }

    public MIndexCompositeIndex compositeIndex() {
        return index;
    }

    public int canonicalId() {
        return canonicalId;
    }

    public int kind() {
        return index.kind(canonicalId);
    }

    public String kindName() {
        return MIndexCompositeIndex.kindName(kind());
    }

    public int laneLength() {
        return index.length(canonicalId);
    }

    public int valueAt(int position) {
        return index.valueAt(canonicalId, position);
    }

    public int[] copyLane() {
        return index.copyLane(canonicalId);
    }

    public long structuralHash64() {
        return index.structuralHash64(canonicalId);
    }

    public long signal64() {
        return index.signal64(canonicalId);
    }

    @Override
    public boolean equals(Object other) {
        return this == other
                || other instanceof MIndexCompositeRef that
                && index == that.index
                && canonicalId == that.canonicalId;
    }

    @Override
    public int hashCode() {
        return 31 * System.identityHashCode(index) + canonicalId;
    }

    @Override
    public String toString() {
        return "MIndexCompositeRef[id=" + canonicalId
                + ",kind=" + kindName() + ']';
    }
}
